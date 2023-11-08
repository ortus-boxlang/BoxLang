package ortus.boxlang.compiler;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

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

public class TestExecution extends TestBase {

	@Test
	public void executeWhile() throws IOException {
		BoxRuntime	instance	= BoxRuntime.getInstance( true );
		IBoxContext	context		= new ScriptingBoxContext( instance.getRuntimeContext() );
		IScope		variables	= context.getScopeNearby( VariablesScope.name );

		instance.executeSource( """
		                        variables['system'] = createObject('java','java.lang.System');

		                        a = 1;
		                        while(a < 10) {
		                           switch(variables.a) {
		                           case 0: {
		                             variables.system.out.println("zero");
		                             break;
		                           }
		                          default: {
		                             variables.system.out.println("non zero");
		                             break;
		                           }
		                        }
		                        if(!a % 2 == 0) {
		                            variables.system.out.println("even and a=#variables.a#");
		                        }
		                        a +=1;

		                        }
		                        assert(variables["a"] == 10);
		                        """ );

		instance.shutdown();

	}

	@Test
	public void executeFor() throws IOException {
		BoxRuntime	instance	= BoxRuntime.getInstance( true );
		IBoxContext	context		= new ScriptingBoxContext( instance.getRuntimeContext() );

		instance.executeSource( """
		                                                               variables['system'] = createObject('java','java.lang.System');
		                        variables.a = 0;
		                                                               for(a = 0; a < 10; a++){
		                                                               	variables.system.out.println(a);
		                                                               }
		                                        assert(variables["a"] == 10);
		                                                               """ );
		instance.shutdown();
	}

	@Test
	public void comparison() throws IOException {
		BoxRuntime	instance	= BoxRuntime.getInstance( true );
		IBoxContext	context		= new ScriptingBoxContext( instance.getRuntimeContext() );

		Object		result		= instance.executeStatement( "6 > 5", context );
		assertThat( result ).isEqualTo( true );

		result = instance.executeStatement( "5 LT 10", context );
		assertThat( result ).isEqualTo( true );

		result = instance.executeStatement( "5 LESS THAN 10", context );
		assertThat( result ).isEqualTo( true );

		instance.shutdown();
	}

	@Test
	public void testDoWhileLoop() {
		BoxRuntime	instance	= BoxRuntime.getInstance( true );
		IBoxContext	context		= new ScriptingBoxContext( instance.getRuntimeContext() );
		IScope		variables	= context.getScopeNearby( VariablesScope.name );

		instance.executeSource(
		    """
		    result = 1;
		        do {
		       result = variables.result + 1;
		        } while( result < 10  );
		        """,
		    context );

		assertThat( variables.dereference( Key.of( "result" ), false ) ).isEqualTo( 10 );

	}

	@Test
	public void testCastAs() {

		BoxRuntime	instance	= BoxRuntime.getInstance( true );
		IBoxContext	context		= new ScriptingBoxContext( instance.getRuntimeContext() );
		assertThrows( Throwable.class, () -> instance.executeStatement( "5 castAs sdf",

		    context ) );

		Object result = instance.executeStatement( "5 castAs 'String'", context );
		assertThat( result ).isEqualTo( "5" );
		assertThat( result.getClass().getName() ).isEqualTo( "java.lang.String" );
	}

	@Test
	public void testTernary() {

		BoxRuntime	instance	= BoxRuntime.getInstance( true );
		IBoxContext	context		= new ScriptingBoxContext( instance.getRuntimeContext() );

		instance.executeSource(
		    """
		    tmp = true;
		      result = tmp ? 'itwastrue' : 'itwasfalse';
		    """,

		    context );
		assertThat( context.getScopeNearby( VariablesScope.name ).dereference( Key.of( "result" ), false ) ).isEqualTo( "itwastrue" );
	}

	@Test
	public void testString3() {

		BoxRuntime	instance	= BoxRuntime.getInstance( true );
		IBoxContext	context		= new ScriptingBoxContext( instance.getRuntimeContext() );
		IScope		variables	= context.getScopeNearby( VariablesScope.name );

		instance.executeSource(
		    """
		    // To escape a quote char, double it.
		    test4 = "Brad ""the guy"" Wood"
		    test5 = 'Luis ''the man'' Majano'
		      """,
		    context );

		assertThat( variables.dereference( Key.of( "test4" ), false ) ).isEqualTo( "Brad \"the guy\" Wood" );
		assertThat( variables.dereference( Key.of( "test5" ), false ) ).isEqualTo( "Luis 'the man' Majano" );
	}

}
