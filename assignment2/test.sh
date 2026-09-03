#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
BUILD="$ROOT/build/test-classes"

mkdir -p "$BUILD"
find "$ROOT/src/main/java" "$ROOT/src/test/java" -name '*.java' -print0 \
    | xargs -0 javac -encoding UTF-8 -d "$BUILD"

java -ea -Djava.awt.headless=true -cp "$BUILD" fleet.TestRunner
