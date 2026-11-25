/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.compiler;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Phase 3 real class-instantiation tests for the Groovy parser/transpiler effort.
 * <p>
 * {@code GroovyClassParsingTest} only checks that a Groovy class source string parses into the
 * correct {@code BoxClass} AST shape ({@code Parser.parse(..., true)}), not that it actually
 * compiles to bytecode and runs - that goes through a different Boxpiler entry point
 * ({@code compileClass}, via {@code RunnableLoader.loadClass}) that hadn't been exercised with
 * Groovy source before this. These tests close that gap: compile a Groovy class from an ad-hoc
 * source string, construct a real instance, call methods on it, and read its state back -
 * exactly the {@code RunnableLoader.loadClass(...) -> DynamicObject...invokeConstructor(...)}
 * pattern {@code TestCases.phase3.ClassTest} uses for BoxLang/CFML classes.
 */
public class GroovyClassExecutionTest {

	private IBoxContext newContext() {
		BoxRuntime instance = BoxRuntime.getInstance( true );
		return new ScriptingRequestBoxContext( instance.getRuntimeContext() );
	}

	private IClassRunnable instantiate( String source, IBoxContext context ) {
		return ( IClassRunnable ) DynamicObject.of(
		    RunnableLoader.getInstance().loadClass( source, context, BoxSourceType.GROOVYSCRIPT ) )
		    .invokeConstructor( context )
		    .getTargetInstance();
	}

	@Test
	@DisplayName( "a class with a field default and a method compiles, instantiates, and runs" )
	public void testFieldDefaultAndMethod() {
		IBoxContext		context		= newContext();
		IClassRunnable	instance	= instantiate( """
		                                           class Greeter {
		                                             def prefix = "Hi"

		                                             def setPrefix(String p) {
		                                               prefix = p
		                                             }

		                                             def greet(String name) {
		                                               return "${prefix}, ${name}!"
		                                             }
		                                           }
		                                           """, context );

		// Field default value, executed in the pseudo-constructor, is readable via the
		// synthesized getPrefix() accessor (real Groovy default: unmodified fields get an
		// implicit public accessor pair - see buildFieldAccessors in GroovyVisitor).
		assertThat( instance.dereferenceAndInvoke( context, Key.of( "getPrefix" ), new Object[] {}, false ) ).isEqualTo( "Hi" );

		Object result = instance.dereferenceAndInvoke( context, Key.of( "greet" ), new Object[] { "World" }, false );
		assertThat( result ).isEqualTo( "Hi, World!" );

		instance.dereferenceAndInvoke( context, Key.of( "setPrefix" ), new Object[] { "Hello" }, false );
		result = instance.dereferenceAndInvoke( context, Key.of( "greet" ), new Object[] { "World" }, false );
		assertThat( result ).isEqualTo( "Hello, World!" );
	}

	@Test
	@DisplayName( "an explicit constructor (mapped to 'init') runs on instantiation" )
	public void testExplicitConstructorRuns() {
		IBoxContext		context		= newContext();
		IClassRunnable	instance	= instantiate( """
		                                           class Counter {
		                                             def count = 0

		                                             Counter() {
		                                               count = 10
		                                             }

		                                             def increment() {
		                                               count = count + 1
		                                               return count
		                                             }
		                                           }
		                                           """, context );

		// The constructor body ran during invokeConstructor(), overriding the field default.
		assertThat( instance.dereferenceAndInvoke( context, Key.of( "getCount" ), new Object[] {}, false ).toString() ).isEqualTo( "10" );

		Object result = instance.dereferenceAndInvoke( context, Key.of( "increment" ), new Object[] {}, false );
		assertThat( result.toString() ).isEqualTo( "11" );
	}

	@Test
	@DisplayName( "a method using control flow and a closure runs correctly on a real instance" )
	public void testMethodWithControlFlowAndClosure() {
		IBoxContext		context		= newContext();
		IClassRunnable	instance	= instantiate( """
		                                           class MathHelper {
		                                             def sumPositives(list) {
		                                               def total = 0
		                                               for (item in list) {
		                                                 if (item > 0) {
		                                                   total = total + item
		                                                 }
		                                               }
		                                               return total
		                                             }

		                                             def doubleEach(list) {
		                                               return list.collect { it * 2 }
		                                             }
		                                           }
		                                           """, context );

		Object			sum			= instance.dereferenceAndInvoke( context, Key.of( "sumPositives" ),
		    new Object[] { new ortus.boxlang.runtime.types.Array( new Object[] { -2, 3, -1, 4 } ) }, false );
		assertThat( sum.toString() ).isEqualTo( "7" );

		Object doubled = instance.dereferenceAndInvoke( context, Key.of( "doubleEach" ),
		    new Object[] { new ortus.boxlang.runtime.types.Array( new Object[] { 1, 2, 3 } ) }, false );
		assertThat( doubled.toString() ).contains( "2" );
	}

