# BoxLang on Android — Architecture, Strategy & Limitations

> Status: **Foundation landed** (portable MVC core + runtime glue + samples + docs, JVM-tested).
> On-device APK execution depends on one remaining runtime item — the **preloaded boxpiler**
> — described under [The hard constraint](#the-hard-constraint).

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
`classInfo.getClassLoader().defineClasses(...)` from a precompiled byte container — which ART
rejects. So **`NoOpBoxpiler` as-is does not run on Android.**

**The fix (critical-path runtime item):** a new **`PreloadedBoxpiler`** (a.k.a.
`DexBoxpiler`) that, given a BoxLang FQN, returns the class via `Class.forName(fqn)` /
the app `ClassLoader` — because the class is *already dexed into the APK and loaded by the
system class loader*. No `defineClass`, no runtime codegen. This requires:

1. BXCompiler to emit **standard `.class` files** (it already produces standard class bytes;
   they just need to be written individually rather than concatenated into the BoxLang
   container) so D8 can consume them.
2. A stable, deterministic FQN scheme shared between BXCompiler output and the device
   resolver (BoxLang already generates deterministic `boxgenerated.*` names).
3. The `PreloadedBoxpiler` registered via ServiceLoader as the only boxpiler in the Android
   distribution.

Everything else in this proposal is implemented and JVM-tested; this is the piece that turns
a buildable module set into a running APK.

## 4. What shipped in this pass

- **Portable MVC core** (`:runtimes:android-mvc`, pure JVM, **29+ unit/integration tests**):
  `Router`/`Route` (path params, verb constraints, named routes, convention + default event),
  `RoutingService` (IService), `MVCEvent` (the `rc` request collection, `setView`/`setLayout`,
  `relocate`), `FlashScope` (one-hop persistence), `ViewRenderer` (render `.bxm` → HTML and
  wrap in a layout), and the `MVCDispatcher` front controller (handler-first → render). Plus
  the Compose `UINode`/`UI` UI-tree model.
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

- **`PreloadedBoxpiler`** (critical path, §3) + the BXCompiler `.class`-emit/dex packaging.
- `AndroidRuntimeConfig` service-disable defaults; method-count/R8 tuning.
- WireBox-lite DI, constraint validation, REST/JSON rendering, security guards, i18n
  (the framework roadmap deferred from the MVC core).
- Full Material widget catalog for Track 1; native "BoxUI" renderer.
- Standalone `ortus-boxlang/boxlang-android` repo + Maven-Central `.aar` publishing.
- Instrumented Espresso/Compose + UIAutomator tests on CI emulators.
