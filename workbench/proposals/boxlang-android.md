# BoxLang on Android — Architecture, Strategy & Limitations

> Status: **Foundation + AOT mechanism + class-loader factory landed.** Portable MVC core,
> runtime glue, samples, docs, and the AOT class pipeline (`BoxClassExtractor` +
> `PreloadedClassLoader` + `PreloadedBoxpiler`) are JVM-tested. The **pluggable class-loader
> factory** (runtime loader + per-module loaders) is **merged into core** (PR #575); the
> Android side wires it via `AndroidClassLoaderFactory` + `AndroidModuleClassLoader`. The one
> remaining **core hook** is wiring the preloaded loader into `ClassInfo` for AOT class
> resolution; see [The hard constraint](#the-hard-constraint).

## 1. Why

BoxLang is a dynamic JVM language with a batteries-included runtime (async, HTTP, JSON,
caching, scheduling), 100% Java interop, and one programming model across web, serverless,
and CLI. Android is a JVM-family target conspicuously missing from that list. The goal is to
let people **build Android apps in BoxLang** — the role Kotlin plays today — and to do it
*better* where BoxLang is strong: dynamic ergonomics, a famous templating engine, the entire
Android SDK available for free via interop, and "learn once, deploy anywhere."

## 2. Execution model

```
.bx/.bxm ──(BXCompiler @ build)──▶ JVM bytecode ──(D8/R8 via AGP)──▶ DEX in APK
                                                          │
                                                          ▼
                                  Preloaded boxpiler on device (resolve only, NO defineClass)
```

BoxLang already compiles ahead-of-time via `ortus.boxlang.compiler.BXCompiler`. Android's
contribution is (a) wiring that compile step into the Android Gradle build so D8/R8 dexes the
output into the APK, and (b) a device-side boxpiler that **resolves already-dexed classes**
rather than defining new ones.

## 3. The hard constraint

Android's ART runtime **cannot `defineClass()` arbitrary JVM bytecode at runtime**, and
`URLClassLoader`/`DiskClassLoader` (which BoxLang's dynamic loaders extend) do not exist on
Android. BoxLang's default model — compile to bytecode in memory and `defineClass` it — is
therefore unavailable on device.

The existing `NoOpBoxpiler` is "load-only" but still calls
`classInfo.getClassLoader().defineClasses(...)`, and `getClassLoader()` returns a
`DiskClassLoader` which (a) `extends URLClassLoader` — **absent on Android** — and (b)
ultimately calls `defineClass(byte[])` on JVM bytecode — **rejected by ART**. So
**`NoOpBoxpiler` as-is does not run on Android.**

The insight: `ClassInfo.getDiskClass()` does `getClassLoader().loadClass(fqn)`, and a normal
class loader delegates to its **parent first**. If the `boxgenerated.*` classes are already
dexed into the APK (loaded by the app class loader), they resolve by delegation with **no
`defineClass` at all**.

**What is now built and JVM-tested** (`:runtimes:android-mvc`, package `…android.aot`):

1. **`BoxClassExtractor`** — unpacks the `BXCompiler` container (magic + name + length-prefixed
   entries) into individual standard `.class` files. *Test:* compile a real `.bx` → extract →
   load every class with a vanilla `URLClassLoader`. This is exactly what D8/R8 dexes.
2. **`PreloadedClassLoader`** — Android-safe (`extends ClassLoader`, **not** `URLClassLoader`);
   resolves classes by parent-first delegation and **never** defines from bytecode (its
   `findClass` fails loudly). *Test:* it resolves an extracted class via the parent and the
   resolved class's loader is the parent (proving no local define).
3. **`PreloadedBoxpiler`** — an `IBoxpiler` (extends `Boxpiler`) whose `compileClassInfo` never
   compiles/defines; ServiceLoader-registered **only** in the Android distribution.

**The one remaining core hook:** make `ClassInfo.getClassLoader()` return the
`PreloadedClassLoader` (app loader) under the Android boxpiler instead of hard-coding
`DiskClassLoader`. This is a small, contained change in core (`ClassInfo` /
`Boxpiler`), best done with the ability to run on a device/emulator. It also assumes the
**compile-time and runtime FQNs are identical** — BoxLang already generates deterministic
`boxgenerated.*` names, but `DiskClassLoader.defineClasses` currently *renames* classes at
load time, so the Android path must skip renaming and rely on stable names.

The build-side AOT is wired in both samples: `compileBoxLangAot` (→ containers) →
`extractBoxLangClasses` (→ dexable `.class`).

## 4. What shipped in this pass

- **Portable MVC core** (`:runtimes:android-mvc`, pure JVM, **29+ unit/integration tests**):
  `Router`/`Route` (path params, verb constraints, named routes, convention + default event),
  `RoutingService` (IService), `MVCEvent` (the `rc` request collection, `setView`/`setLayout`,
  `relocate`), `FlashScope` (one-hop persistence), `ViewRenderer` (render `.bxm` → HTML and
  wrap in a layout), and the `MVCDispatcher` front controller (handler-first → render). Plus
  the Compose `UINode`/`UI` UI-tree model.
- **AOT class pipeline** (`:runtimes:android-mvc`, package `…android.aot`, JVM-tested):
  `BoxClassExtractor`, `PreloadedClassLoader`, `PreloadedBoxpiler` (see §3), plus
  `ModuleArchiver` + `AndroidModuleClassLoader` for per-module DEX isolation (see §5b).
- **Android runtime glue** (`:runtimes:android`, AGP library, SDK-gated): `AndroidBoxRuntime`
  (boot + asset seeding + config), generic manifest-declared `BoxAndroidApplication` /
  `BoxActivity`, `AndroidLifecycleDispatcher` (optional `Application.bx` Android hooks),
  `BoxWebViewRenderer` (in-process routing + JS bridge), `ComposeTreeRenderer` /
  `ComposeBridge` (Kotlin).
- **Two samples / starter templates**: WebView (zero Kotlin) and Compose.
- **Docs** (`runtimes/android/docs/`) and an **agent skill**.

## 5. Two UI tracks

### Track 1 — Compose-interop DSL
BoxLang authors an immutable UI **tree** (`UINode` via the `UI` DSL); a generic Kotlin/Compose
renderer walks it and emits real Compose widgets, wiring node closures back to the runtime and
binding state for recomposition. Compose needs its compiler plugin, so this track carries a
small amount of Kotlin (the renderer + a thin host); app logic stays in `.bx`.

### Track 2 — WebView + BoxLang templating (front-controller MVC)
Plays to BoxLang's templating strength and needs no Kotlin/Compose toolchain. A classic
ColdBox flow: route → **handler action runs first** → populates `rc` and chooses
`view`+`layout` (or `relocate`) → framework renders the view inside the layout → HTML loads
into a `WebView`. Forms and links are captured **in-process** (JS bridge for POST, URL
interception for GET/links) and dispatched as synthetic BoxLang requests — **no web server,
no socket**.

## 5b. BoxLang modules — per-module DEX isolation

BoxLang modules use a class-loader hierarchy for isolation: `ModuleService` gives each module
its own `DynamicClassLoader` (a `URLClassLoader`) parented to the runtime loader, over the
module directory + its `libs/*.jar`. On Android that breaks twice (no `URLClassLoader`; no
runtime `defineClass`). The Android strategy **preserves the same isolation model** using the
one runtime class-loading mechanism ART *does* allow: **DEX loading** via `DexClassLoader`
(API 26+).

**Build (per module):**
1. AOT-compile the module's `.bx` (`ModuleConfig.bx`, `bifs/`, `components/`, `models/`) →
   `.class` (`BXCompiler` + `BoxClassExtractor`).
2. Package those classes + the module's resources — `META-INF/services`, descriptor,
   templates, `public/` — and its `libs/*.jar` contents into one archive (`ModuleArchiver`).
3. Run `d8` over that archive → `assets/modules/<name>.jar` containing `classes.dex` **plus**
   the retained resources.

**Runtime (per module):** `AndroidModuleClassLoader extends DexClassLoader`, over
`modules/<name>.jar`, parented to the runtime loader. This reproduces the desktop model
exactly:
- **Isolation** — each module has its own loader; module A's lib version can't clash with B's.
- **Hierarchy** — parent = runtime, so modules see core but not each other.
- **ServiceLoader** — the archive carries `META-INF/services`, so
  `ServiceLoader.load( BIF.class, moduleLoader )` still discovers the module's providers. (This
  is why each module ships a *jar-with-dex*, not a bare `.dex` — a bare dex can't carry the
  service resources `ServiceLoader` needs.)

**Built + JVM-tested now** (`…android.aot`): `ModuleArchiver` (packages a module into one
archive) and `ModuleAOTTest` — compiles a fixture `ModuleConfig.bx`, packages it with its
`META-INF/services`, and proves via an isolated loader (the JVM stand-in for `DexClassLoader`)
that the module class loads in its own loader and its service resources are discoverable.
`AndroidModuleClassLoader` (the `DexClassLoader` wrapper) ships in `:runtimes:android`.

**The core seam is merged** (PR #575): a single `IClassLoaderFactory` governs both the
runtime loader and each module loader, installed before boot via
`BoxRuntime.setClassLoaderFactory(...)`. The Android side supplies it as
`AndroidClassLoaderFactory` — the runtime loader is the app class loader and each module a
`DexClassLoader` over its archive — so `ModuleService`/`ModuleRecord` are reused unchanged.
What remains is build-side only: the `d8`-per-module Gradle step that produces each
`modules/<name>.jar`.

## 6. Application.bx lifecycle

Bootstraps through the same application-listener contract as web/lambda
(`BaseApplicationListener` / `ApplicationClassListener`): standard
`onApplicationStart/End`, `onRequestStart`, `onRequest`, `onRequestEnd`, `onError`, etc.,
**plus** optional Android hooks fired by convention when defined: `onActivityCreate`,
`onActivityStart/Resume/Pause/Stop/Destroy`, `onActivityResult`, `onPermissionResult`,
`onBackPressed`, `onLowMemory`, `onConfigurationChanged`.

## 7. Limitations (documented, not fought)

- **No on-device runtime compilation / `eval` of new classes.** AOT only (see §3). Dynamic
  features that depend on runtime codegen (runtime-defined classes, `createDynamicProxy` of
  *new* types) are unavailable on device; closures/dynamic dispatch over *existing* compiled
  code work.
- **Services disabled/restricted on Android:** `WatcherService` (NIO `WatchService`),
  file-watch `SchedulerService`, heavy `AsyncService` pools, JDBC/Hikari `DatasourceService`
  (prefer SQLite/Room). `AndroidRuntimeConfig` should disable these by default.
- **Reflection / `Proxy` / `MethodHandles`** work on ART but require R8 `-keep` rules
  (provided in `consumer-rules.pro`); some `java.lang.invoke` paths need auditing.
- **DEX/method-count budget:** start from the slim NoOp distribution minus
  JavaParser/JavaBoxpiler; enable R8 shrinking + multidex.
- **Filesystem:** no `~/.boxlang` home — use `Context.getFilesDir()/getCacheDir()`; config &
  precompiled classes ship in the APK and are seeded to app-private storage on first run.

## 8. Hot reload (dev only)

ART forbids defining raw bytecode but **permits loading DEX** from app-private storage via
`InMemoryDexClassLoader` (API 26+). Dev mode: a `BoxDevServer` on the dev machine watches
`src/main/bx`; on change it (WebView) re-renders the `.bxm` to fresh HTML and reloads the
WebView sub-second, or (Compose) recompiles the changed `.bx` → `.class` → `d8` → `.dex`,
`adb push`es it, and the dev runtime reloads via `InMemoryDexClassLoader` and re-renders —
React-Native-style Fast Refresh. Gated behind `boxlang.dev=true`; never in release builds.

## 9. Roadmap

- **`ClassInfo` loader hook** (the one remaining core change, §3) so `PreloadedBoxpiler`
  is wired on device; verify on an emulator. (`BoxClassExtractor` + `PreloadedClassLoader`
  + `PreloadedBoxpiler` and the build-side AOT are done.)
- **Class-loader factory: DONE** — merged into core (PR #575) and wired on Android via
  `AndroidClassLoaderFactory` + `AndroidModuleClassLoader`. Remaining: the `d8`-per-module
  Gradle step that builds each `modules/<name>.jar`.
- `AndroidRuntimeConfig` service-disable defaults; method-count/R8 tuning.
- WireBox-lite DI, constraint validation, REST/JSON rendering, security guards, i18n
  (the framework roadmap deferred from the MVC core).
- Full Material widget catalog for Track 1; native "BoxUI" renderer.
- Standalone `ortus-boxlang/boxlang-android` repo + Maven-Central `.aar` publishing.
- Instrumented Espresso/Compose + UIAutomator tests on CI emulators.
