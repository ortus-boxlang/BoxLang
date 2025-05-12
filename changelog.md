# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

* * *

## [Unreleased]

### New Features

- [BL-1365](https://ortussolutions.atlassian.net/browse/BL-1365) Add parse\(\) helper methods directly to runtime which returns parse result
- [BL-1388](https://ortussolutions.atlassian.net/browse/BL-1388) new security configuration item: populateServerSystemScope that can allow or not the population of the server.system scope or not

### Improvements

- [BL-1333](https://ortussolutions.atlassian.net/browse/BL-1333) Create links to BoxLang modules
- [BL-1351](https://ortussolutions.atlassian.net/browse/BL-1351) match getHTTPTimeString\(\) and default time to now
- [BL-1358](https://ortussolutions.atlassian.net/browse/BL-1358) Work harder to return partial AST on invalid parse
- [BL-1363](https://ortussolutions.atlassian.net/browse/BL-1363) Error executing dump template \[/dump/html/BoxClass.bxm\]
- [BL-1375](https://ortussolutions.atlassian.net/browse/BL-1375) Compat - Move Legacy Date Format Interception to Module-Specific Interception Point
- [BL-1381](https://ortussolutions.atlassian.net/browse/BL-1381) allow box class to be looped over as collection
- [BL-1382](https://ortussolutions.atlassian.net/browse/BL-1382) Rework event bus interceptors to accelerate during executions
- [BL-1383](https://ortussolutions.atlassian.net/browse/BL-1383) Compat - Allow handling of decimals where timespan is used
- [BL-1387](https://ortussolutions.atlassian.net/browse/BL-1387) Allow Numeric ApplicationTimeout assignment to to be decimal

### Bugs

- [BL-1354](https://ortussolutions.atlassian.net/browse/BL-1354) BoxLang date time not accepted by JDBC as a date object
- [BL-1359](https://ortussolutions.atlassian.net/browse/BL-1359) contracting path doesn't work if casing of mapping doesn't match casing of abs path
- [BL-1366](https://ortussolutions.atlassian.net/browse/BL-1366) sessionInvalidate\(\) "Cannot invoke String.length\(\) because "s" is null"
- [BL-1370](https://ortussolutions.atlassian.net/browse/BL-1370) Some methods not found in java interop
- [BL-1372](https://ortussolutions.atlassian.net/browse/BL-1372) string functions accepting null
- [BL-1374](https://ortussolutions.atlassian.net/browse/BL-1374) onMissingTemplate event mystyped as missingtemplate
- [BL-1377](https://ortussolutions.atlassian.net/browse/BL-1377) fileExists\(\) not working with relative paths
- [BL-1378](https://ortussolutions.atlassian.net/browse/BL-1378) optional capture groups throw NPE in reReplace\(\)
- [BL-1379](https://ortussolutions.atlassian.net/browse/BL-1379) array length incorrect for xml nodes
- [BL-1384](https://ortussolutions.atlassian.net/browse/BL-1384) Numeric Session Timeout Values Should Be Duration of Days  not Seconds

## [1.0.1] - 2025-05-01

### Fixed

- Detection on `boxlang file` execution with invalid paths was not working properly.

## [1.0.0] - 2025-04-30

[Unreleased]: https://github.com/ortus-boxlang/BoxLang/compare/v1.0.1...HEAD

[1.0.1]: https://github.com/ortus-boxlang/BoxLang/compare/v1.0.0...v1.0.1

[1.0.0]: https://github.com/ortus-boxlang/BoxLang/compare/aa8064a2aecbc79fbff9b31c56e0c5c6be71063f...v1.0.0
