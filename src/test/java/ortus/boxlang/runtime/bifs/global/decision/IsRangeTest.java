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
package ortus.boxlang.runtime.bifs.global.decision;

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
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Range;
import ortus.boxlang.runtime.types.Struct;

public class IsRangeTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "isRange returns true for Range instances" )
	@Test
	public void testIsRangeWithRange() {
		Range<?> range = Range.of( 1, 5 );

		assertThat( IsRange.isRange( range ) ).isTrue();
	}

	@DisplayName( "isRange returns false for null" )
	@Test
	public void testIsRangeWithNull() {
		assertThat( IsRange.isRange( null ) ).isFalse();
	}

	@DisplayName( "isRange returns false for string" )
	@Test
	public void testIsRangeWithString() {
		assertThat( IsRange.isRange( "not a range" ) ).isFalse();
	}

	@DisplayName( "isRange returns false for array" )
	@Test
	public void testIsRangeWithArray() {
		Array array = new Array();

		assertThat( IsRange.isRange( array ) ).isFalse();
	}

	@DisplayName( "isRange returns false for struct" )
	@Test
	public void testIsRangeWithStruct() {
		IStruct struct = new Struct();

		assertThat( IsRange.isRange( struct ) ).isFalse();
	}

	@DisplayName( "isRange returns false for number" )
	@Test
	public void testIsRangeWithNumber() {
		assertThat( IsRange.isRange( 42 ) ).isFalse();
	}

	@DisplayName( "isRange returns false for boolean" )
	@Test
	public void testIsRangeWithBoolean() {
		assertThat( IsRange.isRange( true ) ).isFalse();
	}

	@DisplayName( "isRange BIF evaluates true for range literals" )
	@Test
	public void testIsRangeBIFWithRangeLiteral() {
		instance.executeSource(
		    """
		    result = isRange( 1..5 );
		    """,
		    context );

		assertThat( variables.getAsBoolean( Key.of( "result" ) ) ).isTrue();
	}

	@DisplayName( "isRange BIF evaluates false for non-range literals" )
	@Test
	public void testIsRangeBIFWithNonRangeLiteral() {
		instance.executeSource(
		    """
		    result = isRange( [ 1, 2, 3 ] );
		    """,
		    context );

		assertThat( variables.getAsBoolean( Key.of( "result" ) ) ).isFalse();
	}

}