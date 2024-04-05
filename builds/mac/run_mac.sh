#!/bin/bash
DIR="$(cd "$(dirname "$0")" && pwd)"
JAVA_CMD="java"
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA_CMD="$JAVA_HOME/bin/java"
fi
echo "Starting kvolegi arcade on macOS..."
exec "$JAVA_CMD" -jar "$DIR/kvolegi-arcade-mac.jar" "$@"
