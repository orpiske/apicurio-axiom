#!/usr/bin/env bash
#
# Builds the entire Apitomy Axiom project and runs all tests except
# integration tests that require the Claude Code CLI.
#
# Prerequisites:
#   - Java 25+
#   - Maven 3.9+
#
# Usage:
#   ./build.sh
#

set -euo pipefail

echo "============================================"
echo "  Apitomy Axiom — Build + Tests"
echo "============================================"
echo ""

# Check prerequisites
if ! command -v java &>/dev/null; then
    echo "ERROR: java not found on PATH"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | head -1 | sed -E 's/.*"([0-9]+).*/\1/')
if [[ "$JAVA_VERSION" -lt 25 ]]; then
    echo "ERROR: Java 25+ required, found Java $JAVA_VERSION"
    exit 1
fi

if ! command -v mvn &>/dev/null; then
    echo "ERROR: mvn not found on PATH"
    exit 1
fi

echo "Java:   $(java -version 2>&1 | head -1)"
echo "Maven:  $(mvn --version 2>&1 | head -1)"
echo ""

mvn clean package "$@"
