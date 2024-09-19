
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

package ortus.boxlang.runtime.bifs.global.io;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.Ignore;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class GetCanonicalPathTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result				= new Key( "result" );
	static String		testTextFile		= "src/test/resources/tmp/GetCanonicalPathTest/time.txt";
	static String		relativeTextFile	= "src/test/resources/tmp/GetCanonicalPathTest/../GetCanonicalPathTest/time.txt";
	static String		tmpDirectory		= "src/test/resources/tmp/GetCanonicalPathTest";

	@BeforeAll
	public static void setUp() throws IOException {
		instance = BoxRuntime.getInstance( true );

		if ( !FileSystemUtil.exists( testTextFile ) ) {
			FileSystemUtil.write( testTextFile, "file modified time test!".getBytes( "UTF-8" ), true );
		}
	}

	@AfterAll
	public static void teardown() throws IOException {
		if ( FileSystemUtil.exists( tmpDirectory ) ) {
			FileSystemUtil.deleteDirectory( tmpDirectory, true );
		}

	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It tests the BIF GetCanonicalPath" )
	@Test
	@Ignore
	public void testBif() {
		variables.put( Key.of( "testFile" ), relativeTextFile );
		instance.executeSource(
		    """
		    result = GetCanonicalPath( variables.testFile );
		    """,
		    context );
		assertTrue( variables.get( Key.of( "result" ) ) instanceof String );
		assertEquals( variables.getAsString( Key.of( "result" ) ), Path.of( testTextFile ).toAbsolutePath().toString() );
	}

	@DisplayName( "It ignores invalid paths" )
	@Test
	public void testIgnoresInvalidPaths() {
		instance.executeSource(
		    """
		    result = GetCanonicalPath( "C:foo/bar/C:foo/bar/test.txt" );
		    """,
		    context );
		assertThat( variables.get( Key.of( "result" ) ) ).isEqualTo( "C:foo/bar/C:foo/bar/test.txt" );
	}

}
