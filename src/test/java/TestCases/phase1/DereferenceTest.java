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
package TestCases.phase1;

import static com.google.common.truth.Truth.assertThat;

import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

public class DereferenceTest {

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

	@DisplayName( "Single identifier dot access" )
	@Test
	public void testSingleIdentifierReference() {
		variables.assign( context, new Key( "foo" ), "test" );
		instance.executeSource(
		    """
		    foo;
		    """,
		    context );
	}

	@DisplayName( "Multi identifier dot access" )
	@Test
	public void testmultiIdentifierReference() {
		IStruct s = new Struct();
		s.assign( context, new Key( "bar" ), "test" );
		variables.assign( context, new Key( "foo" ), s );
		instance.executeSource(
		    """
		    foo.bar;
		    """,
		    context );
	}

	@DisplayName( "Multi multi identifier dot access" )
	@Test
	public void testmultimultiIdentifierReference() {
		IStruct x = new Struct();
		x.assign( context, new Key( "baz" ), "test" );
		IStruct s = new Struct();
		s.assign( context, new Key( "bar" ), x );
		variables.assign( context, new Key( "foo" ), s );
		instance.executeSource(
		    """
		    foo.bar.baz;
		    """,
		    context );
	}

	@DisplayName( "Bracket string access" )
	@Test
	public void testBracketStringAccess() {
		IStruct s = new Struct();
		s.assign( context, new Key( "bar" ), "test" );
		variables.assign( context, new Key( "foo" ), s );
		instance.executeSource(
		    """
		    foo["bar"];
		    """,
		    context );
	}

	@DisplayName( "Bracket string concat access" )
	@Test
	public void testBracketStringConcatAccess() {
		IStruct s = new Struct();
		s.assign( context, new Key( "bar" ), "test" );
		variables.assign( context, new Key( "foo" ), s );
		instance.executeSource(
		    """
		    foo["b" & "ar"]
		    """,
		    context );
	}

	@DisplayName( "Bracket number access" )
	@Test
	public void testBracketNumberAccess() {
		IStruct s = new Struct();
		s.assign( context, new Key( "7" ), "test" );
		variables.assign( context, new Key( "foo" ), s );
		instance.executeSource(
		    """
		    foo[ 7 ]
		    """,
		    context );
	}

	@DisplayName( "Bracket number expression access" )
	@Test
	public void testBracketNumberExpressionAccess() {
		IStruct s = new Struct();
		s.assign( context, new Key( "12" ), "test" );
		variables.assign( context, new Key( "foo" ), s );
		instance.executeSource(
		    """
		    foo[ 7 + 5 ]
		    """,
		    context );
	}

	@DisplayName( "Bracket object access" )
	@Test
	public void testBracketObjectExpressionAccess() {
		IStruct x = new Struct();
		x.assign( context, new Key( "bar" ), "baz" );
		IStruct s = new Struct();
		s.assign( context, new Key( "12" ), "test" );
		s.assign( context, Key.of( x ), "test" );
		variables.assign( context, new Key( "foo" ), s );
		instance.executeSource(
		    """
		    foo[ { bar : "baz" } ];
		    """,
		    context );
	}

	@DisplayName( "Mixed access" )
	@Test
	public void testBracketMixedAccess() {
		IStruct	aaa		= new Struct();
		IStruct	twelve	= new Struct();
		IStruct	other	= new Struct();
		IStruct	foo		= new Struct();

		foo.assign( context, new Key( "aaa" ), aaa );
		aaa.assign( context, Key.of( 12 ), twelve );
		twelve.assign( context, Key.of( "other" ), other );
		other.assign( context, Key.of( 7 ), "test" );

		variables.assign( context, new Key( "foo" ), foo );
		instance.executeSource(
		    """
		    foo[ "a" & "aa" ][ 12 ].other[ 2 + 5 ];
		    """,
		    context );
	}

	@DisplayName( "dereference enum key" )
	@Test
	public void testDereferenceEnumKey() {
		instance.executeSource(
		    """
		    import ortus.boxlang.runtime.types.BoxLangType;
		    result = BoxLangType.LIST.getKey().getName();
		         """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( "list" );
	}

	@DisplayName( "dereference enum as nested class" )
	@Test
	public void testDereferenceEnumAsNestedClass() {
		instance.executeSource(
		    """
		        import ortus.boxlang.runtime.types.Struct as jStruct;
		    import ortus.boxlang.runtime.types.IStruct;
		          struct = new jStruct( IStruct.TYPES.CASE_SENSITIVE );
		             """,
		    context );
	}

	@DisplayName( "dereference nested class" )
	@Test
	public void testDereferenceNestedClass() {
		instance.executeSource(
		    """
		    import java.util.Map
		      result = Map.Entry;
		         """,
		    context );

		assertThat( variables.get( result ) ).isEqualTo( Map.Entry.class );

		instance.executeSource(
		    """
		    import java.util.Map$Entry;
		      result = Map$Entry;
		         """,
		    context );

		assertThat( DynamicObject.unWrap( variables.get( result ) ) ).isEqualTo( Map.Entry.class );
	}

}
