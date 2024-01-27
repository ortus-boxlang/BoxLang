
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

package ortus.boxlang.runtime.bifs.global.format;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class BooleanFormatTest {

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

	@DisplayName( "It tests the BIF BooleanFormat" )
	@Test
	public void testBif() {
		assertEquals( ( String ) instance.executeStatement( "booleanFormat( 0 )" ), "false" );
		assertEquals( ( String ) instance.executeStatement( "booleanFormat( -1 )" ), "true" );
		assertEquals( ( String ) instance.executeStatement( "booleanFormat( 1 )" ), "true" );
		assertEquals( ( String ) instance.executeStatement( "booleanFormat( 'Y' )" ), "true" );
		assertEquals( ( String ) instance.executeStatement( "booleanFormat( 'N' )" ), "false" );
		assertEquals( ( String ) instance.executeStatement( "booleanFormat( 'Yes' )" ), "true" );
		assertEquals( ( String ) instance.executeStatement( "booleanFormat( 'No' )" ), "false" );
		assertEquals( ( String ) instance.executeStatement( "booleanFormat( 'true' )" ), "true" );
		assertEquals( ( String ) instance.executeStatement( "booleanFormat( 'false' )" ), "false" );
		assertThrows( BoxRuntimeException.class, () -> instance.executeStatement( "booleanFormat( 'blah' )" ) );
	}

	@DisplayName( "It tests the BooleanFormat Member function on a string" )
	@Test
	public void testStringMember() {
		assertEquals( ( String ) instance.executeStatement( "'Y'.booleanFormat()" ), "true" );
		assertEquals( ( String ) instance.executeStatement( "'N'.booleanFormat()" ), "false" );
		assertEquals( ( String ) instance.executeStatement( "'Yes'.booleanFormat()" ), "true" );
		assertEquals( ( String ) instance.executeStatement( "'No'.booleanFormat()" ), "false" );
		assertEquals( ( String ) instance.executeStatement( "'true'.booleanFormat()" ), "true" );
		assertEquals( ( String ) instance.executeStatement( "'false'.booleanFormat()" ), "false" );
		assertThrows( BoxRuntimeException.class, () -> instance.executeStatement( "'blah'.booleanFormat()" ) );
	}

	@DisplayName( "It tests the BooleanFormat Member function on a numeric" )
	@Test
	public void testNumericMember() {
		assertEquals( ( String ) instance.executeStatement( "javacast( 'integer', 0 ).booleanFormat()" ), "false" );
		assertEquals( ( String ) instance.executeStatement( "javacast( 'integer', -1 ).booleanFormat()" ), "true" );
		assertEquals( ( String ) instance.executeStatement( "javacast( 'integer', 1 ).booleanFormat()" ), "true" );
	}

}
