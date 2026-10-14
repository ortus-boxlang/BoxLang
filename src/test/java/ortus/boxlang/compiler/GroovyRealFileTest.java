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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * Exercises the Groovy dialect against real, standalone {@code .groovy} files on disk
 * (under {@code src/test/resources/groovy/}), rather than the inline Java string sources
 * every other Groovy test in this package uses. This proves two things the inline-string
 * tests can't:
 * <ul>
 * <li>{@code Parser.detectFile}'s own ".groovy" -&gt; {@code BoxSourceType.GROOVYSCRIPT}
 * extension mapping is exercised for real, via {@code RunnableLoader.loadClass(
 * ResolvedFilePath, ...)} - the same file-based class-loading path a real BoxLang
 * application uses, not just {@code executeSource(String, ...)}.</li>
 * <li>Larger, more realistic Groovy programs (a class with several members, a
 * multi-feature script, a multi-class-only file) - combining many of the individual
 * features {@code GroovyExecutionTest}/{@code GroovyClassExecutionTest} test in
 * isolation - actually work together end to end.</li>
 * </ul>
 */
public class GroovyRealFileTest {

	private static final Path GROOVY_FIXTURES = Path.of( "src/test/resources/groovy" );

	private IBoxContext newContext() {
		BoxRuntime instance = BoxRuntime.getInstance( true );
		return new ScriptingRequestBoxContext( instance.getRuntimeContext() );
	}

	private IClassRunnable instantiateFromFile( String fileName, IBoxContext context, Object... constructorArgs ) {
		Class<?>		targetClass	= RunnableLoader.getInstance().loadClass( ResolvedFilePath.of( GROOVY_FIXTURES.resolve( fileName ) ), context );
		DynamicObject	instance	= DynamicObject.of( targetClass ).invokeConstructor( context, constructorArgs );
		return ( IClassRunnable ) instance.getTargetInstance();
	}

	private String readFixture( String fileName ) throws IOException {
		return Files.readString( GROOVY_FIXTURES.resolve( fileName ) );
	}

	@Test
	@DisplayName( "a real BankAccount.groovy class file loads from disk, instantiates, and runs" )
	public void testBankAccountClassFile() {
		IBoxContext		context	= newContext();
		IClassRunnable	account	= instantiateFromFile( "BankAccount.groovy", context, "Alice", "CHECKING" );

		account.dereferenceAndInvoke( context, Key.of( "deposit" ), new Object[] { 150 }, false );
		account.dereferenceAndInvoke( context, Key.of( "deposit" ), new Object[] { 25 }, false );
		account.dereferenceAndInvoke( context, Key.of( "withdraw" ), new Object[] { 20 }, false );

		// Field defaults, an explicit constructor, mutation across multiple calls, and a
		// Range smart-switch case (feeTier(), via describe()) all worked together.
		Object describeResult = account.dereferenceAndInvoke( context, Key.of( "describe" ), new Object[] {}, false );
		assertThat( describeResult ).isEqualTo( "Alice (CHECKING) balance=155 tier=standard" );

		// findAll{}/.size() over the instance's own mutated history field.
		Object totalDeposits = account.dereferenceAndInvoke( context, Key.of( "totalDeposits" ), new Object[] {}, false );
		assertThat( totalDeposits.toString() ).isEqualTo( "2" );

		// Regex Pattern smart-switch case.
		Object	validId		= account.dereferenceAndInvoke( context, Key.of( "isValidAccountNumber" ), new Object[] { "AB-123456" }, false );
		Object	invalidId	= account.dereferenceAndInvoke( context, Key.of( "isValidAccountNumber" ), new Object[] { "not-an-id" }, false );
		assertThat( validId ).isEqualTo( true );
		assertThat( invalidId ).isEqualTo( false );

		// Real Java interop: the returned anonymous class instance genuinely implements
		// java.lang.Runnable (a real JDK dynamic proxy), not just a duck-typed lookalike.
		Object notifier = account.dereferenceAndInvoke( context, Key.of( "notifier" ), new Object[] {}, false );
		assertThat( notifier instanceof Runnable ).isTrue();
		( ( Runnable ) notifier ).run();
	}

	@Test
	@DisplayName( "a real inventory.groovy script file loads from disk and runs to completion" )
	public void testInventoryScriptFile() throws IOException {
		IBoxContext	context	= newContext();
		// Read (rather than execute-by-path directly) so this exercises the exact same
		// executeSource() entry point every other GroovyExecutionTest case does, but with
		// a real, substantial, multi-feature Groovy PROGRAM living in its own .groovy file
		// instead of an inline Java string literal.
		String		source	= readFixture( "inventory.groovy" );
		Object		result	= BoxRuntime.getInstance().executeSource( source, context, BoxSourceType.GROOVYSCRIPT );

		// Confirms (together, in one real program): an enum, a Range smart-switch case,
		// map literals with dot-access, "==~" full-match regex, collect{}/findAll{}, a
		// closure reassigning an outer script-level variable, tuple destructuring, and a
		// general (non-curated-name) bare-identifier command-style call ("audit note").
		assertThat( result ).isEqualTo( "widget,gadget total=44 valid=2 report=widget:LOW|gadget:MEDIUM audit=scan-complete" );
	}

	@Test
	@DisplayName( "a real multi-class-only Shapes.groovy file loads its first class from disk and runs" )
	public void testMultiClassOnlyFile() {
		IBoxContext		context	= newContext();
		IClassRunnable	circle	= instantiateFromFile( "Shapes.groovy", context, 2 );

		Object			area	= circle.dereferenceAndInvoke( context, Key.of( "area" ), new Object[] {}, false );
		assertThat( area.toString() ).isEqualTo( "12" );
	}

}
