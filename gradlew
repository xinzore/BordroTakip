#!/usr/bin/env sh
#
# Gradle wrapper startup script for POSIX systems.
#
# This repo previously missed wrapper scripts/jars, which broke local builds.
# This script is intentionally small and only depends on the wrapper jars in
# `gradle/wrapper/`.
#

set -eu

APP_HOME=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd -P)

WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
WRAPPER_SHARED_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper-shared.jar"
WRAPPER_CLI_JAR="$APP_HOME/gradle/wrapper/gradle-cli.jar"
WRAPPER_FILES_JAR="$APP_HOME/gradle/wrapper/gradle-files.jar"

if [ ! -f "$WRAPPER_JAR" ]; then
  echo "Missing $WRAPPER_JAR" >&2
  exit 1
fi

if [ ! -f "$WRAPPER_SHARED_JAR" ]; then
  echo "Missing $WRAPPER_SHARED_JAR" >&2
  exit 1
fi

if [ ! -f "$WRAPPER_CLI_JAR" ]; then
  echo "Missing $WRAPPER_CLI_JAR" >&2
  exit 1
fi

if [ ! -f "$WRAPPER_FILES_JAR" ]; then
  echo "Missing $WRAPPER_FILES_JAR" >&2
  exit 1
fi

CLASSPATH="$WRAPPER_JAR:$WRAPPER_SHARED_JAR:$WRAPPER_CLI_JAR:$WRAPPER_FILES_JAR"

if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVA_CMD="$JAVA_HOME/bin/java"
else
  JAVA_CMD=java
fi

exec "$JAVA_CMD" \
  -Dorg.gradle.appname=gradlew \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"
