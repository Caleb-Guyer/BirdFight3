@echo off
setlocal
powershell -ExecutionPolicy Bypass -File "%~dp0verify-dist.ps1" %*
exit /b %errorlevel%
