#!/usr/bin/env bash
set -euo pipefail
export JAVA_HOME=/Users/karenbarseghyan/.sdkman/candidates/java/25.0.2-tem
export PATH="$JAVA_HOME/bin:$PATH"
EVIDENCE_DIR=/private/tmp/cps13-single-shot-2026-09-16
END='^io\.github\.jutil\.performancelab\.HardwoodMaterializationBenchmark\.(hardwoodToColumnarBatch|hardwoodToExecutorBackedColumnarBatch|hardwoodToArrayList)$'
set -x
git diff --exit-code HEAD -- benchmark-core benchmark-jmh scripts/cps13
java -jar benchmark-jmh/target/benchmarks.jar "$END" -p rowCount=100000,10000000 -p batchSize=1000000 -bm ss -tu ms -wi 5 -i 10 -f 3 -t 1 -gc true -jvmArgs "-Xms4g -Xmx4g -XX:+UseG1GC -Xlog:gc*=info:file=$EVIDENCE_DIR/gc/hardwood-%p-%t.txt:uptime,pid,tags:filecount=0" -prof gc -rf json -rff "$EVIDENCE_DIR/hardwood.json" > "$EVIDENCE_DIR/hardwood.txt" 2>&1
python3 scripts/cps13/summarize.py "$EVIDENCE_DIR"
git diff --check > "$EVIDENCE_DIR/diff-check.txt" 2>&1
set +x
echo 'PASS complete validation and benchmark sequence'
