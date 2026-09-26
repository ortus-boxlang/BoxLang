# BoxLang Code Coverage / Perf-Emission Design

Status: **IMPLEMENTED.** Core span collection (Pass A), mark emission (Pass B), the `CodeProfilerService` runtime, and the branching-span handling are all in place and covered by tests. Remaining work is consumer tooling (TestBox integration, SonarQube export, LSP display) in modules.

## The Feature
Instrument BoxLang bytecode (ASM boxpiler): at compile time, bake a per-file **blueprint** of every executable **span** into the generated class; at load, register it with the runtime; bytecode calls `mark(spanId)` so the runtime records per-span execution count + time. Data can drive:
- code coverage reporting (TestBox first integration; model parity with FusionReactor `cflpi`)
- perf monitoring (accumulated execution time per span = hot-code finding)

Service: `ortus.boxlang.runtime.services.CodeProfilerService` — an `IService` (extends `BaseService`), registered on `BoxRuntime` like the other services, exposed via `runtime.getCodeProfilerService()`. It exposes a static `mark(...)` that delegates to the singleton instance (so instrumented bytecode has a stable static call target). Flag: `Configuration.codeProfilerEnabled` (default false; zero instrumentation when off). ASM only for now — not concerned about Java boxpiler.

## Key Decisions/Takeaways

### 1. Core instrumentation, tools in modules
This is ALL CORE. The instrumentation, the `CodeProfilerService` service, and the data it produces live in core. The TOOLS that consume the data (reporters, LSP display, TestBox integration, SonarQube export) live in MODULES. The service/bytecode contract does NOT need to relocate/decouple for a module later — the module boundary is already at the consumer. Bytecode emits a STATIC call to `CodeProfilerService.mark(...)` — stable target.

### 2. Don't reinvent FR buggy-line workarounds
FR needed a lot of "bugginess overrides" (line 1 comments, closing braces, "no data for file" hacks in `CoverageGenerator.cfc`). We own the compiler, so we should NOT need them. Executable-ness should come from OUR probes, NOT from the `LineNumberTable` — `AsmHelper.translatePosition()` emits LineNumberNodes at both start AND end of nodes, so the line table has phantom "closing brace" lines. Probes (not line table) = source of truth.

### 3. The unit is the SPAN (atomic executable bit)
The fundamental unit is the **executable span** — a contiguous region of source that maps to bytecode that can run. Examples for `foo = bar ? baz : bum;`:
- `foo =`   (executable — runs when the assignment expression runs)
- `bar`     (executable — runs when evaluated; may be `bara || barb`, so not all of it always runs)
- `?`       (NOT executable)
- `baz`     (executable — runs when evaluated; may be complex/nested)
- `:`       (NOT executable)
- `bum`     (executable — runs when evaluated; may be complex/nested)
- `;`       (NOT executable)

Every executable region is an atomic span (no smaller executable piece); non-executable regions (whitespace, `?`, `:`, `;`, braces, `else`, comments) produce no execution and are NOT tracked as spans — the profiler only enumerates the executable ones, and treats everything else as NOT_EXECUTABLE.

### 4. The BLUEPRINT — defines the 3 states (CORE)
The service must know every EXECUTABLE span and its position — produced at COMPILE time and present regardless of whether any code ran. The consumer never infers gaps or file length.

**COVERED**        = an executable span was marked (count > 0)
**MISSED**         = an executable span was NOT marked (count == 0)
**NOT_EXECUTABLE** = any point not inside an executable span (whitespace, comments, braces, `else`, `?`, `:`, `;`, trailing blank lines, etc.) — absent from the blueprint

**How it gets in:** during compilation (`ASMBoxpiler.setupCoverage`, when `codeProfilerEnabled` is on), the boxpiler runs Pass A, builds the `Blueprint`, and registers it immediately via the service. Two registration entry points, both producing the same internal key string but with self-describing kind:
`registerBlueprintForFile( filePath, Blueprint )`   — key = normalized filesystem path
`registerBlueprintForSource( sourceHash, Blueprint )` — key = adhoc source hash (REPL/eval/test)

