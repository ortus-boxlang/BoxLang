
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.net.URI;
import java.net.URISyntaxException;

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
import ortus.boxlang.runtime.types.IStruct;

public class SessionInvalidateTest {

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

	@DisplayName( "It tests the BIF SessionInvalidate" )
	@Test
	public void testBif() {
		instance.executeSource(
		    """
		    application name="unit-test-sm" sessionmanagement="true";
		         session.foo = "bar";
		         initialSession = duplicate( session );
		         SessionInvalidate();
		         result = session;
		         """,
		    context );
		IStruct initialSession = variables.getAsStruct( Key.of( "initialSession" ) );
		assertFalse( variables.getAsStruct( result ).containsKey( Key.of( "foo" ) ) );
		assertNotEquals( initialSession.getAsString( Key.of( "cfid" ) ), variables.getAsStruct( result ).getAsString( Key.of( "cfid" ) ) );
		assertNotEquals( initialSession.getAsDateTime( Key.of( "timeCreated" ) ), variables.getAsStruct( result ).getAsDateTime( Key.of( "timeCreated" ) ) );
		assertNotEquals( initialSession.getAsString( Key.of( "sessionid" ) ), variables.getAsStruct( result ).getAsString( Key.of( "sessionid" ) ) );
	}

	@DisplayName( "It tests onSessionEnd" )
	@Test
	public void testOnSessionEnd() {
		try {
			context = new ScriptingRequestBoxContext( instance.getRuntimeContext(),
			    new URI( "src/test/java/ortus/boxlang/runtime/bifs/global/system/testApp/index.bxm" ) );
		} catch ( URISyntaxException e ) {
			throw new RuntimeException( e );
		}
		instance.executeSource(
		    """
		    	application.brad = "wood";
		    	println( "in test: " & expandPath( "/foobar" ) )
		    	sessionInvalidate();
		    """,
		    context );
	}

}
