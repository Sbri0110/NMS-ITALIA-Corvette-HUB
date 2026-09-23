@echo off
rem ============================================================
rem  Crea un collegamento sul Desktop con il logo del programma.
rem
rem  Serve solo per l'aspetto: senza questo, il collegamento
rem  mostrerebbe l'icona generica di Windows.
rem  Il collegamento punta al file .bat nella cartella in cui ti
rem  trovi ora, quindi se sposti la cartella rifai il collegamento.
rem ============================================================

setlocal
title NMS ITALIA Corvette HUB - collegamento

set "QUI=%~dp0"
set "ICONA=%QUI%res\corvette-hub.ico"
set "DEST=%USERPROFILE%\Desktop\NMS ITALIA Corvette HUB.lnk"

echo.
echo  Creo il collegamento sul Desktop...
echo.

if not exist "%QUI%CorvetteHUB.bat" goto :mancaProgramma
if not exist "%ICONA%" goto :mancalcona

powershell -NoProfile -ExecutionPolicy Bypass -Command "$s = (New-Object -ComObject WScript.Shell).CreateShortcut('%DEST%'); $s.TargetPath = '%QUI%CorvetteHUB.bat'; $s.WorkingDirectory = '%QUI%'; $s.IconLocation = '%ICONA%'; $s.Description = 'NMS ITALIA Corvette HUB'; $s.Save()"

if exist "%DEST%" goto :fatto

echo  Non sono riuscito a creare il collegamento.
echo.
echo  Puoi farlo a mano: tasto destro su CorvetteHUB.bat,
echo  Invia a, Desktop crea collegamento. Poi tasto destro sul
echo  collegamento, Proprieta, Cambia icona, e scegli
echo  res\corvette-hub.ico
echo.
pause
exit /b 1

:fatto
echo  Fatto. Sul Desktop ora c'e'
echo     "NMS ITALIA Corvette HUB"
echo  con il logo al posto dell icona generica.
echo.
echo  Nota: se in futuro sposti questa cartella, rilancia questo
echo  file per rifare il collegamento.
echo.
pause
exit /b 0

:mancaProgramma
echo  Non trovo CorvetteHUB.bat in questa cartella.
echo  Lancia questo file dalla cartella del programma.
echo.
pause
exit /b 1

:mancalcona
echo  Non trovo l icona:  res\corvette-hub.ico
echo.
echo  Controlla che la cartella res sia stata scompattata
echo  insieme al resto.
echo.
pause
exit /b 1
