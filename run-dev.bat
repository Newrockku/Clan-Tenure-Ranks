@echo off
REM Double-click this to build the plugin and launch a RuneLite dev client with it loaded.
REM %~dp0 is this file's own folder, so it works no matter where it is launched from.
cd /d "%~dp0"

echo Building and starting RuneLite... this takes about 20 seconds.
echo Keep this window open - closing it closes the game client.
echo.

call gradlew.bat run

echo.
echo ---- RuneLite exited (code %ERRORLEVEL%) ----
echo Read any error above, then press a key to close.
pause >nul
