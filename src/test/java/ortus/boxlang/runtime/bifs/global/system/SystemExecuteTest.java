
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

		String content = FileSystemUtil.readString( testTextFile );
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

		String content = FileSystemUtil.readString( testTextFile );
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

	@DisplayName( "onLine is called before the process exits and output is still captured" )
	@Test
	public void testOnLineBeforeExit() {
		if ( FileSystemUtil.IS_WINDOWS ) {
			return;
		}
		instance.executeSource(
		    """
		    lines = [];
		    stamps = [];
		    start = getTickCount();
		    result = SystemExecute( name="sh", arguments=[ "-c", "echo first; sleep 2; echo second" ],
		        onLine=( line, stream ) => { lines.append( line & ":" & stream ); stamps.append( getTickCount() - start ); } );
		    total = getTickCount() - start;
		    """,
		    context );
		var lines = variables.getAsArray( Key.of( "lines" ) );
		assertEquals( 2, lines.size() );
		assertEquals( "first:stdout", lines.get( 0 ) );
		assertEquals( "second:stdout", lines.get( 1 ) );
		// first line arrived well before the 2s sleep ended
		assertTrue( ( ( Number ) variables.getAsArray( Key.of( "stamps" ) ).get( 0 ) ).longValue() < 1500 );
		assertEquals( "first\nsecond", variables.getAsStruct( result ).get( Key.output ) );
		assertEquals( false, variables.getAsStruct( result ).get( Key.cancelled ) );
	}

	@DisplayName( "Stdout and stderr are reported separately and a large volume of both does not deadlock" )
	@Test
	public void testBothStreamsDrained() {
		if ( FileSystemUtil.IS_WINDOWS ) {
			return;
		}
		instance.executeSource(
		    """
		    out = 0; err = 0;
		    result = SystemExecute( name="sh", arguments=[ "-c", "i=0; while [ $i -lt 20000 ]; do echo out$i; echo err$i >&2; i=$((i+1)); done" ],
		        onLine=( line, stream ) => { if ( stream == "stdout" ) { out++ } else { err++ } } );
		    """,
		    context );
		assertEquals( 20000, variables.get( Key.of( "out" ) ) );
		assertEquals( 20000, variables.get( Key.of( "err" ) ) );
		assertTrue( variables.getAsStruct( result ).getAsString( Key.output ).startsWith( "out0\n" ) );
		assertTrue( variables.getAsStruct( result ).getAsString( Key.error ).startsWith( "err0\n" ) );
	}

	@DisplayName( "mergeErrors folds stderr into stdout, false keeps them apart" )
	@Test
	public void testMergeErrors() {
		if ( FileSystemUtil.IS_WINDOWS ) {
			return;
		}
		instance.executeSource(
		    """
		    merged = SystemExecute( name="sh", arguments=[ "-c", "echo a; echo b >&2" ], mergeErrors=true );
		    split = SystemExecute( name="sh", arguments=[ "-c", "echo a; echo b >&2" ] );
		    """,
		    context );
		var merged = variables.getAsStruct( Key.of( "merged" ) );
		assertTrue( merged.getAsString( Key.output ).contains( "a" ) );
		assertTrue( merged.getAsString( Key.output ).contains( "b" ) );
		assertEquals( "", merged.getAsString( Key.error ) );
		var split = variables.getAsStruct( Key.of( "split" ) );
		assertEquals( "a", split.get( Key.output ) );
		assertEquals( "b", split.get( Key.error ) );
	}

	@DisplayName( "mergeErrors cannot be combined with an error file" )
	@Test
	public void testMergeErrorsWithErrorFileThrows() {
		org.junit.jupiter.api.Assertions.assertThrows( ortus.boxlang.runtime.types.exceptions.BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    SystemExecute( name="echo", arguments="hi", error="x.log", mergeErrors=true );
		    """,
		    context ) );
	}

	@DisplayName( "Cancellation works on a silent process and is distinct from a timeout" )
	@Test
	public void testCancelSilent() {
		if ( FileSystemUtil.IS_WINDOWS ) {
			return;
		}
		instance.executeSource(
		    """
		    start = getTickCount();
		    result = SystemExecute( name="sleep", arguments="30", isCancelled=() => getTickCount() - start > 500 );
		    elapsed = getTickCount() - start;
		    """,
		    context );
		var r = variables.getAsStruct( result );
		assertTrue( r.getAsBoolean( Key.of( "cancelled" ) ) );
		assertTrue( r.getAsBoolean( Key.terminated ) );
		assertEquals( false, r.get( Key.timeout ) );
		assertTrue( ( ( Number ) variables.get( Key.of( "elapsed" ) ) ).longValue() < 10000 );
	}

	@DisplayName( "Cancellation keeps a line that stalled before its newline" )
	@Test
	public void testCancelMidLine() {
		if ( FileSystemUtil.IS_WINDOWS ) {
			return;
		}
		instance.executeSource(
		    """
		    start = getTickCount();
		    result = SystemExecute( name="sh", arguments=[ "-c", "echo done1; printf partial; sleep 30" ], isCancelled=() => getTickCount() - start > 800 );
		    """,
		    context );
		var r = variables.getAsStruct( result );
		assertTrue( r.getAsBoolean( Key.of( "cancelled" ) ) );
		assertEquals( "done1\npartial", r.get( Key.output ) );
	}

	@DisplayName( "Cancellation works while a process continuously emits output" )
	@Test
	public void testCancelChatty() {
		if ( FileSystemUtil.IS_WINDOWS ) {
			return;
		}
		instance.executeSource(
		    """
		    count = 0;
		    start = getTickCount();
		    result = SystemExecute( name="sh", arguments=[ "-c", "while true; do echo spam; done" ],
		        onLine=( l, s ) => { count++ }, isCancelled=() => getTickCount() - start > 800 );
		    elapsed = getTickCount() - start;
		    """,
		    context );
		assertTrue( variables.getAsStruct( result ).getAsBoolean( Key.of( "cancelled" ) ) );
		assertTrue( ( ( Number ) variables.get( Key.of( "count" ) ) ).intValue() > 0 );
		assertTrue( ( ( Number ) variables.get( Key.of( "elapsed" ) ) ).longValue() < 10000 );
	}

	@DisplayName( "Timeout still works with callbacks on silent, mid-line and chatty processes" )
	@Test
	public void testTimeoutWithCallbacks() {
		if ( FileSystemUtil.IS_WINDOWS ) {
			return;
		}
		instance.executeSource(
		    """
		    silent = SystemExecute( name="sleep", arguments="30", timeout=1, terminateOnTimeout=true, onLine=( l, s ) => {}, isCancelled=() => false );
		    midline = SystemExecute( name="sh", arguments=[ "-c", "printf partial; sleep 30" ], timeout=1, terminateOnTimeout=true, onLine=( l, s ) => {} );
		    chatty = SystemExecute( name="sh", arguments=[ "-c", "while true; do echo spam; done" ], timeout=1, terminateOnTimeout=true, onLine=( l, s ) => {} );
		    """,
		    context );
		for ( String k : new String[] { "silent", "midline", "chatty" } ) {
			var r = variables.getAsStruct( Key.of( k ) );
			assertTrue( k, r.getAsBoolean( Key.timeout ) );
			assertTrue( k, r.getAsBoolean( Key.terminated ) );
			assertEquals( false, r.get( Key.of( "cancelled" ) ), k );
		}
	}

	@DisplayName( "An onLine callback that throws kills the process and rethrows" )
	@Test
	public void testOnLineThrows() {
		if ( FileSystemUtil.IS_WINDOWS ) {
			return;
		}
		long start = System.currentTimeMillis();
		org.junit.jupiter.api.Assertions.assertThrows( Exception.class, () -> instance.executeSource(
		    """
		    SystemExecute( name="sh", arguments=[ "-c", "echo hi; sleep 30" ], onLine=( l, s ) => { throw( "boom" ); } );
		    """,
		    context ) );
		assertTrue( System.currentTimeMillis() - start < 15000 );
	}

	@DisplayName( "Existing calls report cancelled=false" )
	@Test
	public void testDefaultsUnchanged() {
		instance.executeSource( "result = SystemExecute( \"echo\", \"hi\" );", context );
		var r = variables.getAsStruct( result );
		assertEquals( "hi", r.get( Key.output ) );
		assertEquals( false, r.get( Key.of( "cancelled" ) ) );
		assertEquals( false, r.get( Key.terminated ) );
	}

}
