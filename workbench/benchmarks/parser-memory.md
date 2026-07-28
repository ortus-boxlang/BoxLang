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
analysis in JDK Mission Control.

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

| Metric | Baseline | Optimized | Change |
|---|---:|---:|---:|
| AST nodes | 268,499 | 268,499 | 0 |
| Graph objects | 2,615,793 | 1,541,414 | -1,074,379 |
| Graph bytes | 92,513,632 | 67,665,008 | -24,848,624 (-26.86%) |
| Bytes per node | 344.56 | 252.01 | -92.55 |

In the worst-case lazy-source scenario, retaining every node's resolved source
string increases the optimized graph to 1,721,436 objects and 79,627,296 bytes,
or 296.56 bytes per node. This remains 12,886,336 bytes below the baseline.

A representative optimized JFR recording is written to
`build/parser-memory/parser-memory.jfr` when the benchmark runs with `--jfr`.
