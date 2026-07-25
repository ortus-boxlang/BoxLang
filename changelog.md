# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

* * *

## [Unreleased]

### Added

- New `obfuscate` CLI action command (`boxlang obfuscate --source <path> --target <dir>`) that produces a
  deployable, non-revealing form of BoxLang/CFML source by stripping comments/documentation and renaming
  `var`-declared local variables to short opaque names. Function and argument names are intentionally left
  unchanged so callers keep working (named arguments, `this.method()`, and dynamic invocation all resolve by
  name). The obfuscated output remains valid, behavior-preserving source.
- New `encrypt` CLI action command (`boxlang encrypt --source <path> --target <dir> --key <secret> --key-id <label>`)
  for source-level encryption at rest. Encrypted files (AES-256-GCM) are unreadable on disk and transparently
  decrypted in memory just before parsing, so distributed code stays hidden yet runs on any runtime version
  (not bytecode, no version binding). Each file's header carries a key-id label; at load the runtime resolves
  the matching key by that label from the `BOXLANG_CODE_KEY_<KEYID>` environment variable or the new
  `security.codeKeys` map in `boxlang.json` — letting a vendor lock each module/artifact with its own key and
  hand each customer only the keys they purchased. See `ortus.boxlang.runtime.util.CodeEncryption`.

## [1.15.0] - 2026-07-08

## [1.14.0] - 2026-06-03

## [1.13.0] - 2026-05-01

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

[unreleased]: https://github.com/ortus-boxlang/BoxLang/compare/v1.15.0...HEAD
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
