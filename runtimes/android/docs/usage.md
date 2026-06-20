# Usage

## Runtime configuration (`boxlang.json`)

The bundled `boxlang.json` (under `src/main/bx`) is loaded on boot. The runtime home is set
to an app-private directory (`filesDir/boxlang`) seeded from the APK's `bx/` payload on first
launch. Use the standard config keys (same as web/lambda):

| Key | Purpose |
|---|---|
| `mappings` | Path mappings (`/handlers`, `/views`, `/layouts`, `/models`, `/`) |
| `modulesDirectory` | Drop-in BoxLang modules folder(s) — auto-registered |
| `javaLibraryPaths` | Third-party JAR folder(s) |
| `customComponentsDirectory` | Custom component (tag) folder(s) |
| `modules` | Per-module settings |

## The `Application.bx` lifecycle on Android

Standard BoxLang listener methods fire on the same triggers as web/lambda:
`onApplicationStart`/`onApplicationEnd`, `onRequestStart`, `onRequest`, `onRequestEnd`,
`onError`, `onAbort`, `onSessionStart`/`onSessionEnd`.

**Android-specific hooks** are optional — define any of them and the runtime will call it:

```java
function onActivityCreate( event, savedState ) {}
function onActivityResume( event ) {}
function onActivityPause( event ) {}
function onActivityResult( requestCode, resultCode, data ) {}
function onPermissionResult( requestCode, permissions, grantResults ) {}
function onBackPressed( event ) { return true; }   // return false to consume
function onLowMemory() {}
function onConfigurationChanged( newConfig ) {}
```

## Choosing a UI track

- **WebView/MVC** — author `.bxm` views + layouts; handlers run first and set the view/layout.
  Zero Kotlin. Best for fast delivery and web-skill reuse.
- **Compose** — author a `UINode` tree in BoxLang; a thin Kotlin host renders native widgets.
  Best for native look & feel.

You can mix per screen.

## HTTP / JSON / async

BoxLang's batteries are available on device:

```java
var res = bx:http( url = "https://api.example.com/items", method = "GET" );
var data = jsonDeserialize( res.fileContent );
```

Add `<uses-permission android:name="android.permission.INTERNET" />` (the library manifest
already declares it). Prefer structured concurrency / `bx:thread` over heavy executor pools
on mobile.

## Android SDK interop

The entire Android SDK is reachable via Java interop:

```java
var toast = createObject( "java", "android.widget.Toast" );
// ... or inject the Activity/Context through a handler argument and call SDK APIs directly.
```

## Hot reload (dev)

Run the dev server, then edit `.bxm`/`.bx` and watch the app update without a reinstall
(WebView: sub-second re-render; Compose: dex push + recompose). Gated behind `boxlang.dev=true`;
never present in release builds. See the proposal's hot-reload section.

## Build & deploy

```bash
./gradlew :app:compileBoxLangAot      # AOT-compile bx sources
./gradlew :app:assembleDebug          # build the APK (AOT output dexed in)
./gradlew :app:installDebug           # install on emulator/device
./gradlew :app:connectedAndroidTest   # instrumented tests (needs an emulator)
```

> On-device execution depends on the **preloaded boxpiler** runtime item — see the proposal's
> "hard constraint" section for status.
