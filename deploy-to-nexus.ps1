Write-Host "============================================"
Write-Host " Build & Deploy Spigot to Mythical Nexus"
Write-Host "============================================"
Write-Host ""
Write-Host "This script builds Spigot via BuildTools (if not already built)"
Write-Host "then deploys the artifacts to your Nexus so other devs can pull them."
Write-Host ""

# === Configuration ===
$NEXUS_URL = "https://nexus.mythical.systems/repository/maven-snapshots"
$REPO_ID = "mythical-snapshots"
$M2 = "$env:USERPROFILE\.m2\repository\org\spigotmc\spigot"
$TEMP_DIR = "$env:TEMP\spigot-deploy"
$BUILD_DIR = "$PSScriptRoot\BuildTools"

# JDK paths - adjust these to your system
$JAVA8  = "C:\Program Files\Amazon Corretto\jdk1.8.0_492\bin\java.exe"
$JAVA16 = "C:\Program Files\Amazon Corretto\jdk16.0.2_7\bin\java.exe"
$JAVA17 = "C:\Program Files\Amazon Corretto\jdk17.0.19_10\bin\java.exe"
$JAVA21 = "C:\Program Files\Amazon Corretto\jdk21.0.11_10\bin\java.exe"
$JAVA25 = "C:\Program Files\Amazon Corretto\jdk25.0.3_9\bin\java.exe"

# Version -> Java mapping
$versionMap = [ordered]@{
    "1.8.8"  = $JAVA8
    "1.9.2"  = $JAVA8
    "1.9.4"  = $JAVA8
    "1.10.2" = $JAVA8
    "1.11.2" = $JAVA8
    "1.12.2" = $JAVA8
    "1.13.2" = $JAVA8
    "1.14.4" = $JAVA8
    "1.15.1" = $JAVA8
    "1.16.1" = $JAVA8
    "1.16.2" = $JAVA8
    "1.16.4" = $JAVA8
    "1.17"   = $JAVA16
    "1.18.2" = $JAVA17
    "1.19"   = $JAVA17
    "1.20.6" = $JAVA21
    "1.21.1" = $JAVA21
    "26.1.2" = $JAVA25
}

# === Step 1: BuildTools (only for missing versions) ===
$needsBuild = @()
foreach ($ver in $versionMap.Keys) {
    $snapshotVer = "$ver-R0.1-SNAPSHOT"
    $jarPath = "$M2\$snapshotVer\spigot-$snapshotVer.jar"
    if (-not (Test-Path $jarPath)) {
        $needsBuild += $ver
    }
}

if ($needsBuild.Count -gt 0) {
    Write-Host "Missing $($needsBuild.Count) version(s) in local .m2. Running BuildTools..." -ForegroundColor Yellow
    Write-Host "Versions to build: $($needsBuild -join ', ')" -ForegroundColor Yellow
    Write-Host ""

    # Download BuildTools if needed
    if (-not (Test-Path $BUILD_DIR)) { New-Item -ItemType Directory -Path $BUILD_DIR -Force | Out-Null }
    $btJar = "$BUILD_DIR\BuildTools.jar"
    if (-not (Test-Path $btJar)) {
        Write-Host "Downloading BuildTools.jar..."
        Invoke-WebRequest -Uri "https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar" -OutFile $btJar
    }

    $originalDir = Get-Location
    Set-Location $BUILD_DIR

    foreach ($ver in $needsBuild) {
        $java = $versionMap[$ver]
        if (-not (Test-Path $java)) {
            Write-Host "SKIP BUILD: $ver (Java not found at $java)" -ForegroundColor Red
            continue
        }
        Write-Host ""
        Write-Host "=== Building $ver with $java ===" -ForegroundColor Cyan
        & $java -jar BuildTools.jar --rev $ver 2>&1 | Select-String -Pattern "(Success|FAILURE|BUILD|Error|version you have requested)" | ForEach-Object { Write-Host $_.Line }
        if ($LASTEXITCODE -ne 0) {
            Write-Host "WARNING: BuildTools failed for $ver" -ForegroundColor Red
        }
    }

    Set-Location $originalDir
    Write-Host ""
}

# === Step 2: Deploy to Nexus ===
Write-Host "Deploying to $NEXUS_URL..." -ForegroundColor Cyan
Write-Host ""

if (Test-Path $TEMP_DIR) { Remove-Item -Recurse -Force $TEMP_DIR }
New-Item -ItemType Directory -Path $TEMP_DIR -Force | Out-Null

$failed = 0
$deployed = 0
$skipped = 0

foreach ($ver in $versionMap.Keys) {
    $snapshotVer = "$ver-R0.1-SNAPSHOT"
    $jarSrc = "$M2\$snapshotVer\spigot-$snapshotVer.jar"
    $pomSrc = "$M2\$snapshotVer\spigot-$snapshotVer.pom"

    if (-not (Test-Path $jarSrc)) {
        Write-Host "SKIP: spigot-$snapshotVer (not available)" -ForegroundColor Yellow
        $skipped++
        continue
    }

    # Copy to temp (Maven refuses to deploy from .m2 directly)
    $jarDst = "$TEMP_DIR\spigot-$snapshotVer.jar"
    $pomDst = "$TEMP_DIR\spigot-$snapshotVer.pom"
    Copy-Item $jarSrc $jarDst -Force
    if (Test-Path $pomSrc) { Copy-Item $pomSrc $pomDst -Force }

    Write-Host "=== Deploying spigot-$snapshotVer ===" -ForegroundColor Cyan

    $mvnArgs = @(
        "deploy:deploy-file",
        "-DgroupId=org.spigotmc",
        "-DartifactId=spigot",
        "-Dversion=$snapshotVer",
        "-Dpackaging=jar",
        "-Dfile=$jarDst",
        "-DrepositoryId=$REPO_ID",
        "-Durl=$NEXUS_URL",
        "-DupdateReleaseInfo=true",
        "--no-transfer-progress",
        "-N"
    )
    if (Test-Path $pomDst) { $mvnArgs += "-DpomFile=$pomDst" }

    & mvn $mvnArgs 2>&1 | Select-String -Pattern "(BUILD|ERROR|Uploaded|Uploading|401|403|500)" | ForEach-Object { Write-Host $_.Line }

    if ($LASTEXITCODE -eq 0) {
        Write-Host "=== $snapshotVer deployed ===" -ForegroundColor Green
        $deployed++
    } else {
        Write-Host "WARNING: $snapshotVer failed to deploy" -ForegroundColor Red
        $failed++
    }
    Write-Host ""
}

# Cleanup
Remove-Item -Recurse -Force $TEMP_DIR -ErrorAction SilentlyContinue

Write-Host "============================================"
Write-Host " Deployed: $deployed | Failed: $failed | Skipped: $skipped"
Write-Host "============================================"
if ($deployed -gt 0) {
    Write-Host ""
    Write-Host " Your devs can now build without BuildTools!" -ForegroundColor Green
    Write-Host " They just need to clone and run: build.bat" -ForegroundColor Green
}
