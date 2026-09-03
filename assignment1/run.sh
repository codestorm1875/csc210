#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")" && pwd)"
BUILD="$ROOT/build/classes"

mkdir -p "$BUILD"
find "$ROOT/src/main/java" -name '*.java' -print0 \
    | xargs -0 javac -encoding UTF-8 -d "$BUILD"
cp -R "$ROOT/src/main/resources/." "$BUILD/"

java -cp "$BUILD" serp.Main
