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
Generic, config-driven `Activity`. Declared directly in the manifest. Hosts the WebView track
and forwards every Android lifecycle callback to the matching `Application.bx` hook.
- `navigate( path, method, params )` — dispatch a route in the WebView track.
- `setBoxContent( uiTree )` — render the Compose track from a BoxLang UI tree/closure.

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
Owns the `Router` + per-runtime `FlashScope`. Registered like any BoxLang service.

### `MVCEvent` — the `event` object
- Collection: `getCollection()` / `getRC()`, `getValue(k[,default])`, `setValue(k,v)`,
  `valueExists(k)`.
- Rendering: `setView(name)`, `getView()`, `setLayout(name)`, `getLayout()`, `noLayout()`.
- Redirect: `relocate(target)`, `isRelocating()`, `getRelocateTarget()`.
- `getFlash()`, `getHTTPMethod()`, `getCurrentEvent()`.

### `FlashScope`
One-hop persistence: `put(k,v)` (stages for next request), `get(k[,default])`, `exists(k)`,
`keep()`, `clear()`. The dispatcher calls `persist()` at the start of each request.

### `MVCDispatcher`
`dispatch(context, path, method, params)` → `DispatchResult`. Flow: resolve route → build
`rc` → **run handler action first** → relocate or render view-in-layout. The action receives
`event`, `rc`, `flash`, and every `rc` entry as a named argument.

### `ViewRenderer`
`render(context, event)` renders the view (under `viewsRoot`) and wraps it in the layout
(under `layoutsRoot`), exposing `event`, `rc`, `flash` to templates and the view markup as
`renderedView` to the layout. `renderView(...)`, `renderTemplate(context, absPath)`.

## Compose UI tree (`ortus.boxlang.runtime.android.ui`)

### `UI` (factory DSL)
`column()`, `row()`, `box()`, `text(value)`, `button(label)`, `textField(value)`, `spacer(size)`.

### `UINode`
`prop(name, value)`/`getProp(name)`, `on(event, closure)`/`getHandler(event)`,
`child(node)`/`children(...)`, `getType()`, `getChildren()`. Node types map 1:1 to Compose
widgets in `ComposeTreeRenderer`. Events fire BoxLang closures; call
`ComposeBridge$RenderState.requestRender()` to recompose.

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

`compileBoxLangAot` runs `ortus.boxlang.compiler.BXCompiler --source src/main/bx --target
<out>`; the bytecode is dexed into the APK by D8/R8. `stageBoxApp` mirrors `src/main/bx` into
the APK assets for on-device reads.

## R8 / ProGuard

`consumer-rules.pro` (auto-applied to consumers) keeps `ortus.boxlang.**`, `boxgenerated.**`,
ServiceLoader providers, annotations, and the `@JavascriptInterface` bridge. See the file for
the full set.
