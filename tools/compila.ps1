param([switch]$Pacchetto)
$ErrorActionPreference = 'Stop'
$taskRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Set-Location -LiteralPath $taskRoot
$taskJava = Join-Path $taskRoot 'jre\bin\java.exe'
if (-not (Test-Path -LiteralPath $taskJava)) { $taskJava = (Get-Command java -ErrorAction Stop).Source }
$versionSource = Get-Content -LiteralPath 'src\it\nmsitalia\corvettehub\Main.java' -Raw
$version = [regex]::Match($versionSource, 'VERSIONE = "([^"]+)"').Groups[1].Value
if (-not $version) { throw 'Versione non trovata.' }
$taskBuild = Join-Path $taskRoot ('build\compile-' + [guid]::NewGuid().ToString('N'))
$taskClasses = Join-Path $taskBuild 'classes'
[IO.Directory]::CreateDirectory($taskClasses) | Out-Null
$sources = @(Get-ChildItem -LiteralPath 'src','src-stub' -Recurse -Filter '*.java' | Sort-Object FullName | ForEach-Object { '"' + $_.FullName + '"' })
$sourceList = Join-Path $taskBuild 'sources.txt'
[IO.File]::WriteAllLines($sourceList, $sources, (New-Object Text.UTF8Encoding($false)))
& $taskJava -jar tools/ecj.jar -encoding UTF-8 -source 1.8 -target 1.8 -warn:none -cp 'lib/nms-parser.jar;lib/flatlaf.jar' -d $taskClasses ('@' + $sourceList)
if ($LASTEXITCODE -ne 0) { throw 'Compilazione fallita. Il programma precedente resta disponibile.' }
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem
function Add-ArchiveFile($archive, [string]$path, [string]$name) {
    $entry = $archive.CreateEntry($name.Replace('\','/'), [IO.Compression.CompressionLevel]::Optimal)
    $entry.LastWriteTime = [DateTimeOffset]::new(2000,1,1,0,0,0,[TimeSpan]::Zero)
    $inputStream = [IO.File]::OpenRead($path)
    $outputStream = $entry.Open()
    try { $inputStream.CopyTo($outputStream) } finally { $inputStream.Dispose(); $outputStream.Dispose() }
}
$manifest = "Manifest-Version: 1.0`r`nImplementation-Title: NMS ITALIA Corvette HUB`r`nImplementation-Version: $version`r`nClass-Path: lib/nms-parser.jar lib/flatlaf.jar lib/nms-icons.jar`r`nMain-Class: it.nmsitalia.corvettehub.Main`r`n`r`n"
[IO.File]::WriteAllText((Join-Path $taskRoot 'build\MANIFEST.MF'), $manifest, (New-Object Text.UTF8Encoding($false)))
$taskJar = Join-Path $taskBuild 'NMSITALIA-CorvetteHUB.jar'
$archive = [IO.Compression.ZipFile]::Open($taskJar, [IO.Compression.ZipArchiveMode]::Create)
try {
    Add-ArchiveFile $archive (Join-Path $taskRoot 'build\MANIFEST.MF') 'META-INF/MANIFEST.MF'
    foreach ($f in Get-ChildItem -LiteralPath $taskClasses -Recurse -File -Filter '*.class' | Sort-Object FullName) {
        Add-ArchiveFile $archive $f.FullName $f.FullName.Substring($taskClasses.Length + 1)
    }
    foreach ($f in Get-ChildItem -LiteralPath 'res' -Recurse -File | Sort-Object FullName) {
        Add-ArchiveFile $archive $f.FullName $f.FullName.Substring($taskRoot.Length + 1)
    }
} finally { $archive.Dispose() }
$destination = Join-Path $taskRoot 'NMSITALIA-CorvetteHUB.jar'
if (Test-Path -LiteralPath $destination) {
    [IO.File]::Replace($taskJar, $destination, (Join-Path $taskBuild 'programma-precedente.jar'))
} else { [IO.File]::Move($taskJar, $destination) }
Write-Output "Compilato Corvette HUB $version. Classi: $taskClasses"
if ($Pacchetto) {
    if (-not (Test-Path -LiteralPath 'jre\bin\java.exe')) { throw 'Per il pacchetto portabile serve il runtime nella cartella jre.' }
    [IO.Directory]::CreateDirectory((Join-Path $taskRoot 'dist')) | Out-Null
    $taskZip = Join-Path $taskBuild 'pacchetto.zip'
    $archive = [IO.Compression.ZipFile]::Open($taskZip, [IO.Compression.ZipArchiveMode]::Create)
    try {
        foreach ($path in @('NMSITALIA-CorvetteHUB.jar','CorvetteHUB.bat','Crea collegamento sul Desktop.bat','LICENSE','NOTICE','README.md','docs\GUIDA-UTENTE.md','docs\NOTE-TECNICHE.md','docs\REVISIONE-1.2.0.md','docs\REVISIONE-1.2.1.md')) {
            Add-ArchiveFile $archive (Join-Path $taskRoot $path) $path
        }
        foreach ($path in @('lib','jre','res','docs\img\premium')) {
            foreach ($f in Get-ChildItem -LiteralPath $path -Recurse -File | Sort-Object FullName) {
                Add-ArchiveFile $archive $f.FullName $f.FullName.Substring($taskRoot.Length + 1)
            }
        }
        Add-ArchiveFile $archive (Join-Path $taskRoot 'docs\img\logo.png') 'docs/img/logo.png'
    } finally { $archive.Dispose() }
    $package = Join-Path $taskRoot "dist\NMSITALIA-CorvetteHUB-$version-Windows.zip"
    if (Test-Path -LiteralPath $package) { [IO.File]::Replace($taskZip,$package,(Join-Path $taskBuild 'pacchetto-precedente.zip')) }
    else { [IO.File]::Move($taskZip,$package) }
    $hash = (Get-FileHash -LiteralPath $package -Algorithm SHA256).Hash.ToLowerInvariant()
    [IO.File]::WriteAllText($package + '.sha256', "$hash  $([IO.Path]::GetFileName($package))`r`n", (New-Object Text.UTF8Encoding($false)))
    Write-Output "Pacchetto: $package"
    Write-Output "SHA-256: $hash"
}
