@echo off
cd /d C:\Users\26767\Doubao\chats\2026-11-18\new-chat\what-to-eat
set JAVA_HOME=D:\edge\jdk-25.0.4.1+1
set GRADLE_USER_HOME=D:\Android\.gradle
call gradlew.bat :desktopApp:run --args="--tab 2" --console=plain > _shots\run_lib.log 2> _shots\run_lib.err
