# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

* * *

## [Unreleased]

## [1.17.5] - 2026-09-14

### Fixed

- BL-2674 Restored the `onJsonQuerySerialize` interception announcement that was dropped during query serialization refactoring

## [1.17.4] - 2026-09-11

### Added

- BL-2674 Bit columns across databases are now consistently cast to integers (matching Adobe behavior)

### Fixed

- BL-2669 Ensure the runtime security `.seed` file is materialized on startup for bx-plus compatibility
- BL-2671 `listAppend()` regression — no longer prepends a delimiter when appending a delimited string to an empty list
- BL-2677 Clob/Blob/NClob values manually added to a query are now converted to `String`/`byte[]` instead of leaking raw JDBC instances

### Improved

- BL-2670 `isDate()` performance — added fast pre-rejection logic for inputs that can never be dates
- BL-2672 Guarded exception utilities against circular cause references and excessive nested cause chains
- BL-2678 `numberFormat()` with `.__` now forces trailing zeros in fractional placeholders

## [1.17.3] - 2026-09-05

### Fixed

- BL-2667 `bx:file` component `action="write"` now resolves relative file paths against the temp directory, matching CFML/`fileWrite()` behavior

### Improved

- BL-2665 JSON serialization of queries no longer converts stored `null` values to empty strings
- BL-2666 `listAppend()` performance when `includeEmptyFields` is `false` — skips parsing the base list when not needed

## [1.17.2] - 2026-09-04

### Fixed

- BL-2664 Session persistence now runs before redirects are sent, preventing a race condition where the next request could read a session variable that wasn't stored yet

## [1.17.1] - 2026-09-01

### Fixed

- BL-2657 `parseDateTime()` now accepts 24-hour timestamps with a meridian (e.g. `2/5/2026 12:00:00 AM`)
- BL-2658 `structGet()` now unwraps query column values (e.g. `structGet( "qry.col" )`)

## [1.17.0] - 2026-08-28

- <https://boxlang.ortusbooks.com/readme/release-history/1.17.0>

## [1.16.0] - 2026-07-30

- <https://boxlang.ortusbooks.com/readme/release-history/1.16.0>

## [1.15.0] - 2026-07-08

- <https://boxlang.ortusbooks.com/readme/release-history/1.15.0>

## [1.14.0] - 2026-06-03

- <https://boxlang.ortusbooks.com/readme/release-history/1.14.0>

## [1.13.0] - 2026-05-01

- <https://boxlang.ortusbooks.com/readme/release-history/1.13.0>

## [1.12.0] - 2026-04-08

- <https://boxlang.ortusbooks.com/readme/release-history/1.12.0>

## [1.11.0] - 2026-03-04

- <https://boxlang.ortusbooks.com/readme/release-history/1.11.0>

## [1.10.1] - 2026-02-04

- <https://boxlang.ortusbooks.com/readme/release-history/1.10.0>

## [1.10.0] - 2026-02-02

- <https://boxlang.ortusbooks.com/readme/release-history/1.10.0>

## [1.9.0] - 2026-01-08

- <https://boxlang.ortusbooks.com/readme/release-history/1.9.0>

## [1.8.0] - 2025-12-05

- <https://boxlang.ortusbooks.com/readme/release-history/1.8.0>

## [1.7.0] - 2025-11-04

- <https://boxlang.ortusbooks.com/readme/release-history/1.7.0>

## [1.6.0] - 2025-10-03

- <https://boxlang.ortusbooks.com/readme/release-history/1.6.0>

## [1.5.0] - 2025-08-30

- <https://boxlang.ortusbooks.com/readme/release-history/1.5.0>

## [1.4.0] - 2025-08-02

- <https://boxlang.ortusbooks.com/readme/release-history/1.4.0>

## [1.3.0] - 2025-06-23

- <https://boxlang.ortusbooks.com/readme/release-history/1.3.0>

## [1.2.0] - 2025-05-29

- <https://boxlang.ortusbooks.com/readme/release-history/1.2.0>

## [1.1.0] - 2025-05-12

- <https://boxlang.ortusbooks.com/readme/release-history/1.1.0>

## [1.0.1] - 2025-05-01

- <https://boxlang.ortusbooks.com/readme/release-history/1.0.1>

## [1.0.0] - 2025-04-30

- <https://boxlang.ortusbooks.com/readme/release-history/1.0.0>

[unreleased]: https://github.com/ortus-boxlang/BoxLang/compare/v1.17.5...HEAD
[1.17.5]: https://github.com/ortus-boxlang/BoxLang/compare/v1.17.4...v1.17.5
[1.17.4]: https://github.com/ortus-boxlang/BoxLang/compare/v1.17.3...v1.17.4
[1.17.3]: https://github.com/ortus-boxlang/BoxLang/compare/v1.17.2...v1.17.3
[1.17.2]: https://github.com/ortus-boxlang/BoxLang/compare/v1.17.1...v1.17.2
[1.17.1]: https://github.com/ortus-boxlang/BoxLang/compare/v1.17.0...v1.17.1
[1.17.0]: https://github.com/ortus-boxlang/BoxLang/compare/v1.16.0...v1.17.0
[1.16.0]: https://github.com/ortus-boxlang/BoxLang/compare/v1.15.0...v1.16.0
[1.15.0]: https://github.com/ortus-boxlang/BoxLang/compare/v1.14.0...v1.15.0
[1.14.0]: https://github.com/ortus-boxlang/BoxLang/compare/v1.13.0...v1.14.0
[1.13.0]: https://github.com/ortus-boxlang/BoxLang/compare/v1.12.0...v1.13.0
[1.12.0]: https://github.com/ortus-boxlang/BoxLang/compare/v1.11.0...v1.12.0
[1.11.0]: https://github.com/ortus-boxlang/BoxLang/compare/v1.10.1...v1.11.0
[1.10.1]: https://github.com/ortus-boxlang/BoxLang/compare/v1.10.0...v1.10.1
[1.10.0]: https://github.com/ortus-boxlang/BoxLang/compare/v1.9.0...v1.10.0
[1.9.0]: https://github.com/ortus-boxlang/BoxLang/compare/v1.8.0...v1.9.0
[1.8.0]: https://github.com/ortus-boxlang/BoxLang/compare/v1.7.0...v1.8.0
[1.7.0]: https://github.com/ortus-boxlang/BoxLang/compare/v1.6.0...v1.7.0
[1.6.0]: https://github.com/ortus-boxlang/BoxLang/compare/v1.5.0...v1.6.0
[1.5.0]: https://github.com/ortus-boxlang/BoxLang/compare/v1.4.0...v1.5.0
[1.4.0]: https://github.com/ortus-boxlang/BoxLang/compare/v1.3.0...v1.4.0
[1.3.0]: https://github.com/ortus-boxlang/BoxLang/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/ortus-boxlang/BoxLang/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/ortus-boxlang/BoxLang/compare/v1.0.1...v1.1.0
[1.0.1]: https://github.com/ortus-boxlang/BoxLang/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/ortus-boxlang/BoxLang/compare/aa8064a2aecbc79fbff9b31c56e0c5c6be71063f...v1.0.0