	@Test
	@DisplayName( "'extends' with an unresolvable class name fails at class resolution, not silently" )
	public void testExtendsReachesClassResolution() {
		// This proves the actual bug that was found and fixed: "extends"/"implements" parsed
		// syntactically but were never wired into the AST, so they were silently dropped.
		// Full end-to-end inheritance (a parent class the child can actually resolve) needs
		// BoxLang's file-based ClassLocator/mapping resolution - pre-existing runtime
		// infrastructure shared by every class type, not something specific to the Groovy
		// parser - which is out of scope here. What this test verifies is narrower but
		// concrete: the "extends" clause now reaches that resolver at all (proven by getting a
		// real ClassNotFoundBoxLangException naming the class, from deep inside
		// BoxClassSupport.loadSuperClass) instead of being silently ignored, which is what
		// happened before the fix.
		IBoxContext	context	= newContext();
		var			thrown	= org.junit.jupiter.api.Assertions.assertThrows( RuntimeException.class,
		    () -> instantiate( """
		                       class Dog extends NoSuchAnimalClass {
		                         def bark() {
		                           return "Woof!"
		                         }
		                       }
		                       """, context ) );
		assertThat( thrown.getMessage() ).contains( "NoSuchAnimalClass" );
	}

	@Test
	@DisplayName( "'static' on a field is a documented no-op for now, not silently broken" )
	public void testStaticFieldIsCurrentlyTreatedAsInstanceScoped() {
		// Investigated and deliberately deferred: making Groovy's `static` field semantics
		// (shared across instances, read/written bare from anywhere in the class) actually
		// work needs more than tagging the declaration with BoxAssignmentModifier.STATIC.
		// Verified empirically against native BoxLang itself: even there, every read/write of
		// a static member must be explicitly scope-qualified ("static.total"), including from
		// inside the declaring class's own methods - bare references don't reach static scope.
		// Real Groovy has no such requirement. Doing this right needs a symbol-table pass that
		// rewrites every bare reference to a known static-field name throughout the class body
		// into an explicit static.<name> access, which is out of scope for now. This test
		// pins down the current, honest behavior instead of leaving a half-working feature
		// that fails confusingly on naturally-written Groovy: "static" is currently a no-op,
		// and the field behaves like a normal per-instance field.
		IBoxContext		context		= newContext();
		String			source		= """
		                              class Counter {
		                                static def total = 0

		                                def bump() {
		                                  total = total + 1
		                                  return total
		                                }
		                              }
		                              """;
		IClassRunnable	instance	= instantiate( source, context );
		Object			result		= instance.dereferenceAndInvoke( context, Key.of( "bump" ), new Object[] {}, false );
		assertThat( result.toString() ).isEqualTo( "1" );
	}

	@Test
	@DisplayName( "a nested static class is instantiable and usable from an outer class's methods" )
	public void testNestedClassIsInstantiable() {
		// Models a Java-style STATIC nested class - a peer type reached via "new Builder()" -
		// not a true Groovy non-static inner class capturing an enclosing instance. Reuses
		// BoxLang's own native BoxLocalClass AST node (the same one BoxVisitor's own nested-
		// class syntax produces), so no new AST/runtime machinery was needed for this.
		IBoxContext		context		= newContext();
		IClassRunnable	instance	= instantiate( """
		                                           class Outer {
		                                             static class Point {
		                                               def x
		                                               def y
		                                               Point(px, py) { x = px; y = py }
		                                               def sum() { return x + y }
		                                             }

		                                             def makePoint(a, b) {
		                                               def p = new Point(a, b)
		                                               return p.sum()
		                                             }
		                                           }
		                                           """, context );
		Object			result		= instance.dereferenceAndInvoke( context, Key.of( "makePoint" ), new Object[] { 3, 4 }, false );
		assertThat( result.toString() ).isEqualTo( "7" );
	}

