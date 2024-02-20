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
package ortus.boxlang.runtime.bifs.global.binary;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class BitMaskSetTest {

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

	@DisplayName( "Bitwise Mask Set operation with positive integers" )
	@Test
	public void testBitwiseMaskSetWithPositiveIntegers() {
		instance.executeSource( "result = bitMaskSet(5, 3, 2, 4);", context );
		assertThat( variables.get( result ) ).isEqualTo( 13 );
	}

	@DisplayName( "Bitwise Mask Set operation with negative integers" )
	@Test
	public void testBitwiseMaskSetWithNegativeIntegers() {
		instance.executeSource( "result = bitMaskSet(-5, -3, 1, 3);", context );
		assertThat( variables.get( result ) ).isEqualTo( -5 );
	}

	@DisplayName( "Bitwise Mask Set operation with zero" )
	@Test
	public void testBitwiseMaskSetWithZero() {
		instance.executeSource( "result = bitMaskSet(0, 10, 2, 4);", context );
		assertThat( variables.get( result ) ).isEqualTo( 40 );
	}

	@DisplayName( "Bitwise Mask Set operation with large integers" )
	@Test
	public void testBitwiseMaskSetWithLargeIntegers() {
		instance.executeSource( "result = bitMaskSet(123456789, 987654321, 5, 10);", context );
		assertThat( variables.get( result ) ).isEqualTo( 123442741 );
	}

	@DisplayName( "Bitwise Mask Set operation with invalid length" )
	@Test
	public void testBitwiseMaskSetWithInvalidLength() {
		assertThrows( BoxRuntimeException.class, () -> instance.executeSource( "result = bitMaskSet(5, 3, 2, 32);", context ) );
	}

	@DisplayName( "Bitwise Mask Set operation with invalid start" )
	@Test
	public void testBitwiseMaskSetWithInvalidStart() {
		assertThrows( BoxRuntimeException.class, () -> instance.executeSource( "result = bitMaskSet(5, 3, 32, 4);", context ) );
	}
}
