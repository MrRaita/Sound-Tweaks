#!/bin/sh
# Kısaltılmış gradlew. gradle-wrapper.jar yoksa önce `gradle wrapper --gradle-version 9.5.0` çalıştır
# (GitHub Actions workflow'u bunu otomatik yapar ve resmi gradlew ile değiştirir).
APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then JAVACMD="$JAVA_HOME/bin/java"; else JAVACMD=java; fi
exec "$JAVACMD" -Xmx64m -Xms64m -classpath "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
