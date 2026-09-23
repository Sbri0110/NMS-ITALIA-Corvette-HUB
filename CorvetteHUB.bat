@echo off
rem ============================================================
rem  NMS ITALIA Corvette HUB
rem
rem  Avvio portabile: nessuna installazione, nessuna scrittura nel
rem  registro di sistema, nessun file fuori da questa cartella,
rem  tranne CorvetteHUB.conf, Builds, Backup e i log.
rem  Il tool non contatta mai Internet.
rem ============================================================

setlocal
title NMS ITALIA Corvette HUB

set "LOG=%~dp0CorvetteHUB-avvio.log"
set "CONSOLE=%~dp0CorvetteHUB-console.log"
echo. >> "%LOG%"
echo ==== avvio %DATE% %TIME% ==== >> "%LOG%"

if not exist "%~dp0NMSITALIA-CorvetteHUB.jar" goto :senzaProgramma

rem --- Ricerca del runtime Java -------------------------------
rem  Ordine di preferenza:
rem   1. il runtime incluso nel pacchetto (cartella jre\)
rem   2. le installazioni di sistema piu' comuni
rem   3. il java del PATH, ma solo se e' davvero un runtime
rem
rem  Ogni candidato viene PROVATO eseguendo java -version e
rem  controllando che risponda con la parola "version". Non basta
rem  che il file esista: sul mercato girano cartelle Java vuote e
rem  collegamenti rotti che rispondono "ok" e poi fanno crashare
rem  il programma.

set "JAVA="

if exist "%~dp0jre\bin\java.exe" set "JAVA=%~dp0jre\bin\java.exe"
if not defined JAVA call :cerca "%ProgramFiles%\Java"
if not defined JAVA call :cerca "%ProgramFiles%\Eclipse Adoptium"
if not defined JAVA call :cerca "%ProgramFiles%\Microsoft"
if not defined JAVA call :cerca "%ProgramFiles%\Zulu"
if not defined JAVA call :cerca "%ProgramFiles%\Amazon Corretto"
if not defined JAVA call :cerca "%ProgramFiles%\BellSoft"
if not defined JAVA call :cerca "%ProgramFiles%\Semeru"
if not defined JAVA call :cerca "%ProgramFiles(x86)%\Java"

if not defined JAVA call :provaPath

rem --- Avvio --------------------------------------------------
if not defined JAVA goto :senzaJava

if defined JAVA echo runtime scelto: %JAVA% >> "%LOG%"

rem  javaw non apre la finestra nera: l'output del programma finisce
rem  nel file di log, cosi' l'utente vede solo l'interfaccia e chi
rem  deve aiutarlo ha comunque i messaggi.
set "LANCIO=%JAVA%"
for %%J in ("%JAVA%") do if exist "%%~dpJjavaw.exe" set "LANCIO=%%~dpJjavaw.exe"
if exist "%~dp0jre\bin\javaw.exe" set "LANCIO=%~dp0jre\bin\javaw.exe"

echo avvio del programma con: %LANCIO% >> "%LOG%"
"%LANCIO%" -Xmx2g -jar "%~dp0NMSITALIA-CorvetteHUB.jar" %* > "%CONSOLE%" 2>&1
set "ESITO=%errorlevel%"
echo programma terminato con esito %ESITO% >> "%LOG%"

if not "%ESITO%"=="0" goto :errore
endlocal
exit /b 0

rem ------------------------------------------------------------
rem  Cerca java.exe sotto una cartella di installazione.
rem  Copre sia i JDK (bin\java.exe) sia i JDK con JRE interno
rem  (jre\bin\java.exe).
rem ------------------------------------------------------------
:cerca
if defined JAVA exit /b 0
if not exist "%~1" exit /b 0
for /d %%D in ("%~1\*") do (
    if not defined JAVA if exist "%%D\bin\java.exe" set "JAVA=%%D\bin\java.exe"
)
if not defined JAVA for /d %%D in ("%~1\*") do (
    if not defined JAVA if exist "%%D\jre\bin\java.exe" set "JAVA=%%D\jre\bin\java.exe"
)
call :verifica
exit /b 0

rem ------------------------------------------------------------
rem  Un runtime va usato solo se e' davvero un runtime: si chiede
rem  la versione e si pretende che risponda. La sola esistenza del
rem  file non basta, e nemmeno un codice di uscita 0: lo stub che
rem  Oracle lascia in Common Files risponde 0 senza stampare nulla.
rem ------------------------------------------------------------
:verifica
if not defined JAVA exit /b 0
"%JAVA%" -version 2>&1 | findstr /i /c:"version" >nul 2>&1
if errorlevel 1 set "JAVA="
exit /b 0

:provaPath
java -version 2>&1 | findstr /i /c:"version" >nul 2>&1
if not errorlevel 1 set "JAVA=java"
exit /b 0

rem ------------------------------------------------------------
rem  Nessun runtime: si spiega cosa fare invece di fallire in
rem  silenzio. Le parentesi tonde sono vietate in questi testi:
rem  dentro un blocco if chiuderebbero il blocco in anticipo.
rem ------------------------------------------------------------
:senzaJava
echo runtime NON trovato >> "%LOG%"
echo.
echo  Non ho trovato un runtime Java funzionante.
echo.
echo  NMS ITALIA Corvette HUB ha bisogno di Java 8 o superiore.
echo.
echo  Due modi per risolvere:
echo.
echo   1. Installa Java da  https://adoptium.net/
echo      Scegli la versione 8 o successiva, installazione
echo      normale. Poi rilancia questo file.
echo.
echo   2. Se hai gia' una cartella jre di un altro programma
echo      Java, copiala qui accanto a questo file, in modo che
echo      diventi  jre\bin\java.exe
echo.
echo  Non serve installare nulla di strano: il programma non
echo  tocca il registro di sistema e non contatta Internet.
echo.
pause
endlocal
exit /b 1

:senzaProgramma
echo programma NON trovato >> "%LOG%"
echo.
echo  Non trovo il file NMSITALIA-CorvetteHUB.jar.
echo.
echo  Questo file va tenuto nella STESSA cartella del programma.
echo  Se hai scompattato l'archivio, controlla di aver estratto
echo  tutto e non solo una parte.
echo.
pause
endlocal
exit /b 1

:errore
echo.
echo Il programma si e chiuso con un errore. Dettagli in
echo   %~dp0CorvetteHUB-avvio.log
echo   %~dp0CorvetteHUB-console.log
echo.
pause
endlocal
exit /b 0
