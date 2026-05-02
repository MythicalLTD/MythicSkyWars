@echo off
setlocal EnableExtensions DisableDelayedExpansion

set "ROOT=%~dp0"
set "PANEL_BASE=https://panel.mythical.systems"
set "SERVER_ID=e310d39e"
set "REVISION=5.6.40"
set "UPLOAD_NAME=SWR-%REVISION%.jar"
rem Shaded jar from skywars-assembly (includes v1_8_R3 NMS etc.) — NOT SkyWarsReloadedCore-only jar
set "BUILD_JAR=%ROOT%target\SWR-%REVISION%.jar"
set "COOKIE=%SWR_PANEL_COOKIE%"

if "%COOKIE%"=="" (
  echo ERROR: SWR_PANEL_COOKIE environment variable is not set.
  echo Set it with: set "SWR_PANEL_COOKIE=remember_token=YOUR_TOKEN_HERE"
  exit /b 1
)

where curl.exe >nul 2>&1
if errorlevel 1 (
  echo curl.exe was not found on PATH.
  exit /b 1
)

cd /d "%ROOT%" || exit /b 1
echo Building shaded plugin ^(Core + all NMS modules^)...
call mvn clean package -DskipTests
if errorlevel 1 (
  echo Build failed.
  exit /b 1
)

if not exist "%BUILD_JAR%" (
  echo Built jar not found: "%BUILD_JAR%"
  exit /b 1
)

echo.
echo Shaded plugin ready ^(upload this to /plugins^):
echo   %BUILD_JAR%
echo.

set "HDR=%TEMP%\swr-build-hdr.txt"
set "BODY=%TEMP%\swr-build-body.txt"
set "CODE=%TEMP%\swr-build-code.txt"

echo Deleting old jar on panel...
del "%HDR%" "%BODY%" "%CODE%" 2>nul
curl.exe -sS --location -D "%HDR%" -o "%BODY%" -w "%%{http_code}" ^
  -X "DELETE" ^
  "%PANEL_BASE%/api/user/servers/%SERVER_ID%/delete-files" ^
  -H "content-type: application/json" ^
  -H "accept: application/json, text/plain, */*" ^
  -H "origin: %PANEL_BASE%" ^
  -b "%COOKIE%" ^
  --data-raw "{\"root\":\"/plugins\",\"files\":[\"%UPLOAD_NAME%\"]}" > "%CODE%"
if errorlevel 1 (
  echo curl failed while calling delete-files.
  goto :dump_and_fail
)
for /f "usebackq delims=" %%C in ("%CODE%") do set "HTTP=%%C"
if not defined HTTP goto :dump_and_fail
if %HTTP% geq 200 if %HTTP% lss 300 goto :delete_ok
echo Delete returned HTTP %HTTP% ^(expected 2xx^).
goto :dump_and_fail

:delete_ok
del "%HDR%" "%BODY%" "%CODE%" 2>nul

echo Uploading new jar...
del "%HDR%" "%BODY%" "%CODE%" 2>nul
curl.exe -sS --location -D "%HDR%" -o "%BODY%" -w "%%{http_code}" ^
  -X "POST" ^
  "%PANEL_BASE%/api/user/servers/%SERVER_ID%/upload-file?path=%%2Fplugins&filename=%UPLOAD_NAME%" ^
  -H "content-type: application/octet-stream" ^
  -H "accept: application/json, text/plain, */*" ^
  -H "origin: %PANEL_BASE%" ^
  -b "%COOKIE%" ^
  --data-binary "@%BUILD_JAR%" > "%CODE%"
if errorlevel 1 (
  echo curl failed while uploading.
  goto :dump_and_fail
)
for /f "usebackq delims=" %%C in ("%CODE%") do set "HTTP=%%C"
if not defined HTTP goto :dump_and_fail
if %HTTP% geq 200 if %HTTP% lss 300 goto :upload_ok
echo Upload returned HTTP %HTTP% ^(expected 2xx^).
goto :dump_and_fail

:upload_ok
del "%HDR%" "%BODY%" "%CODE%" 2>nul

echo Restarting server...
del "%HDR%" "%BODY%" "%CODE%" 2>nul
curl.exe -sS --location -D "%HDR%" -o "%BODY%" -w "%%{http_code}" ^
  -X "POST" ^
  "%PANEL_BASE%/api/user/servers/%SERVER_ID%/power/restart" ^
  -H "accept: application/json, text/plain, */*" ^
  -H "content-type: application/json" ^
  -H "origin: %PANEL_BASE%" ^
  -b "%COOKIE%" ^
  --data-raw "{}" > "%CODE%"
if errorlevel 1 (
  echo curl failed while calling restart.
  goto :dump_and_fail
)
for /f "usebackq delims=" %%C in ("%CODE%") do set "HTTP=%%C"
if not defined HTTP goto :dump_and_fail
if %HTTP% geq 200 if %HTTP% lss 300 goto :restart_ok
echo Restart returned HTTP %HTTP% ^(expected 2xx^).
goto :dump_and_fail

:restart_ok
del "%HDR%" "%BODY%" "%CODE%" 2>nul

echo Done.
exit /b 0

:dump_and_fail
echo.
echo -------- Panel HTTP debug --------
if defined HTTP echo HTTP status: %HTTP%
if exist "%HDR%" (
  echo.
  echo --- Response headers ---
  type "%HDR%"
)
if exist "%BODY%" (
  echo.
  echo --- Response body ---
  type "%BODY%"
)
if not exist "%BODY%" if not exist "%HDR%" echo No response capture files found.
echo -------- end --------
del "%HDR%" "%BODY%" "%CODE%" 2>nul
exit /b 1
