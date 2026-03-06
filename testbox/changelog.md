# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

* * *

## [Unreleased]

## [6.5.0] - 2026-01-25

## [6.4.0] - 2025-09-18

### New Features

- [TESTBOX-423](https://ortussolutions.atlassian.net/browse/TESTBOX-423) BoxLang Prime Updates

### Bugs

- [TESTBOX-421](https://ortussolutions.atlassian.net/browse/TESTBOX-421) \`equalize\` should use the Java \`equals\` check at the end of the function instead of \`false\`

### Tasks

- [TESTBOX-424](https://ortussolutions.atlassian.net/browse/TESTBOX-424) Add Dumb Adobe jvm flags to allow for dynamic arguments

## [6.3.2] - 2025-04-29

## [6.3.1] - 2025-04-01

### Fixed

- Update the `run` runners so they use the calculated location paths.

## [6.3.1] - 2025-04-01

### Fixed

- Fixed a typo in BaseReporter

## [6.3.0] - 2025-03-31

### Bugs

- [TESTBOX-418](https://ortussolutions.atlassian.net/browse/TESTBOX-418) initArgs.bundles can be an array or simple value, consolidate it in boxlang runner
- [TESTBOX-420](https://ortussolutions.atlassian.net/browse/TESTBOX-420) Reporter options not being passed correctly

### Improvements

- [TESTBOX-417](https://ortussolutions.atlassian.net/browse/TESTBOX-417) BoxLang only usage improvements
- [TESTBOX-419](https://ortussolutions.atlassian.net/browse/TESTBOX-419) Console reporter now includes colors and abiliy to execute via boxlang

## [6.3.0] - 2025-02-25

## [6.2.1] - 2025-02-06

## [6.2.0] - 2025-01-31

## [6.1.0] - 2025-01-28

### New Features

- [TESTBOX-412](https://ortussolutions.atlassian.net/browse/TESTBOX-412) Updated to use cbMockData now instead of MockDataCFC

### Improvements

- [TESTBOX-409](https://ortussolutions.atlassian.net/browse/TESTBOX-409) Support BoxLang without needing compat

### Bugs

- [TESTBOX-408](https://ortussolutions.atlassian.net/browse/TESTBOX-408) Allow toHaveKey to support struct like objects
- [TESTBOX-410](https://ortussolutions.atlassian.net/browse/TESTBOX-410) Error when using the url.excludes with the HTML runner
- [TESTBOX-411](https://ortussolutions.atlassian.net/browse/TESTBOX-411) fix missing \`cfloop\` on test browser

## [6.0.1] - 2024-12-05

## [6.0.0] - 2024-09-27

### New Features

- TESTBOX-391 MockBox converted to script
- TESTBOX-392 BoxLang classes support
- TESTBOX-393 New environment helpers to do skip detections or anything you see fit: isAdobe, isLucee, isBoxLang, isWindows, isMac, isLinux
- TESTBOX-394 new `test(), xtest(), ftest()` alias for more natuarl testing
- TESTBOX-397 debug() get's two new arguments: label and showUDFs
- TESTBOX-398 DisplayName on a bundle now shows up in the reports
- TESTBOX-399 xUnit new annotation for @DisplayName so it can show instead of the function name
- TESTBOX-401 BoxLang CLI mode and Runner
- TESTBOX-402 New matcher: toHaveKeyWithCase()
- TESTBOX-403 Assertions: key() and notKey() now have a CaseSensitive boolean argument

## Improvements

- TESTBOX-289 showUDFs = false option with debug()
- TESTBOX-331 TextReporter doesn't correctly support testBundles URL param
- TESTBOX-395 adding missing focused argument to spec methods
- TESTBOX-396 Generating a repeatable id for specs to track them better in future UIs

## Bugs

- TESTBOX-123 If test spec descriptor contains a comma, it can not be drilled down to run that one spec directly
- TESTBOX-338 describe handler in non-called test classes being executed

## Tasks

- TESTBOX-400 Drop Adobe 2018 support

[unreleased]: https://github.com/Ortus-Solutions/TestBox/compare/v6.5.0...HEAD
[6.5.0]: https://github.com/Ortus-Solutions/TestBox/compare/v6.4.0...v6.5.0
[6.4.0]: https://github.com/Ortus-Solutions/TestBox/compare/v6.3.2...v6.4.0
[6.3.2]: https://github.com/Ortus-Solutions/TestBox/compare/v6.3.1...v6.3.2
[6.3.1]: https://github.com/Ortus-Solutions/TestBox/compare/v6.3.0...v6.3.1
[6.3.0]: https://github.com/Ortus-Solutions/TestBox/compare/v6.3.0...v6.3.0
[6.2.1]: https://github.com/Ortus-Solutions/TestBox/compare/v6.2.0...v6.2.1
[6.2.0]: https://github.com/Ortus-Solutions/TestBox/compare/v6.1.0...v6.2.0
[6.1.0]: https://github.com/Ortus-Solutions/TestBox/compare/v6.0.1...v6.1.0
[6.0.1]: https://github.com/Ortus-Solutions/TestBox/compare/v6.0.0...v6.0.1
[6.0.0]: https://github.com/Ortus-Solutions/TestBox/compare/bc7774b4cc681cd8dfab08b2f3bba26a75f5601b...v6.0.0
