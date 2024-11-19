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

package ortus.boxlang.runtime.bifs.global.string;

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
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Struct;

public class ReFindTest {

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
	public void testPOSIX() {
		instance.executeSource(
		    """
		    result = reFind("[:lower:]", "test");
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Integer.class );
		assertThat( variables.get( result ) ).isEqualTo( 1 );

		instance.executeSource(
		    """
		    result = reFind("[[:lower:]]", "Btest");
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Integer.class );
		assertThat( variables.get( result ) ).isEqualTo( 2 );

		instance.executeSource(
		    """
		    result = reFind("foo[:lower:]bar[:upper:]baz", "fooabarBbaz");
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Integer.class );
		assertThat( variables.get( result ) ).isEqualTo( 1 );

		instance.executeSource(
		    """
		    result = reFind("[1[:lower:]2[:upper:]3]", "412A32b");
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Integer.class );
		assertThat( variables.get( result ) ).isEqualTo( 2 );
	}

	@Test
	public void testFindOneNoSub() {
		instance.executeSource(
		    """
		    result = reFind("(1)[2-3]", "test 123 test 123!",10,false,"one");
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Integer.class );
		assertThat( variables.get( result ) ).isEqualTo( 15 );
	}

	@Test
	public void testFindOneNoSubMember() {
		instance.executeSource(
		    """
		    result = "test 123 test 123!".reFind("(1)[2-3]",10,false,"one");
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Integer.class );
		assertThat( variables.get( result ) ).isEqualTo( 15 );
	}

	@Test
	public void testFindOneNoSubStart() {
		instance.executeSource(
		    """
		    result = reFind("(1)[2-3]", "test 123 test 123!",1,false,"one");
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Integer.class );
		assertThat( variables.get( result ) ).isEqualTo( 6 );
	}

	@Test
	public void testFindAllNoSub() {
		instance.executeSource(
		    """
		    result = reFind("(1)[2-3]", "test 123 test 123!",1,false,"all");
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Array.class );
		assertThat( variables.getAsArray( result ).size() ).isEqualTo( 2 );
		assertThat( variables.getAsArray( result ).get( 0 ) ).isEqualTo( 6 );
		assertThat( variables.getAsArray( result ).get( 1 ) ).isEqualTo( 15 );
	}

	@Test
	public void testFindOneNoSubNoMatch() {
		instance.executeSource(
		    """
		    result = reFind("1[2-3]", "sdf",1,false,"one");
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Integer.class );
		assertThat( variables.get( result ) ).isEqualTo( 0 );
	}

	@Test
	public void testFindAllNoSubNoMatch() {
		instance.executeSource(
		    """
		    result = reFind("1[2-3]", "sdf",1,false,"all");
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Integer.class );
		assertThat( variables.get( result ) ).isEqualTo( 0 );
	}

