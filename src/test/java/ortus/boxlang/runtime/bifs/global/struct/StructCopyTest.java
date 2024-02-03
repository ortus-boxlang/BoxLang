
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

public class StructCopyTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			resultKey	= new Key( "result" );
	static Key			refKey		= new Key( "ref" );

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

	@DisplayName( "It tests the BIF StructCopy" )
	@Test
	public void testStructCopy() {
		instance.executeSource(
		    """
		         ref = {
		    timestamp: now(),
		      	foo : {
		      		bar : "baz"
		      	}
		      };
		         result = StructCopy( ref );
		      result.foo.bar = "blah";
		         """,
		    context );
		IStruct	ref		= StructCaster.cast( variables.get( refKey ) );
		IStruct	result	= StructCaster.cast( variables.get( resultKey ) );
		assertTrue( ref.getAsStruct( Key.of( "foo" ) ).containsKey( "bar" ) );
		assertTrue( result.getAsStruct( Key.of( "foo" ) ).containsKey( "bar" ) );
		// Test that only references were copied above the top keys
		assertEquals( result.getAsStruct( Key.of( "foo" ) ).get( Key.of( "bar" ) ), "blah" );
		assertEquals( ref.getAsStruct( Key.of( "foo" ) ).get( Key.of( "bar" ) ), "blah" );
	}

	@DisplayName( "It tests the Struct.copy member function" )
	@Test
	public void testMemberFunction() {
		instance.executeSource(
		    """
		         ref = {
		    timestamp: now(),
		      	foo : {
		      		bar : "baz"
		      	}
		      };
		         result = ref.copy();
		      result.foo.bar = "blah";
		         """,
		    context );
		IStruct	ref		= StructCaster.cast( variables.get( refKey ) );
		IStruct	result	= StructCaster.cast( variables.get( resultKey ) );
		assertTrue( ref.getAsStruct( Key.of( "foo" ) ).containsKey( "bar" ) );
		assertTrue( result.getAsStruct( Key.of( "foo" ) ).containsKey( "bar" ) );
		// Test that only references were copied above the top keys
		assertEquals( result.getAsStruct( Key.of( "foo" ) ).get( Key.of( "bar" ) ), "blah" );
		assertEquals( ref.getAsStruct( Key.of( "foo" ) ).get( Key.of( "bar" ) ), "blah" );
	}

}
