# Quick Start

Get a BoxLang Android app running on an emulator. Assumes the Android SDK is installed and
`ANDROID_HOME` is set (the Android modules are only included in the Gradle build when it is).

## 1. Create the app payload (100% BoxLang)

Everything lives under `src/main/bx`:

```
src/main/bx/
├─ boxlang.json
├─ Application.bx
├─ handlers/Main.bx
├─ views/main/index.bxm
└─ layouts/main.bxm
```

`boxlang.json` — point the runtime at the app folders:

```json
{
  "mappings": { "/": "./", "/handlers": "./handlers", "/views": "./views", "/layouts": "./layouts" },
  "modulesDirectory": [ "./modules" ],
  "javaLibraryPaths": [ "./lib" ]
}
```

`Application.bx` — wire the app + routes:

```java
class {
    this.name = "MyApp";
    function configureRouter( router ) {
        router.setDefaultEvent( "Main.index" );   // "/" -> Main.index
    }
    function onApplicationStart() { return true; }
}
```

`handlers/Main.bx` — the handler runs first and sets the view:

```java
class {
    function index( event, rc ) {
        event.setValue( "title", "Hello from BoxLang" );
        event.setView( "main/index" );
    }
}
```

`views/main/index.bxm` and `layouts/main.bxm`:

```html
<!-- views/main/index.bxm -->
<bx:output><h2>#rc.title#</h2></bx:output>

<!-- layouts/main.bxm -->
<bx:output><html><body><main>#renderedView#</main></body></html></bx:output>
```

## 2. Manifest — point at the generic entry points (no host classes)

```xml
<application android:name="ortus.boxlang.runtime.android.BoxAndroidApplication" ...>
    <activity android:name="ortus.boxlang.runtime.android.BoxActivity" android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
</application>
```

## 3. Depend on the runtime

```gradle
dependencies { implementation project( ':runtimes:android' ) } // or io.boxlang:boxlang-android in a standalone repo
```

## 4. Build, install, run

```bash
./gradlew :runtimes:android-sample-web:assembleDebug   # AOT-compiles bx + builds the APK
./gradlew :runtimes:android-sample-web:installDebug    # deploy to a running emulator/device
adb shell am start -n com.example.boxweb/ortus.boxlang.runtime.android.BoxActivity
```

You should see the `Main.index` screen. Tap a link → it routes in-process to the matching
handler; submit a form → the action runs and re-renders. **No web server is involved.**

See [tutorial.md](tutorial.md) for the full list + detail + add walkthrough.
