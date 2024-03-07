
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

import org.junit.jupiter.api.*;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BitwiseOperatorsTest {

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

	@DisplayName( "It can BitwiseComplement" )
	@Test
	public void testBitwiseComplement() {
		Number result = ( Number ) instance.executeStatement( "b~ 6" );
		assertEquals( result, -7 );
	}

	@DisplayName( "It can BitwiseAnd" )
	@Test
	public void testBitwiseAnd() {
		Number result = ( Number ) instance.executeStatement( "6 b& 5" );
		assertEquals( result, 4 );
	}

	@DisplayName( "It can BitwiseOr" )
	@Test
	public void testBitwiseOr() {
		Number result = ( Number ) instance.executeStatement( "6 b| 5" );
		assertEquals( result, 7 );
	}

	@DisplayName( "It can BitwiseXor" )
	@Test
	public void testBitwiseXor() {
		Number result = ( Number ) instance.executeStatement( "6 b^ 5" );
		assertEquals( result, 3 );
	}

	@DisplayName( "It can BitwiseSignedLeftShift" )
	@Test
	public void testBitwiseSignedLeftShift() {
		Number result = ( Number ) instance.executeStatement( "-12 b<< 2" );
		assertEquals( result, -48 );
	}

	@DisplayName( "It can BitwiseSignedRightShift" )
	@Test
	public void testBitwiseSignedRightShift() {
		Number result = ( Number ) instance.executeStatement( "-12 b>> 2" );
		assertEquals( result, -3 );
	}

	@DisplayName( "It can BitwiseUnsignedRightShift" )
	@Test
	public void testBitwiseUnsignedRightShift() {
		Number result = ( Number ) instance.executeStatement( "-12 b>>> 2" );
		assertEquals( result, 1073741821 );
	}

}
