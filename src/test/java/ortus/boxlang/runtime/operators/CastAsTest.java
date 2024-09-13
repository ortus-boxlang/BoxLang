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

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.MathUtil;

public class CastAsTest {

	private static final IBoxContext context = new ScriptingRequestBoxContext();

	@DisplayName( "It can cast to null" )
	@Test
	void testItCanCastToNull() {
		assertThat( CastAs.invoke( context, "", "null" ) ).isNull();
		assertThat( CastAs.invoke( context, "sfsfdsdf", "null" ) ).isNull();
		assertThat( CastAs.invoke( context, 123, "null" ) ).isNull();
	}

	@DisplayName( "It can cast to bigdecimal" )
	@Test
	void testItCanCastToBigDecimal() {
		assertThat( CastAs.invoke( context, 5, "bigdecimal" ).getClass().getName() ).isEqualTo( "java.math.BigDecimal" );
		assertThat( CastAs.invoke( context, "5", "bigdecimal" ).getClass().getName() ).isEqualTo( "java.math.BigDecimal" );
		assertThat(
		    EqualsEquals.invoke(
		        CastAs.invoke( context, 5, "bigdecimal" ),
		        5
		    )
		).isTrue();
	}

	@DisplayName( "It can cast to decimal" )
	@Test
	void testItCanCastToDecimal() {
		assertThat( CastAs.invoke( context, 5, "decimal" ).getClass().getName() ).isEqualTo( "java.math.BigDecimal" );
		assertThat( CastAs.invoke( context, "5", "decimal" ).getClass().getName() ).isEqualTo( "java.math.BigDecimal" );
		assertThat(
		    EqualsEquals.invoke(
		        CastAs.invoke( context, 5, "decimal" ),
		        5
		    )
		).isTrue();
	}

	@DisplayName( "It can cast to boolean" )
	@Test
	void testItCanCastToBoolean() {
		assertThat( CastAs.invoke( context, true, "boolean" ).getClass().getName() ).isEqualTo( "java.lang.Boolean" );
		assertThat( CastAs.invoke( context, "true", "boolean" ).getClass().getName() ).isEqualTo( "java.lang.Boolean" );
		assertThat( CastAs.invoke( context, 1, "boolean" ).getClass().getName() ).isEqualTo( "java.lang.Boolean" );
		assertThat(
		    EqualsEquals.invoke(
		        CastAs.invoke( context, true, "boolean" ),
		        true
		    )
		).isTrue();
	}

	@DisplayName( "It can cast to byte" )
	@Test
	void testItCanCastToByte() {
		assertThat( CastAs.invoke( context, 1, "byte" ).getClass().getName() ).isEqualTo( "java.lang.Byte" );
		assertThat( CastAs.invoke( context, "0", "byte" ).getClass().getName() ).isEqualTo( "java.lang.Byte" );
		assertThat(
		    EqualsEquals.invoke(
		        CastAs.invoke( context, 1, "byte" ),
		        1
		    )
		).isTrue();
	}

	@DisplayName( "It can cast to char" )
	@Test
	void testItCanCastToChar() {
		assertThat( CastAs.invoke( context, "B", "char" ).getClass().getName() ).isEqualTo( "java.lang.Character" );
		assertThat( CastAs.invoke( context, 66, "char" ).getClass().getName() ).isEqualTo( "java.lang.Character" );
		assertThat(
		    EqualsEquals.invoke(
		        CastAs.invoke( context, 66, "char" ),
		        "B"
		    )
		).isTrue();
	}

	@DisplayName( "It can cast to int" )
	@Test
	void testItCanCastToInt() {
		assertThat( CastAs.invoke( context, 0, "int" ).getClass().getName() ).isEqualTo( "java.lang.Integer" );
		assertThat( CastAs.invoke( context, 5, "int" ).getClass().getName() ).isEqualTo( "java.lang.Integer" );
		assertThat( CastAs.invoke( context, "5", "int" ).getClass().getName() ).isEqualTo( "java.lang.Integer" );
		assertThat( CastAs.invoke( context, "-5", "int" ).getClass().getName() ).isEqualTo( "java.lang.Integer" );
		assertThat( CastAs.invoke( context, "+5", "int" ).getClass().getName() ).isEqualTo( "java.lang.Integer" );
		assertThat( CastAs.invoke( context, "-0", "int" ).getClass().getName() ).isEqualTo( "java.lang.Integer" );
		assertThat( CastAs.invoke( context, "-2147483647", "int" ).getClass().getName() ).isEqualTo( "java.lang.Integer" );
		assertThat( CastAs.invoke( context, "+2147483647", "int" ).getClass().getName() ).isEqualTo( "java.lang.Integer" );
		assertThat( CastAs.invoke( context, true, "int" ).getClass().getName() ).isEqualTo( "java.lang.Integer" );
		assertThat(
		    EqualsEquals.invoke(
		        CastAs.invoke( context, 5.7, "int" ),
		        5
		    )
		).isTrue();

		assertThrows( BoxRuntimeException.class, () -> {
			CastAs.invoke( context, "xy", "int" );
		} );
		assertThrows( BoxRuntimeException.class, () -> {
			CastAs.invoke( context, "1.2.3.4", "int" );
		} );
		assertThrows( BoxRuntimeException.class, () -> {
			CastAs.invoke( context, "false", "int" );
		} );
		assertThrows( BoxRuntimeException.class, () -> {
			CastAs.invoke( context, "-", "int" );
		} );
		assertThrows( BoxRuntimeException.class, () -> {
			CastAs.invoke( context, "1-1", "int" );
		} );
	}

