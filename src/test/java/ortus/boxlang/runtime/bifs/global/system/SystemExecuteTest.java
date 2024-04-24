
/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ortus.boxlang.runtime.bifs.global.system;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class SystemExecuteTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result			= new Key( "result" );
	static String		testTextFile	= "src/test/resources/tmp/executeTest/output.txt";
	static String		tmpDirectory	= "src/test/resources/tmp/executeTest";

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
		if ( !FileSystemUtil.exists( tmpDirectory ) )
			FileSystemUtil.createDirectory( tmpDirectory );
	}

	@AfterAll
	public static void teardown() {
		if ( FileSystemUtil.exists( tmpDirectory ) )
			FileSystemUtil.deleteDirectory( tmpDirectory, true );
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
		if ( FileSystemUtil.exists( testTextFile ) ) {
			FileSystemUtil.deleteFile( testTextFile );
		}
	}

	@DisplayName( "It tests the BIF SystemExecute with default args" )
	@Test
	public void testBifExecute() {
		instance.executeSource(
		    """
		    result = SystemExecute( "java", "--version" );
		    """,
		    context );
		assertTrue(
		    variables.get( result ) instanceof Struct
		);
		assertTrue(
		    variables.getAsStruct( result ).containsKey( Key.output )
		);
		assertTrue(
		    variables.getAsStruct( result ).containsKey( Key.error )
		);
		assertThat( variables.getAsStruct( result ).get( Key.error ) ).isEqualTo( "" );
		assertTrue(
		    variables.getAsStruct( result ).containsKey( Key.timeout )
		);
		assertTrue(
		    variables.getAsStruct( result ).containsKey( Key.terminated )
		);

		assertTrue( variables.getAsStruct( result ).getAsString( Key.output ).length() > 0 );

	}

	@DisplayName( "It tests the BIF SystemExecute with default args" )
	@Test
	@Disabled( "Not working on windows: Cannot run program 'echo': CreateProcess error=2, The system cannot find the file specified " )
	public void testQuotedStringArgs() {
		instance.executeSource(
		    """
		    result = SystemExecute( "echo", "blah 'foo bar baz'" );
		    """,
		    context );
		assertTrue(
		    variables.get( result ) instanceof Struct
		);
		assertTrue(
		    variables.getAsStruct( result ).containsKey( Key.output )
		);
		assertTrue(
		    variables.getAsStruct( result ).containsKey( Key.error )
		);
		assertThat( variables.getAsStruct( result ).get( Key.error ) ).isEqualTo( "" );

		assertEquals( "blah 'foo bar baz'", variables.getAsStruct( result ).getAsString( Key.output ) );

	}

	@DisplayName( "It tests the BIF SystemExecute with an error" )
	@Test
	public void testBifError() {
		instance.executeSource(
		    """
		    result = SystemExecute( "java", "--blah" );
		    """,
		    context );
		assertTrue(
		    variables.get( result ) instanceof Struct
		);
		assertTrue(
		    variables.getAsStruct( result ).containsKey( Key.output )
		);
		assertTrue(
		    variables.getAsStruct( result ).containsKey( Key.error )
		);
		assertTrue( variables.getAsStruct( result ).getAsString( Key.error ).length() > 0 );
		assertTrue(
		    variables.getAsStruct( result ).containsKey( Key.timeout )
		);
		assertTrue(
		    variables.getAsStruct( result ).containsKey( Key.terminated )
		);

	}

	@DisplayName( "It tests the BIF SystemExecute with output to a file" )
	@Test
	public void testBifFileOutput() {
		variables.put( Key.of( "outputFile" ), testTextFile );
		instance.executeSource(
		    """
		    result = SystemExecute( name="java", arguments="--version", output=outputFile );
		    """,
		    context );
		assertTrue(
		    variables.get( result ) instanceof Struct
		);
		assertTrue(
		    variables.getAsStruct( result ).containsKey( Key.output )
		);
		assertTrue( variables.getAsStruct( result ).getAsString( Key.output ) == null );
		assertTrue(
		    variables.getAsStruct( result ).containsKey( Key.error )
		);
		assertTrue( variables.getAsStruct( result ).getAsString( Key.error ).length() == 0 );
		assertTrue(
		    variables.getAsStruct( result ).containsKey( Key.timeout )
		);
		assertTrue(
		    variables.getAsStruct( result ).containsKey( Key.terminated )
		);

		assertTrue(
		    FileSystemUtil.exists( testTextFile )
		);

		String content = StringCaster.cast( FileSystemUtil.read( testTextFile ) );
		assertTrue( content.length() > 0 );

	}

	@DisplayName( "It tests the BIF SystemExecute with output to a file" )
	@Test
	public void testBifFileError() {
		variables.put( Key.of( "outputFile" ), testTextFile );
		instance.executeSource(
		    """
		    result = SystemExecute( name="java", arguments="--blah", error=outputFile );
		    """,
		    context );
		assertTrue(
		    variables.get( result ) instanceof Struct
		);
		assertTrue(
		    variables.getAsStruct( result ).containsKey( Key.output )
		);
		assertTrue( variables.getAsStruct( result ).getAsString( Key.output ).length() == 0 );
		assertTrue(
		    variables.getAsStruct( result ).containsKey( Key.error )
		);
		assertTrue( variables.getAsStruct( result ).getAsString( Key.error ) == null );
		assertTrue(
		    variables.getAsStruct( result ).containsKey( Key.timeout )
		);
		assertTrue(
		    variables.getAsStruct( result ).containsKey( Key.terminated )
		);

		assertTrue(
		    FileSystemUtil.exists( testTextFile )
		);

		String content = StringCaster.cast( FileSystemUtil.read( testTextFile ) );
		assertTrue( content.length() > 0 );

	}

	@DisplayName( "It tests the BIF SystemExecute with timeout" )
	@Test
	public void testBifExecuteWithTimeout() {
		// Skipping this whole test block on Windows, for now, as attempting to use timeout from ProcessManager will fail
		// https://www.ibm.com/support/pages/timeout-command-run-batch-job-exits-immediately-and-returns-error-input-redirection-not-supported-exiting-process-immediately
		if ( !FileSystemUtil.IS_WINDOWS ) {
			variables.put( Key.of( "cmd" ), FileSystemUtil.IS_WINDOWS ? "timeout" : "sleep" );
			instance.executeSource(
			    """
			    result = SystemExecute( cmd, "5", 1 );
			    """,
			    context );

			assertTrue(
			    variables.get( result ) instanceof Struct
			);
			assertTrue(
			    variables.getAsStruct( result ).containsKey( Key.output )
			);
			assertThat( variables.getAsStruct( result ).get( Key.output ) ).isEqualTo( "" );
			assertTrue(
			    variables.getAsStruct( result ).containsKey( Key.error )
			);
			assertThat( variables.getAsStruct( result ).get( Key.error ) ).isEqualTo( "" );
			assertTrue(
			    variables.getAsStruct( result ).containsKey( Key.timeout )
			);
			assertTrue(
			    variables.getAsStruct( result ).containsKey( Key.terminated )
			);

			assertTrue( variables.getAsStruct( result ).getAsBoolean( Key.timeout ) );

			instance.executeSource(
			    """
			    result = SystemExecute( cmd, "5", 1, true );
			    """,
			    context );
			assertTrue(
			    variables.get( result ) instanceof Struct
			);
			assertTrue(
			    variables.getAsStruct( result ).containsKey( Key.output )
			);
			assertThat( variables.getAsStruct( result ).get( Key.output ) ).isEqualTo( null );
			assertTrue(
			    variables.getAsStruct( result ).containsKey( Key.error )
			);
			assertThat( variables.getAsStruct( result ).get( Key.error ) ).isEqualTo( null );
			assertTrue(
			    variables.getAsStruct( result ).containsKey( Key.timeout )
			);
			assertTrue(
			    variables.getAsStruct( result ).containsKey( Key.terminated )
			);

			assertTrue( variables.getAsStruct( result ).getAsBoolean( Key.timeout ) );
			assertTrue( variables.getAsStruct( result ).getAsBoolean( Key.terminated ) );
		}

	}

}
