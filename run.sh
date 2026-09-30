#!/bin/bash
cd "$(dirname "$0")"
ls lib/*.jar >/dev/null 2>&1 || { echo "ERROR: put mysql-connector-j jar into lib/ first"; exit 1; }
mkdir -p out
javac -encoding UTF-8 -cp "lib/*" -d out $(find src -name "*.java") && java -cp "out:lib/*" com.expense.Main
