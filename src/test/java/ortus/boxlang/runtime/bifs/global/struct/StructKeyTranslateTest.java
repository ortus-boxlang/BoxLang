
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

package ortus.boxlang.runtime.bifs.global.struct;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;

public class StructKeyTranslateTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@AfterAll
	public static void teardown() {
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		variables.clear();
	}

	@DisplayName( "It tests the BIF StructKeyTranslate with defaults" )
	@Test
	public void testBif() {
		instance.executeSource(
		    """
		    result = {
		    	cow: {
		    		noise: "moo",
		    		size: "large"
		    	},
		    	"bird.noise": "chirp",
		    	"bird.size": "small"
		    };
		    structKeyTranslate( result );
		    """,
		    context );
		assertTrue( variables.get( result ) instanceof IStruct );
		assertTrue( variables.getAsStruct( result ).containsKey( Key.of( "cow" ) ) );
		assertTrue( variables.getAsStruct( result ).containsKey( Key.of( "bird" ) ) );
		assertFalse( variables.getAsStruct( result ).containsKey( Key.of( "bird.noise" ) ) );
		assertFalse( variables.getAsStruct( result ).containsKey( Key.of( "bird.size" ) ) );
		IStruct bird = StructCaster.cast( variables.getAsStruct( result ).get( Key.of( "bird" ) ) );
		assertEquals( bird.get( Key.of( "noise" ) ), "chirp" );
		assertEquals( bird.get( Key.of( "size" ) ), "small" );

	}

	@DisplayName( "It tests the member function for StructKeyTranslate" )
	@Test
	public void testMemberFunction() {
		// Remove use the following examples to create a test for your member function
		// Full source execution:
		// instance.executeSource(
		// """
		// myObj="foo";
		// result = myObj.StructKeyTranslate();
		// """,
		// context );
		// assertThat( variables.get( result ) ).isEqualTo( "foo" );

		// Statement execution only and return the result:
		// assertThat( ( Boolean ) instance.executeStatement( " ' + "foo" +'.StructKeyTranslate()" ) ).isTrue();
	}

}
