@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0test.ps1" %*
exit /b %errorlevel%
