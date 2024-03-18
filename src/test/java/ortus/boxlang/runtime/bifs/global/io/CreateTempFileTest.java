
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

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

public class CreateTempFileTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() throws IOException {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() throws IOException {
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It can create a temp file with all defaults" )
	@Test
	@Ignore
	public void testBif() {
		// @formatter:off
		instance.executeSource(
		    """
		       	result = createTempFile();
		    """,
		    context );
		// @formatter:on
		assertThat( Files.isRegularFile(
		    Paths.get( variables.getAsString( result ) )
		) ).isTrue();
	}

	@Test
	@DisplayName( "It can create a temp file with a directory passed" )
	public void testBifWithDirectory() {
		// @formatter:off
		instance.executeSource(
		    """
		       	result = createTempFile( getTempDirectory() );
		    """,
		    context );
		// @formatter:on
		assertThat( Files.isRegularFile(
		    Paths.get( variables.getAsString( result ) )
		) ).isTrue();
	}

	@Test
	@DisplayName( "It can create a temp file with a directory passed and a prefix" )
	public void testBifWithDirectoryAndPrefix() {
		// @formatter:off
		instance.executeSource(
		    """
		       	result = createTempFile( getTempDirectory(), "luigi" );
		    """,
		    context );
		// @formatter:on
		assertThat( Files.isRegularFile(
		    Paths.get( variables.getAsString( result ) )
		) ).isTrue();
		assertThat( variables.getAsString( result ) ).contains( "luigi" );
	}

	@Test
	@DisplayName( "It can create a temp file with a directory passed and a prefix and a suffix" )
	public void testBifWithDirectoryAndPrefixAndSuffix() {
		// @formatter:off
		instance.executeSource(
		    """
		       	result = createTempFile( getTempDirectory(), "luigi", ".mario" );
		    """,
		    context );
		// @formatter:on
		assertThat( Files.isRegularFile(
		    Paths.get( variables.getAsString( result ) )
		) ).isTrue();
		assertThat( variables.getAsString( result ) ).contains( "luigi" );
		assertThat( variables.getAsString( result ) ).contains( ".mario" );
	}
}
