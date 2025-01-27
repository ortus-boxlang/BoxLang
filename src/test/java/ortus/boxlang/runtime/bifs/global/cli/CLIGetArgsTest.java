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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.CLIOptions;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;

public class CLIGetArgsTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	Key					result	= Key.result;

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		// Create a partial mock of the instance
		instance	= Mockito.spy( instance );
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "Can build the args when not passed" )
	@Test
	public void testEmpty() {
		// Execute the source
		// @formatter:off
		instance.executeSource(
		    """
		      	result = CLIGetArgs()
		    	println( result )
		    """,
		    context );
		// @formatter:on

		IStruct args = ( IStruct ) variables.get( result );
		assertThat( args ).isInstanceOf( IStruct.class );
		assertThat( args.size() ).isEqualTo( 2 );
		assertThat( args.getAsStruct( Key.of( "options" ) ).size() ).isEqualTo( 0 );
		assertThat( args.getAsArray( Key.of( "positionals" ) ).size() ).isEqualTo( 0 );
	}

	@DisplayName( "Can build the args with args passed" )
	@Test
	public void testWithArgs() {
		// Use mockito to return my own CLIOptions when calling runtime.getCLIOptions()
		CLIOptions options = mock( CLIOptions.class );
		when( options.cliArgs() ).thenReturn( List.of( "--debug", "--!verbose", "--bundles=Spec", "-o='/path/to/file'", "-v", "my/path/template" ) );
		when( instance.getCliOptions() ).thenReturn( options );

		// Execute the source
		// @formatter:off
		instance.executeSource(
		    """
		      	result = CLIGetArgs()
		    	println( result )
		    """,
		    context );
		// @formatter:on

		IStruct args = ( IStruct ) variables.get( result );
		assertThat( args ).isInstanceOf( IStruct.class );
		assertThat( args.size() ).isEqualTo( 2 );
	}
}
