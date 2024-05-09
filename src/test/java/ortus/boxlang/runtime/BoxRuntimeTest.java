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
package ortus.boxlang.runtime;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.MissingIncludeException;

public class BoxRuntimeTest {

	@DisplayName( "It can startup" )
	@Test
	@Disabled( "We can't shutdown the runtime singleton in the middle of an async test suite" )
	public void testItCanStartUp() {
		BoxRuntime runtime = BoxRuntime.getInstance( true );
		assertThat( BoxRuntime.getInstance() ).isSameInstanceAs( runtime );
		assertThat( runtime.getStartTime() ).isNotNull();

		// Shutdown and restart to ensure we're in debug mode - just in case the runtime was already running from another test.
		runtime.shutdown();
		BoxRuntime runtime2 = BoxRuntime.getInstance( true );
		assertThat( runtime2.inDebugMode() ).isTrue();
	}

	@DisplayName( "It can shutdown" )
	@Test
	@Disabled( "We can't shutdown the runtime singleton in the middle of an async test suite" )
	public void testItCanShutdown() {
		BoxRuntime runtime = BoxRuntime.getInstance( true );
		// Ensure shutdown sets instance to null
		runtime.shutdown();
		assertThat( BoxRuntime.hasInstance() ).isFalse();
	}

	@DisplayName( "It can execute a template" )
	@Test
	public void testItCanExecuteATemplate() {
		String testTemplate;
		try {
			testTemplate = ( new File( getClass().getResource( "/test-templates/BoxRuntime.cfm" ).toURI() ) ).getPath();
		} catch ( URISyntaxException e ) {
			throw new MissingIncludeException( "Invalid template path to execute.", "", getClass().getResource( "/test-templates/BoxRuntime.bx" ).toString(),
			    e );
		}
		assertDoesNotThrow( () -> {
			BoxRuntime instance = BoxRuntime.getInstance( true );
			instance.executeTemplate( testTemplate );

		} );
	}

	@DisplayName( "It can execute a template URL" )
	@Test
	public void testItCanExecuteATemplateURL() {
		URL testTemplate = getClass().getResource( "/test-templates/BoxRuntime.cfm" );

		assertDoesNotThrow( () -> {
			BoxRuntime instance = BoxRuntime.getInstance( true );
			instance.executeTemplate( testTemplate );

		} );
	}

	@DisplayName( "It can execute an expression" )
	@Test
	public void testItCanExecuteAnExpression() {

		BoxRuntime	instance	= BoxRuntime.getInstance( true );
		Object		result;
		result = instance.executeStatement( "3+3" );
		assertThat( result ).isEqualTo( 6 );

		result = instance.executeStatement( "3" );
		assertThat( result ).isEqualTo( 3 );

		result = instance.executeStatement( "'foo' & 'bar'" );
		assertThat( result ).isEqualTo( "foobar" );

		result = instance.executeStatement( "5*6" );
		assertThat( result ).isEqualTo( 30 );

	}

	@DisplayName( "It can execute a statement" )
	@Test
	public void testItCanExecuteAStatement() {

		BoxRuntime	instance	= BoxRuntime.getInstance( true );
		IBoxContext	context		= new ScriptingRequestBoxContext();

		Object		result		= instance.executeStatement( "foo=2+2", context );
		assertThat( result ).isEqualTo( 4 );
		assertThat( context.getScopeNearby( VariablesScope.name ).get( Key.of( "foo" ) ) ).isEqualTo( 4 );

		result = instance.executeStatement( "variables.bar=2+3", context );
		assertThat( result ).isEqualTo( 5 );
		assertThat( context.getScopeNearby( VariablesScope.name ).get( Key.of( "bar" ) ) ).isEqualTo( 5 );

	}

	@DisplayName( "It can execute statements" )
	@Test
	public void testItCanExecuteStatements() {

		BoxRuntime	instance	= BoxRuntime.getInstance( true );
		IBoxContext	context		= new ScriptingRequestBoxContext();

		String		src			= """
		                          brad='wood';
		                          luis=brad & ' majano'
		                          """;
		instance.executeSource( src, context );
		assertThat( context.getScopeNearby( VariablesScope.name ).get( Key.of( "brad" ) ) ).isEqualTo( "wood" );
		assertThat( context.getScopeNearby( VariablesScope.name ).get( Key.of( "luis" ) ) ).isEqualTo( "wood majano" );

	}

	@DisplayName( "It can execute more statements" )
	@Test
	public void testItCanExecuteMoreStatements() {

		BoxRuntime	instance	= BoxRuntime.getInstance( true );
		IBoxContext	context		= new ScriptingRequestBoxContext();

		String		src			= """
		                          variables['system'] = createObject('java','java.lang.System');

		                          a = 1;
		                          while(a < 10) {
		                             switch(variables.a) {
		                             case 0: {
		                          	 break;
		                             }
		                            default: {
		                          	 break;
		                             }
		                          }
		                          if(!a % 2 == 0) {
		                          }
		                          a +=1;

		                          }
		                          //assert(variables["a"] == 10);
		                          """;
		instance.executeSource( src, context );
		assertThat( context.getScopeNearby( VariablesScope.name ).containsKey( Key.of( "system" ) ) ).isTrue();

	}

	@DisplayName( "It can get the default file extensions" )
	@Test
	public void testItCanGetTheDefaultFileExtensions() {
		BoxRuntime instance = BoxRuntime.getInstance( true );
		assertThat( instance.getRuntimeFileExtensions() ).containsExactly(
		    ".bx", ".bxs", ".bxm"
		);
	}

	@DisplayName( "It can add a file extension" )
	@Test
	public void testItCanAddAFileExtension() {
		BoxRuntime instance = BoxRuntime.getInstance( true );
		instance.registerFileExtensions( ".cfm", ".cfc" );
		assertThat( instance.getRuntimeFileExtensions() ).containsExactly(
		    ".bx", ".bxs", ".bxm", ".cfm", ".cfc"
		);
	}

}
