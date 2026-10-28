#!/bin/sh
# Shortened gradlew. If gradle-wrapper.jar is missing, first run `gradle wrapper --gradle-version <version in gradle-wrapper.properties>`
# (the GitHub Actions workflow does this automatically and replaces this with the official gradlew).
APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then JAVACMD="$JAVA_HOME/bin/java"; else JAVACMD=java; fi
exec "$JAVACMD" -Xmx64m -Xms64m -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
