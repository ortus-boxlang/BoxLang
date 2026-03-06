# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

* * *

## [Unreleased]

## [2.2.0] - 2025-02-19

## [2.1.0] - 2025-01-22

### Added

- Updated github actions
- BoxLang certification

## [2.1.0] - 2025-01-22

### Added

- Adobe 2023 support
- Github Actions
- ColdBox 7 auto testing
- Clearer naming showing when methods return Java collections instead of native CFML collections such as `getNativeArray` and `collectToMap`.
- Additional methods such as `collectToArray` to return CFML collections natively.

## [2.0.1] => 2022-SEP-29

### Fixed

- Use the release of `cbproxies` instead of the snapshot

## [2.0.0] => 2022-SEP-29

### New Features

- New ColdBox helpers: `stream(), streamBuilder()`
- Migration to leverage `cbproxies` instead of duplicating work. This allows us to use `parallel()` finally with no issues on all engines.
- Added `collectAsSet()` to collect items into a non-duplicate set instance
- Added mappings for API Docs
- Added more experimental Adobe/Lucee CFML Context loading for parallel streams
- Migrated to github actions
- Migrated to new module template

### Fixed

- `StreamBuilder.new()` had the wrong argument `predicate` when it was `primitive`

### Changed

- Dropped Adobe 2016

## [1.5.0] => 2019-JUL-02

- Added ACF support for `java.util.ArrayList` native arrays to be casted correctly to Java Streams.
- Experimental: Added the ability to transfer page contexts and fusion contexts for running parallel threads. This is a major breakthrough for parallelization of the fork join framework and bridging to the CFML engines. Only works on ACF, and partially.

## [1.4.0]

- Ability to add a file encoding when doing file streams via `ofFile( path, encoding = "UTF-8" )` with UTF-8 being the default.
- The `Optional` class gets several new methods:
  - `isEmpty()` - Returns true if the value is empty else false
  - `ifPresentOrElse( consumer, runnable )` - If a value is present, performs the given action with the value, otherwise performs the given empty-based action.
  - `orElseRun( runnable )` - Runs the `runnable` closure/lambda if the value is not set and the same optional instance is returned.
  - `$or( supplier ), or( supplier )` - If a value is present, returns an Optional describing the value, otherwise returns an Optional produced by the supplying function value.
  - `orElseThrow( type, message )` - If a value is present, returns the value, otherwise throws NoSuchElementException.

## [1.3.0]

- Native ColdFusion Query Support
- Native Java arrays support when passing native java arrays to build streams out of them

## [1.2.1]

- Fixes on `map()` when using ranges to switch the types to `any`

## [1.2.0]

- Fix the `generate()` to use the correct stream class. Only works on lucee, adobe fails on interface default method executions.
- Removed the `iterate()` not working with dynamic proxies
- Rework of ranges in order to work with strong typed streams

## [1.1.0]

- Added Adobe 2018 Build process
- Updated keyserver for build process
- Updated API Doc generation
- Updated usage docs on API
- CFConfig additions for further testing
- Fixes to interfaces for ACF Compat

## [1.0.0]

- First iteration of this module

[unreleased]: https://github.com/coldbox-modules/cbstreams/compare/v2.2.0...HEAD
[2.2.0]: https://github.com/coldbox-modules/cbstreams/compare/v2.1.0...v2.2.0
[2.1.0]: https://github.com/coldbox-modules/cbstreams/compare/1098b1ab2bb3d6abf16dbf3912b46846a2ee8a2c...v2.1.0
