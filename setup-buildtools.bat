@echo off
setlocal EnableExtensions EnableDelayedExpansion
echo ============================================
echo  SkyWarsReloaded - BuildTools Setup (Windows)
echo ============================================
echo.
echo This script runs Spigot BuildTools with the correct Java version
echo for each MC version to install org.spigotmc:spigot into your local .m2.
echo.

rem === Configure your JDK paths here ===
set "JAVA8=C:\Program Files\Amazon Corretto\jdk1.8.0_492\bin\java.exe"
set "JAVA16=C:\Program Files\Amazon Corretto\jdk16.0.2_7\bin\java.exe"
set "JAVA17=C:\Program Files\Amazon Corretto\jdk17.0.19_10\bin\java.exe"
set "JAVA21=C:\Program Files\Amazon Corretto\jdk21.0.11_10\bin\java.exe"
set "JAVA25=C:\Program Files\Amazon Corretto\jdk25.0.3_9\bin\java.exe"

rem Verify JDKs exist
for %%J in ("%JAVA8%" "%JAVA16%" "%JAVA17%" "%JAVA21%" "%JAVA25%") do (
    if not exist %%J (
        echo ERROR: JDK not found at %%J
        echo Please edit this script and set the correct paths.
        exit /b 1
    )
)

if not exist "BuildTools" mkdir BuildTools
pushd BuildTools

if not exist "BuildTools.jar" (
    echo Downloading BuildTools.jar...
    curl -L -o BuildTools.jar https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar
    if errorlevel 1 (
        echo ERROR: Failed to download BuildTools.jar
        popd
        exit /b 1
    )
)

echo.
echo Building Spigot for each required NMS version...
echo First run takes 5-15 min per version. Subsequent runs are faster.
echo.

set FAILED=0

rem --- Java 8 versions (1.8.8 - 1.12.2) ---
for %%v in (1.8.8 1.9.2 1.9.4 1.10.2 1.11.2 1.12.2) do (
    echo.
    echo === [%%v] Building with Java 8... ===
    "%JAVA8%" -jar BuildTools.jar --rev %%v
    if errorlevel 1 (
        echo WARNING: %%v failed, continuing...
        set /a FAILED+=1
    ) else (
        echo === [%%v] Done ===
    )
)

rem --- Java 8 versions (1.13.2 - 1.16.4) ---
for %%v in (1.13.2 1.14.4 1.15.1 1.16.1 1.16.2 1.16.4) do (
    echo.
    echo === [%%v] Building with Java 8... ===
    "%JAVA8%" -jar BuildTools.jar --rev %%v
    if errorlevel 1 (
        echo WARNING: %%v failed, continuing...
        set /a FAILED+=1
    ) else (
        echo === [%%v] Done ===
    )
)

rem --- Java 16 version (1.17) ---
echo.
echo === [1.17] Building with Java 16... ===
"%JAVA16%" -jar BuildTools.jar --rev 1.17
if errorlevel 1 (
    echo WARNING: 1.17 failed, continuing...
    set /a FAILED+=1
) else (
    echo === [1.17] Done ===
)

rem --- Java 17 versions (1.18.2, 1.19) ---
for %%v in (1.18.2 1.19) do (
    echo.
    echo === [%%v] Building with Java 17... ===
    "%JAVA17%" -jar BuildTools.jar --rev %%v
    if errorlevel 1 (
        echo WARNING: %%v failed, continuing...
        set /a FAILED+=1
    ) else (
        echo === [%%v] Done ===
    )
)

rem --- Java 21 versions (1.20.6, 1.21.1) ---
for %%v in (1.20.6 1.21.1) do (
    echo.
    echo === [%%v] Building with Java 21... ===
    "%JAVA21%" -jar BuildTools.jar --rev %%v
    if errorlevel 1 (
        echo WARNING: %%v failed, continuing...
        set /a FAILED+=1
    ) else (
        echo === [%%v] Done ===
    )
)

rem --- Java 25 version (26.1.2) ---
echo.
echo === [26.1.2] Building with Java 25... ===
"%JAVA25%" -jar BuildTools.jar --rev 26.1.2
if errorlevel 1 (
    echo WARNING: 26.1.2 failed, continuing...
    set /a FAILED+=1
) else (
    echo === [26.1.2] Done ===
)

popd

echo.
echo ============================================
if !FAILED! gtr 0 (
    echo  Completed with !FAILED! failure(s).
    echo  Check output above for details.
) else (
    echo  All versions built successfully!
)
echo.
echo  You can now run build.bat to compile SkyWarsReloaded.
echo ============================================
exit /b 0
