
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

public class FindOneOfTest {

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

	@DisplayName( "tests the BIF findOneOf" )
	@Test
	public void testFindOneOfSet() {
		instance.executeSource(
		    """
		    result = findOneOf("aeiou", "BoxLang is great" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 2 );
		instance.executeSource(
		    """
		    result = findOneOf("aeiou", "BoxLang is great", 2 );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 2 );
	}

	@DisplayName( "It returns zero for substring not found" )
	@Test
	public void testFindSubstringNotFound() {
		instance.executeSource(
		    """
		    result = findOneOf("yz", "BoxLang is great");
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 0 );
	}

	@DisplayName( "Tests the Bif with Negative Start" )
	@Test
	public void testFindOneOfNegativeStart() {
		instance.executeSource(
		    """
		    result = findOneOf("aeiou", "BoxLang is great", -1);
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 2 ); // Negative start position should be treated as 1
	}

	@DisplayName( "It returns for member function call" )
	@Test
	public void testFindMemberFunctionCall() {
		instance.executeSource(
		    """
		    result = "BoxLang is great".findOneOf("aeiou");
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 2 ); // "aeiou" starts at position 4
	}

}
