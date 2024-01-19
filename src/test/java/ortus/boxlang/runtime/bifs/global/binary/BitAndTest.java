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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class BitAndTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingBoxContext( instance.getRuntimeContext() );
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

	@DisplayName( "Bitwise AND operation with positive integers" )
	@Test
	public void testBitwiseAndWithPositiveIntegers() {
		instance.executeSource( "result = bitAnd(5, 3);", context );
		assertThat( variables.get( result ) ).isEqualTo( 1 );
	}

	@DisplayName( "Bitwise AND operation with negative integers" )
	@Test
	public void testBitwiseAndWithNegativeIntegers() {
		instance.executeSource( "result = bitAnd(-5, -3);", context );
		assertThat( variables.get( result ) ).isEqualTo( -7 );
	}

	@DisplayName( "Bitwise AND operation with zero" )
	@Test
	public void testBitwiseAndWithZero() {
		instance.executeSource( "result = bitAnd(0, 10);", context );
		assertThat( variables.get( result ) ).isEqualTo( 0 );
	}

	@DisplayName( "Bitwise AND operation with large integers" )
	@Test
	public void testBitwiseAndWithLargeIntegers() {
		instance.executeSource( "result = bitAnd(123456789, 987654321);", context );
		assertThat( variables.get( result ) ).isEqualTo( 39471121 );
	}
}
