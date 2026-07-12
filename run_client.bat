@echo off
echo ================================
echo   Fireball Predict - 启动客户端
echo ================================
cd /d "%~dp0"
call gradlew runClient --rerun-tasks
pause