The `Blueprint` carries a `kind` (`FILE | SOURCE`) so generic reporting over all registered blueprints can tell them apart. The boxpiler computes the source hash for adhoc code (same MD5 it uses for `ClassInfo.forScript`); the test reuses that same helper.

`Blueprint` does **not** store the source text — the runtime never re-reads source for coverage (the reporter reads the file/hash itself), so storing it would be wasted memory.

`Blueprint` is **total for the file**:
- `totalLines` — the file's full line count (incl. trailing blank/comment lines)
- the ordered enumeration of EXECUTABLE spans: `{ startLine, startCol, endLine, endCol, executable=true }` — span ids are the array positions (source order)
- a per-line index of which executable spans touch each line (for `lineAt`)

Only **executable** spans are listed, and each gets an `id` (its index). Non-executable regions are simply absent.

### 5. The runtime is SPAN-ID based (mark becomes trivial)
Because every executable span is known in the blueprint, bytecode passes only its **span id**, not the positions:
```java
// fileId from registration; spanId indexes the blueprint's executable spans; the
// service pre-allocated a counter (count + totalNanos) for every executable span.
public static void mark( int fileId, int spanId )
```
- Hot path: `counters[fileId][spanId].count.incrementAndGet()` — a direct array index. No positions, no `pack()`, no hash, no map lookup.
- Timing: probe-charging keyed by span id; thread-local `lastSpanId`/`lastNano`.
- Correct by construction: `spanId` can never reference a non-executable span or a phantom; a bad id is caught by the id range.
- Non-executable spans have no id → never marked.

