# BoxLang on Android — Documentation

Build native Android apps in **BoxLang**. One language for web, serverless, CLI — and now
Android — with batteries-included runtime (HTTP, JSON, async, caching) and 100% Java interop
(the whole Android SDK for free).

## Contents

- **[quick-start.md](quick-start.md)** — From zero to a running screen in minutes (both UI tracks).
- **[usage.md](usage.md)** — Runtime config, the `Application.bx` lifecycle on Android,
  choosing a UI track, HTTP/JSON/async, SDK interop, hot reload, build & deploy.
- **[reference.md](reference.md)** — API reference: `AndroidBoxRuntime`, `BoxActivity`, the
  `Application.bx` lifecycle (standard + Android hooks), the MVC front controller
  (router, `event`/`rc`, `setView`/`setLayout`, flash), the Compose UI-tree DSL, the
  starter-template layout, the AOT task, config keys, and R8 keep rules.
- **[tutorial.md](tutorial.md)** — Build a real list+detail app, shown **twice** (WebView and
  Compose tracks), plus a limitations & gotchas section.

## The two UI tracks at a glance

| | Track 1 — Compose | Track 2 — WebView (MVC) |
|---|---|---|
| UI authored as | BoxLang UI **node tree** (`UI` DSL) | BoxLang **`.bxm` templates** + layouts |
| Rendered by | Native Jetpack Compose widgets | `WebView` (HTML from the templating engine) |
| Kotlin needed? | A thin host (Compose is Kotlin-first) | **None** — 100% BoxLang |
| Best for | Native look & feel, animations | Fast delivery, web-skill reuse, portability |

## Architecture & limitations

See **[../../../workbench/proposals/boxlang-android.md](../../../workbench/proposals/boxlang-android.md)**
for the execution model, the ART/AOT constraint, and the roadmap.

## Module map

- `:runtimes:android-mvc` — portable, pure-JVM MVC framework (unit-tested, reusable across targets).
- `:runtimes:android` — Android library glue (AGP, publishes the `.aar`).
- `:runtimes:android-sample-web` / `:runtimes:android-sample-compose` — runnable samples / templates.
