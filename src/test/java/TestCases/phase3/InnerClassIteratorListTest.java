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
package TestCases.phase3;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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

/**
 * Tests that a BoxLang class can implement java.lang.Iterable and use an inner class
 * implementing java.util.Iterator to provide custom iteration behavior.
 */
public class InnerClassIteratorListTest {

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

	@DisplayName( "Can instantiate BoxLang class implementing Iterable" )
	@Test
	public void testInstantiateIterable() {
		instance.executeSource(
		    """
		    result = new src.test.java.TestCases.phase3.InnerClassIteratorList( [ "a", "b", "c" ] );
		    """,
		    context );
		Object res = variables.get( result );
		assertThat( res ).isInstanceOf( Iterable.class );
	}

	@DisplayName( "size() returns correct count" )
	@Test
	public void testSize() {
		instance.executeSource(
		    """
		    myList = new src.test.java.TestCases.phase3.InnerClassIteratorList( [ "x", "y", "z" ] );
		    result = myList.size();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 3 );
	}

	@DisplayName( "get() returns correct element by index" )
	@Test
	public void testGet() {
		instance.executeSource(
		    """
		    myList = new src.test.java.TestCases.phase3.InnerClassIteratorList( [ "alpha", "beta", "gamma" ] );
		    result = myList.get( 1 );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "beta" );
	}

	@DisplayName( "iterator() returns working Iterator from inner class" )
	@Test
	public void testIterator() {
		instance.executeSource(
		    """
		    myList = new src.test.java.TestCases.phase3.InnerClassIteratorList( [ "one", "two", "three" ] );
		    iter = myList.iterator();
		    collected = [];
		    while ( iter.hasNext() ) {
		        collected.append( iter.next() );
		    }
		    result = collected;
		    """,
		    context );
		Object res = variables.get( result );
		assertThat( res ).isInstanceOf( ortus.boxlang.runtime.types.Array.class );
		ortus.boxlang.runtime.types.Array arr = ( ortus.boxlang.runtime.types.Array ) res;
		assertThat( arr.size() ).isEqualTo( 3 );
		assertThat( arr.get( 0 ) ).isEqualTo( "one" );
		assertThat( arr.get( 1 ) ).isEqualTo( "two" );
		assertThat( arr.get( 2 ) ).isEqualTo( "three" );
	}

	@DisplayName( "Can use BoxLang Iterable in Java for-each loop via iterator" )
	@Test
	public void testForEachInterop() {
		instance.executeSource(
		    """
		    myList = new src.test.java.TestCases.phase3.InnerClassIteratorList( [ 10, 20, 30 ] );
		    result = myList;
		    """,
		    context );
		Object res = variables.get( result );
		assertThat( res ).isInstanceOf( Iterable.class );
		Iterable<?> iterable = ( Iterable<?> ) res;
		// Use Java's iterator directly from the Iterable
		Iterator<?> it = iterable.iterator();
		List<Object> collected = new ArrayList<>();
		while ( it.hasNext() ) {
			collected.add( it.next() );
		}
		assertThat( collected ).containsExactly( 10, 20, 30 ).inOrder();
	}

}
