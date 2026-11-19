@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo ============================================================
echo   Iniciando DEDTE Mail System - Grupo #11 SA (INF513)
echo ============================================================
if exist "C:\Program Files\Java\jdk-21.0.10\bin\java.exe" (
    "C:\Program Files\Java\jdk-21.0.10\bin\java.exe" -Dfile.encoding=UTF-8 -jar target\dedte-mail-system-1.0.0-jar-with-dependencies.jar
) else (
    java -Dfile.encoding=UTF-8 -jar target\dedte-mail-system-1.0.0-jar-with-dependencies.jar
)
pause
