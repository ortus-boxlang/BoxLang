# Parser Memory Benchmark

This benchmark measures the object graph retained by parsed AST roots. It uses
ColdBox Platform revision `389fc37eb19d519324dd55a046c7da318b6c843f` as a
repeatable real-world CFML corpus. CFML transpilation is disabled so the
benchmark measures the parser's retained AST directly.

Run the normal retained-heap scenario:

```bash
./workbench/scripts/benchmark-parser-memory.sh
```

Run the worst-case lazy-source scenario, where every node's source text is
requested and the returned strings are retained before measurement:

```bash
./workbench/scripts/benchmark-parser-memory.sh --materialize-source-text
```

Use `--limit N` for faster development comparisons. Use `--corpus PATH` to
measure another local corpus. Add `--jfr` to write
`build/parser-memory/parser-memory.jfr` for allocation, GC, and process-level
analysis in JDK Mission Control. Add `--census-strings` to report equal semantic
string values retained as distinct objects, excluding source text and comment
text.

The primary metrics are `ast.graph.bytes` and `ast.bytes.per.node`. Parse time
is included as a regression signal, not as a statistically rigorous throughput
benchmark. Run each implementation in a fresh process several times and compare
the median retained size.

## Baseline

On OpenJDK 21 with the first 100 sorted ColdBox source files:

| Metric | Baseline |
|---|---:|
| Source bytes | 732,423 |
| Retained files | 100 |
| AST nodes | 64,119 |
| Graph objects | 624,607 |
| Graph bytes | 21,656,080 |
| Bytes per node | 337.75 |
| `Point` objects | 121,108 |
| `Position` objects | 60,554 |
| `ArrayList` objects | 146,673 |
| `String` objects | 92,317 |

Object sizes depend on JVM object layout. Compare variants using the same JDK
and JVM arguments.

## Incremental Results

The first 100 sorted ColdBox files provide these implementation checkpoints:

| Variant | Graph objects | Graph bytes | Bytes/node | Change from baseline |
|---|---:|---:|---:|---:|
| Baseline | 624,607 | 21,656,080 | 337.75 | - |
| Shared source ranges | 592,082 | 21,538,424 | 335.91 | -0.54% |
| Compact positions | 470,974 | 19,116,264 | 298.14 | -11.73% |
| Shared empty collections | 375,503 | 16,729,640 | 260.92 | -22.75% |

Shared source ranges remove overlapping source strings but retain one complete
copy of each file and two character offsets per position. This makes its net
retained-memory improvement modest on this corpus. Compact positions remove
121,108 retained `Point` objects. Shared empty collections reduce retained
`ArrayList` instances from 146,673 to 53,248.

Retaining every returned source string raises the combined 100-file graph to
18,684,744 bytes.

## Full Corpus Results

The complete pinned ColdBox corpus contains 656 source files and 2,789,516
source bytes. Baseline and optimized per-file manifests contain the same node
count and type histogram for every file, establishing structural equivalence
across all 268,499 unique AST nodes.

| Metric | Baseline | Shared ranges, compact positions, and empty collections | Parser allocation and collection capacity pass | Total change |
|---|---:|---:|---:|---:|
| AST nodes | 268,499 | 268,499 | 268,499 | 0 |
| Graph objects | 2,615,793 | 1,541,414 | 1,541,414 | -1,074,379 |
| Graph bytes | 92,513,632 | 67,665,008 | 63,144,240 | -29,369,392 (-31.75%) |
| Bytes per node | 344.56 | 252.01 | 235.17 | -109.39 |

In the worst-case lazy-source scenario, retaining every node's resolved source
string increases the final graph to 1,721,436 objects and 75,106,528 bytes, or
279.73 bytes per node. This is 4,520,768 bytes less than the first optimization
pass and remains 17,407,104 bytes below the original non-materialized baseline.

The final pass constructs positions from primitive coordinates in parser hot
paths, counts newlines without temporary strings, and gives promoted AST child
and comment lists smaller initial capacities. The retained object count is
unchanged, but the right-sized backing arrays save 4,520,768 bytes (6.68%) over
the first optimization pass.

Three full-corpus runs in isolated JVMs produced these parse-time samples:

| Variant | Runs (ms) | Median (ms) |
|---|---:|---:|
| First optimization pass | 7,450.14, 7,337.46, 6,286.99 | 7,337.46 |
| Final pass | 6,264.72, 7,426.08, 6,463.34 | 6,463.34 |

The final median is 11.91% lower. These timings are a directional regression
signal from the retained-heap harness, not a formal parser throughput benchmark.

