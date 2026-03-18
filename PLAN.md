# BoxLang Native Image & Performance Plan

**Goal:** Improve startup performance and lower the footprint of BoxLang distributables (CLI tools, BoxLings, etc.) — potentially via GraalVM Native Image.

---

## Background

BoxLang compiles source files to JVM bytecode at runtime using the ASM-based `asmboxpiler`. This dynamic `defineClass()` approach, combined with unbounded reflection in `DynamicInteropService` and custom `URLClassLoader`-based class loading (`DiskClassLoader`, `BoxLangClassLoader`), makes full GraalVM Native Image compilation **incompatible** with the current execution model.

GraalVM Native Image requires a "closed-world" assumption — all classes, reflection targets, and classloading must be known at build time. BoxLang breaks all three constraints by design.

---

## Phases

### Phase 1 — Zero-Code-Change Wins

**Effort:** Low | **Startup gain:** 40–60%

- [x] **AppCDS (Application Class-Data Sharing)**
  Bundle a pre-built `.jsa` class-data archive with all distributions.

  ```bash
  # Generate class list (run at packaging time)
  java -XX:DumpLoadedClassList=boxlang.classlist -jar boxlang.jar --version

  # Build the shared archive
  java -Xshare:dump \
    -XX:SharedClassListFile=boxlang.classlist \
    -XX:SharedArchiveFile=boxlang.jsa \
    -jar boxlang.jar

  # Use in distribution launch scripts
  java -Xshare:on -XX:SharedArchiveFile=boxlang.jsa -jar boxlang.jar "$@"
  ```

  - [x] Integrate `.jsa` creation into the `build.gradle` packaging task (`generateCdsClassList`, `generateCdsArchive`)
  - [x] Bundle the `.jsa` alongside the fat JAR in all distribution archives
  - [x] Update CLI launch scripts (`boxlang`, `boxlang.bat`) to use `-Xshare:on`

  **Measured results (Java 21.0.10, BoxLang 1.12.0-snapshot, 8 runs):**

  | Mode | Min | Avg | Max |
  |------|-----|-----|-----|
  | No AppCDS | 1923ms | 2023ms | 2100ms |
  | With AppCDS | 1196ms | 1387ms | 1632ms |
  | **Improvement** | | **31.4% faster** | |

- [ ] **Switch to GraalVM CE 21 as the bundled/recommended JRE**
  No code changes. GraalVM CE provides a better JIT (Graal JIT vs. C2) — improved throughput and faster warm-up for long-running programs and CLI tools.
  - Update CI/CD and distribution tooling to use GraalVM CE 21 build
  - Update `README.md` to recommend GraalVM CE as the runtime

---

### Phase 2 — JVM Daemon + Native Thin Launcher

**Effort:** Medium-High | **Perceived startup gain:** ~95% after first run

This is the approach used by Gradle Daemon, Buck, and Bazel for CLI tools with heavy JVM startup costs.

- [ ] **BoxLang Daemon**
  A persistent background JVM process that:
  - Accepts execution requests over a local socket (or Unix domain socket on macOS/Linux)
  - Keeps the runtime, class pool, and module system warm between invocations
  - Shuts down after a configurable idle timeout

- [ ] **Native Thin Launcher binary**
  A small GraalVM Native Image compiled binary (~5ms startup) that:
  - Parses CLI arguments (using the existing `CLIUtil` logic, extracted to a small standalone project)
  - Checks if the BoxLang daemon is running (PID file / socket probe)
  - If running → forwards command + streams stdout/stderr back
  - If not running → starts the daemon JVM, waits for readiness, then forwards

  This is the only part of the codebase that is a natural fit for `native-image` right now.

- [ ] **BoxLings integration**
  Once the daemon + launcher are implemented, `boxlang BoxLings.bx` should feel near-instant after the first run.

---

### Phase 3 — GraalVM Truffle Language (Long-term)

**Effort:** Very High | **Outcome:** Full native-image support, maximum peak performance

The architecturally correct path for native-image compatibility is to implement BoxLang as a [Truffle Language Interpreter](https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/). GraalPy, TruffleRuby, and Graal.js all use this approach.

**What this unlocks:**

- Full `native-image` compilation through Truffle's Substrate VM support
- Partial Evaluation → compiled-to-native-speed execution
- Polyglot interop with other GraalVM languages

**What it requires:**

- Replace `asmboxpiler` with a Truffle AST interpreter
  - The existing `compiler/ast/` hierarchy maps naturally to Truffle `Node` subclasses
  - `asmboxpiler` transformers become Truffle `@Specialization` operations
- Replace `DynamicInteropService` with Truffle's built-in Java interop (`InteropLibrary`)
- Replace `DiskClassLoader` / `BoxLangClassLoader` with Truffle `TruffleLanguage` context lifecycle
- Retain the ANTLR parsers — they feed into the new Truffle node tree instead of ASM

- [ ] **Spike:** Implement a minimal Truffle `TruffleLanguage` wrapper that can execute a single `.bxs` script (arithmetic + print). Validates the feasibility and infrastructure setup.
- [ ] **Design doc:** Map existing AST node types to Truffle Node equivalents.
- [ ] **Incremental migration:** Run `asmboxpiler` and Truffle interpreter in parallel, gate by a flag.
- [ ] **Full migration:** Replace `asmboxpiler` once Truffle path achieves feature parity.

---

## References

- [GraalVM Native Image docs](https://www.graalvm.org/latest/reference-manual/native-image/)
- [GraalVM Truffle Language Framework](https://www.graalvm.org/latest/graalvm-as-a-platform/language-implementation-framework/)
- [AppCDS — JEP 310](https://openjdk.org/jeps/310)
- [Gradle Daemon architecture](https://docs.gradle.org/current/userguide/gradle_daemon.html) — reference for Phase 3 daemon design
