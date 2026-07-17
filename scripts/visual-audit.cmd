@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0visual-audit.ps1" %*
exit /b %errorlevel%
