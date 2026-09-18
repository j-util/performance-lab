# Benchmark automation policy

This is the accepted policy for future automation, not an operational integration.
It does not enable CodSpeed, finalize Bencher, or change existing benchmark recipes.

## Current behavior

- [Build and Test](../.github/workflows/ci.yml) uses Temurin 25 and
  `./mvnw -B -ntp clean verify`, without running benchmark timings.
- [Manual Benchmarks](../.github/workflows/benchmark-manual.yml) remains available
  with its existing suite choices, Temurin 26, JSON artifacts, and environment metadata.
- The [Bencher workflow](../.github/workflows/bencher.yml) is an experimental,
  manual-only Java 26 workflow. It is not the accepted final integration.
- There is no implemented CodSpeed workflow. The policies below describe later work.

## Initial automated suite

The two-method suite, single parameter combination, and recipe below are frozen
for the initial CodSpeed and Bencher implementations. Workflow implementation
must not silently broaden the selector or modify this contract. Changing a
method, parameter, JDK, OS, runner, testbed, harness, JVM flag, or recipe requires
a separate reviewed policy change and a distinct history/testbed identity.

Both services will select exactly these methods:

- `io.github.jutil.performancelab.marketdata.ColumnarProjectionStoreIterationBenchmark.cursorLastTradePriceSum`
- `io.github.jutil.performancelab.collections.iteration.ReadyCollectionIterationBenchmark.tenSegmentSpliceListIterator`

Use this exact anchored include expression, without broader class selectors:

```text
^io\.github\.jutil\.performancelab\.(marketdata\.ColumnarProjectionStoreIterationBenchmark\.cursorLastTradePriceSum|collections\.iteration\.ReadyCollectionIterationBenchmark\.tenSegmentSpliceListIterator)$
```

Both classes use `rowCount`. Track one fixed parameter combination:

```text
rowCount=100000
```

The planned standard-JMH recipe is:

```text
-p rowCount=100000
-bm avgt -tu ns
-f 1 -wf 0 -t 1
-wi 5 -w 1s -wbs 1
-i 5 -r 1s -bs 1
-to 30s -foe true -gc false
-jvmArgs '-Xms512m -Xmx512m -XX:+UseG1GC'
-rf json
```

Output paths are integration-specific and will be supplied with `-rff`.
These are argument specifications, not standalone shell commands. Use Temurin
Java 25 for both integrations and record its exact build with the run metadata.

