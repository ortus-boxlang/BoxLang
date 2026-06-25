# API Reference

## Runtime

### `AndroidBoxRuntime`
Singleton entry point. `boot(Context)` boots the runtime (AOT/NoOp mode), seeds the app home
from APK assets, loads `boxlang.json`, and builds the MVC dispatcher. `getInstance()`,
`getRuntime()`, `getAppHome()`, `getRoutingService()`, `getDispatcher()`, `shutdown()`.

### `BoxAndroidApplication`
Generic `android.app.Application`. Declared in the app manifest's `android:name`. Boots the
runtime on `onCreate`. No subclass required.

### `BoxActivity`
Generic, config-driven `Activity` (100% Java). Declared directly in the manifest. Hosts the
WebView and forwards every Android lifecycle callback to the matching `Application.bx` hook.
- `navigate( path, method, params )` — dispatch a route into the in-process runtime.

## MVC front controller (`ortus.boxlang.runtime.android.mvc`)

### `Router` / `Route`
Fluent route table + resolution.
- `route(pattern)`, `get(pattern)`, `post(pattern)` → `RouteBuilder`.
- `RouteBuilder.withName(name)`, `.withMethods(...)`, `.to("Handler.action")`.
- `setDefaultEvent("Main.index")` — root (`/`) mapping.
- `resolve(path, method)` → `RouteMatch` (default event → explicit → convention).
- Path params: `/items/:id` → `{ id }`. Convention: `/users/save` → `Users.save`,
  `/users` → `Users.index`.

### `RoutingService` (IService)
Owns the `Router`. Registered like any BoxLang service.

### `MVCEvent` — the `event` object
- Collection: `getCollection()` / `getRC()`, `getValue(k[,default])`, `setValue(k,v)`,
  `valueExists(k)`.
- Rendering: `setView(name)`, `getView()`, `setLayout(name)`, `getLayout()`, `noLayout()`.
- Redirect: `relocate(target)`, `isRelocating()`, `getRelocateTarget()`.
- `getHTTPMethod()`, `getCurrentEvent()`.

> There is no flash/session scope — this is a single in-process app. To carry data across a
> `relocate()`, append it to the query string (e.g. `relocate("/items?notice=...")`); the
> dispatcher parses query strings into `rc` automatically.

### `MVCDispatcher`
`dispatch(context, path, method, params)` → `DispatchResult`. Flow: parse query string →
resolve route → build `rc` → **run handler action first** → relocate or render
view-in-layout. The action receives `event`, `rc`, and every `rc` entry as a named argument.

### `ViewRenderer`
`render(context, event)` renders the view (under `viewsRoot`) and wraps it in the layout
(under `layoutsRoot`), exposing `event` and `rc` to templates and the view markup as
`renderedView` to the layout. `renderView(...)`, `renderTemplate(context, absPath)`.

## Starter-template layout

```
src/main/bx/
├─ boxlang.json        # runtime config (loaded on boot)
├─ Application.bx      # lifecycle + configureRouter()
├─ handlers/           # Main.bx (index), ...
├─ views/  layouts/    # .bxm templates (WebView track)
├─ models/             # app services/domain
├─ modules/            # drop-in BoxLang modules (auto-registered)
└─ lib/                # third-party JARs
```

## AOT Gradle task

Three chained tasks turn `src/main/bx` into dexed bytecode, wired ahead of AGP's dexing so
they run automatically on `assembleDebug`/`assembleRelease`:

1. `compileBoxLangAot` — `ortus.boxlang.compiler.BXCompiler --source src/main/bx --target
   <out>` compiles `.bx`/`.bxm` to the BoxLang AOT class container.
2. `extractBoxLangClasses` — unpacks the containers into standard `.class` files.
3. `jarBoxLangAot` — packages those `.class` files into `boxlang-aot.jar`, declared on the
   app's `implementation` classpath so AGP's own **D8** dexes the `boxgenerated.*` classes
   into the APK (no manual `d8`/dex-merge).

`stageBoxApp` mirrors `src/main/bx` into the APK assets for on-device reads.

> **Slim APK:** on device BoxLang runs AOT/NoOp and never parses source, so the `:runtimes:android`
> module excludes the parser toolchain (ANTLR runtime + grammars, JavaParser, its Guava/Javassist)
> from the dexed runtime. The matching standalone artifact is `boxlang-noop-*.jar`, which excludes
> the same families.

## R8 / ProGuard

`consumer-rules.pro` (auto-applied to consumers) keeps `ortus.boxlang.**`, `boxgenerated.**`,
ServiceLoader providers, annotations, and the `@JavascriptInterface` bridge. See the file for
the full set.
