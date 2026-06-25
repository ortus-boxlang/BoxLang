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

## The UI — WebView + templating

Author `.bxm` views + layouts; handlers run first and set the view/layout, the framework
renders the view inside the layout and loads the HTML into a `WebView`. **Zero Kotlin — the
app is 100% BoxLang.** (A pure-Java native-widget renderer is a roadmap option; there is no
Compose/Kotlin track.)

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

## BoxLang modules

Drop modules under `src/main/bx/modules/<name>` (auto-registered via `modulesDirectory`).
On Android each module keeps its **own isolated class loader** — the desktop model preserved
via per-module DEX:

- **Build:** each module's `.bx` is AOT-compiled + extracted, then packaged with its
  `libs/*.jar` and resources (`META-INF/services`, descriptor, templates) and `d8`-converted to
  `assets/modules/<name>.jar` (a `classes.dex` + resources archive).
- **Runtime:** an `AndroidModuleClassLoader` (a `DexClassLoader`) loads each module archive,
  parented to the runtime loader — giving isolation, hierarchy, and working `ServiceLoader`
  discovery of the module's BIFs/components/interceptors.

Pure-`.bx` modules just work; modules carrying Java libs are dexed in at build time. See the
proposal's "BoxLang modules" section for the full design and the remaining device-wiring hook.

## Hot reload (dev)

Run the dev server, then edit `.bxm`/`.bx` and watch the app update without a reinstall
(`.bxm` edits: sub-second re-render; `.bx` class edits: dex push + reload). Gated behind `boxlang.dev=true`;
never present in release builds. See the proposal's hot-reload section.

## Toolchain (verified)

The Android modules build with:

- **Android Gradle Plugin 9.2.1** on **Gradle 9.5.1**, **JDK 21**
- **compileSdk/targetSdk 35**, **minSdk 26**, build-tools 34
- `android.useAndroidX=true` (root `gradle.properties`)

One-time SDK install (Linux, no Android Studio needed):

```bash
SDK=$HOME/android-sdk
curl -o cmdtools.zip https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip
mkdir -p "$SDK/cmdline-tools" && unzip -q cmdtools.zip -d "$SDK/cmdline-tools" \
  && mv "$SDK/cmdline-tools/cmdline-tools" "$SDK/cmdline-tools/latest"
export ANDROID_HOME=$SDK ANDROID_SDK_ROOT=$SDK
yes | "$SDK/cmdline-tools/latest/bin/sdkmanager" --licenses
"$SDK/cmdline-tools/latest/bin/sdkmanager" "platform-tools" "platforms;android-35" "build-tools;34.0.0"
```

The Android modules are only included in the Gradle build when `ANDROID_HOME`/`ANDROID_SDK_ROOT`
is set (see `settings.gradle`).

## Build & deploy

```bash
./gradlew :runtimes:android:assembleDebug              # build the .aar library  (verified)
./gradlew :runtimes:android-sample-web:assembleDebug   # build the app APK        (verified)
./gradlew :runtimes:android-sample-web:installDebug    # install on emulator/device
./gradlew :runtimes:android-sample-web:connectedAndroidTest  # instrumented tests (needs an emulator)
```

> The `.aar` and the sample APK build cleanly today (the APK bundles the full runtime via
> multidex plus the `assets/bx` app payload). **On-device execution** additionally depends on
> the **preloaded boxpiler** runtime item (AOT class resolution) — see the proposal's
> "hard constraint" section. Emulator/instrumented tests require a host with KVM.
