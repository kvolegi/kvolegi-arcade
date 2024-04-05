@echo off
title kvolegi arcade
echo Starting kvolegi arcade on Windows...
where javaw >nul 2>nul
if %errorlevel% equ 0 (
    start javaw -jar "%~dp0kvolegi-arcade-windows.jar"
    exit /b
)
where java >nul 2>nul
if %errorlevel% equ 0 (
    java -jar "%~dp0kvolegi-arcade-windows.jar"
    exit /b
)
echo [ERROR] Java Runtime is not installed or not in PATH.
echo Please install Java 17, 21 or later to run kvolegi arcade.
pause
