# BoxLang on Android — Documentation

Build Android apps in **BoxLang**. One language for web, serverless, CLI — and now
Android — with batteries-included runtime (HTTP, JSON, async, caching) and 100% Java interop
(the whole Android SDK for free). **Zero Kotlin: the app is 100% BoxLang.**

## Contents

- **[quick-start.md](quick-start.md)** — From zero to a running screen in minutes.
- **[usage.md](usage.md)** — Runtime config, the `Application.bx` lifecycle on Android,
  HTTP/JSON/async, SDK interop, modules, hot reload, build & deploy.
- **[reference.md](reference.md)** — API reference: `AndroidBoxRuntime`, `BoxActivity`, the
  `Application.bx` lifecycle (standard + Android hooks), the MVC front controller
  (router, `event`/`rc`, `setView`/`setLayout`, relocate), the starter-template layout, the
  AOT task, config keys, and R8 keep rules.
- **[tutorial.md](tutorial.md)** — Build a real list+detail app, plus a limitations & gotchas section.

## The UI model

The UI is the **WebView + BoxLang templating** track:

| | WebView (MVC) |
|---|---|
| UI authored as | BoxLang **`.bxm` templates** + layouts |
| Rendered by | `WebView` (HTML from the templating engine) |
| Kotlin needed? | **None** — 100% BoxLang |
| Best for | Fast delivery, web-skill reuse, cross-target portability |

A handler action runs first, populates the request collection (`rc`) and chooses the
view + layout; the framework renders the view inside the layout and loads the HTML into a
`WebView`. Links and forms route back into the in-process runtime — no web server.

> A pure-Java native-widget renderer (`UINode` → `android.widget`, **no Kotlin/Compose**) is a
> roadmap option if a non-WebView UI is wanted later.

## Architecture & limitations

See **[../../../workbench/proposals/boxlang-android.md](../../../workbench/proposals/boxlang-android.md)**
for the execution model, the ART/AOT constraint, and the roadmap.

## Module map

- `:runtimes:android-mvc` — portable, pure-JVM MVC framework + AOT pipeline (unit-tested).
- `:runtimes:android` — Android library glue (AGP, publishes the `.aar`, 100% Java).
- `:runtimes:android-sample-web` — the runnable sample / starter template.
