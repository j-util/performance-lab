<!--
Sync Impact Report
- Version change: template -> 1.0.0
- Modified principles: none (initial ratification)
- Added principles: Realistic Client Workloads; Equivalent Work; Explicit Boundaries;
  Reproducible Inputs; Correctness Before Measurement; Recorded Environment;
  Evidence-Bounded Claims
- Added sections: Experiment Definition; Integrity Gates
- Removed sections: none
- Follow-up TODOs: none
-->
# performance-lab Constitution

## Core Principles

### I. Realistic Client Workloads
Benchmarks MUST use APIs, data access patterns, and lifecycle behavior available to a normal client.
Benchmark-only shortcuts, privileged internal access, precomputed answers, or other techniques a normal
client would not use are prohibited. Any intentionally synthetic workload MUST still model a named
client scenario and state where it differs from production use.

### II. Equivalent Work
Approaches in a comparison MUST consume equivalent inputs and produce the same observable result.
Differences in parsing, materialization, traversal, retention, or aggregation MUST be the variable the
experiment is designed to study, not hidden differences in required work.

### III. Explicit Boundaries
Every experiment MUST state which setup, input generation, file access, parsing, allocation,
materialization, traversal, aggregation, and teardown work occurs inside and outside the measured
operation. Results from experiments with different boundaries MUST NOT be presented as direct
comparisons unless that boundary difference is itself the declared subject.

### IV. Reproducible Inputs
Benchmark parameters and datasets MUST be reproducible. The experiment MUST record parameter values,
dataset size and provenance, generator inputs and seed when generated, and any transformation needed to
recreate the measured input. Mutable or externally sourced data MUST be versioned or checksummed.

### V. Correctness Before Measurement
Correctness MUST be validated before performance is measured. Compared approaches MUST pass an
automated equivalence check against the experiment's expected result, including relevant row counts,
aggregates, or checksums. A failed or missing correctness check invalidates performance results.

### VI. Recorded Environment
Published results MUST identify the repository revision, hardware, operating system, JDK distribution
and version, JVM options, benchmark parameters, and measurement configuration. Relevant runtime
conditions and profilers MUST also be recorded so another operator can assess or reproduce the run.

### VII. Evidence-Bounded Claims
Conclusions MUST stay within the workloads, environments, metrics, and uncertainty demonstrated by the
measurements. Specifications MUST describe the experiment, hypothesis, variables, boundaries, and
acceptance criteria; they MUST NOT contain measured results or conclusions that belong in a result
record.

## Experiment Definition

Before benchmark implementation, an experiment specification MUST name the client scenario, compared
approaches, equivalence oracle, dataset and parameters, measured metric, measurement boundaries, and
acceptance criteria. Architectural differences that prevent strict equivalence MUST be explicit and
must narrow the comparison claim accordingly.

## Integrity Gates

Before measurement, reviewers MUST verify realistic usage, equivalent work, reproducible inputs,
explicit boundaries, and passing correctness checks. Before publication, reviewers MUST verify the
recorded environment and ensure every conclusion is supported by the reported measurements.

## Governance

This constitution governs all experiment specifications, benchmark implementations, and published
results in `performance-lab`. Amendments require a documented rationale and review for their effect on
existing experiments. Versions follow semantic versioning: MAJOR for incompatible principle changes or
removals, MINOR for new principles or materially expanded obligations, and PATCH for clarifications.
Every experiment review MUST check compliance; a justified exception MUST be written into the
experiment specification before measurement and MUST narrow any resulting claim.

**Version**: 1.0.0 | **Ratified**: 2026-08-05 | **Last Amended**: 2026-08-05
