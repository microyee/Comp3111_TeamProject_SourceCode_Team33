@echo off
setlocal EnableExtensions
title HKUST Marketplace

rem Always run from the folder that contains this script.
cd /d "%~dp0"

rem Keep Maven's wrapper and local repository inside the project, matching run.sh.
rem This also avoids failures when the user cannot write to %USERPROFILE%\.m2.
if not defined MAVEN_USER_HOME set "MAVEN_USER_HOME=%CD%\.mvn-home"
if not defined MAVEN_LOCAL_REPO set "MAVEN_LOCAL_REPO=%CD%\.m2-repository"

rem The Maven build needs a full JDK (java + javac), not only a JRE.
where java >nul 2>nul
if errorlevel 1 (
    if not exist "%JAVA_HOME%\bin\java.exe" (
        echo [ERROR] Java was not found on PATH or in JAVA_HOME.
        echo Install JDK 17 or newer from https://adoptium.net/ and open a new terminal.
        pause
        exit /b 1
    )
)
where javac >nul 2>nul
if errorlevel 1 (
    if not exist "%JAVA_HOME%\bin\javac.exe" (
        echo [ERROR] javac was not found. You need a full JDK, not only a JRE.
        echo Install JDK 17 or newer from https://adoptium.net/ and open a new terminal.
        pause
        exit /b 1
    )
)

if not exist "%~dp0mvnw.cmd" (
    echo [ERROR] mvnw.cmd was not found next to run.bat.
    echo Keep run.bat in the project root together with mvnw.cmd and pom.xml.
    pause
    exit /b 1
)

echo Starting HKUST Marketplace...
echo Maven user home:        %MAVEN_USER_HOME%
echo Maven local repository: %MAVEN_LOCAL_REPO%
echo.

call "%~dp0mvnw.cmd" "-Dmaven.repo.local=%MAVEN_LOCAL_REPO%" clean compile javafx:run
set "EXIT_CODE=%ERRORLEVEL%"

if not "%EXIT_CODE%"=="0" (
    echo.
    echo [ERROR] The application exited with code %EXIT_CODE%.
    echo Check the Maven output above for the first error.
    pause
)

endlocal & exit /b %EXIT_CODE%
