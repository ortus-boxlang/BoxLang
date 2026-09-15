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

}
