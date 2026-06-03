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
package ortus.boxlang.runtime.operators;

import static com.google.common.truth.Truth.assertThat;

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
import ortus.boxlang.runtime.types.BoxSet;

public class SetOperatorsTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "+ operator computes union of two Sets" )
	@Test
	public void testPlusUnion() {
		instance.executeSource(
		    """
		    a = [1, 2, 3].toSet();
		    b = [3, 4, 5].toSet();
		    result = a + b;
		    """,
		    context );
		BoxSet u = ( BoxSet ) variables.get( result );
		assertThat( u.size() ).isEqualTo( 5 );
		assertThat( u.contains( 1 ) ).isTrue();
		assertThat( u.contains( 5 ) ).isTrue();
	}

	@DisplayName( "- operator computes difference of two Sets" )
	@Test
	public void testMinusDifference() {
		instance.executeSource(
		    """
		    a = [1, 2, 3, 4].toSet();
		    b = [3, 4].toSet();
		    result = a - b;
		    """,
		    context );
		BoxSet d = ( BoxSet ) variables.get( result );
		assertThat( d.size() ).isEqualTo( 2 );
		assertThat( d.contains( 1 ) ).isTrue();
		assertThat( d.contains( 2 ) ).isTrue();
		assertThat( d.contains( 3 ) ).isFalse();
	}

	@DisplayName( "Numeric + still works after operator widening" )
	@Test
	public void testNumericPlusUnchanged() {
		instance.executeSource( "result = 2 + 3;", context );
		assertThat( ( ( Number ) variables.get( result ) ).intValue() ).isEqualTo( 5 );
	}

	@DisplayName( "Numeric - still works after operator widening" )
	@Test
	public void testNumericMinusUnchanged() {
		instance.executeSource( "result = 10 - 4;", context );
		assertThat( ( ( Number ) variables.get( result ) ).intValue() ).isEqualTo( 6 );
	}

	@DisplayName( "Set + Array (cast) computes union" )
	@Test
	public void testSetPlusArray() {
		instance.executeSource(
		    """
		    a = [1, 2].toSet();
		    result = a + [2, 3, 4].toSet();
		    """,
		    context );
		assertThat( ( ( BoxSet ) variables.get( result ) ).size() ).isEqualTo( 4 );
	}

}
