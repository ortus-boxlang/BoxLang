# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

* * *

## [Unreleased]

- [BL-2753](https://ortussolutions.atlassian.net/browse/BL-2753) New `generateKeyPair()` BIF for Ed25519, Ed448, X25519, X448, RSA and EC key pairs
- [BL-2758](https://ortussolutions.atlassian.net/browse/BL-2758) Comparisons now pass the caller's context down to the date caster, so date compares no longer rebuild the whole runtime config struct, and a date string compared inside a request is read in the request timezone. `Compare` and the comparison operators gain context-taking overloads; the old ones are deprecated
- [BL-2759](https://ortussolutions.atlassian.net/browse/BL-2759) Interceptors announced without a context (BIF, query and transaction events) now run on the current request when there is one, so they see its settings and no longer rebuild the runtime config on every read
- [BL-2781](https://ortussolutions.atlassian.net/browse/BL-2781) `new #expr#()` creates a class from a dynamic name, the same as `new "#expr#"()`, in BoxLang and CF script
- [BL-2780](https://ortussolutions.atlassian.net/browse/BL-2780) `boxlang format` no longer changes what code does. Audited over about 3,800 BoxLang and CFML files (BoxLang tests, bx-orm, cborm, ColdBox, ContentBox, TimeBox): every formatted file now parses back to the same code, and formatting it again changes nothing, except a few comments in unusual places (such as after a multi-line call) that move once. Fixed: `static`, `final` and `abstract` dropped from functions; `class final {}` output for final classes; `;` inside single-expression lambdas; script attribute values such as `timeout=10` turned into strings; an alignment padding run before `=` in single-line attributes; empty `{}` component bodies dropped and body-less custom tags self-closed; a `cf` prefix added to CF script components (`cfsavecontent`); tag components losing `<cfimport>`, `<cfargument>`s and closing tags; plain `/* ****` comments turned into doc comments; doc comment banner rows changing the hint; a comment before `else`, `else if` or `catch` dropping the branch; comments inside `<cfelse>` lost; a `//` comment absorbing the next line in files with CR line endings; destructuring patterns and the second `for ( a, b in c )` variable lost; `castAs int` printed as `castAs "int"`; quoted named arguments printed bare; `<bx:while condition>` printed as an interpolation; `<bx:bx:finally>`; taglib imports dropped; open-ended ranges and bare member references (`.map( .getName )`) crashing; formatter-ignore regions re-indented; and line breaks, alignment and comment spacing that changed on every pass. Struct, property and function parameter line breaks are now decided from the printed width rather than the source, so they no longer change on the next pass; numbers print as written (`0.95` no longer becomes `.95`); and a comment between two chained calls breaks the chain instead of landing mid-line. Also checked with the bx-orm, ColdBox and TimeBox formatter configs and with each non-default option: `semicolons: false` keeps the semicolons the next line would otherwise join (a bare `return`, `break`, `continue`, `param`, `import`, a function without a body, before a line starting with `(` or `[`, and after an open-ended range); `binaryOperatorsPadding: false` keeps the spaces around word operators and around `&&`, `||`, `&` and `^`, which the parser does not read without them; `comments.wrap` no longer adds a star to the closing line, keeps star banners, and leaves doc comment lines with an `@` whole; `member_spacing` is applied between all class members, so `preserve_blank_lines: false` no longer changes the spacing on the next pass; and closure arguments and chains are measured from their printed text. The parser now trims `param` names and ends a template `elseif` body before the next branch, so comments in a later `<cfelse>` attach to it. `continue // note` without a semicolon keeps `continue` a keyword

## [1.18.0] - 2026-10-02

- <https://boxlang.ortusbooks.com/readme/release-history/1.18.0>

## [1.17.6] - 2026-09-25

- <https://boxlang.ortusbooks.com/readme/release-history/1.17.0#patch-releases>

## [1.17.5] - 2026-09-14

- <https://boxlang.ortusbooks.com/readme/release-history/1.17.0#patch-releases>

## [1.17.4] - 2026-09-11

- <https://boxlang.ortusbooks.com/readme/release-history/1.17.0#patch-releases>

## [1.17.3] - 2026-09-05

- <https://boxlang.ortusbooks.com/readme/release-history/1.17.0#patch-releases>

## [1.17.2] - 2026-09-04

- <https://boxlang.ortusbooks.com/readme/release-history/1.17.0#patch-releases>

## [1.17.1] - 2026-09-01

- <https://boxlang.ortusbooks.com/readme/release-history/1.17.0#patch-releases>

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

[unreleased]: https://github.com/ortus-boxlang/BoxLang/compare/v1.18.0...HEAD
[1.18.0]: https://github.com/ortus-boxlang/BoxLang/compare/v1.17.6...v1.18.0
[1.17.6]: https://github.com/ortus-boxlang/BoxLang/compare/v1.17.5...v1.17.6
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