	@Test
	public void testFindAllWithSubNoMatch() {
		instance.executeSource(
		    """
		    result = reFind("1[2-3]", "sdf",1,true,"all");
		    """,
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( Array.class );
		assertThat( variables.getAsArray( result ).size() ).isEqualTo( 1 );

		assertThat( variables.getAsArray( result ).get( 0 ) ).isInstanceOf( Struct.class );
		Struct firstResult = ( Struct ) variables.getAsArray( result ).get( 0 );
		assertThat( firstResult ).containsKey( Key.of( "len" ) );
		assertThat( firstResult ).containsKey( Key.of( "match" ) );
		assertThat( firstResult ).containsKey( Key.of( "pos" ) );

		assertThat( firstResult.get( "len" ) ).isInstanceOf( Array.class );
		assertThat( firstResult.get( "match" ) ).isInstanceOf( Array.class );
		assertThat( firstResult.get( "pos" ) ).isInstanceOf( Array.class );

		assertThat( firstResult.getAsArray( Key.of( "len" ) ) ).hasSize( 1 );
		assertThat( firstResult.getAsArray( Key.of( "match" ) ) ).hasSize( 1 );
		assertThat( firstResult.getAsArray( Key.of( "pos" ) ) ).hasSize( 1 );

		assertThat( firstResult.getAsArray( Key.of( "len" ) ).get( 0 ) ).isEqualTo( 0 );
		assertThat( firstResult.getAsArray( Key.of( "match" ) ).get( 0 ) ).isEqualTo( "" );
		assertThat( firstResult.getAsArray( Key.of( "pos" ) ).get( 0 ) ).isEqualTo( 0 );

	}

	@Test
	public void testFindOneWithSubNoMatch() {
		instance.executeSource(
		    """
		    result = reFind("1[2-3]", "sdf",1,true,"one");
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Struct.class );
		assertThat( variables.getAsStruct( result ) ).containsKey( Key.of( "len" ) );
		assertThat( variables.getAsStruct( result ) ).containsKey( Key.of( "match" ) );
		assertThat( variables.getAsStruct( result ) ).containsKey( Key.of( "pos" ) );

		assertThat( variables.getAsStruct( result ).get( "len" ) ).isInstanceOf( Array.class );
		assertThat( variables.getAsStruct( result ).get( "match" ) ).isInstanceOf( Array.class );
		assertThat( variables.getAsStruct( result ).get( "pos" ) ).isInstanceOf( Array.class );

		assertThat( variables.getAsStruct( result ).getAsArray( Key.of( "len" ) ) ).hasSize( 1 );
		assertThat( variables.getAsStruct( result ).getAsArray( Key.of( "match" ) ) ).hasSize( 1 );
		assertThat( variables.getAsStruct( result ).getAsArray( Key.of( "pos" ) ) ).hasSize( 1 );

		assertThat( variables.getAsStruct( result ).getAsArray( Key.of( "len" ) ).get( 0 ) ).isEqualTo( 0 );

		assertThat( variables.getAsStruct( result ).getAsArray( Key.of( "match" ) ).get( 0 ) ).isEqualTo( "" );

		assertThat( variables.getAsStruct( result ).getAsArray( Key.of( "pos" ) ).get( 0 ) ).isEqualTo( 0 );
	}

	@Test
	public void testFindOneNoGroupWithSub() {
		instance.executeSource(
		    """
		    result = reFind("1[2-3]", "test 123 test 123!",1,true,"one");
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Struct.class );
		assertThat( variables.getAsStruct( result ) ).containsKey( Key.of( "len" ) );
		assertThat( variables.getAsStruct( result ) ).containsKey( Key.of( "match" ) );
		assertThat( variables.getAsStruct( result ) ).containsKey( Key.of( "pos" ) );

		assertThat( variables.getAsStruct( result ).get( "len" ) ).isInstanceOf( Array.class );
		assertThat( variables.getAsStruct( result ).get( "match" ) ).isInstanceOf( Array.class );
		assertThat( variables.getAsStruct( result ).get( "pos" ) ).isInstanceOf( Array.class );

		assertThat( variables.getAsStruct( result ).getAsArray( Key.of( "len" ) ) ).hasSize( 1 );
		assertThat( variables.getAsStruct( result ).getAsArray( Key.of( "match" ) ) ).hasSize( 1 );
		assertThat( variables.getAsStruct( result ).getAsArray( Key.of( "pos" ) ) ).hasSize( 1 );

		assertThat( variables.getAsStruct( result ).getAsArray( Key.of( "len" ) ).get( 0 ) ).isEqualTo( 2 );

		assertThat( variables.getAsStruct( result ).getAsArray( Key.of( "match" ) ).get( 0 ) ).isEqualTo( "12" );

		assertThat( variables.getAsStruct( result ).getAsArray( Key.of( "pos" ) ).get( 0 ) ).isEqualTo( 6 );
	}

	@Test
	public void testFindOneWithGroupWithSub() {
		instance.executeSource(
		    """
		    result = reFind("(1)[2-3]", "test 123 test 123!",1,true,"one");
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Struct.class );
		assertThat( variables.getAsStruct( result ) ).containsKey( Key.of( "len" ) );
		assertThat( variables.getAsStruct( result ) ).containsKey( Key.of( "match" ) );
		assertThat( variables.getAsStruct( result ) ).containsKey( Key.of( "pos" ) );

		assertThat( variables.getAsStruct( result ).get( "len" ) ).isInstanceOf( Array.class );
		assertThat( variables.getAsStruct( result ).get( "match" ) ).isInstanceOf( Array.class );
		assertThat( variables.getAsStruct( result ).get( "pos" ) ).isInstanceOf( Array.class );

		assertThat( variables.getAsStruct( result ).getAsArray( Key.of( "len" ) ) ).hasSize( 2 );
		assertThat( variables.getAsStruct( result ).getAsArray( Key.of( "match" ) ) ).hasSize( 2 );
		assertThat( variables.getAsStruct( result ).getAsArray( Key.of( "pos" ) ) ).hasSize( 2 );

		assertThat( variables.getAsStruct( result ).getAsArray( Key.of( "len" ) ).get( 0 ) ).isEqualTo( 2 );
		assertThat( variables.getAsStruct( result ).getAsArray( Key.of( "len" ) ).get( 1 ) ).isEqualTo( 1 );

		assertThat( variables.getAsStruct( result ).getAsArray( Key.of( "match" ) ).get( 0 ) ).isEqualTo( "12" );
		assertThat( variables.getAsStruct( result ).getAsArray( Key.of( "match" ) ).get( 1 ) ).isEqualTo( "1" );

		assertThat( variables.getAsStruct( result ).getAsArray( Key.of( "pos" ) ).get( 0 ) ).isEqualTo( 6 );
		assertThat( variables.getAsStruct( result ).getAsArray( Key.of( "pos" ) ).get( 1 ) ).isEqualTo( 6 );
	}

	@Test
	public void testFindAllNoGroupWithSub() {
		instance.executeSource(
		    """
		    result = reFind("1[2-3]", "test 123 test 123!",1,true,"all");
		    """,
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( Array.class );
		assertThat( variables.getAsArray( result ).size() ).isEqualTo( 2 );

		assertThat( variables.getAsArray( result ).get( 0 ) ).isInstanceOf( Struct.class );
		Struct firstResult = ( Struct ) variables.getAsArray( result ).get( 0 );
		assertThat( firstResult ).containsKey( Key.of( "len" ) );
		assertThat( firstResult ).containsKey( Key.of( "match" ) );
		assertThat( firstResult ).containsKey( Key.of( "pos" ) );

		assertThat( firstResult.get( "len" ) ).isInstanceOf( Array.class );
		assertThat( firstResult.get( "match" ) ).isInstanceOf( Array.class );
		assertThat( firstResult.get( "pos" ) ).isInstanceOf( Array.class );

		assertThat( firstResult.getAsArray( Key.of( "len" ) ) ).hasSize( 1 );
		assertThat( firstResult.getAsArray( Key.of( "match" ) ) ).hasSize( 1 );
		assertThat( firstResult.getAsArray( Key.of( "pos" ) ) ).hasSize( 1 );

		assertThat( firstResult.getAsArray( Key.of( "len" ) ).get( 0 ) ).isEqualTo( 2 );
		assertThat( firstResult.getAsArray( Key.of( "match" ) ).get( 0 ) ).isEqualTo( "12" );
		assertThat( firstResult.getAsArray( Key.of( "pos" ) ).get( 0 ) ).isEqualTo( 6 );

		assertThat( variables.getAsArray( result ).get( 1 ) ).isInstanceOf( Struct.class );
		Struct secondResult = ( Struct ) variables.getAsArray( result ).get( 1 );
		assertThat( secondResult ).containsKey( Key.of( "len" ) );
		assertThat( secondResult ).containsKey( Key.of( "match" ) );
		assertThat( secondResult ).containsKey( Key.of( "pos" ) );

		assertThat( secondResult.get( "len" ) ).isInstanceOf( Array.class );
		assertThat( secondResult.get( "match" ) ).isInstanceOf( Array.class );
		assertThat( secondResult.get( "pos" ) ).isInstanceOf( Array.class );

		assertThat( secondResult.getAsArray( Key.of( "len" ) ) ).hasSize( 1 );
		assertThat( secondResult.getAsArray( Key.of( "match" ) ) ).hasSize( 1 );
		assertThat( secondResult.getAsArray( Key.of( "pos" ) ) ).hasSize( 1 );

		assertThat( secondResult.getAsArray( Key.of( "len" ) ).get( 0 ) ).isEqualTo( 2 );
		assertThat( secondResult.getAsArray( Key.of( "match" ) ).get( 0 ) ).isEqualTo( "12" );
		assertThat( secondResult.getAsArray( Key.of( "pos" ) ).get( 0 ) ).isEqualTo( 15 );
	}

	@Test
	public void testFindAllWithGroupWithSub() {
		instance.executeSource(
		    """
		    result = reFind("(1)[2-3]", "test 123 test 123!",1,true,"all");
		    """,
		    context );

		assertThat( variables.get( result ) ).isInstanceOf( Array.class );
		assertThat( variables.getAsArray( result ).size() ).isEqualTo( 2 );

		assertThat( variables.getAsArray( result ).get( 0 ) ).isInstanceOf( Struct.class );
		Struct firstResult = ( Struct ) variables.getAsArray( result ).get( 0 );
		assertThat( firstResult ).containsKey( Key.of( "len" ) );
		assertThat( firstResult ).containsKey( Key.of( "match" ) );
		assertThat( firstResult ).containsKey( Key.of( "pos" ) );

		assertThat( firstResult.get( "len" ) ).isInstanceOf( Array.class );
		assertThat( firstResult.get( "match" ) ).isInstanceOf( Array.class );
		assertThat( firstResult.get( "pos" ) ).isInstanceOf( Array.class );

		assertThat( firstResult.getAsArray( Key.of( "len" ) ) ).hasSize( 2 );
		assertThat( firstResult.getAsArray( Key.of( "match" ) ) ).hasSize( 2 );
		assertThat( firstResult.getAsArray( Key.of( "pos" ) ) ).hasSize( 2 );

		assertThat( firstResult.getAsArray( Key.of( "len" ) ).get( 0 ) ).isEqualTo( 2 );
		assertThat( firstResult.getAsArray( Key.of( "len" ) ).get( 1 ) ).isEqualTo( 1 );
		assertThat( firstResult.getAsArray( Key.of( "match" ) ).get( 0 ) ).isEqualTo( "12" );
		assertThat( firstResult.getAsArray( Key.of( "match" ) ).get( 1 ) ).isEqualTo( "1" );
		assertThat( firstResult.getAsArray( Key.of( "pos" ) ).get( 0 ) ).isEqualTo( 6 );
		assertThat( firstResult.getAsArray( Key.of( "pos" ) ).get( 1 ) ).isEqualTo( 6 );

		assertThat( variables.getAsArray( result ).get( 1 ) ).isInstanceOf( Struct.class );
		Struct secondResult = ( Struct ) variables.getAsArray( result ).get( 1 );
		assertThat( secondResult ).containsKey( Key.of( "len" ) );
		assertThat( secondResult ).containsKey( Key.of( "match" ) );
		assertThat( secondResult ).containsKey( Key.of( "pos" ) );

		assertThat( secondResult.get( "len" ) ).isInstanceOf( Array.class );
		assertThat( secondResult.get( "match" ) ).isInstanceOf( Array.class );
		assertThat( secondResult.get( "pos" ) ).isInstanceOf( Array.class );

		assertThat( secondResult.getAsArray( Key.of( "len" ) ) ).hasSize( 2 );
		assertThat( secondResult.getAsArray( Key.of( "match" ) ) ).hasSize( 2 );
		assertThat( secondResult.getAsArray( Key.of( "pos" ) ) ).hasSize( 2 );

		assertThat( secondResult.getAsArray( Key.of( "len" ) ).get( 0 ) ).isEqualTo( 2 );
		assertThat( secondResult.getAsArray( Key.of( "len" ) ).get( 1 ) ).isEqualTo( 1 );
		assertThat( secondResult.getAsArray( Key.of( "match" ) ).get( 0 ) ).isEqualTo( "12" );
		assertThat( secondResult.getAsArray( Key.of( "match" ) ).get( 1 ) ).isEqualTo( "1" );
		assertThat( secondResult.getAsArray( Key.of( "pos" ) ).get( 0 ) ).isEqualTo( 15 );
		assertThat( secondResult.getAsArray( Key.of( "pos" ) ).get( 1 ) ).isEqualTo( 15 );
	}

	@DisplayName( "Test with TestBox failing case" )
	@Test
	public void testTestBoxFailingCase() {
		// @formatter:off
		instance.executeSource(
		    """
				methodName = "test"
				result = reFindNoCase( "^(f|x)?test$", methodName )
				println( result )
		    """,
		    context );
		// @formatter:on
	}

	@Test
	public void testPerlStyleCurlyLooseness() {
		// @formatter:off
		instance.executeSource(
		    """
				input   = "String with {{TOKEN}}";
				result = ReFind( "{{[A-Z]+}}", input );
		    """,
		    context );
			assertThat( variables.get( result ) ).isEqualTo(13);
		// @formatter:on
		// @formatter:off
		instance.executeSource(
		    """
				input   = "String with {{TOKEN}}";
				result = ReFind( "\\{\\{[A-Z]+\\}\\}", input );
		    """,
		    context );
			assertThat( variables.get( result ) ).isEqualTo(13);
		// @formatter:on
	}

	@Test
	public void testIgnoreRandomCurlies() {
		// @formatter:off
		instance.executeSource(
		    """
				input   = "{String} wi{th random} cu{{rlies}} {1,2,3,4,5}";
				result = ReFind( input, "test " & input );
		    """,
		    context );
			assertThat( variables.get( result ) ).isEqualTo( 6 );
		// @formatter:on
	}

	@DisplayName( "qb parse table name" )
	@Test
	public void testParseQBTableName() {
		// @formatter:off
		instance.executeSource(
			"""
			result = reFindNoCase(
                "(.*?)(?:\\s(?:AS\\s){0,1})([^\\)]+)$",
                "users people",
                1,
                true
            );
			""",
		context );
		// @formatter:on
		assertThat( variables.get( result ) ).isEqualTo( Struct.of(
		    "LEN", Array.of( 12, 5, 6 ),
		    "MATCH", Array.of( "users people", "users", "people" ),
		    "POS", Array.of( 1, 1, 7 )
		) );
	}

}
