# BL-1046: Adobe savecontent assignment syntax

[Ticket](https://ortussolutions.atlassian.net/browse/BL-1046) ·
[PR #684](https://github.com/ortus-boxlang/BoxLang/pull/684) ·
[Adobe reference](https://guides.adobe.com/coldfusion/en/docs/cfml-reference/__references__/cfsavecontent.html)

Adobe documents this syntax starting with ColdFusion 2021:

```cfml
result = savecontent {
    writeOutput( "test" );
}
```

The ticket reports that a direct function argument such as
`writeDump( savecontent { writeOutput( "test" ); } )` is invalid in Adobe and
that Lucee does not support the assignment syntax.

## Changes since the original PR

The original published revision (`f58e7ee2250fcc3ef4ca924c4f62e036a6220e85`)
recognized a general savecontent expression, then rewrote complete assignments
into the existing component with a source-text `variable` attribute. It accepted
extra attributes, rejected final/static modifiers, and synthesized local
declarations before capture.

Following the review feedback, this revision:

- Moves recognition into the assignment grammar and removes the previous-token
  gate and statement-level rewrite.
- Preserves the destination expression, operator, modifiers, and body in
  `BoxSaveContentAssignment`, supporting computed/function/deep destinations
  through existing assignment compilation on both backends.
- Supports compound operators and final/static modifiers; rejects extra
  attributes on the assignment form instead of assuming tag attributes apply.
- Captures before assigning without clearing existing locals, restores the
  surrounding buffer on exceptions and early exits, and reuses component result
  handling without temporary variables in BoxLang scopes.
- Adds visitor and pretty-printer integration plus regressions for the reported
  cases, AST replacement/round trips, and execution with the ASM profiler enabled.

These changes are scoped to BL-1046. The validation below establishes tested
BoxLang behavior; the remaining Adobe compatibility questions are listed
separately and have not been independently verified on Adobe.

## Behavior

- CF script assignments preserve their destination expressions, assignment
  operators, and modifiers. Coverage includes `&=`, arithmetic assignments,
  `final var`, static assignments inside a class, interpolated/computed keys,
  function-returned destinations, and deep member assignments.
- The capture body runs before destination assignment. Existing local values
  remain available during capture. The surrounding output buffer is restored
  before evaluating and assigning the destination.
- Nested capture bodies are supported, including the form in Adobe's reference.
- On an early exit or exception, the destination is not assigned. The output
  buffer is restored in `finally`. Tests cover function return, component-loop
  break/continue, and loops inside the capture body.
- Extra attributes in the assignment form produce parse errors, following the
  reviewer's Adobe tests. Ordinary savecontent statements/tags retain their
  existing `variable`, `trim`, and `append` behavior.
- The original PR's complete-statement restriction is retained for chained or
  embedded assignments. Whether Adobe accepts those forms remains unverified.
- The native BoxLang grammar is unchanged; this is CF syntax support.

## Implementation

The CF assignment grammar recognizes the capture body. A separate invalid-input
rule recognizes attribute syntax solely to issue a parse diagnostic, preventing
fallback into a lookup of a variable named `savecontent`.

`BoxSaveContentAssignment` retains the original target, operator, modifiers, and
component body. It does not replace the source with a statement block or turn
the destination into a string. Both pretty printers and AST visitors understand
this node.

The ASM and Java compilers reuse component-body/early-exit handling. A typed
callback invokes the existing assignment transformer with the captured string
and original context. `SaveContent.captureAssignment` manages the buffer.
There are no generated temporary variables in BoxLang scopes.

## Validation

The selected groups are `ortus.boxlang.compiler.*`,
`ortus.boxlang.runtime.components.*`, and `TestCases.*`, not the full repository
suite. Runs use Gradle offline mode and fresh BoxLang class-cache directories.

| Compiler | Tests reported in JUnit XML | Failures | Errors | Skipped |
| --- | ---: | ---: | ---: | ---: |
| Java | 1,522 | 0 | 0 | 342 |
| ASM | 1,529 | 0 | 0 | 7 |

Final selected-suite validation was rerun on 2026-11-04. All 42 common focused
runtime/AST cases ran without skips on both compilers. One additional profiler
regression ran on ASM and is intentionally skipped on Java, where that profiler
is not supported. It verifies capture output and execution counts for registered
spans in the example, not comprehensive timing equivalence. The broader skip
totals above include compiler-specific skips. Reports are archived under
`build/reports/tests/bl1046-final-asm` and `bl1046-final-java`.

Pre-publication focused checks were rerun on 2026-11-07 with fresh generated-class
caches: ASM reported 43 tests with no failures/errors/skips; Java reported 43
tests with no failures/errors and one intentional ASM-profiler skip. Repository
`spotlessCheck` and `git diff --check` passed. The broader results above remain
dated November 4; the focused rerun does not establish independent Adobe parity.

A focused ASM command, after creating a fresh writable cache directory:

```sh
BOXLANG_COMPILER=asm BOXLANG_CLASSGENERATIONDIRECTORY=/path/to/fresh/cache ./gradlew --offline test --tests 'ortus.boxlang.runtime.components.system.SaveContentTest' --tests 'ortus.boxlang.compiler.SaveContentAssignmentTest'
```

For Java, use `BOXLANG_COMPILER=java` and add `-PaddJavaCompiler=true`.
A fresh class cache matters because previously compiled script bytecode can
otherwise survive compiler source changes. Gradle creates build outputs,
module fixtures, reports, and caches. Java formatting uses the repository's
Eclipse profile through Spotless; the repository-wide `spotlessCheck` passed.

## Compatibility verification still needed

No independent Adobe execution was performed. The ticket, Adobe documentation,
and the reviewer's examples establish the basis for this implementation.
Confirm Adobe's exact destination-evaluation order, chained/embedded-assignment
support, and early-exit behavior before claiming complete Adobe parity.
CF-to-native-BoxLang transpilation also needs a separate decision: a CF source
pretty-print round trip is tested, but the native grammar does not accept this
assignment form.
