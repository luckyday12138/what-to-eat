@echo off
cd /d C:\Users\26767\Doubao\chats\2026-11-18\new-chat\what-to-eat
set JAVA_HOME=D:\edge\jdk-25.0.4.1+1
set GRADLE_USER_HOME=D:\Android\.gradle
call gradlew.bat :desktopApp:run --args="--tab 1 --section 1" --console=plain > _shots\run_r2.log 2> _shots\run_r2.err