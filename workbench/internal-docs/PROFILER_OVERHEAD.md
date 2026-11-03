# Code Profiler — Instrumentation Overhead Report

Status: **measured** (2026-10). Producer: *(benchmark harness removed 2026-10-16 — it ran in every CI test run and slowed the suite)*
(tagged `benchmark`). Raw output: `build/profiler-overhead-benchmark.txt`.

## The question this answers

The inclusive call-timing feature ("`sleeper()` shows how long it was on the stack") adds an extra
`markEnter`/`markExit` bracket around every BoxLang invocation. **How much does that add on top of the
instrumentation we already have?**

This measures **base instrumentation (span marks only)** vs **new instrumentation (span marks + the
additive inclusive brackets)**. Both are profiler-ON and both record into the service; the ONLY
difference is the brackets, so the delta is the marginal cost of the feature.

## Method

Same script run N times via `BoxRuntime.executeSource`, best-of-3 reps after warmup, mark counts
delta-measured over the timed rep. The class is force-recompiled for each variant (a static toggle,
`Transpiler.setInclusiveTimingEnabled`, controls whether brackets are emitted). Single JVM, local dev
machine; absolute numbers vary by hardware.

## Results — MARGINAL cost of the inclusive feature

| Workload | Base instr (marks only) | New instr (+ inclusive) | **Marginal cost** |
|---|---|---|---|
| call-heavy (5 UDF calls/run) | 6.62 us/run | 6.66 us/run | **+0.6%** (~+4 ns/run) |
| tight loop (50 iters, no UDF) | 18.97 us/run | 18.94 us/run | **−0.1%** (noise; no calls) |
| UDF call loop (50 calls/run) | 52.13 us/run | 50.86 us/run | **−2.4%** (noise) |

(Across repeated runs the UDF-loop delta bounces between ~+3% and ~−2%, so the true marginal cost
is **at or below the run-to-run noise floor** — i.e. a few tens of nanoseconds per call at most.)

## Reading the numbers

- **The marginal cost is ≤ a few tens of ns per call** — two service calls (`markEnter` + `markExit`)
  plus a small per-thread stack push/pop — and is indistinguishable from measurement noise on this
  machine for realistic call mixes.
- **Loop-only code pays nothing** — no calls, no brackets.
- For comparison, the **existing** base instrumentation already costs ~40–70 ns per executed span
  (measured separately). The inclusive feature adds at most about the cost of *one extra mark per
  call* — it is not the expensive part.

## Verdict

**The juice is worth the squeeze.** The inclusive-timing feature's marginal cost is within run-to-run
noise (≲30 ns per call), which is negligible next to the real work any call does and next to the
~50 ns/mark the base instrumentation already pays. Instrumentation stays opt-in
(`codeProfilerEnabled`, default off), so production is unaffected unless profiling is explicitly
enabled.

Recommended: **ship the feature**, keep the flag opt-in, and treat the base per-mark cost (not this
feature) as the thing to watch if always-on profiling is ever considered.
