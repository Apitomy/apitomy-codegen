#!/bin/bash
set -euo pipefail

REQUIRED_JAVA_MAJOR=17

JAVA_MAJOR=$(java -version 2>&1 | head -1 | sed 's/.*"\([0-9]*\)\..*/\1/')
if [ "$JAVA_MAJOR" -lt "$REQUIRED_JAVA_MAJOR" ]; then
    echo "Error: Java $REQUIRED_JAVA_MAJOR+ is required (found Java $JAVA_MAJOR)"
    exit 1
fi

./mvnw clean
./mvnw install "$@"
