@echo off
cd /d "%~dp0.."
call gradlew.bat :desktopApp:run --args="--tab 3" --console=plain
