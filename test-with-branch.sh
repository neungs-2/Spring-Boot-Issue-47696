#!/usr/bin/env bash
set -euo pipefail

SPRING_BOOT_REPO="https://github.com/neungs-2/spring-boot.git"
BRANCH="${1:?Usage: $0 <branch-name>  (e.g. fix-47969-repository-observed-interceptor)}"
CLONE_DIR=".spring-boot-build"

# Java 25+ is required to build spring-boot main branch.
# Set SPRING_BOOT_JAVA_HOME if your default JAVA_HOME is not Java 25+.
BUILD_JAVA_HOME="${SPRING_BOOT_JAVA_HOME:-$JAVA_HOME}"

echo "============================================"
echo "Branch: $BRANCH"
echo "JAVA_HOME for spring-boot build: $BUILD_JAVA_HOME"
echo "============================================"

# 1. Clone spring-boot (shallow, single branch)
echo ""
echo "[1/3] Cloning spring-boot branch '$BRANCH'..."
rm -rf "$CLONE_DIR"
git clone --depth 1 --branch "$BRANCH" "$SPRING_BOOT_REPO" "$CLONE_DIR"

# 2. Publish to mavenLocal (exclude docs and tests)
echo ""
echo "[2/3] Publishing to mavenLocal..."
cd "$CLONE_DIR"
JAVA_HOME="$BUILD_JAVA_HOME" ./gradlew publishToMavenLocal \
  -x dokkaGenerate \
  -x test \
  -x :documentation:spring-boot-docs:publishMavenPublicationToMavenLocal \
  -x :documentation:spring-boot-docs:publishToMavenLocal
cd ..

# 3. Run demo tests
echo ""
echo "[3/3] Running demo tests..."
./gradlew test

echo ""
echo "============================================"
echo "Done! Branch '$BRANCH' - test results above."
echo "============================================"
