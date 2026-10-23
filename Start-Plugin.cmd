@echo off
setlocal
title AFK Alerts
pushd "%~dp0"
if not exist "Start-Plugin.ps1" (
  echo The launcher script is missing.
  echo Right-click the downloaded ZIP, choose Extract All, then open the extracted folder.
  goto finished
)
if not exist "build.gradle" (
  echo The full plugin project is missing.
  echo Extract All from the ZIP before starting this launcher.
  goto finished
)
echo Starting AFK Alerts. Keep this window open.
if /i "%~1"=="--check" goto check
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Start-Plugin.ps1"
echo.
echo The launcher has stopped. Any error message is shown above.
:finished
echo.
echo If RuneLite did not open, send a screenshot of the error above.
pause
popd
exit /b
:check
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Start-Plugin.ps1" -PrepareOnly
exit /b %errorlevel%
