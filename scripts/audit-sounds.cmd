@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0audit-sounds.ps1" %*
exit /b %errorlevel%
