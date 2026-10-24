# BL-1046 Support Adobe's savecontent expression syntax

1. Problem Statement
   - Adobe ColdFusion allows the result of savecontent to be assigned directly to a variable:

         result = savecontent {
             writeOutput( "test" );
         }

   - BoxLang only supported savecontent as a statement: savecontent variable="result" { ... }
   - In CF script, the parser read result = savecontent as an assignment from a variable named
     savecontent, followed by a separate block, so the code failed at runtime with:
     "The requested key [savecontent] was not located in any scope or it's undefined"
   - This syntax appears in existing Adobe client code. Per the ticket, Lucee does not support it.

2. Proposed Solution
   - Rewrite the expression form at parse time into the existing savecontent component:
     result = savecontent { body } becomes savecontent variable="result" { body }
   - No runtime code changes. SaveContent.java is untouched, so trim, append and output capture
     behave exactly as before.
   - The change is limited to the CF parser, so BoxLang (.bx) files are unaffected.
   - Supported:
     - result = savecontent { ... }
     - var result = savecontent { ... }, which stays local to the function
     - Scoped and bracket targets such as local.result, variables.page.html, data[ "html" ] and items[ i ].html
     - Attributes such as trim=true and append=true
   - Reported as a clear parse error:
     - final or static, which would otherwise be silently ignored or write to the wrong scope
     - A variable attribute used together with the expression form
     - Any position other than a full assignment statement, such as a = b = savecontent { ... }
   - Still not supported (out of scope):
     - writeDump( savecontent { ... } ), which Adobe does not support either
     - html &= savecontent { ... }

3. Implementation Steps
   - Step 1: CFGrammar.g4
     - Add the exprSaveContent alternative to el2, so savecontent with optional attributes and a
       block can appear as the value of an assignment.
   - Step 2: CFParserControl.java
     - Add isSaveContentExpression(), a gate that only allows the rule when savecontent directly
       follows an = sign.
   - Step 3: CFVisitor.java
     - In visitExprStatInvocable, detect target = savecontent { ... } (optionally with var) and build
       the regular savecontent component, with the variable attribute set to the left side as written.
     - For var, declare the variable first so the content stays local to the function.
   - Step 4: CFExpressionVisitor.java
     - Add visitExprSaveContent() as a safety net that reports a clear error for any unsupported position.

4. Validation Plan
   - Tests added to SaveContentTest.java (9):
     - Expression syntax, bracket target, trim=true, and var inside a function without leaking into variables
     - The regular savecontent variable="result" { } statement still works
     - Errors for final, static, a duplicate variable attribute, and a = b = savecontent { }
   - Verification steps:
     - ./gradlew spotlessApply
     - ./gradlew test --tests "*SaveContentTest"
     - ./gradlew test
     - Manually ran CF scripts covering nested savecontent, loops, closures, classes, return inside
       the body, error line numbers, BoxLang script files, and CF to BX transpiling
   - Success criteria:
     - All added behavior is documented and tested, and all SaveContentTest tests pass (15: 6 existing, 9 new)
     - The ticket's example now captures and prints its content
     - Existing savecontent forms behave exactly as before
     - The full test suite passes (./gradlew test)
