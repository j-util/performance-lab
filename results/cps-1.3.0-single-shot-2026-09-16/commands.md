# Verification and benchmark commands

All commands ran from `/Users/karenbarseghyan/Projects/j-util/performance-lab`.
The complete executable source is `scripts/cps13/run-evidence.sh` at measured
commit `41afe3444622e9a46d3f0bf2d9f5e9d2e6fadcee`; `driver.txt` captures the
expanded invocations. `continuation-driver.txt` records the remaining steps
after the command session ended following the completed independent repeat.
The continuation command was:

```sh
bash /private/tmp/cps13-correction/finish-evidence.sh > /private/tmp/cps13-single-shot-2026-09-16/continuation-driver.txt 2>&1
```

`finish-evidence.sh` contains only the remaining commands copied from the committed
runner plus the same JDK, selectors and output-directory variables. The shaded
artifact hash was checked unchanged before continuation.

```sh
export JAVA_HOME=/Users/karenbarseghyan/.sdkman/candidates/java/25.0.2-tem
export PATH="$JAVA_HOME/bin:$PATH"
EVIDENCE_DIR=/private/tmp/cps13-single-shot-2026-09-16
# The runner created this new directory and its gc/ and validator/ children.
git diff --exit-code HEAD -- benchmark-core benchmark-jmh scripts/cps13
./mvnw -B -ntp clean verify > "$EVIDENCE_DIR/clean-verify.txt" 2>&1
DEST='^io\.github\.jutil\.performancelab\.HardwoodDestinationMaterializationBenchmark\.(columnarSequentialRangedBatches|columnarSingleThreadedAppender|columnarFixedPoolPerBatchBarrierAppender|columnarFixedPoolPipelinedAppender|arrayListRows)$'
END='^io\.github\.jutil\.performancelab\.HardwoodMaterializationBenchmark\.(hardwoodToColumnarBatch|hardwoodToExecutorBackedColumnarBatch|hardwoodToArrayList)$'
java -jar benchmark-jmh/target/benchmarks.jar -l "$DEST" > "$EVIDENCE_DIR/destination-list.txt" 2>&1
javac -proc:none -cp benchmark-jmh/target/benchmarks.jar -d "$EVIDENCE_DIR/validator" scripts/cps13/ValidateDestinations.java
java -Xms4g -Xmx4g -XX:+UseG1GC --add-opens=java.base/java.util=ALL-UNNAMED -cp "$EVIDENCE_DIR/validator:benchmark-jmh/target/benchmarks.jar" ValidateDestinations > "$EVIDENCE_DIR/destination-validation.txt" 2>&1
java -Xlog:gc*=info:file="$EVIDENCE_DIR/gc/probe-%p-%t.txt":uptime,pid,tags:filecount=0 -version > "$EVIDENCE_DIR/gc-filename-probe.txt" 2>&1
java -jar benchmark-jmh/target/benchmarks.jar "$DEST" -p rowCount=100000,10000000 -p batchSize=8192,1000000 -bm ss -tu ms -wi 5 -i 10 -f 3 -t 1 -gc true -jvmArgs "-Xms4g -Xmx4g -XX:+UseG1GC -Xlog:gc*=info:file=$EVIDENCE_DIR/gc/destination-primary-%p-%t.txt:uptime,pid,tags:filecount=0" -prof gc -rf json -rff "$EVIDENCE_DIR/destination-primary.json" > "$EVIDENCE_DIR/destination-primary.txt" 2>&1
java -jar benchmark-jmh/target/benchmarks.jar "$DEST" -p rowCount=10000000 -p batchSize=8192,1000000 -bm ss -tu ms -wi 5 -i 10 -f 3 -t 1 -gc true -jvmArgs "-Xms4g -Xmx4g -XX:+UseG1GC -Xlog:gc*=info:file=$EVIDENCE_DIR/gc/destination-repeat-%p-%t.txt:uptime,pid,tags:filecount=0" -prof gc -rf json -rff "$EVIDENCE_DIR/destination-repeat.json" > "$EVIDENCE_DIR/destination-repeat.txt" 2>&1
java -jar benchmark-jmh/target/benchmarks.jar "$END" -p rowCount=100000,10000000 -p batchSize=1000000 -bm ss -tu ms -wi 5 -i 10 -f 3 -t 1 -gc true -jvmArgs "-Xms4g -Xmx4g -XX:+UseG1GC -Xlog:gc*=info:file=$EVIDENCE_DIR/gc/hardwood-%p-%t.txt:uptime,pid,tags:filecount=0" -prof gc -rf json -rff "$EVIDENCE_DIR/hardwood.json" > "$EVIDENCE_DIR/hardwood.txt" 2>&1
python3 scripts/cps13/summarize.py "$EVIDENCE_DIR"
git diff --check > "$EVIDENCE_DIR/diff-check.txt" 2>&1
```

The listing is additionally asserted against the exact five method names by the
embedded Python check in the runner. Environment capture runs `date -u`,
`java -version`, `./mvnw -version`, `sw_vers`, `uname -a`, hardware `sysctl`,
`pmset -g batt`, and repository `git rev-parse`/`git status`; see `environment.txt`.
Artifact SHA-256 computation is the embedded Python block in the runner.
`dependency-provenance.json` verifies the installed candidate contents against
existing consumer builds without rebuilding or changing either consumer.

Before committing the source correction, the following checks also passed:

```sh
JAVA_HOME=/Users/karenbarseghyan/.sdkman/candidates/java/25.0.2-tem ./mvnw -B -ntp verify
/Users/karenbarseghyan/.sdkman/candidates/java/25.0.2-tem/bin/javac -proc:none \
  -cp benchmark-jmh/target/benchmarks.jar -d /private/tmp/cps13-correction/validator \
  scripts/cps13/ValidateDestinations.java
/Users/karenbarseghyan/.sdkman/candidates/java/25.0.2-tem/bin/java \
  -Xms4g -Xmx4g -XX:+UseG1GC --add-opens=java.base/java.util=ALL-UNNAMED \
  -cp /private/tmp/cps13-correction/validator:benchmark-jmh/target/benchmarks.jar \
  ValidateDestinations
bash -n scripts/cps13/run-evidence.sh
# Python compile() checked summarize.py syntax without executing the analysis.
git diff --check
```

The initial validator compilation caught a call to a package-private fixture
helper; it was changed to the public sealed store view before committing. Both
complete validator runs passed with the committed public-API call. The initial
Python py_compile command was prevented from writing its macOS cache; the
subsequent in-memory compile() syntax check passed. Neither affected measurements.

The final scope/historical-preservation audit and Git diff checks are retained
with the evidence. No formatter or additional release/publishing gate was added.

Additional evidence audits after measurement:

```sh
python3 /private/tmp/cps13-correction/audit-gc.py /private/tmp/cps13-single-shot-2026-09-16
# The retained audit-scope.py verifies tracked-file hashes, SHAs and historical preservation.
python3 /private/tmp/cps13-correction/audit-scope.py /private/tmp/cps13-single-shot-2026-09-16
git diff --check
git diff --cached --check
```