### 6. Line aggregation (derived from spans)
A line's data is derived from the spans that touch it:
- **line covered** = ANY executable span touching the line has `count > 0`
- **line count** = count of the FIRST/OUTERMOST executable span that covers the line (the statement's leading span, e.g. `foo =`). Well-defined per line.
- **line totalNanos** = SUM of `totalNanos` over ALL spans touching the line (disjoint via probe-charging → exact)

The multi-span line (`foo = bar ? baz : bum`) resolves cleanly: count from the leading span; time/covered from all touching spans.

### 7. Timing (probe-charging) — no double-count
Each `mark(spanId)` reads `System.nanoTime()`; the interval since the previous probe on that thread is charged to the span that was "current" (the one that opened the interval). Intervals are strictly disjoint → `sum over line = line total`, no double-count. Condition-eval time lands on the condition span.
- Tail time: `markEnd(fileId)` is appended at script-body end so the last span's interval is charged (no synthetic entry/exit span needed at script level).
- Open: on THROW/short-circuit mid-line, the pending interval is charged to whatever span opened it (may never close). Lean: accept & document — throwing isn't hot, misattribution bounded.

### 8. Branches are just spans
No statementId/kind needed. An `if`'s condition, each `&&`/`||` operand, each ternary operand, each branch body is its own executable span with its own counter. Short-circuiting means only evaluated sub-spans get marked → `foo || bar` where `foo` is true leaves `bar` MISSED. "Which branch ran" = "which branch's span has count > 0".

### 9. Perf / data volume
- DATA VOLUME: non-issue. Blueprint is a few bytes per span; runtime counters are a pre-allocated array (one `long` count + `long` nanos per executable span). No per-execution allocation.
- RUNTIME: `mark(spanId)` is a single array-index increment — the fastest possible hot path, and constant regardless of line layout.
- Real lever remains: when capture is OFF, mark returns immediately (volatile read).

## Implementation approach — TWO PASSES (collect then emit)

The ASM boxpiler runs two passes over the AST. This separates *span discovery* (Pass A, needs the whole tree) from *mark emission* (Pass B, happens during transform).

### Pass A — collect spans → build the blueprint
Walk the AST once and gather every EXECUTABLE region from node positions. Descend into closures, function bodies, ternary operands, `&&`/`||` operands — a statement is NOT assumed to be one span; a ternary/closure/`||` yields multiple spans. Span IDs are assigned in source (visit) order as the running-span walk emits them. Only EXECUTABLE spans are registered as `SpanDef`s; non-executable regions (whitespace, punctuation, braces, comments, trailing lines) are simply ABSENT — the runtime treats any point not inside an executable span as NOT_EXECUTABLE (`spanAt` returns null for it).

The blueprint does NOT store source text. `mark(fileId, spanId)` is the only runtime call.

### Pass B — emit marks during transform
The existing `AsmTranspiler.transform(BoxNode, ...)` is the central emission hook, but it does NOT compute spans — it LOOKS THEM UP:
`int spanId = transpiler.getSpanId( node.getPosition().start )`
- found → prepend `mark(fileId, spanId)` to the produced instructions
- not found → not a span start; skip

**Tie spans to nodes by source position, not node identity.** Pass A and Pass B derive from the same AST, so a node's `getPosition()` is stable. Pass A builds a `position → spanId` map (or sorted structure) keyed by start position; Pass B queries it. Bytecode emission order doesn't matter — each mark hits its own counter.

**Mark dedup (`claimSpanMark`).** Multiple AST nodes can share a span's start position (e.g. a statement and the call inside it). `Transpiler.claimSpanMark(spanId)` returns `true` only the FIRST time a span's mark is emitted per compilation, so exactly one mark instruction is baked per span — keeping runtime counts exact (a span can never double-count from duplicate marks at the same bytecode location).

**Mark placement:**
- **Central hook (`transform`):** statement-level nodes (e.g. `BoxExpressionStatement`). The common case — looks up its whole-expression span.
- **Specialized transformers** (ternary, `&&`/`||`, null-coalesce, function bodies, closures): must emit PER-OPERAND/PER-BODY marks, because only they know their inner short-circuit / body structure. These call `getSpanId` with their own operand/body position (not the statement position).
- **Closing probe (`markEnd`):** the script body appends `markEnd(fileId)` after its last statement. It closes the final span's probe-charging interval so the last span's self-time is charged (no synthetic entry/exit span needed at script level).

**Duplicated AST bodies (the `finally` case).** `BoxTryTransformer` compiles a `finally` body in THREE bytecode copies: inline-after-try, inline-after-catch, and the exceptional handler. Mark dedup would give the mark only to the first (inline) copy, so a throwing try body would execute the finally via the exceptional copy with NO mark (count=0). Fix: `Transpiler.snapshotEmittedMarks()` / `restoreEmittedMarks(Set)` — the transformer snapshots before the inline finally transform and restores after, so the catch-inline and exceptional copies re-claim (re-emit) the same marks. Since only one copy executes at runtime, counts stay exact.

**First vertical slice (minimal):** `BoxExpressionStatement` only (e.g. `2+2`). Pass A assigns one span to the expression statement; central hook prepends its mark; wire behind `codeProfilerEnabled`. **DONE** — plus the full branching-span handling (ternary, if/else, while/do/for/for-in, switch, try/catch/finally, assert) with strict per-span/per-line/per-timing tests.

**Expression-level partial-execution (all implemented + tested):**
- `BoxComparisonOperation` is its OWN node class (not a `BoxBinaryOperation`) — it gets the same left-continues/right-breaks treatment.
- `BoxParenthesis` is transparent: the running span threads through it so the inner expression's start is the boundary (Pass B claims the inner node, not the paren). Binary/concat/comparison/ternary break-points unwrap parens.
- `&&`/`||` (And/Or), Elvis `?:` — right operand breaks into its own span; short-circuit leaves it MISSED. The Elvis right is wrapped in a lazily-invoked producer lambda, so the eager right transform is SKIPPED for elvis (otherwise it would claim the mark into discarded instructions and the running lambda copy would be unmarked — a real bug found & fixed).

**UDF / function declarations (all implemented + tested):**
- `BoxFunctionDeclaration` gets a custom Pass A visit: the declaration SHELL (modifiers, name, parens, arg names/types, literal defaults) is one running span that runs at declaration (UDFs are hoisted). Arg declarations are NOT statements — the generic statement hook must not wrap them.
- Literal defaults evaluate inline at declaration → stay inside the shell span.
- NON-literal defaults compile to a lazy `defaultExpr_N` method (via `getDefaultExpression`); they break into their own span and their mark fires ONLY when the arg is omitted at call time.
- The function BODY statements run at invocation: each is its own span with marks inside the UDF invoker method; the body span's count = number of invocations.
- The UDF invoker method appends `markEnd(fileId)` after its last statement so the final body span's self-time is charged (mirrors the script body).

**Closures and lambdas (implemented + tested):**
- `BoxClosure` / `BoxLambda` get the same shell/default/body span model as UDFs via a shared `visitClosureLike` walk: creation shell (incl. literal defaults) runs when the closure/lambda is created; non-literal defaults break into their own lazy `defaultExpr_N` spans (marked only when the arg is omitted at call time); the single body statement (expression or block) runs at invocation with marks inside the `invokeClosure_N` / `invokeLambda_N` invoker method.
- Both invoker methods append `markEnd(fileId)` so the final body span's self-time is charged.

**Local / inner classes (implemented + tested):**
- A `BoxLocalClass` in a script is pre-compiled as a separate auxiliary JVM class (no bytecode at the declaration site — loaded lazily). From the USER's perspective its code is part of the container script, so its spans live in the OUTER script's blueprint (the child transpiler ADOPTS the parent's profiling context — same fileId + span registry — via `Transpiler.adoptProfilingContext`). No synthetic per-class blueprint keys leak to the data model.
- `visit(BoxClass)` (and `BoxLocalClass` delegating to it) walks the class body: class declaration text emits no span; static function bodies + `BoxStaticInitializer` body statements run at CLASS LOAD; non-static body statements run at INSTANTIATION (`_pseudoConstructor`); member function bodies run at INVOCATION — each opens its own span.
- Property DEFAULT values are tracked: literal defaults (inline in the properties map) run at CLASS LOAD; complex/non-literal defaults (array/struct literals, identifiers) are tracked as their own spans.
- `BoxClassTransformer` appends `markEnd(fileId)` to both `staticInitializer` and `_pseudoConstructor` so their final spans' self-times are charged.

## Environment/scope facts (recall for new session)
- `AsmTranspiler.transform(BoxNode, TransformerContext, ReturnValueContext)` (~line 886) is the CHOKEPOINT for PASS B emission. The central hook looks up a precomputed spanId; it does NOT compute spans.
- `DividerNode` prepended to statements with `ReturnValueContext.EMPTY`; `MethodSplitter` strips them as split points. Marks ride inside statement segments.
- `node.getPosition()` has start/end line+col; filePath already a transpiler property. The blueprint span list is derived from node positions during Pass A.
- `ConfigLoader`/`Configuration.process()` pattern to add `codeProfilerEnabled` (mirror `storeClassFilesOnDisk`).
- Testing: existing unit test `src/test/java/ortus/boxlang/runtime/services/CodeProfilerServiceTest.java` (service-only, registers blueprints + calls mark). Integration tests (bytecode→spans→mark end-to-end) live in `src/test/java/ortus/boxlang/compiler/CodeProfilerTest.java` — 84 tests covering expression/ternary/if/throw/binary-chain statements, all loop forms, switch, try/catch/finally, assert, `markEnd` trailing-time, loop timing accumulation, the finally mark fix, expression-level partial-execution (comparisons, short-circuit `&&`/`||`/Elvis, compound literals, spread, parens, unary/negate, null-safe access), UDF declarations (shell spans, literal + non-literal defaults, invocation-counted body spans, body first/last timing), closures/lambdas (shell/body spans, literal defaults, body timing, call-site overhead), local classes (static-at-load, body-at-instantiation, member-at-invocation, per-region timing — all under the OUTER script key), property defaults (literal, array/struct literal, identifier), and a real on-disk `.bxs` file (`src/test/resources/profiler/ProfilerSample.bxs`) executed via `executeTemplate` whose blueprint is keyed by the normalized absolute file path (`registerBlueprintForFile`).
- `AsmHelper.transformBodyExpressionsFromScript` and `transpile(BoxScript)` are where body statements flow; the central hook in `transform()` covers them.

## Things still to solve / discuss
- Throw / short-circuit mid-line: the pending nano interval is charged to whatever span opened it (may never close). Accepted & documented — throwing isn't hot, misattribution bounded.
- ~~`mark(fileId, spanId)` trailing-time~~ **RESOLVED**: `markEnd(fileId)` appended at script-body end closes the final interval.
- ~~Whether line-aggregate should live in the service~~ **RESOLVED**: service-computed (`lineAt`, `fileLines`).
- Consumer tooling (TestBox coverage generator, SonarQube exporter, LSP per-expression display) lives in modules and is not yet written.

## Implemented behavioral notes (from tests)
- `spanAt(file, line, col)` at a shared boundary prefers the span that STARTS at that column (e.g. col 34 of `1 : 2` is the start of `2`, not the end of `1 : `). This disambiguation was added after the ternary-boundary test exposed it.
- Loop bodies accumulate per-iteration counts: a `while` body that runs 3× reports count=3 on its body span; the header reports 1.
- Nested loops multiply: inner body of a 3× outer × 2× inner loop reports count=6.
- Zero-iteration loops (false condition / empty collection) report count=0 on the body span.
- A throwing `finally` body reports count=1 thanks to the snapshot/restore mark fix (see Pass B).
- Passing asserts count 1; a failing assert's span counts 0 (the throw preempts its mark).
- Comparisons (`==`, `<`, etc.) split into left/right spans just like binary ops.
- Short-circuit `&&`/`||`/Elvis `?:` leave the untaken right operand MISSED (count 0); when the right runs (e.g. elvis with a null left), it counts 1.
- Compound literals (`[ ... ]`, `{ ... }`, spread) are one span when all elements are safe; a throwing middle element leaves trailing elements MISSED.
- Parens never create their own span — the inner expression's start is the boundary.
- `spanAt(line, col)` only treats a span as a boundary-start if the span's START LINE is the queried line — a multi-line span (e.g. a function shell passing through a body line) doesn't claim columns on lines it merely crosses.
- UDF declaration shells count 1 at declaration; non-literal arg defaults count 1 only when the arg is omitted at call time; body spans count per-invocation.

---

# API Specification

Two API surfaces:
1. **Instrumentation-facing** — the static methods compiled bytecode calls (perf-critical).
2. **Tool-facing** — how a consumer reads the data (cold path).

## 0. The core record

```java
Span             // one executable unit from the blueprint
  int     id        // 0..N-1, indexes the file's executable span array
  int     startLine  // 1-based
  int     startCol   // 0-based
  int     endLine    // 1-based
  int     endCol     // 0-based
  SpanStats stats    // runtime counter for this span
```

```java
SpanStats          // mutated by mark(); read by tools
  long   count       // raw execution count (EXACT, always incremented)
  long   totalNanos  // SELF time charged to this span (disjoint, sum-able)
```

## 1. Instrumentation-facing API (static, hot path)

**The ONLY method compiled into user code is `mark(fileId, spanId)`.** Positions live in the blueprint, not the bytecode.

```java
// fileId is returned by registerBlueprintForFile/ForSource; spanId indexes that
// file's executable spans (pre-allocated counter array).
public static void mark( int fileId, int spanId )
```
Behavior (in priority order):
1. `volatile boolean active` == false → return immediately.
2. Read `System.nanoTime()`.
3. Charge `now - lastNano[thread]` to the PREVIOUS span's `totalNanos` (probe-charging). Skip if no previous span / negative delta.
4. Set `lastNano[thread] = now`.
5. `counters[fileId][spanId].count.incrementAndGet()` — ALWAYS, raw exact.

```java
// Closes the probe-charging interval after the script body's last statement so
// the final span's self-time is charged. Emitted once at the end of the body.
public static void markEnd( int fileId )
```

Notes:
- Two ints; direct array index into that file's span/counter array. No positions, no map, no allocation, no lock.
- File is the compact `fileId` returned at registration.
- Thread-local `lastNano`/`lastSpanId` avoid cross-thread contention.
- `resetThreadClock()` clears the current thread's probe state (used after thread pool reuse so stale deltas aren't charged).

### 1.1 Function-entry / function-exit
At script level the body appends `markEnd(fileId)` (see above) so the probe-charging chain stays unbroken. Function bodies follow the same pattern within their own compiled segments.

### 1.2 Lifecycle / registration (non-hot)
```java
// Run-time capture on/off. When OFF, mark() is near-no-op.
public static void setActive( boolean active );
public static boolean isActive();

// Clear all collected data.
public static void reset();

// THE critical registration: the file's blueprint. Pre-allocates the counter
// array for every executable span. Called on first load of the compiled class.
public static int registerBlueprintForFile( String filePath, Blueprint blueprint );
public static int registerBlueprintForSource( String sourceHash, Blueprint blueprint );

// List every registered blueprint (self-describing via Blueprint.kind).
public static Map<String, Blueprint> trackedBlueprints();
```

## 2. Tool-facing API (cold path)

All static; return immutable snapshots.

### 2.1 Query a span ("did THIS bit of code run?")
```java
// The executable span containing (line, col), resolved via the blueprint's
// ordered/containment structure. Null if the point is in no executable span.
public static Span spanAt( String filePath, int line, int col );

// The executable span by id.
public static Span span( String filePath, int spanId );

// All executable spans touching the given line.
public static List<Span> spansOnLine( String filePath, int line );
```
A span's 3-state status (COVERED / MISSED / NOT_EXECUTABLE) comes from blueprint membership + `stats.count` (see §4 "The BLUEPRINT" above). Non-executable points return null from `spanAt` (they're NOT_EXECUTABLE, not missed). At a column that is simultaneously one span's end and another's start, `spanAt` prefers the span that STARTS there (boundary disambiguation).

