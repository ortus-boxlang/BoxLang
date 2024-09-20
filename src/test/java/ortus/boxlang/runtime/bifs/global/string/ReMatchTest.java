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
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;

public class ReMatchTest {

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
	public void testMatch() {
		instance.executeSource(
		    """
		    result = reMatch("[abc]", "This is a test of the emergency broadcast system");
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Array.class );
		Array arr = variables.getAsArray( result );
		assertThat( arr.size() ).isEqualTo( 6 );
		assertThat( arr.get( 0 ) ).isEqualTo( "a" );
		assertThat( arr.get( 1 ) ).isEqualTo( "c" );
		assertThat( arr.get( 2 ) ).isEqualTo( "b" );
		assertThat( arr.get( 3 ) ).isEqualTo( "a" );
		assertThat( arr.get( 4 ) ).isEqualTo( "c" );
		assertThat( arr.get( 5 ) ).isEqualTo( "a" );

	}

	@Test
	public void testMatchMember() {
		instance.executeSource(
		    """
		    result = "This is a test of the emergency broadcast system".reMatch("[abc]" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Array.class );
		Array arr = variables.getAsArray( result );
		assertThat( arr.size() ).isEqualTo( 6 );
		assertThat( arr.get( 0 ) ).isEqualTo( "a" );
		assertThat( arr.get( 1 ) ).isEqualTo( "c" );
		assertThat( arr.get( 2 ) ).isEqualTo( "b" );
		assertThat( arr.get( 3 ) ).isEqualTo( "a" );
		assertThat( arr.get( 4 ) ).isEqualTo( "c" );
		assertThat( arr.get( 5 ) ).isEqualTo( "a" );

	}

	@Test
	public void testMatchCase() {
		instance.executeSource(
		    """
		    result = "THIS IS A TEST OF THE EMERGENCY broadcast system".reMatch("[abc]" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Array.class );
		Array arr = variables.getAsArray( result );
		assertThat( arr.size() ).isEqualTo( 4 );
		assertThat( arr.get( 0 ) ).isEqualTo( "b" );
		assertThat( arr.get( 1 ) ).isEqualTo( "a" );
		assertThat( arr.get( 2 ) ).isEqualTo( "c" );
		assertThat( arr.get( 3 ) ).isEqualTo( "a" );

	}

	@Test
	public void testPerlStyleCurlyLooseness() {
		// @formatter:off
		instance.executeSource(
		    """
				input   = "String with {{TOKEN}}";
				result = ReMatch( "{{[A-Z]+}}", input );
		    """,
		    context );
			assertThat( variables.get( result ) ).isInstanceOf( Array.class );
			assertThat( variables.get( result ) ).isEqualTo(Array.of( "{{TOKEN}}" ));
		// @formatter:on
	}

}
