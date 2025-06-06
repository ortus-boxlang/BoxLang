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
package ortus.boxlang.runtime.bifs.global.cli;

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.jupiter.api.AfterEach;
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

public class CLIReadTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	Key					result	= Key.result;
	static InputStream	originalIn;

	@BeforeAll
	public static void setUp() {
		originalIn = System.in;
		String					simulatedInput	= "Hello, World!\nAnotherWorld\n";
		ByteArrayInputStream	inputStream		= new ByteArrayInputStream( simulatedInput.getBytes() );
		System.setIn( inputStream );
		instance = BoxRuntime.getInstance( true );
	}

	@AfterEach
	public void tearDown() {
		// Reset System.in to avoid issues with reused input streams
		System.setIn( originalIn );
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It can read from the system with no prompt" )
	@Test
	public void testRead() {
		// Execute the source
		instance.executeSource(
		    """
		    	result = CLIRead()
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Hello, World!" );
	}

	@DisplayName( "It can read from the system with a prompt" )
	@Test
	public void testReadWithPrompt() {
		// Execute the source
		instance.executeSource(
		    """
		    	result = CLIRead( "Enter a value: " )
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "AnotherWorld" );
	}

}
