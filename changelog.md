# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

* * *

## [Unreleased]

## [1.4.0] - 2025-08-02

## [1.3.0] - 2025-06-23

## [1.2.0] - 2025-05-29

### New Features

- [BL-1453](https://ortussolutions.atlassian.net/browse/BL-1453) Maven pom.xml for the BoxLang Home so you can integrate with any Java library
- [BL-1464](https://ortussolutions.atlassian.net/browse/BL-1464) implement nested grouped output/looping
- [BL-1474](https://ortussolutions.atlassian.net/browse/BL-1474) Logger appenders in the boxlang.json can now chose their own encoder: text or json
- [BL-1476](https://ortussolutions.atlassian.net/browse/BL-1476) Added ability for loggers to chose between file and console appenders
- [BL-1482](https://ortussolutions.atlassian.net/browse/BL-1482) new event ON_FUNCTION_EXCEPTION
- [BL-1485](https://ortussolutions.atlassian.net/browse/BL-1485) Update the error template to include a section where the user can contact us if the error is not good enough or we can improve it.

### Improvements

- [BL-1393](https://ortussolutions.atlassian.net/browse/BL-1393) Add executionTime to result object of bx:http
- [BL-1397](https://ortussolutions.atlassian.net/browse/BL-1397) FileCopy( source, targetDirectory ) when using a target directory doesn't work on BoxLang but works on Lucee
- [BL-1400](https://ortussolutions.atlassian.net/browse/BL-1400) File bifs have too many casts, do one cast for performance
- [BL-1404](https://ortussolutions.atlassian.net/browse/BL-1404) Add a unique request id metadata header when making requests in http so it can track easily
- [BL-1405](https://ortussolutions.atlassian.net/browse/BL-1405) Add missing contexts to data interceptors
- [BL-1406](https://ortussolutions.atlassian.net/browse/BL-1406) Add a \`request\` struct to the bxhttp result object
- [BL-1417](https://ortussolutions.atlassian.net/browse/BL-1417) Add Allow Arguments to FileCopy and FileMove for granular extension security overrides
- [BL-1419](https://ortussolutions.atlassian.net/browse/BL-1419) server.java.defaultLocale, server.java.availableLocales
- [BL-1420](https://ortussolutions.atlassian.net/browse/BL-1420) New bif: BoxModuleReload( \[name] ) to easily reload modules for testing purposes
- [BL-1421](https://ortussolutions.atlassian.net/browse/BL-1421) optimize when LocalizationUtil string casts
- [BL-1422](https://ortussolutions.atlassian.net/browse/BL-1422) optimize Struct.putAll()
- [BL-1423](https://ortussolutions.atlassian.net/browse/BL-1423) optimize when basescope creates lockname to on demand
- [BL-1424](https://ortussolutions.atlassian.net/browse/BL-1424) optimize string compare check for unicode
- [BL-1425](https://ortussolutions.atlassian.net/browse/BL-1425) optimize case insensitive instanceof check
- [BL-1426](https://ortussolutions.atlassian.net/browse/BL-1426) optimize isNumeric locale parsing and casting
- [BL-1427](https://ortussolutions.atlassian.net/browse/BL-1427) optimize file detection/reading from disk unless needed
- [BL-1428](https://ortussolutions.atlassian.net/browse/BL-1428) optimize getConfig() by caching at request context
- [BL-1429](https://ortussolutions.atlassian.net/browse/BL-1429) Improve performance of string lowercasing and key creation
- [BL-1433](https://ortussolutions.atlassian.net/browse/BL-1433) Only include source lines in exceptions when in debug mode
- [BL-1434](https://ortussolutions.atlassian.net/browse/BL-1434) Optimize hash base 64 encoding
- [BL-1435](https://ortussolutions.atlassian.net/browse/BL-1435) Optimize regex cache key generation
- [BL-1436](https://ortussolutions.atlassian.net/browse/BL-1436) Move/Copy BIFs should all default to an overwrite value of true
- [BL-1437](https://ortussolutions.atlassian.net/browse/BL-1437) Update the way loggers are setup and retrieved to avoid string and key lookups and accelerate the runtime
- [BL-1438](https://ortussolutions.atlassian.net/browse/BL-1438) bif invocation interceptors missing the actual bif
- [BL-1439](https://ortussolutions.atlassian.net/browse/BL-1439) BIF Interceptors hot code, only create events if they are states for it
- [BL-1443](https://ortussolutions.atlassian.net/browse/BL-1443) order keys in struct dump alphabetically
- [BL-1445](https://ortussolutions.atlassian.net/browse/BL-1445) DateTime and Duration Math Should Represent Fractional Days in Math Operations
- [BL-1446](https://ortussolutions.atlassian.net/browse/BL-1446) Compat: DateAdd Should accept numeric fractional days as date argument
- [BL-1450](https://ortussolutions.atlassian.net/browse/BL-1450) Exclude javaparser and other debug libraries from final jar
- [BL-1454](https://ortussolutions.atlassian.net/browse/BL-1454) Optimize FQN class by removing regex usage in hot code
- [BL-1455](https://ortussolutions.atlassian.net/browse/BL-1455) optimize generated setter by caching resolved file path
- [BL-1456](https://ortussolutions.atlassian.net/browse/BL-1456) Optimize dynamic object by converting stream to loop in hot code
- [BL-1457](https://ortussolutions.atlassian.net/browse/BL-1457) optimize output check by caching recursive lookups
- [BL-1458](https://ortussolutions.atlassian.net/browse/BL-1458) optimize isEmpty code paths
- [BL-1459](https://ortussolutions.atlassian.net/browse/BL-1459) optimize getting function enclosing class by caching
- [BL-1460](https://ortussolutions.atlassian.net/browse/BL-1460) optimize number caster true/false string checks
- [BL-1461](https://ortussolutions.atlassian.net/browse/BL-1461) optimize BoxStructSerializer class by avoiding struct.entrySet()
- [BL-1462](https://ortussolutions.atlassian.net/browse/BL-1462) optimize string compare by removing unnecessary string to lower case
- [BL-1473](https://ortussolutions.atlassian.net/browse/BL-1473) Update to use StatusPrinter2 from deprecated StatusPrinter using LogBack
- [BL-1481](https://ortussolutions.atlassian.net/browse/BL-1481) Speed improvements for function invocation on hot code
- [BL-1486](https://ortussolutions.atlassian.net/browse/BL-1486) Better handle low level parsing errors like  java.util.EmptyStackException
- [BL-1489](https://ortussolutions.atlassian.net/browse/BL-1489) Improve error messages when registering interceptors using the registration bifs when sending things other than interceptors
- [BL-1490](https://ortussolutions.atlassian.net/browse/BL-1490) Add function name to interceptor data for ease of use
- [BL-1493](https://ortussolutions.atlassian.net/browse/BL-1493) Accelerate dynamic object method handle executions

### Bugs

- [BL-1356](https://ortussolutions.atlassian.net/browse/BL-1356) postBIFInvocation event
- [BL-1357](https://ortussolutions.atlassian.net/browse/BL-1357) Simple preFunctionInvoke interceptor throws errors due to recursing into itself
- [BL-1385](https://ortussolutions.atlassian.net/browse/BL-1385) Module that defines an interceptor has to specify "too much" for the class path
- [BL-1386](https://ortussolutions.atlassian.net/browse/BL-1386) ModuleService reload and reloadAll() methods to provide ability for module reloading on development
- [BL-1394](https://ortussolutions.atlassian.net/browse/BL-1394) forgot to populate the \`populateServerSystemScope\` from the override boxlang.json
- [BL-1396](https://ortussolutions.atlassian.net/browse/BL-1396) isValid Boolean Returning Incorrect Result on Struct
- [BL-1398](https://ortussolutions.atlassian.net/browse/BL-1398) Move default disallowed file extensions to web support and keep CLI open
- [BL-1399](https://ortussolutions.atlassian.net/browse/BL-1399) Compat: CreateTime Should Support 0 hour argument
- [BL-1401](https://ortussolutions.atlassian.net/browse/BL-1401) FileSystemUtil not absoluting paths when checking existence
- [BL-1402](https://ortussolutions.atlassian.net/browse/BL-1402) \`replaceNoCase\` does not handle \`null\` strings like Lucee or ACF
- [BL-1403](https://ortussolutions.atlassian.net/browse/BL-1403) http not using the user agent if passed by the user
- [BL-1409](https://ortussolutions.atlassian.net/browse/BL-1409) Compat: add \`server.coldfusion.supportedLocales\`
- [BL-1412](https://ortussolutions.atlassian.net/browse/BL-1412) Add Application.bx/cfc support for overriding allowed and disallowed extensions
- [BL-1414](https://ortussolutions.atlassian.net/browse/BL-1414) this.logger is null in when getting an attempt() from a box future
- [BL-1416](https://ortussolutions.atlassian.net/browse/BL-1416) Compat:  Support ACF/Lucee \`blockedExtForFileUpload\` Application Setting
- [BL-1418](https://ortussolutions.atlassian.net/browse/BL-1418) parameterized QoQ with maxLength errors
- [BL-1431](https://ortussolutions.atlassian.net/browse/BL-1431) function dump template doesn't work in compat
- [BL-1432](https://ortussolutions.atlassian.net/browse/BL-1432) CF transpiler not turning off accessors for child classes
- [BL-1441](https://ortussolutions.atlassian.net/browse/BL-1441) getPageContext().getRequest() has no getScheme()
- [BL-1442](https://ortussolutions.atlassian.net/browse/BL-1442) empty file fields in forms throw error on submit
- [BL-1444](https://ortussolutions.atlassian.net/browse/BL-1444) Boxlang does not pickup custom tags that are in the same folder as the file that calls them
- [BL-1447](https://ortussolutions.atlassian.net/browse/BL-1447) Compat: DateDiff should support fractional days as date argument
- [BL-1449](https://ortussolutions.atlassian.net/browse/BL-1449) when doing a boxlang {action} command it should break and execute
- [BL-1451](https://ortussolutions.atlassian.net/browse/BL-1451) Custom tag search is case-sensitive
- [BL-1452](https://ortussolutions.atlassian.net/browse/BL-1452) inline annotation errors when literal value is a negative number
- [BL-1463](https://ortussolutions.atlassian.net/browse/BL-1463) parser errors on class annotation called abstract
- [BL-1466](https://ortussolutions.atlassian.net/browse/BL-1466) self-closing defaultcase tag not parsing
- [BL-1467](https://ortussolutions.atlassian.net/browse/BL-1467) if you use options or params that include file extensions, the runner explodes
- [BL-1468](https://ortussolutions.atlassian.net/browse/BL-1468) cfqueryparam tag not allowed outside of cfquery tag
- [BL-1469](https://ortussolutions.atlassian.net/browse/BL-1469) Building query fails when part of the query is build inside function that outputs directly
- [BL-1470](https://ortussolutions.atlassian.net/browse/BL-1470) query escaping of single quotes only escapes the first one, not all
- [BL-1475](https://ortussolutions.atlassian.net/browse/BL-1475) \`this.sessionStorage\` assignment with Application-defined cache throws error.
- [BL-1477](https://ortussolutions.atlassian.net/browse/BL-1477) Errors within application startup leave app in unusable state
- [BL-1479](https://ortussolutions.atlassian.net/browse/BL-1479) Compat:  Error thrown in QueryCompat interception when null param is encountered
- [BL-1483](https://ortussolutions.atlassian.net/browse/BL-1483) calling java method with invoke() and no args fails
- [BL-1484](https://ortussolutions.atlassian.net/browse/BL-1484) filewrite operations on existing files were not truncating it and leaving content behind.
- [BL-1488](https://ortussolutions.atlassian.net/browse/BL-1488) isSimpleValue doesn't work with Keys

## [1.1.0] - 2025-05-12

### New Features

- [BL-1365](https://ortussolutions.atlassian.net/browse/BL-1365) Add parse() helper methods directly to runtime which returns parse result
- [BL-1388](https://ortussolutions.atlassian.net/browse/BL-1388) new security configuration item: populateServerSystemScope that can allow or not the population of the server.system scope or not

### Improvements

- [BL-1333](https://ortussolutions.atlassian.net/browse/BL-1333) Create links to BoxLang modules
- [BL-1351](https://ortussolutions.atlassian.net/browse/BL-1351) match getHTTPTimeString() and default time to now
- [BL-1358](https://ortussolutions.atlassian.net/browse/BL-1358) Work harder to return partial AST on invalid parse
- [BL-1363](https://ortussolutions.atlassian.net/browse/BL-1363) Error executing dump template \[/dump/html/BoxClass.bxm]
- [BL-1375](https://ortussolutions.atlassian.net/browse/BL-1375) Compat - Move Legacy Date Format Interception to Module-Specific Interception Point
- [BL-1381](https://ortussolutions.atlassian.net/browse/BL-1381) allow box class to be looped over as collection
- [BL-1382](https://ortussolutions.atlassian.net/browse/BL-1382) Rework event bus interceptors to accelerate during executions
- [BL-1383](https://ortussolutions.atlassian.net/browse/BL-1383) Compat - Allow handling of decimals where timespan is used
- [BL-1387](https://ortussolutions.atlassian.net/browse/BL-1387) Allow Numeric ApplicationTimeout assignment to to be decimal

### Bugs

- [BL-1354](https://ortussolutions.atlassian.net/browse/BL-1354) BoxLang date time not accepted by JDBC as a date object
- [BL-1359](https://ortussolutions.atlassian.net/browse/BL-1359) contracting path doesn't work if casing of mapping doesn't match casing of abs path
- [BL-1366](https://ortussolutions.atlassian.net/browse/BL-1366) sessionInvalidate() "Cannot invoke String.length() because "s" is null"
- [BL-1370](https://ortussolutions.atlassian.net/browse/BL-1370) Some methods not found in java interop
- [BL-1372](https://ortussolutions.atlassian.net/browse/BL-1372) string functions accepting null
- [BL-1374](https://ortussolutions.atlassian.net/browse/BL-1374) onMissingTemplate event mystyped as missingtemplate
- [BL-1377](https://ortussolutions.atlassian.net/browse/BL-1377) fileExists() not working with relative paths
- [BL-1378](https://ortussolutions.atlassian.net/browse/BL-1378) optional capture groups throw NPE in reReplace()
- [BL-1379](https://ortussolutions.atlassian.net/browse/BL-1379) array length incorrect for xml nodes
- [BL-1384](https://ortussolutions.atlassian.net/browse/BL-1384) Numeric Session Timeout Values Should Be Duration of Days  not Seconds

## [1.0.1] - 2025-05-01

### Fixed

- Detection on `boxlang file` execution with invalid paths was not working properly.

## [1.0.0] - 2025-04-30

[unreleased]: https://github.com/ortus-boxlang/BoxLang/compare/v1.4.0...HEAD
[1.4.0]: https://github.com/ortus-boxlang/BoxLang/compare/v1.3.0...v1.4.0
[1.3.0]: https://github.com/ortus-boxlang/BoxLang/compare/v1.2.0...v1.3.0
[1.2.0]: https://github.com/ortus-boxlang/BoxLang/compare/v1.1.0...v1.2.0
[1.1.0]: https://github.com/ortus-boxlang/BoxLang/compare/v1.0.1...v1.1.0
[1.0.1]: https://github.com/ortus-boxlang/BoxLang/compare/v1.0.0...v1.0.1
[1.0.0]: https://github.com/ortus-boxlang/BoxLang/compare/aa8064a2aecbc79fbff9b31c56e0c5c6be71063f...v1.0.0