A representative final-pass JFR recording is written to
`build/parser-memory/parser-memory.jfr` when the benchmark runs with `--jfr`.

## Semantic Strings And Compact Children

A follow-up pass measured 141,305 semantic string references containing 129,479
distinct objects but only 16,298 distinct values. Of the 113,181 duplicate
objects across the corpus, 87,079 occur within individual ASTs. Canonicalizing
exact-case semantic values per AST removes those within-AST duplicates without
using the JVM string pool or retaining values after their AST becomes
unreachable. Source text and comment text are excluded; structured documentation
annotation values remain part of the semantic census.

Generic child-list sizes on the same corpus are heavily weighted toward small
lists:

| Child count | Nodes |
|---|---:|
| 0 | 129,102 |
| 1 | 58,322 |
| 2 | 61,351 |
| 3 or more | 19,724 |

An experimental `SmallChildrenList` stored the first two children inline and
spilled into an `ArrayList` for larger nodes while retaining mutable `List`
behavior.

| Variant | Graph objects | Graph bytes | Bytes/node | Change from previous |
|---|---:|---:|---:|---:|
| Parser allocation and capacity pass | 1,541,414 | 63,144,240 | 235.17 | - |
| Per-AST semantic string canonicalization | 1,385,528 | 59,053,992 | 219.94 | -4,090,248 (-6.48%) |
| Inline child storage | 1,285,506 | 57,780,944 | 215.20 | -1,273,048 (-2.16%) |

Together these follow-up changes save 5,363,296 bytes (8.49%) over the preceding
checkpoint. The final graph is 34,732,688 bytes (37.54%) smaller than the
92,513,632-byte original baseline. Per-file node counts and type histograms
remain identical across all 656 files.

When every node's source text is materialized and retained, the follow-up graph
contains 1,465,528 objects and 69,743,232 bytes, or 259.75 bytes per node. This
is also 5,363,296 bytes below the preceding materialized-source checkpoint.

## Inline parser positions

Parser-created positions now store their four normal-range coordinates in one
`long` and their source indexes in a second `long` directly on `BoxNode`.
Coordinates outside the unsigned 16-bit compact range retain a regular
`Position` as a fallback. Caller-supplied positions remain stored by identity,
so public AST construction keeps its existing shared-mutation behavior.

`getPosition()` exposes a live, weak node-backed view for compact positions. The
view carries a fallback snapshot, does not retain the AST, serializes as a
standalone `Position`, and promotes safely when a coordinate leaves the compact
range. Internal comment association compares packed coordinates directly to
avoid allocating views in its hot path.

| Variant | Graph objects | Graph bytes | Bytes/node | Change from previous |
|---|---:|---:|---:|---:|
| Inline child storage | 1,285,506 | 57,780,944 | 215.20 | - |
| Inline parser positions | 1,028,644 | 52,438,344 | 195.30 | -5,342,600 (-9.25%) |

This removes all 256,863 retained `Position` objects from the corpus graph. The
single compact no-source sentinel means the net object reduction is 256,862.
The graph is now 40,075,288 bytes (43.32%) below the original 92,513,632-byte
baseline. The 656-file structural manifest remains byte-for-byte identical to
the preceding checkpoint.

With every node's source text materialized and retained, this graph contains
1,208,666 objects and 64,400,632 bytes, or 239.85 bytes per node. This is the
same 5,342,600-byte reduction from the preceding materialized-source graph.

## Trimmed standard child lists

The final implementation removes `SmallChildrenList` in favor of standard
`ArrayList` instances. Empty child lists remain the shared immutable `List.of()`
instance. Every completed `ParsingResult` recursively calls `trimToSize()` on
its mutable child lists, avoiding retained excess capacity without a custom
collection implementation.

| Variant | Graph objects | Graph bytes | Bytes/node | Change from previous |
|---|---:|---:|---:|---:|
| Inline positions with inline child storage | 1,028,644 | 52,438,344 | 195.30 | - |
| Inline positions with trimmed `ArrayList` children | 1,128,666 | 53,680,224 | 199.93 | +1,241,880 (+2.37%) |

The simpler representation costs 1.24 MB on the full corpus but remains
38,833,408 bytes (41.98%) below the original 92,513,632-byte baseline. It avoids
tuning the collection representation around one observed child-count
distribution while retaining the majority of the measured parser-memory gain.

With every node's source text materialized and retained, the final graph
contains 1,308,688 objects and 65,642,512 bytes, or 244.48 bytes per node.