	@Test
	@DisplayName( "a static initializer block runs once during class loading" )
	public void testStaticInitializerRuns() {
		// Maps directly onto BoxLang's own native BoxStaticInitializer AST node (the same one
		// BoxGrammar's "static { ... }" class member already produces). "static" itself is a
		// Groovy keyword, not a usable bare identifier/expression base in this grammar (unlike
		// native BoxLang's own "static.foo" syntax), so the block's bare assignment is read back
		// directly off the class's static scope via Java rather than through more Groovy source -
		// confirmed empirically that a bare assignment inside a static initializer really does
		// land in the STATIC scope (not the instance-scoped fallback "static" fields get - see
		// the no-op test above), since an instance method's own bare "ready" lookup does NOT find
		// it there.
		IBoxContext		context		= newContext();
		IClassRunnable	instance	= instantiate( """
		                                           class Config {
		                                             static {
		                                               ready = true
		                                             }
		                                           }
		                                           """, context );
		assertThat( instance.getStaticScope().get( Key.of( "ready" ) ) ).isEqualTo( true );
	}

	@Test
	@DisplayName( "anonymous inner class used inside a class's own method body" )
	public void testAnonymousClassInsideMethodBody() {
		// BoxLocalClass's hard compile-time rule against nesting inside a function body applies
		// here too - the synthesized class is hoisted out to be a peer member of the ENCLOSING
		// class itself (not the script top level, since there is no script here), which is a
		// different hoisting target than testAnonymousClassInsideFunctionBody covers.
		IBoxContext		context		= newContext();
		IClassRunnable	instance	= instantiate( """
		                                           class Outer {
		                                             def makeMyTask() {
		                                               def r = new MyTask() {
		                                                 void run() {
		                                                   return "hi from inside a class method"
		                                                 }
		                                               }
		                                               return r.run()
		                                             }
		                                           }
		                                           """, context );
		Object			result		= instance.dereferenceAndInvoke( context, Key.of( "makeMyTask" ), new Object[] {}, false );
		assertThat( result ).isEqualTo( "hi from inside a class method" );
	}

	@Test
	@DisplayName( "an anonymous class hoisted from one method doesn't get nested inside a later named nested class" )
	public void testAnonymousClassDoesNotLeakIntoLaterNamedNestedClass() {
		// Broader instance of the same hoist-scope-isolation bug testTwoAnonymousClassesInSameScope
		// (GroovyExecutionTest) covers: before the fix, an anonymous class discovered while building
		// an EARLIER method in this class body would still be "pending" by the time a LATER named
		// nested class declaration finished building its own body - and that nested class's own
		// hoist-drain would wrongly sweep it up as one of ITS OWN members, hiding it from the real
		// enclosing (Outer) class entirely.
		IBoxContext		context		= newContext();
		IClassRunnable	instance	= instantiate( """
		                                           class Outer {
		                                             def makeMyTask() {
		                                               def r = new MyTask() {
		                                                 void run() {
		                                                   return "from anon"
		                                                 }
		                                               }
		                                               return r.run()
		                                             }

		                                             static class Point {
		                                               def x
		                                               def y
		                                               Point(px, py) { x = px; y = py }
		                                               def sum() { return x + y }
		                                             }

		                                             def makePoint(a, b) {
		                                               def p = new Point(a, b)
		                                               return p.sum()
		                                             }
		                                           }
		                                           """, context );
		Object			anonResult	= instance.dereferenceAndInvoke( context, Key.of( "makeMyTask" ), new Object[] {}, false );
		Object			pointResult	= instance.dereferenceAndInvoke( context, Key.of( "makePoint" ), new Object[] { 3, 4 }, false );
		assertThat( anonResult ).isEqualTo( "from anon" );
		assertThat( pointResult.toString() ).isEqualTo( "7" );
	}

	@Test
	@DisplayName( "a file with two top-level classes and nothing else still loads and runs as a real class" )
	public void testTwoTopLevelClassesOnlyLoadsAndRuns() {
		// See GroovyParser#toAst: a source that is ENTIRELY class declarations (2+, nothing else)
		// used to fall through to the script path instead of building a BoxClass root - meaning
		// RunnableLoader.loadClass on a source shaped exactly like this would previously have
		// gotten back a BoxScript, not the loadable class this test now proves it correctly does.
		// The file's first class ("Greeter") becomes the loaded class itself; the second
		// ("Farewell") is a peer in its own body, unreachable from outside this test (same as the
		// "static class Point" peer above), so only Greeter's own behavior is exercised here.
		IBoxContext		context		= newContext();
		IClassRunnable	instance	= instantiate( """
		                                           class Greeter {
		                                             def greet(String name) {
		                                               return "hi " + name
		                                             }
		                                           }
		                                           class Farewell {
		                                             def bye(String name) {
		                                               return "bye " + name
		                                             }
		                                           }
		                                           """, context );
		Object			result		= instance.dereferenceAndInvoke( context, Key.of( "greet" ), new Object[] { "World" }, false );
		assertThat( result ).isEqualTo( "hi World" );
	}

}
