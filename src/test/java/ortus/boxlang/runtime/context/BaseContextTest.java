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
package ortus.boxlang.runtime.context;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.LocalScope;
import ortus.boxlang.runtime.scopes.RequestScope;
import ortus.boxlang.runtime.scopes.ServerScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;

public class BaseContextTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {

	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@Test
	public void testScripting() {

		instance.executeSource(
		    """
		    result = getBoxContext().getVisibleScopes()
		      """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
		IStruct vars = variables.getAsStruct( result );

		assertThat( vars.getAsStruct( Key.contextual ) ).isInstanceOf( IStruct.class );
		IStruct contextual = vars.getAsStruct( Key.contextual );
		assertThat( contextual ).containsKey( VariablesScope.name );
		assertThat( contextual ).containsKey( RequestScope.name );
		assertThat( contextual ).containsKey( ServerScope.name );

		assertThat( vars.getAsStruct( Key.lexical ) ).isInstanceOf( IStruct.class );
		IStruct lexical = vars.getAsStruct( Key.lexical );
		assertThat( lexical.size() ).isEqualTo( 0 );

	}

	@Test
	public void testUDF() {

		instance.executeSource(
		    """
		    function foo() {
		      variables.result = getBoxContext().getVisibleScopes()
		    }
		    foo()
		        """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
		IStruct vars = variables.getAsStruct( result );

		assertThat( vars.getAsStruct( Key.contextual ) ).isInstanceOf( IStruct.class );
		IStruct contextual = vars.getAsStruct( Key.contextual );
		assertThat( contextual ).containsKey( VariablesScope.name );
		assertThat( contextual ).containsKey( RequestScope.name );
		assertThat( contextual ).containsKey( ServerScope.name );
		assertThat( contextual ).containsKey( ArgumentsScope.name );
		assertThat( contextual ).containsKey( LocalScope.name );

		assertThat( vars.getAsStruct( Key.lexical ) ).isInstanceOf( IStruct.class );
		IStruct lexical = vars.getAsStruct( Key.lexical );
		assertThat( lexical.size() ).isEqualTo( 0 );
	}

	@Test
	public void testClosure() {

		Object foo = instance.executeStatement(
		    """
		    foo = () => {
		      return getBoxContext().getVisibleScopes()
		    }
		    return foo()
		    	""",
		    context );
		assertThat( foo ).isInstanceOf( IStruct.class );
		IStruct vars = ( IStruct ) foo;

		assertThat( vars.getAsStruct( Key.contextual ) ).isInstanceOf( IStruct.class );
		IStruct contextual = vars.getAsStruct( Key.contextual );
		assertThat( contextual ).containsKey( RequestScope.name );
		assertThat( contextual ).containsKey( ServerScope.name );
		assertThat( contextual ).containsKey( ArgumentsScope.name );
		assertThat( contextual ).containsKey( LocalScope.name );

		assertThat( vars.getAsStruct( Key.lexical ) ).isInstanceOf( IStruct.class );
		IStruct lexical = vars.getAsStruct( Key.lexical );
		assertThat( lexical.size() ).isEqualTo( 1 );
		assertThat( lexical ).containsKey( Key.of( "foo" ) );

		IStruct fooScopes = lexical.getAsStruct( Key.of( "foo" ) );
		assertThat( fooScopes.size() ).isEqualTo( 1 );
		assertThat( fooScopes ).containsKey( VariablesScope.name );

	}

	@Test
	public void testClosureInUDF() {

		Object foo = instance.executeStatement(
		    """
		    function foo() {
		    	bar = () => {
		    		return getBoxContext().getVisibleScopes()
		    	}
		    	return bar()
		    }
		    return foo()
		          	""",
		    context );
		assertThat( foo ).isInstanceOf( IStruct.class );
		IStruct vars = ( IStruct ) foo;

		assertThat( vars.getAsStruct( Key.contextual ) ).isInstanceOf( IStruct.class );
		IStruct contextual = vars.getAsStruct( Key.contextual );
		assertThat( contextual ).containsKey( RequestScope.name );
		assertThat( contextual ).containsKey( ServerScope.name );
		assertThat( contextual ).containsKey( ArgumentsScope.name );
		assertThat( contextual ).containsKey( LocalScope.name );

		assertThat( vars.getAsStruct( Key.lexical ) ).isInstanceOf( IStruct.class );
		IStruct lexical = vars.getAsStruct( Key.lexical );
		assertThat( lexical.size() ).isEqualTo( 1 );
		assertThat( lexical ).containsKey( Key.of( "bar" ) );

		IStruct barScopes = lexical.getAsStruct( Key.of( "bar" ) );
		assertThat( barScopes.size() ).isEqualTo( 3 );
		assertThat( barScopes ).containsKey( VariablesScope.name );
		assertThat( barScopes ).containsKey( ArgumentsScope.name );
		assertThat( barScopes ).containsKey( LocalScope.name );

	}

	@Test
	public void testClosureInClosureInUDF() {

		Object foo = instance.executeStatement(
		    """
		     function foo() {
		     	bar = () => {
		    baz = () => {
		     			return getBoxContext().getVisibleScopes()
		    }
		    return baz();
		     	}
		     	return bar()
		     }
		     return foo()
		           	""",
		    context );
		assertThat( foo ).isInstanceOf( IStruct.class );
		IStruct vars = ( IStruct ) foo;

		assertThat( vars.getAsStruct( Key.contextual ) ).isInstanceOf( IStruct.class );
		IStruct contextual = vars.getAsStruct( Key.contextual );
		assertThat( contextual ).containsKey( RequestScope.name );
		assertThat( contextual ).containsKey( ServerScope.name );
		assertThat( contextual ).containsKey( ArgumentsScope.name );
		assertThat( contextual ).containsKey( LocalScope.name );

		assertThat( vars.getAsStruct( Key.lexical ) ).isInstanceOf( IStruct.class );
		IStruct lexical = vars.getAsStruct( Key.lexical );
		assertThat( lexical.size() ).isEqualTo( 2 );
		assertThat( lexical ).containsKey( Key.of( "bar" ) );

		IStruct barScopes = lexical.getAsStruct( Key.of( "bar" ) );
		assertThat( barScopes.size() ).isEqualTo( 3 );
		assertThat( barScopes ).containsKey( VariablesScope.name );
		assertThat( barScopes ).containsKey( ArgumentsScope.name );
		assertThat( barScopes ).containsKey( LocalScope.name );

		assertThat( lexical ).containsKey( Key.of( "baz" ) );

		IStruct bazScopes = lexical.getAsStruct( Key.of( "baz" ) );
		assertThat( bazScopes.size() ).isEqualTo( 2 );
		assertThat( bazScopes ).containsKey( ArgumentsScope.name );
		assertThat( bazScopes ).containsKey( LocalScope.name );

	}

	@Test
	public void testLambda() {

		Object foo = instance.executeStatement(
		    """
		    foo = () -> {
		      return getBoxContext().getVisibleScopes()
		    }
		    return foo()
		    	""",
		    context );
		assertThat( foo ).isInstanceOf( IStruct.class );
		IStruct vars = ( IStruct ) foo;

		assertThat( vars.getAsStruct( Key.contextual ) ).isInstanceOf( IStruct.class );
		IStruct contextual = vars.getAsStruct( Key.contextual );
		assertThat( contextual ).containsKey( ArgumentsScope.name );
		assertThat( contextual ).containsKey( LocalScope.name );

		assertThat( vars.getAsStruct( Key.lexical ) ).isInstanceOf( IStruct.class );
		IStruct lexical = vars.getAsStruct( Key.lexical );
		assertThat( lexical.size() ).isEqualTo( 0 );
	}

}