	@DisplayName( "It can cast to long" )
	@Test
	void testItCanCastToLong() {
		assertThat( CastAs.invoke( context, 5, "long" ).getClass().getName() ).isEqualTo( "java.lang.Long" );
		assertThat( CastAs.invoke( context, true, "long" ).getClass().getName() ).isEqualTo( "java.lang.Long" );
		assertThat(
		    EqualsEquals.invoke(
		        CastAs.invoke( context, 5.7, "long" ),
		        5
		    )
		).isTrue();
	}

	@DisplayName( "It can cast to float" )
	@Test
	void testItCanCastToFloat() {
		assertThat( CastAs.invoke( context, 5, "float" ).getClass().getName() ).isEqualTo( "java.lang.Float" );
		assertThat( CastAs.invoke( context, true, "float" ).getClass().getName() ).isEqualTo( "java.lang.Float" );
		Float comparison = 5.7f;
		assertThat(
		    EqualsEquals.invoke(
		        CastAs.invoke( context, 5.7, "float" ),
		        comparison
		    )
		).isTrue();
	}

	@DisplayName( "It can cast to double/numeric/number" )
	@Test
	void testItCanCastToDouble() {
		assertThat( CastAs.invoke( context, 5, "double" ).getClass().getName() ).isEqualTo( "java.lang.Double" );
		assertThat( CastAs.invoke( context, 5, "numeric" ).getClass().getName() ).isEqualTo( "java.lang.Integer" );
		assertThat( CastAs.invoke( context, 5.5, "number" ).getClass().getName() ).isEqualTo( "java.lang.Double" );
		assertThat( CastAs.invoke( context, 5, "numeric" ).getClass().getName() ).isEqualTo( "java.lang.Integer" );
		assertThat( CastAs.invoke( context, 5.5, "number" ).getClass().getName() ).isEqualTo( "java.lang.Double" );
		assertThat( CastAs.invoke( context, "5", "numeric" ).getClass().getName() ).isEqualTo( "java.lang.Integer" );
		assertThat( CastAs.invoke( context, "5.5", "number" ).getClass().getName() )
		    .isEqualTo( MathUtil.isHighPrecisionMath() ? "java.math.BigDecimal" : "java.lang.Double" );
		assertThat( CastAs.invoke( context, true, "double" ).getClass().getName() ).isEqualTo( "java.lang.Double" );
		assertThat(
		    EqualsEquals.invoke(
		        CastAs.invoke( context, 5.7, "double" ),
		        5.7
		    )
		).isTrue();
	}

	@DisplayName( "It can cast to short" )
	@Test
	void testItCanCastToShort() {
		assertThat( CastAs.invoke( context, 5, "short" ).getClass().getName() ).isEqualTo( "java.lang.Short" );
		assertThat( CastAs.invoke( context, true, "short" ).getClass().getName() ).isEqualTo( "java.lang.Short" );
		assertThat(
		    EqualsEquals.invoke(
		        CastAs.invoke( context, 5.7, "short" ),
		        5
		    )
		).isTrue();
	}

	@DisplayName( "It can cast to string" )
	@Test
	void testItCanCastToString() {
		assertThat( CastAs.invoke( context, 5, "string" ).getClass().getName() ).isEqualTo( "java.lang.String" );
		assertThat( CastAs.invoke( context, true, "string" ).getClass().getName() ).isEqualTo( "java.lang.String" );
		assertThat(
		    EqualsEquals.invoke(
		        CastAs.invoke( context, 5.7, "string" ),
		        "5.7"
		    )
		).isTrue();
	}

	@DisplayName( "It can cast Array to array" )
	@Test
	void testItCanCastArrayToArray() {
		Object result = CastAs.invoke( context, new Object[] { "Brad", "Wood" }, "string[]" );
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
		Object result = CastAs.invoke( context, Arrays.asList( new Object[] { "Brad", "Wood" } ), "string[]" );
		assertThat( result.getClass().isArray() ).isTrue();
		Object[] arrResult = ( ( Object[] ) result );
		assertThat( arrResult.length ).isEqualTo( 2 );
		assertThat( arrResult[ 0 ] instanceof String ).isTrue();
		assertThat( arrResult[ 0 ] ).isEqualTo( "Brad" );
		assertThat( arrResult[ 1 ] instanceof String ).isTrue();
		assertThat( arrResult[ 1 ] ).isEqualTo( "Wood" );
	}

	@DisplayName( "It can cast class to class" )
	@Test
	void testItCanCastClassToClass() {
		IClassRunnable	boxClass	= ( IClassRunnable ) DynamicObject.of( RunnableLoader.getInstance().loadClass(
		    """
		    class {}
		      """, context, BoxSourceType.BOXSCRIPT ) )
		    .invokeConstructor( context )
		    .getTargetInstance();

		Object			result		= CastAs.invoke( context, boxClass, "component" );
		assertThat( result ).isInstanceOf( IClassRunnable.class );
		result = CastAs.invoke( context, boxClass, "class" );
		assertThat( result ).isInstanceOf( IClassRunnable.class );
	}

}
