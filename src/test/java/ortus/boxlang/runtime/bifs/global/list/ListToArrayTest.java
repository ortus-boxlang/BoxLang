
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

package ortus.boxlang.runtime.bifs.global.list;

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
import ortus.boxlang.runtime.types.Array;

public class ListToArrayTest {

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

	@DisplayName( "It tests the BIF ListToArray with the defaults" )
	@Test
	public void testBif() {
		Array result = ( Array ) instance.executeStatement( "listToArray( 'foo,bar' )" );
		assertEquals( result.size(), 2 );
		assertEquals( result.get( 0 ), "foo" );
	}

	@DisplayName( "It tests the BIF ListToArray with the defaults member" )
	@Test
	public void testBifMember() {
		Array result = ( Array ) instance.executeStatement( "'foo,bar'.listToArray(  )" );
		assertEquals( result.size(), 2 );
		assertEquals( result.get( 0 ), "foo" );
	}

	@DisplayName( "It tests the BIF ListToArray with a custom delimiter" )
	@Test
	public void testDelim() {
		Array result = ( Array ) instance.executeStatement( "listToArray( 'foo|bar', '|' )" );
		assertEquals( result.size(), 2 );
		assertEquals( result.get( 0 ), "foo" );
	}

	@DisplayName( "It tests the BIF ListToArray will remove empty values by default" )
	@Test
	public void testsRemoveEmpty() {
		Array result = ( Array ) instance.executeStatement( "listToArray( 'foo,bar,,' )" );
		assertEquals( result.size(), 2 );
		assertEquals( result.get( 0 ), "foo" );
		assertEquals( result.get( 1 ), "bar" );
	}

	@DisplayName( "It tests the BIF ListToArray will keep empty values when specified" )
	@Test
	public void testsKeepEmpty() {
		Array result = ( Array ) instance.executeStatement( "listToArray( list='foo,bar,,', includeEmptyFields=true )" );
		assertEquals( result.size(), 4 );
		assertEquals( result.get( 0 ), "foo" );
		assertEquals( result.get( 1 ), "bar" );
		assertEquals( result.get( 3 ), "" );
	}

	@DisplayName( "It tests the BIF ListToArray will split on multiple delimiters" )
	@Test
	public void testsMultiDelim() {
		Array result = ( Array ) instance.executeStatement( "listToArray( 'foo,bar|baz', ',|' )" );
		assertEquals( result.size(), 3 );
		assertEquals( result.get( 0 ), "foo" );
		assertEquals( result.get( 1 ), "bar" );
		assertEquals( result.get( 2 ), "baz" );
	}

	@DisplayName( "It tests the BIF ListToArray will split on multi-character delimiters" )
	@Test
	public void testsMultiCharDelim() {
		Array result = ( Array ) instance.executeStatement( "listToArray( list='foo,|bar,|baz', delimiter=',|', multiCharacterDelimiter=true )" );
		assertEquals( result.size(), 3 );
		assertEquals( result.get( 0 ), "foo" );
		assertEquals( result.get( 1 ), "bar" );
		assertEquals( result.get( 2 ), "baz" );
	}

}
