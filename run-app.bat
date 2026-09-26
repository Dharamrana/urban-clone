@echo off
set M2_HOME=C:\tools\apache-maven-3.9.6
set PATH=%M2_HOME%\bin;%PATH%
cd /d D:\project\urban-company-clone

echo Stopping old app...
taskkill /F /IM javaw.exe >nul 2>&1
timeout /t 3 /nobreak >nul

echo Building...
call mvn -q package -DskipTests
if errorlevel 1 (
    echo Build failed.
    pause
    exit /b 1
)

echo Starting UrbanService on http://localhost:8080 ...
start "" javaw -jar target\urban-company-clone-1.0.0.jar --spring.profiles.active=dev
timeout /t 15 /nobreak >nul
echo Done. Open http://localhost:8080 in your browser.
start http://localhost:8080
