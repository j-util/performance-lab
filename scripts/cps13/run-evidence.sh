#!/usr/bin/env bash
set -euo pipefail
# Run from the performance-lab root. Use a new absolute evidence directory.
EVIDENCE_DIR=${1:?Pass a new absolute evidence directory}
: "${JAVA_HOME:?Select the exact JDK before running}"
case "$EVIDENCE_DIR" in /*) ;; *) echo 'Evidence path must be absolute' >&2; exit 1;; esac
if [ -e "$EVIDENCE_DIR" ]; then echo 'Evidence directory must be new' >&2; exit 1; fi
mkdir -p "$EVIDENCE_DIR/gc" "$EVIDENCE_DIR/validator"
export PATH="$JAVA_HOME/bin:$PATH"
exec > >(tee "$EVIDENCE_DIR/driver.txt") 2>&1
set -x
# Refuse to measure modified benchmark sources (untracked evidence is permitted).
git diff --exit-code HEAD -- benchmark-core benchmark-jmh scripts/cps13
{
  date -u
  java -version
  ./mvnw -version
  sw_vers
  uname -a
  sysctl hw.memsize hw.logicalcpu hw.physicalcpu machdep.cpu.brand_string
  pmset -g batt
  for repo in . ../columnar-projection-store ../columnar-projection-store-hardwood; do
    git -C "$repo" rev-parse --show-toplevel HEAD
    git -C "$repo" status --short --branch
  done
} > "$EVIDENCE_DIR/environment.txt" 2>&1
./mvnw -B -ntp clean verify > "$EVIDENCE_DIR/clean-verify.txt" 2>&1
DEST='^io\.github\.jutil\.performancelab\.HardwoodDestinationMaterializationBenchmark\.(columnarSequentialRangedBatches|columnarSingleThreadedAppender|columnarFixedPoolPerBatchBarrierAppender|columnarFixedPoolPipelinedAppender|arrayListRows)$'
END='^io\.github\.jutil\.performancelab\.HardwoodMaterializationBenchmark\.(hardwoodToColumnarBatch|hardwoodToExecutorBackedColumnarBatch|hardwoodToArrayList)$'
java -jar benchmark-jmh/target/benchmarks.jar -l "$DEST" > "$EVIDENCE_DIR/destination-list.txt" 2>&1
python3 - "$EVIDENCE_DIR/destination-list.txt" <<'PY'
import sys
from pathlib import Path
actual={x.rsplit('.',1)[-1] for x in Path(sys.argv[1]).read_text().splitlines() if x.startswith('io.github.')}
assert actual == {'columnarSequentialRangedBatches','columnarSingleThreadedAppender','columnarFixedPoolPerBatchBarrierAppender','columnarFixedPoolPipelinedAppender','arrayListRows'},actual
print('PASS exact five-method shaded JMH selector')
PY
javac -proc:none -cp benchmark-jmh/target/benchmarks.jar -d "$EVIDENCE_DIR/validator" scripts/cps13/ValidateDestinations.java
java -Xms4g -Xmx4g -XX:+UseG1GC --add-opens=java.base/java.util=ALL-UNNAMED -cp "$EVIDENCE_DIR/validator:benchmark-jmh/target/benchmarks.jar" ValidateDestinations > "$EVIDENCE_DIR/destination-validation.txt" 2>&1
python3 - "$EVIDENCE_DIR/artifact-sha256.txt" <<'PY'
import hashlib,sys
from pathlib import Path
files=[Path('benchmark-jmh/target/benchmarks.jar')]
for artifact,version in [('columnar-projection-store','1.3.0-SNAPSHOT'),('columnar-projection-store-processor','1.3.0-SNAPSHOT'),('columnar-projection-store-hardwood','1.1.0-SNAPSHOT'),('columnar-projection-store-hardwood-processor','1.1.0-SNAPSHOT')]:
 files.append(Path.home()/'.m2/repository/io/github/j-util'/artifact/version/(artifact+'-'+version+'.jar'))
with open(sys.argv[1],'w') as out:
 for file in files: out.write(hashlib.sha256(file.read_bytes()).hexdigest()+'  '+str(file)+'\n')
PY
java -Xlog:gc*=info:file="$EVIDENCE_DIR/gc/probe-%p-%t.txt":uptime,pid,tags:filecount=0 -version > "$EVIDENCE_DIR/gc-filename-probe.txt" 2>&1
java -jar benchmark-jmh/target/benchmarks.jar "$DEST" -p rowCount=100000,10000000 -p batchSize=8192,1000000 -bm ss -tu ms -wi 5 -i 10 -f 3 -t 1 -gc true -jvmArgs "-Xms4g -Xmx4g -XX:+UseG1GC -Xlog:gc*=info:file=$EVIDENCE_DIR/gc/destination-primary-%p-%t.txt:uptime,pid,tags:filecount=0" -prof gc -rf json -rff "$EVIDENCE_DIR/destination-primary.json" > "$EVIDENCE_DIR/destination-primary.txt" 2>&1
java -jar benchmark-jmh/target/benchmarks.jar "$DEST" -p rowCount=10000000 -p batchSize=8192,1000000 -bm ss -tu ms -wi 5 -i 10 -f 3 -t 1 -gc true -jvmArgs "-Xms4g -Xmx4g -XX:+UseG1GC -Xlog:gc*=info:file=$EVIDENCE_DIR/gc/destination-repeat-%p-%t.txt:uptime,pid,tags:filecount=0" -prof gc -rf json -rff "$EVIDENCE_DIR/destination-repeat.json" > "$EVIDENCE_DIR/destination-repeat.txt" 2>&1
java -jar benchmark-jmh/target/benchmarks.jar "$END" -p rowCount=100000,10000000 -p batchSize=1000000 -bm ss -tu ms -wi 5 -i 10 -f 3 -t 1 -gc true -jvmArgs "-Xms4g -Xmx4g -XX:+UseG1GC -Xlog:gc*=info:file=$EVIDENCE_DIR/gc/hardwood-%p-%t.txt:uptime,pid,tags:filecount=0" -prof gc -rf json -rff "$EVIDENCE_DIR/hardwood.json" > "$EVIDENCE_DIR/hardwood.txt" 2>&1
python3 scripts/cps13/summarize.py "$EVIDENCE_DIR"
git diff --check > "$EVIDENCE_DIR/diff-check.txt" 2>&1
set +x
echo 'PASS complete validation and benchmark sequence'
