@echo off
setlocal

set "APP_HOME=%~dp0.."
set "JAVA_EXE=java"

if defined JAVA_HOME (
    if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
)

"%JAVA_EXE%" --enable-native-access=ALL-UNNAMED -cp "%APP_HOME%\lib\*" com.example.birdgame3.Launcher %*
exit /b %errorlevel%