### 2.2 Line-level aggregation
```java
public static LineCoverage lineAt( String filePath, int line );
public static Map<Integer, LineCoverage> fileLines( String filePath );  // FR getLineMetrics equivalent
```
```java
LineCoverage
  int    line;
  boolean covered;    // any executable span touching this line has count > 0
  long   count;       // count of the FIRST/OUTERMOST executable span covering the line
  long   totalNanos;  // SUM of totalNanos over all spans touching the line (disjoint)
```

### 2.3 File-wide / capture-wide export
```java
public static List<Span> fileSpans( String filePath );        // blueprint order
public static Map<String, Blueprint> trackedBlueprints();     // every registered blueprint (kind: FILE | SOURCE)
```

### 2.4 Capture lifecycle (FR swap parity)
```java
public static void setActive( boolean active );   // capture on/off
public static void reset();                        // clear all collected data
// Consumers compose these two for a capture session; no convenience wrapper in core.
```

## 3. Open decisions still to finalize
1. **File keying.** RESOLVED: literal `String` key. FILE blueprints normalize the path (canonical case + `Path.normalize()`); SOURCE blueprints use the adhoc source hash as-is (no path normalization).
2. **Column basis.** RESOLVED: 0-based (default: parser `Position`).
3. **`count` exactness.** RESOLVED: always incremented (raw exact).
4. **Mid-line throw / short-circuit interval.** Lean: accept & document — charged to the span that opened it, bounded.
5. **Line-aggregate placement.** RESOLVED: service-computed (`lineAt`, `fileLines`).
6. **How `mark` binds to a file.** RESOLVED: `mark(fileId, spanId)` — the file is the compact id returned at registration (`registerBlueprintForFile`/`ForSource`).

## 4. Tool modules (per §1)
The instrumentation + `CodeProfilerService` + produced data are ALL CORE. Consumer tools (TestBox coverage generator, SonarQube exporter, LSP per-expression display) are MODULES that read the §2 API. Core ships no tooling; it just captures and serves the data.