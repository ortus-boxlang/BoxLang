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
package TestCases.asm.literal;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
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

public class StructLiteralTest {

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
		instance.useASMBoxPiler();
	}

	@AfterEach
	public void teardownEach() {
		instance.useJavaBoxpiler();
	}

	@DisplayName( "Can declare an empty literal" )
	@Test
	public void testDeclareEmptyStructLiteral() {
		var result = instance.executeStatement(
		    """
		    {};
		        """,
		    context );

		assertThat( result ).isInstanceOf( Struct.class );
		;
	}

	@DisplayName( "Can declare an empty ordered struct literal" )
	@Test
	public void testDeclareEmptyOrderedStructLiteral() {
		var result = instance.executeStatement(
		    """
		    [:];
		        """,
		    context );

		assertThat( result ).isInstanceOf( Struct.class );
		assertThat( ( ( Struct ) result ).getType() ).isInstanceOf( IStruct.TYPES.LINKED.getClass() );
		;
	}

	@DisplayName( "Can declare a struct literal with keys using colons" )
	@Test
	public void testDeclareStructLiteralWithColons() {
		var result = instance.executeStatement(
		    """
		       {
		    	a: "test"
		    };
		           """,
		    context );

		assertThat( result ).isInstanceOf( Struct.class );
		assertThat( ( ( Struct ) result ).containsKey( "a" ) ).isEqualTo( true );
		assertThat( ( ( Struct ) result ).get( "a" ) ).isEqualTo( "test" );
		;
	}

	@DisplayName( "Can declare a struct literal with keys equals" )
	@Test
	public void testDeclareStructLiteralWithEquals() {
		var result = instance.executeStatement(
		    """
		       {
		    	a= "test"
		    };
		           """,
		    context );

		assertThat( result ).isInstanceOf( Struct.class );
		assertThat( ( ( Struct ) result ).containsKey( "a" ) ).isEqualTo( true );
		assertThat( ( ( Struct ) result ).get( "a" ) ).isEqualTo( "test" );

	}

}