The CPS fixture is constructed and validated in trial setup. Each measured
invocation resets its accumulator, creates a cursor, and scans the sealed store.
The Splice List fixture is also constructed and validated in trial setup. At the
frozen `rowCount=100000`, `Math.ceilDiv(100000, 10)` gives a regular segment
capacity of 10000, so the fixture has exactly ten full segments. Each measured
invocation traverses those segments and sums values without consuming the list.
Both are safe for repeated execution. See the existing
[CPS iteration methodology](../README.md#columnarprojectionstoreiterationbenchmark)
and [collection iteration methodology](../README.md#readycollectioniterationbenchmark).

No processor or streaming case is included initially: the representative cases
add file access, parsing, allocation, or scheduling variability to this small
in-memory suite. Each selected method is compared with its own history, not with
the other method as a cross-library comparison.

Before upload, require exactly the two named methods and two JMH JSON entries,
each with only `rowCount=100000`. Reject missing, duplicate, additional, failed,
or non-finite results, and validate the recorded recipe and effective mode.
Preserve raw JSON and the measured commit SHA, command, and environment metadata.
An anchored selector plus exact result-set validation prevents silent expansion.

## Responsibilities

The merge-gate decisions in this table are policy, not a statement that repository
branch-protection settings or future service jobs have been configured.

| Layer | Work and triggers | Authority, history, and reporting |
| --- | --- | --- |
| Ordinary correctness CI | Existing `clean verify` on PRs and pushes to `main`; no benchmark timing. | Required correctness/build authority. No performance history or regression claim. |
| CodSpeed (planned) | The two-method suite on Temurin 25 with the CodSpeed JMH fork. Default to dedicated ARM64 Graviton runner `codspeed-macro-arm64-graviton-ubuntu-22-04`. Pushes to `main` establish baselines; trusted same-repository PRs provide comparisons; manual dispatch is limited to `main`. | Publish service history and advisory comparisons/profiles. Not a merge gate initially; does not replace controlled publication-quality measurements. |
| Bencher (planned) | The same methods on Temurin 25 using standard JMH 1.37 and testbed `gha-ubuntu2404-x64-temurin25-jmh137-inmemory-v1`. Publish accepted `main` history only, after exact-commit correctness verification. Manual validation on `main` produces artifacts without service publication. | No PR publication or regression thresholds initially. Shared-runner history is exploratory and advisory, not a merge gate. Validate the exact result set before upload and preserve raw JMH JSON. |
| Existing manual GitHub Actions benchmarks | Explicit manual dispatch of an existing suite/preset; produce artifacts and environment metadata. | Remain available without automatically publishing service history. Hosted-runner timings are diagnostic, not universal performance evidence or a merge gate. |
| Controlled local runs | Explicit investigations using workload-specific methodology and preserved reproduction instructions. | Remain the venue for publication-quality measurements after repeatability and environment review. Evidence publication is explicit, not automatic. |

Continue using the [existing benchmark recipes](../README.md#run-the-benchmarks),
including the [Hardwood materialization methodology and reproduction links](../README.md#hardwooddestinationmaterializationbenchmark).
This automation policy does not supersede historical evidence or reproduction instructions.

## Service limitations and history identity

CodSpeed's [Java integration](https://codspeed.io/docs/benchmarks/java) currently
supports only walltime. It uses a custom JMH mode and ignores ordinary
benchmark-mode declarations. The requested `avgt`
recipe therefore does not make CodSpeed results interchangeable with standard
JMH results. Verify actual instrumentation rather than accepting an ordinary
uninstrumented run as CodSpeed evidence.

The default planned environment is the [ARM64 Graviton macro runner](https://codspeed.io/docs/features/macro-runners),
with explicit label `codspeed-macro-arm64-graviton-ubuntu-22-04`. The selected
two-method in-memory suite is suitable for this runner. CodSpeed's own
[Java CI](https://github.com/CodSpeedHQ/codspeed-jvm/blob/main/.github/workflows/ci.yml)
runs walltime benchmarks using the legacy `codspeed-macro` label, which continues
to select the same Graviton runner.

Comparisons are valid only against runs using the same runner label and
environment. Ordinary GitHub-hosted runners may execute benchmarks for integration
smoke, but their noisy walltime results are not authoritative CodSpeed regression
history. Controlled local publication-quality measurements remain separate.

Every plan currently includes 600 Graviton runner minutes per month, not unlimited
free execution. Additional open-source sponsored minutes may be requested from
CodSpeed but must not be assumed.

The Ryzen x64 runner `codspeed-macro-x64-ryzen-9950x-ubuntu-24-04` is optional
and is not included in the ordinary free allowance. Use it only if CodSpeed grants
sponsored access or a paid plan is intentionally adopted, with a separate history
identity for that environment.

Bencher's current `java_jmh` adapter does not preserve parameter identity or JMH
secondary metrics; see the [reviewed adapter source](https://github.com/bencherdev/bencher/blob/v0.6.12/lib/bencher_adapter/src/adapters/java/jmh.rs).
The initial policy therefore uses one fixed parameter combination and tracks
latency only, in nanoseconds per complete traversal, with no allocation claim.

Do not mix incompatible results or combine CodSpeed and standard JMH histories.

## Exclusions from initial automation

Exclude Hardwood end-to-end materialization, large CSV workloads, OneBRC-style
file processing, native/off-heap comparisons, very large allocations, destructive
Splice List merge-only methods, allocation-sensitive append benchmarks, parameter
sweeps, processor/streaming cases, and publication-scale workloads.

These exclusions do not make the benchmarks invalid. They remain available for
manual or controlled local use under their existing methodology.

## Security policy

- CodSpeed initially uses [OIDC authentication](https://codspeed.io/docs/integrations/ci/github-actions/configuration)
  with job-scoped `contents: read` and `id-token: write`. No static CodSpeed token
  is required by this policy.
- Skip fork-PR performance execution initially. `pull_request_target` must not
  execute untrusted benchmark code; follow [GitHub's security guidance](https://docs.github.com/en/actions/reference/security/securely-using-pull_request_target).
- Bencher publishing credentials are available only to trusted `main` publication.
  Never expose `BENCHER_API_KEY` to benchmark execution from untrusted code.
- Keep Bencher measurement and validated file upload in separate steps. Provide
  the key only to the upload step, which does not execute a benchmark command.
  Step separation is not isolation from hostile earlier code; initial publication
  is restricted to trusted `main`, with no PR publishing path.
- Bencher initially needs only `contents: read`, without GitHub checks/comments
  permissions. The current manual-only workflow is not a fork-PR trigger;
  extending it to untrusted execution would require a different security design.

## External setup prerequisites

These are unresolved setup prerequisites, not repository defects:

- Confirm access to the default ARM64 Graviton macro runner within the included
  monthly allowance. Additional sponsorship requires coordination with CodSpeed;
  x64 access is not a blocker or prerequisite for Step 3.
- Confirm the CodSpeed GitHub App connection for `j-util/performance-lab` and
  organization runner-group authorization for this public repository; follow
  the [official macro-runner setup](https://codspeed.io/docs/integrations/ci/github-actions/macro-runners.md).
- Confirm that Bencher project UUID `019fd168-9f5a-74f0-97f3-e505dc84583c`
  belongs to the intended account or organization.
- Validate the project-scoped `BENCHER_API_KEY` for that project without exposing
  its value.
- Restrict the `bencher-publish` environment to the `main` branch.
- Confirm that the new Bencher testbed has no inherited thresholds or
  incompatible history before publishing its first report.

Implementing workflows, provisioning these services, and running benchmarks
remain separate work from documenting this policy.
