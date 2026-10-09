param([string]$FixtureSteam, [string]$FixtureXbox, [switch]$Anteprime)
$ErrorActionPreference = 'Stop'
$taskRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Set-Location -LiteralPath $taskRoot
if ([bool]$FixtureSteam -ne [bool]$FixtureXbox) { throw 'Indica entrambe le fixture, oppure nessuna.' }
$taskJava = Join-Path $taskRoot 'jre\bin\java.exe'
if (-not (Test-Path -LiteralPath $taskJava)) { $taskJava = (Get-Command java -ErrorAction Stop).Source }
$taskTest = Join-Path $taskRoot ('build\verify-' + [guid]::NewGuid().ToString('N'))
[IO.Directory]::CreateDirectory($taskTest) | Out-Null
$sources = @(Get-ChildItem -LiteralPath 'tests' -Recurse -Filter '*.java' | ForEach-Object { '"' + $_.FullName + '"' })
$sourceList = Join-Path $taskTest 'sources.txt'
[IO.File]::WriteAllLines($sourceList,$sources,(New-Object Text.UTF8Encoding($false)))
$testProgram = Join-Path $taskTest 'programma.jar'
[IO.File]::Copy((Join-Path $taskRoot 'NMSITALIA-CorvetteHUB.jar'),$testProgram)
$taskClasspath = "$testProgram;lib/nms-parser.jar;lib/flatlaf.jar;lib/nms-icons.jar"
& $taskJava -jar tools/ecj.jar -encoding UTF-8 -source 1.8 -target 1.8 -warn:none -cp $taskClasspath -d $taskTest ('@' + $sourceList)
if ($LASTEXITCODE -ne 0) { throw 'Compilazione prove fallita.' }
$taskClasspath = "$taskTest;$taskClasspath"
$fixtureArgs = @()
if ($FixtureSteam) { $fixtureArgs = @($FixtureSteam,$FixtureXbox) }
& $taskJava -Xmx2g '-Djava.awt.headless=true' -cp $taskClasspath it.nmsitalia.corvettehub.safety.Regressioni @fixtureArgs *> (Join-Path $taskTest 'regressioni.log')
$testExit = $LASTEXITCODE
Get-Content -LiteralPath (Join-Path $taskTest 'regressioni.log') -Tail 16
if ($testExit -ne 0) { throw "Regressioni fallite. Dettagli in $taskTest\regressioni.log" }
if ($Anteprime) {
    foreach ($scale in @('1','1.75','2')) {
        & $taskJava -Xmx2g '-Djava.awt.headless=true' -cp $taskClasspath it.nmsitalia.corvettehub.ui.AnteprimePremium $scale
        if ($LASTEXITCODE -ne 0) { throw "Verifica UI fallita alla scala $scale" }
    }
}
Write-Output "Verifiche concluse. Log: $taskTest"
