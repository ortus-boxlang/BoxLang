
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
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

public class StructNewTest {

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

	@DisplayName( "It tests the BIF StructNew" )
	@Test
	public void testBif() {
		instance.executeSource(
		    """
		    result = StructNew();
		    """,
		    context );
		assertEquals( variables.getAsStruct( result ).getType(), IStruct.TYPES.DEFAULT );
		instance.executeSource(
		    """
		    result = StructNew( "ordered" );
		    """,
		    context );
		assertEquals( variables.getAsStruct( result ).getType(), IStruct.TYPES.LINKED );
		instance.executeSource(
		    """
		    result = StructNew( "casesensitive" );
		    """,
		    context );
		assertEquals( variables.getAsStruct( result ).getType(), IStruct.TYPES.CASE_SENSITIVE );
		assertTrue( variables.getAsStruct( result ).isCaseSensitive() );
		instance.executeSource(
		    """
		    result = StructNew( "ordered-casesensitive" );
		    """,
		    context );
		assertEquals( variables.getAsStruct( result ).getType(), IStruct.TYPES.LINKED_CASE_SENSITIVE );
		assertTrue( variables.getAsStruct( result ).isCaseSensitive() );
		instance.executeSource(
		    """
		    result = StructNew( "sorted" );
		    """,
		    context );
		assertEquals( variables.getAsStruct( result ).getType(), IStruct.TYPES.SORTED );
		instance.executeSource(
		    """
		    result = StructNew( "soft" );
		    """,
		    context );
		assertEquals( variables.getAsStruct( result ).getType(), IStruct.TYPES.SOFT );
		assertTrue( variables.getAsStruct( result ).isSoftReferenced() );
		instance.executeSource(
		    """
		    result = StructNew( "weak" );
		    """,
		    context );
		assertEquals( variables.getAsStruct( result ).getType(), IStruct.TYPES.WEAK );

	}

	@DisplayName( "It tests StructNew with sorting indicators" )
	@Test
	public void testSortedStructs() {
		instance.executeSource(
		    """
		       result = StructNew( "sorted", "text", "desc" );
		    result.insert( "bar", "foo" );
		    result.insert( "foo", "bar" );
		       """,
		    context );
		assertEquals( variables.getAsStruct( result ).getType(), IStruct.TYPES.SORTED );
		assertEquals( variables.getAsStruct( result ).keySet().toArray()[ 0 ], Key.of( "foo" ) );
		instance.executeSource(
		    """
		          result = StructNew( "sorted", "text", "asc" );
		       result.insert( "foo", "bar" );
		    result.insert( "bar", "foo" );
		          """,
		    context );
		assertEquals( variables.getAsStruct( result ).getType(), IStruct.TYPES.SORTED );
		assertEquals( variables.getAsStruct( result ).keySet().toArray()[ 0 ], Key.of( "bar" ) );
		instance.executeSource(
		    """
		       result = StructNew( "sorted", "numeric", "asc" );
		    result.insert( 2, "foo" );
		    result.insert( 1, "bar" );
		       """,
		    context );

		assertEquals( variables.getAsStruct( result ).getType(), IStruct.TYPES.SORTED );
		assertEquals( variables.getAsStruct( result ).keySet().toArray()[ 0 ], Key.of( 1 ) );
		instance.executeSource(
		    """
		       result = StructNew( "sorted", "numeric", "desc" );
		    result.insert( 1, "bar" );
		    result.insert( 2, "foo" );
		       """,
		    context );

		assertEquals( variables.getAsStruct( result ).getType(), IStruct.TYPES.SORTED );
		assertEquals( variables.getAsStruct( result ).keySet().toArray()[ 0 ], Key.of( 2 ) );

	}

	@DisplayName( "It tests ordered StructNew with locale sensitivity and no comparator" )
	@Test
	public void testLSSansComparator() {
		instance.executeSource(
		    """
		       result = StructNew( "ordered", "text", "asc", true );
		    result.insert( "foo", "bar" );
		    result.insert( "flea", "flah" );
		       """,
		    context );

		assertEquals( "flea", variables.getAsStruct( result ).getKeys().get( 0 ).getName() );
		assertEquals( "foo", variables.getAsStruct( result ).getKeys().get( 1 ).getName() );

	}

	@DisplayName( "It tests StructNew with comparator callback" )
	@Test
	public void testSortedComparator() {
		instance.executeSource(
		    """
		       result = StructNew( callback=( a, b ) => compare( b, a ) );
		    result.insert( 1, "bar" );
		    result.insert( 2, "foo" );
		       """,
		    context );

		assertEquals( variables.getAsStruct( result ).getType(), IStruct.TYPES.SORTED );
		assertEquals( variables.getAsStruct( result ).keySet().toArray()[ 0 ], Key.of( 2 ) );
	}

	@DisplayName( "case sensitive access" )
	@Test
	public void testCaseSenstiveAccess() {
		variables.put( Key.of( "struct" ), new Struct( Struct.TYPES.CASE_SENSITIVE ) );
		instance.executeSource(
		    """
		    struct[ "foo" ] = "bar";
		    struct[ "flea" ] = "flah";

		    assert struct.size() == 2;
		    assert struct.keyExists( "foo" );
		    assert !struct.keyExists( "fOO" );
		    assert struct.keyExists( "flea" );
		    assert !struct.keyExists( "Fleah" );
		    struct.delete( "FOO" );
		    assert struct.size() == 2;
		    struct.delete( "foo" );
		    assert struct.size() == 1;
		       """,
		    context );
	}

}
