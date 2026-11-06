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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.services.CodeProfilerService;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Method splitting and profiling instrumentation are both "behind the scenes"
 * bytecode transforms, and they interact badly unless they agree on the shape of
 * the generated method:
 * <ul>
 * <li>The profiler wraps each method body in a catch-all try whose handler reads a
 * method-local temp. A split sub-method inherits the parent body verbatim (so it
 * still references the parent's local slots) but gets a fresh tracker, so a
 * sub-method local must never be allocated on top of an inherited slot.</li>
 * <li>A method is only split at statement boundaries (DividerNodes). A large
 * SCRIPT body compiles its statements with an EMPTY_UNLESS_JUMPING context, which
 * is also "empty" - gating dividers on identity with EMPTY left scripts with no
 * split point at all.</li>
 * <li>A split must never land inside a try-catch block's protected range: the
 * exception table entry is only written when the block's start/end/handler labels
 * all survive in the same method, so cutting the range silently drops the handler
 * and lets the exception escape uncaught.</li>
 * </ul>
 * These tests compile and RUN synthetic sources only - no client code involved.
 */
// Profiling instrumentation is ASM-boxpiler-specific; skip under the Java boxpiler.
@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
class ProfilerMethodSplitTest {

	static BoxRuntime	runtime;
	CodeProfilerService	service;
	IBoxContext			context;
	IScope				variables;
	boolean				prevProfiler;
	Key					x		= new Key( "x" );
	Key					result	= new Key( "result" );

	@BeforeAll
	static void setUp() {
		runtime = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	void setup() {
		service											= runtime.getCodeProfilerService();
		context											= new ScriptingRequestBoxContext( runtime.getRuntimeContext() );
		variables										= context.getScopeNearby( VariablesScope.name );
		prevProfiler									= runtime.getConfiguration().codeProfilerEnabled;
		runtime.getConfiguration().codeProfilerEnabled	= true;
		RunnableLoader.getInstance().getBoxpiler().clearPagePool();
		RunnableLoader.getInstance().getBoxpiler().clearClassFiles();
		service.setActive( true );
		service.reset();
	}

	@AfterEach
	void teardown() {
		runtime.getConfiguration().codeProfilerEnabled = prevProfiler;
		service.setActive( false );
		service.reset();
	}

	/**
	 * The sum of 0..(n-1) - the value {@code x} holds after running {@code n} of
	 * {@code x = x + i} statements starting from 1.
	 */
	private static int sumPlusOne( int n ) {
		return 1 + n * ( n - 1 ) / 2;
	}

	/**
	 * A big CFML template whose - small - {@code <cftry>} sits at the TOP of the
	 * page, followed by a long run of assignments. This is the shape of the reported
	 * failure: the template body must be split, and the split sub-method that keeps
	 * the try also inherits the parent's local slots.
	 */
	private static String templateWithSmallTryAtTop( int n ) {
		StringBuilder sb = new StringBuilder();
		sb.append( "<cfset result = 'none'>\n" );
		sb.append( "<cfset x = 1>\n" );
		sb.append( "<cftry>\n" );
		sb.append( "  <cfset foo = 1>\n" );
		sb.append( "  <cfcatch><cfset bar = 2></cfcatch>\n" );
		sb.append( "</cftry>\n" );
		for ( int i = 0; i < n; i++ ) {
			sb.append( "<cfset x = x + " ).append( i ).append( ">\n" );
		}
		sb.append( "<cfset result = 'done'>\n" );
		return sb.toString();
	}

	/**
	 * A big CFML template whose {@code <cftry>} body is itself large enough to
	 * attract a naive split, with more large code after the try. The try body must
	 * stay in one piece: the throw happens at its very start, so a split inside the
	 * range would drop the handler and let the exception escape.
	 */
	private static String templateWithLargeTryAndLargeTail( int inTry, int afterTry ) {
		StringBuilder sb = new StringBuilder();
		sb.append( "<cfset result = 'none'>\n" );
		sb.append( "<cftry>\n" );
		sb.append( "  <cfset x = 1>\n" );
		sb.append( "  <cfif x EQ 1>\n" );
		sb.append( "    <cfthrow message='boom'>\n" );
		sb.append( "  </cfif>\n" );
		for ( int i = 0; i < inTry; i++ ) {
			sb.append( "  <cfset x = x + " ).append( i ).append( ">\n" );
		}
		sb.append( "  <cfcatch><cfset result = 'caught'></cfcatch>\n" );
		sb.append( "</cftry>\n" );
		for ( int i = 0; i < afterTry; i++ ) {
			sb.append( "<cfset x = x + " ).append( i ).append( ">\n" );
		}
		return sb.toString();
	}

	/**
	 * A big BoxLang SCRIPT whose LAST statement is a plain expression statement, so
	 * its body is compiled with EMPTY_UNLESS_JUMPING.
	 */
	private static String scriptEndingWithExpression( int n ) {
		StringBuilder sb = new StringBuilder();
		sb.append( "x = 1;\n" );
		for ( int i = 0; i < n; i++ ) {
			sb.append( "x = x + " ).append( i ).append( ";\n" );
		}
		return sb.toString();
	}

	/**
	 * A template whose ENTIRE body is one try/catch - too large to keep the protected
	 * range in one method, but still compilable by cutting through the range.
	 */
	private static String templateWrappedInTry( int n ) {
		StringBuilder sb = new StringBuilder();
		sb.append( "<cfset result = 'none'>\n" );
		sb.append( "<cftry>\n" );
		sb.append( "  <cfset x = 1>\n" );
		for ( int i = 0; i < n; i++ ) {
			sb.append( "  <cfset x = x + " ).append( i ).append( ">\n" );
		}
		sb.append( "  <cfcatch><cfset result = 'caught'></cfcatch>\n" );
		sb.append( "</cftry>\n" );
		return sb.toString();
	}

	/**
	 * A template whose body is one try/catch containing a single huge {@code <cfif>}:
	 * there is no statement boundary inside it at all, so no split can ever bring it
	 * under the JVM's per-method limit.
	 */
	private static String templateWithUnsplittableTry( int n ) {
		StringBuilder sb = new StringBuilder();
		sb.append( "<cfset result = 'none'>\n" );
		sb.append( "<cftry>\n" );
		sb.append( "  <cfset x = 1>\n" );
		sb.append( "  <cfif x EQ 1>\n" );
		for ( int i = 0; i < n; i++ ) {
			sb.append( "    <cfset x = x + " ).append( i ).append( ">\n" );
		}
		sb.append( "  </cfif>\n" );
		sb.append( "  <cfcatch><cfset result = 'caught'></cfcatch>\n" );
		sb.append( "</cftry>\n" );
		return sb.toString();
	}

	@DisplayName( "It splits a profiled template with a small try at the top without a VerifyError" )
	@Test
	void testProfiledTemplateWithTryAtTopSplitsAndRuns() {
		int n = 2000;
		assertDoesNotThrow( () -> runtime.executeSource( templateWithSmallTryAtTop( n ), context, BoxSourceType.CFTEMPLATE ) );
		assertEquals( sumPlusOne( n ), ( ( Number ) variables.get( x ) ).intValue() );
		assertEquals( "done", variables.get( result ) );
	}

	@DisplayName( "It splits a profiled script whose last statement is an expression" )
	@Test
	void testProfiledScriptEndingWithExpressionSplitsAndRuns() {
		int n = 2000;
		assertDoesNotThrow( () -> runtime.executeSource( scriptEndingWithExpression( n ), context, BoxSourceType.BOXSCRIPT ) );
		assertEquals( sumPlusOne( n ), ( ( Number ) variables.get( x ) ).intValue() );
	}

	@DisplayName( "It does not split inside a try body, so the catch still runs" )
	@Test
	void testSplitKeepsTryCatchProtection() {
		// The try body is big enough to attract a split on its own, and there is more
		// large code after the try, so the method MUST be split - just never inside the
		// protected range. The throw at the top of the try then has to be caught.
		assertDoesNotThrow( () -> runtime.executeSource( templateWithLargeTryAndLargeTail( 700, 900 ), context, BoxSourceType.CFTEMPLATE ) );
		assertEquals( "caught", variables.get( result ) );
	}

	@DisplayName( "It does not split inside a try body when profiling is disabled either" )
	@Test
	void testSplitKeepsTryCatchProtectionWithoutProfiling() {
		runtime.getConfiguration().codeProfilerEnabled = false;
		RunnableLoader.getInstance().getBoxpiler().clearPagePool();
		RunnableLoader.getInstance().getBoxpiler().clearClassFiles();

		assertDoesNotThrow( () -> runtime.executeSource( templateWithLargeTryAndLargeTail( 700, 900 ), context, BoxSourceType.CFTEMPLATE ) );
		assertEquals( "caught", variables.get( result ) );
	}

	@DisplayName( "It still compiles a file whose whole body is one oversized try/catch" )
	@Test
	void testOversizedTryStillCompiles() {
		// The protected range alone is bigger than a JVM method, so it CANNOT be kept in
		// one piece. Failing the compile here would break files that used to work, so the
		// splitter falls back to cutting through the range. The code must still run.
		int n = 2500;
		assertDoesNotThrow( () -> runtime.executeSource( templateWrappedInTry( n ), context, BoxSourceType.CFTEMPLATE ) );
		assertEquals( sumPlusOne( n ), ( ( Number ) variables.get( x ) ).intValue() );
	}

	@DisplayName( "It reports an unsplittable oversized construct as a source-level error" )
	@Test
	void testOversizedUnsplittableConstructReportsSourceError() {
		// A single <cfif> holds every statement, so there is no split boundary at all.
		// The user must get a clear message about what to change, never ASM's
		// "Method too large" from the depths of the class writer.
		BoxRuntimeException e = assertThrows( BoxRuntimeException.class,
		    () -> runtime.executeSource( templateWithUnsplittableTry( 2500 ), context, BoxSourceType.CFTEMPLATE ) );

		assertThat( e.getMessage() ).contains( "64KB" );
		assertThat( e.getMessage() ).containsMatch( "try/catch block starting on line [1-9][0-9]*" );
		assertThat( e.getMessage() ).contains( "into a function and call it" );
		assertThat( e.getMessage() ).contains( "into an include" );
		assertThat( e.getMessage() ).doesNotContain( "Method too large" );
	}

	@DisplayName( "It reports an oversized construct outside a try as a source-level error" )
	@Test
	void testOversizedConstructOutsideTryReportsSourceError() {
		StringBuilder sb = new StringBuilder();
		sb.append( "<cfset x = 1>\n" );
		sb.append( "<cfif x EQ 1>\n" );
		for ( int i = 0; i < 2500; i++ ) {
			sb.append( "  <cfset x = x + " ).append( i ).append( ">\n" );
		}
		sb.append( "</cfif>\n" );

		BoxRuntimeException e = assertThrows( BoxRuntimeException.class,
		    () -> runtime.executeSource( sb.toString(), context, BoxSourceType.CFTEMPLATE ) );

		assertThat( e.getMessage() ).contains( "64KB" );
		assertThat( e.getMessage() ).containsMatch( "code starting on line [1-9][0-9]* cannot be split" );
		assertThat( e.getMessage() ).contains( "one huge if/loop/switch body" );
		assertThat( e.getMessage() ).contains( "into an include" );
		assertThat( e.getMessage() ).doesNotContain( "Method too large" );
	}

	@DisplayName( "It builds a source-level error message from a file path and a reason" )
	@Test
	void testOversizedMethodExceptionMessage() {
		BoxRuntimeException e = AsmHelper.oversizedMethodException( "/app/views/login.cfm", "The try/catch block is too big." );

		assertThat( e.getMessage() ).contains( "/app/views/login.cfm" );
		assertThat( e.getMessage() ).contains( "64KB" );
		assertThat( e.getMessage() ).contains( "The try/catch block is too big." );
		assertThat( e.getMessage() ).contains( "into a function and call it" );
		assertThat( e.getMessage() ).contains( "into an include" );
		assertThat( e.getMessage() ).doesNotContain( "<cf" );
	}
}
