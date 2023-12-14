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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class CastAsTest {

	@DisplayName( "It can cast to null" )
	@Test
	void testItCanCastToNull() {
		assertThat( CastAs.invoke( "", "null" ) ).isNull();
		assertThat( CastAs.invoke( "sfsfdsdf", "null" ) ).isNull();
		assertThat( CastAs.invoke( 123, "null" ) ).isNull();
	}

	@DisplayName( "It can cast to bigdecimal" )
	@Test
	void testItCanCastToBigDecimal() {
		assertThat( CastAs.invoke( 5, "bigdecimal" ).getClass().getName() ).isEqualTo( "java.math.BigDecimal" );
		assertThat( CastAs.invoke( "5", "bigdecimal" ).getClass().getName() ).isEqualTo( "java.math.BigDecimal" );
		assertThat(
		    EqualsEquals.invoke(
		        CastAs.invoke( 5, "bigdecimal" ),
		        5
		    )
		).isTrue();
	}

	@DisplayName( "It can cast to boolean" )
	@Test
	void testItCanCastToBoolean() {
		assertThat( CastAs.invoke( true, "boolean" ).getClass().getName() ).isEqualTo( "java.lang.Boolean" );
		assertThat( CastAs.invoke( "true", "boolean" ).getClass().getName() ).isEqualTo( "java.lang.Boolean" );
		assertThat( CastAs.invoke( 1, "boolean" ).getClass().getName() ).isEqualTo( "java.lang.Boolean" );
		assertThat(
		    EqualsEquals.invoke(
		        CastAs.invoke( true, "boolean" ),
		        true
		    )
		).isTrue();
	}

	@DisplayName( "It can cast to byte" )
	@Test
	void testItCanCastToByte() {
		assertThat( CastAs.invoke( 1, "byte" ).getClass().getName() ).isEqualTo( "java.lang.Byte" );
		assertThat( CastAs.invoke( "0", "byte" ).getClass().getName() ).isEqualTo( "java.lang.Byte" );
		assertThat(
		    EqualsEquals.invoke(
		        CastAs.invoke( 1, "byte" ),
		        1
		    )
		).isTrue();
	}

	@DisplayName( "It can cast to char" )
	@Test
	void testItCanCastToChar() {
		assertThat( CastAs.invoke( "B", "char" ).getClass().getName() ).isEqualTo( "java.lang.Character" );
		assertThat( CastAs.invoke( 66, "char" ).getClass().getName() ).isEqualTo( "java.lang.Character" );
		assertThat(
		    EqualsEquals.invoke(
		        CastAs.invoke( 66, "char" ),
		        "B"
		    )
		).isTrue();
	}

	@DisplayName( "It can cast to int" )
	@Test
	void testItCanCastToInt() {
		assertThat( CastAs.invoke( 0, "int" ).getClass().getName() ).isEqualTo( "java.lang.Integer" );
		assertThat( CastAs.invoke( 5, "int" ).getClass().getName() ).isEqualTo( "java.lang.Integer" );
		assertThat( CastAs.invoke( "5", "int" ).getClass().getName() ).isEqualTo( "java.lang.Integer" );
		assertThat( CastAs.invoke( "-5", "int" ).getClass().getName() ).isEqualTo( "java.lang.Integer" );
		assertThat( CastAs.invoke( "+5", "int" ).getClass().getName() ).isEqualTo( "java.lang.Integer" );
		assertThat( CastAs.invoke( "-0", "int" ).getClass().getName() ).isEqualTo( "java.lang.Integer" );
		assertThat( CastAs.invoke( "-2147483647", "int" ).getClass().getName() ).isEqualTo( "java.lang.Integer" );
		assertThat( CastAs.invoke( "+2147483647", "int" ).getClass().getName() ).isEqualTo( "java.lang.Integer" );
		assertThat( CastAs.invoke( true, "int" ).getClass().getName() ).isEqualTo( "java.lang.Integer" );
		assertThat(
		    EqualsEquals.invoke(
		        CastAs.invoke( 5.7, "int" ),
		        5
		    )
		).isTrue();

		assertThrows( BoxRuntimeException.class, () -> {
			CastAs.invoke( "xy", "int" );
		} );
		assertThrows( BoxRuntimeException.class, () -> {
			CastAs.invoke( "1.2.3.4", "int" );
		} );
		assertThrows( BoxRuntimeException.class, () -> {
			CastAs.invoke( "false", "int" );
		} );
		assertThrows( BoxRuntimeException.class, () -> {
			CastAs.invoke( "-", "int" );
		} );
		assertThrows( BoxRuntimeException.class, () -> {
			CastAs.invoke( "1-1", "int" );
		} );
	}

	@DisplayName( "It can cast to long" )
	@Test
	void testItCanCastToLong() {
		assertThat( CastAs.invoke( 5, "long" ).getClass().getName() ).isEqualTo( "java.lang.Long" );
		assertThat( CastAs.invoke( true, "long" ).getClass().getName() ).isEqualTo( "java.lang.Long" );
		assertThat(
		    EqualsEquals.invoke(
		        CastAs.invoke( 5.7, "long" ),
		        5
		    )
		).isTrue();
	}

	@DisplayName( "It can cast to float" )
	@Test
	void testItCanCastToFloat() {
		assertThat( CastAs.invoke( 5, "float" ).getClass().getName() ).isEqualTo( "java.lang.Float" );
		assertThat( CastAs.invoke( true, "float" ).getClass().getName() ).isEqualTo( "java.lang.Float" );
		Float comparison = 5.7f;
		assertThat(
		    EqualsEquals.invoke(
		        CastAs.invoke( 5.7, "float" ),
		        comparison
		    )
		).isTrue();
	}

	@DisplayName( "It can cast to double" )
	@Test
	void testItCanCastToDouble() {
		assertThat( CastAs.invoke( 5, "double" ).getClass().getName() ).isEqualTo( "java.lang.Double" );
		assertThat( CastAs.invoke( true, "double" ).getClass().getName() ).isEqualTo( "java.lang.Double" );
		assertThat(
		    EqualsEquals.invoke(
		        CastAs.invoke( 5.7, "double" ),
		        5.7
		    )
		).isTrue();
	}

	@DisplayName( "It can cast to short" )
	@Test
	void testItCanCastToShort() {
		assertThat( CastAs.invoke( 5, "short" ).getClass().getName() ).isEqualTo( "java.lang.Short" );
		assertThat( CastAs.invoke( true, "short" ).getClass().getName() ).isEqualTo( "java.lang.Short" );
		assertThat(
		    EqualsEquals.invoke(
		        CastAs.invoke( 5.7, "short" ),
		        5
		    )
		).isTrue();
	}

	@DisplayName( "It can cast to string" )
	@Test
	void testItCanCastToString() {
		assertThat( CastAs.invoke( 5, "string" ).getClass().getName() ).isEqualTo( "java.lang.String" );
		assertThat( CastAs.invoke( true, "string" ).getClass().getName() ).isEqualTo( "java.lang.String" );
		assertThat(
		    EqualsEquals.invoke(
		        CastAs.invoke( 5.7, "string" ),
		        "5.7"
		    )
		).isTrue();
	}

	@DisplayName( "It can cast Array to array" )
	@Test
	void testItCanCastArrayToArray() {
		Object result = CastAs.invoke( new Object[] { "Brad", "Wood" }, "string[]" );
		assertThat( result.getClass().isArray() ).isTrue();
		Object[] arrResult = ( ( Object[] ) result );
		assertThat( arrResult.length ).isEqualTo( 2 );
		assertThat( arrResult[ 0 ] instanceof String ).isTrue();
		assertThat( arrResult[ 0 ] ).isEqualTo( "Brad" );
		assertThat( arrResult[ 1 ] instanceof String ).isTrue();
		assertThat( arrResult[ 1 ] ).isEqualTo( "Wood" );
	}

	@DisplayName( "It can cast List to array" )
	@Test
	void testItCanCastListToArray() {
		Object result = CastAs.invoke( Arrays.asList( new Object[] { "Brad", "Wood" } ), "string[]" );
		assertThat( result.getClass().isArray() ).isTrue();
		Object[] arrResult = ( ( Object[] ) result );
		assertThat( arrResult.length ).isEqualTo( 2 );
		assertThat( arrResult[ 0 ] instanceof String ).isTrue();
		assertThat( arrResult[ 0 ] ).isEqualTo( "Brad" );
		assertThat( arrResult[ 1 ] instanceof String ).isTrue();
		assertThat( arrResult[ 1 ] ).isEqualTo( "Wood" );
	}

}
