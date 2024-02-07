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

import static com.google.common.truth.Truth.assertThat;

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

public class ToStringTest {

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

	@DisplayName( "It can turn string into string" )
	@Test
	public void testStringIntoString() {
		instance.executeSource(
		    """
		    result = toString( "Hello World" )
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "Hello World" );
	}

	@DisplayName( "It can turn number into string" )
	@Test
	public void testNumberIntoString() {
		instance.executeSource(
		    """
		    result = toString( 42 )
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "42" );
	}

	@DisplayName( "It can turn date" )
	@Test
	public void testDateIntoString() {
		instance.executeSource(
		    """
		    result = toString( now() )
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( String.class );
	}

	@DisplayName( "It can turn array into string" )
	@Test
	public void testArrayIntoString() {
		instance.executeSource(
		    """
		    result = toString( [1,2,3] )
		    """,
		    context );
		assertThat( variables.getAsString( result ).replaceAll( "\\s", "" ) ).isEqualTo( "[1,2,3]" );
	}

	@DisplayName( "It can turn struct into string" )
	@Test
	public void testStructIntoString() {
		instance.executeSource(
		    """
		    result = toString( {'foo':'bar'} )
		    """,
		    context );
		assertThat( variables.getAsString( result ).replaceAll( "\\s", "" ) ).isEqualTo( "{foo:bar}" );
	}

	@DisplayName( "It can turn XML into string" )
	@Test
	public void testXMLIntoString() {
		instance.executeSource(
		    """
		    result = toString( XMLParse( "<foo>bar</foo>" ) )
		    """,
		    context );
		assertThat( variables.getAsString( result ).replaceAll( "\\s", "" ) )
		    .isEqualTo( "<?xmlversion=\"1.0\"encoding=\"UTF-8\"standalone=\"no\"?><foo>bar</foo>" );
	}

	@DisplayName( "It can turn null into string" )
	@Test
	public void testNullIntoString() {
		instance.executeSource(
		    """
		    result = toString( null )
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "" );
	}

	@DisplayName( "It can turn bytes into string" )
	@Test
	public void testBytesIntoString() {
		instance.executeSource(
		    """
		    result = toString( "test".getBytes() )
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "test" );
	}

	@DisplayName( "It can turn bytes into string with encoding" )
	@Test
	public void testBytesIntoStringWithEncoding() {
		instance.executeSource(
		    """
		    result = toString( "test".getBytes(), "UTF-8" )
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "test" );
	}

	@DisplayName( "It can turn anything into string" )
	@Test
	public void testAnythingIntoString() {
		instance.executeSource(
		    """
		    import java.lang.System;
		       result = toString( System )
		       """,
		    context );
		// This assertion is iffy. This is dependant on whatever the object's toString() method returns.
		assertThat( variables.getAsString( result ) ).contains( "System" );
	}

}
