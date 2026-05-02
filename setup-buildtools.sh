#!/usr/bin/env bash
set -euo pipefail

echo "============================================"
echo " SkyWarsReloaded - BuildTools Setup (Linux/macOS)"
echo "============================================"
echo ""
echo "This script runs Spigot BuildTools with the correct Java version"
echo "for each MC version to install org.spigotmc:spigot into your local .m2."
echo ""

# === Configure your JDK paths here ===
# Set these to the java binary for each major version.
# Examples:
#   JAVA8="/usr/lib/jvm/java-8-openjdk-amd64/bin/java"
#   JAVA16="/usr/lib/jvm/java-16-openjdk-amd64/bin/java"
#   JAVA17="/usr/lib/jvm/java-17-openjdk-amd64/bin/java"
#   JAVA21="/usr/lib/jvm/java-21-openjdk-amd64/bin/java"
#
# On macOS with Homebrew:
#   JAVA8="/Library/Java/JavaVirtualMachines/temurin-8.jdk/Contents/Home/bin/java"
#   JAVA17="/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home/bin/java"
#   JAVA21="/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java"

# Auto-detect using SDKMAN, update-alternatives, or set manually:
find_java() {
    local version="$1"
    # Try common locations
    local candidates=(
        "/usr/lib/jvm/java-${version}-openjdk-amd64/bin/java"
        "/usr/lib/jvm/java-${version}-openjdk/bin/java"
        "/usr/lib/jvm/java-${version}/bin/java"
        "/Library/Java/JavaVirtualMachines/temurin-${version}.jdk/Contents/Home/bin/java"
        "/Library/Java/JavaVirtualMachines/corretto-${version}.jdk/Contents/Home/bin/java"
        "/Library/Java/JavaVirtualMachines/amazon-corretto-${version}.jdk/Contents/Home/bin/java"
    )
    for candidate in "${candidates[@]}"; do
        if [ -x "$candidate" ]; then
            echo "$candidate"
            return 0
        fi
    done
    return 1
}

# Allow override via environment variables
JAVA8="${JAVA8:-$(find_java 8 2>/dev/null || echo "")}"
JAVA16="${JAVA16:-$(find_java 16 2>/dev/null || echo "")}"
JAVA17="${JAVA17:-$(find_java 17 2>/dev/null || echo "")}"
JAVA21="${JAVA21:-$(find_java 21 2>/dev/null || echo "")}"
JAVA25="${JAVA25:-$(find_java 25 2>/dev/null || echo "")}"

# Validate
missing=0
for var_name in JAVA8 JAVA16 JAVA17 JAVA21 JAVA25; do
    val="${!var_name}"
    if [ -z "$val" ] || [ ! -x "$val" ]; then
        echo "ERROR: $var_name not found or not executable: '$val'"
        echo "       Set $var_name=/path/to/java before running this script."
        missing=1
    fi
done
if [ "$missing" -eq 1 ]; then
    echo ""
    echo "Example: JAVA8=/usr/lib/jvm/java-8-openjdk-amd64/bin/java JAVA17=/usr/lib/jvm/java-17/bin/java ./setup-buildtools.sh"
    exit 1
fi

echo "Using:"
echo "  JAVA8  = $JAVA8"
echo "  JAVA16 = $JAVA16"
echo "  JAVA17 = $JAVA17"
echo "  JAVA21 = $JAVA21"
echo "  JAVA25 = $JAVA25"
echo ""

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$SCRIPT_DIR/BuildTools"

mkdir -p "$BUILD_DIR"
cd "$BUILD_DIR"

if [ ! -f "BuildTools.jar" ]; then
    echo "Downloading BuildTools.jar..."
    if command -v curl &>/dev/null; then
        curl -L -o BuildTools.jar https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar
    elif command -v wget &>/dev/null; then
        wget -O BuildTools.jar https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar
    else
        echo "ERROR: Neither curl nor wget found."
        exit 1
    fi
fi

echo ""
echo "Building Spigot for each required NMS version..."
echo "First run takes 5-15 min per version. Subsequent runs are faster."
echo ""

FAILED=0

build_version() {
    local java_bin="$1"
    local version="$2"
    echo ""
    echo "=== [$version] Building with $java_bin... ==="
    if "$java_bin" -jar BuildTools.jar --rev "$version"; then
        echo "=== [$version] Done ==="
    else
        echo "WARNING: $version failed, continuing..."
        ((FAILED++)) || true
    fi
}

# Java 8 versions (1.8.8 - 1.16.4)
for ver in 1.8.8 1.9.2 1.9.4 1.10.2 1.11.2 1.12.2 1.13.2 1.14.4 1.15.1 1.16.1 1.16.2 1.16.4; do
    build_version "$JAVA8" "$ver"
done

# Java 16 (1.17)
build_version "$JAVA16" "1.17"

# Java 17 (1.18.2, 1.19)
for ver in 1.18.2 1.19; do
    build_version "$JAVA17" "$ver"
done

# Java 21 (1.20.6, 1.21.1)
for ver in 1.20.6 1.21.1; do
    build_version "$JAVA21" "$ver"
done

# Java 25 (26.1.2)
build_version "$JAVA25" "26.1.2"

cd "$SCRIPT_DIR"

echo ""
echo "============================================"
if [ "$FAILED" -gt 0 ]; then
    echo " Completed with $FAILED failure(s)."
    echo " Check output above for details."
else
    echo " All versions built successfully!"
fi
echo ""
echo " You can now run ./build.sh or build.bat to compile SkyWarsReloaded."
echo "============================================"
