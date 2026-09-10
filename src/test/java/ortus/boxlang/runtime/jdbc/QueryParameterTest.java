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

package ortus.boxlang.runtime.jdbc;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;

public class QueryParameterTest {

	static Stream<Object> numericMetadataValues() {
		return Stream.of( ( byte ) 2, ( short ) 2, 2, 2L, 2.0F, 2.0D, new BigInteger( "2" ), new BigDecimal( "2.00" ), "2.0" );
	}

	@ParameterizedTest
	@MethodSource( "numericMetadataValues" )
	void testNumericScaleCoercion( Object scale ) {
		QueryParameter parameter = QueryParameter.fromAny( Struct.of( Key.value, new BigDecimal( "12.50" ), Key.sqltype, "decimal", Key.scale, scale ) );
		assertThat( parameter.getScaleOrLength() ).isEqualTo( 2 );
	}

	static Stream<Object> invalidMetadataValues() {
		return Stream.of( 2.5D, 2.5F, new BigDecimal( "2.5" ), "2.5", "invalid", Double.NaN, Double.POSITIVE_INFINITY,
		    Double.NEGATIVE_INFINITY, 2147483648L, -2147483649L, new BigInteger( "2147483648" ), new BigDecimal( "2147483648" ) );
	}

	@ParameterizedTest
	@MethodSource( "invalidMetadataValues" )
	void testInvalidScaleRejected( Object scale ) {
		assertThrows( BoxCastException.class,
		    () -> QueryParameter.fromAny( Struct.of( Key.value, 12.5, Key.sqltype, "decimal", Key.scale, scale ) ) );
	}

	@Test
	void testOptionalScale() {
		assertThat( QueryParameter.fromAny( Struct.of( Key.value, 12.5, Key.sqltype, "decimal" ) ).getScaleOrLength() ).isNull();
		assertThat( QueryParameter.fromAny( Struct.of( Key.value, 12.5, Key.sqltype, "decimal", Key.scale, null ) ).getScaleOrLength() ).isNull();
		assertThat( QueryParameter.fromAny( Struct.of( Key.value, 12, Key.sqltype, "decimal", Key.scale, 0.0D ) ).getScaleOrLength() ).isEqualTo( 0 );
	}

	@ParameterizedTest
	@MethodSource( "numericMetadataValues" )
	void testNumericMaxLengthCoercion( Object maxLength ) {
		QueryParameter parameter = QueryParameter.fromAny( Struct.of( Key.value, "ab", Key.sqltype, "varchar", Key.maxLength, maxLength ) );
		assertThat( parameter.getScaleOrLength() ).isEqualTo( 2 );
	}

	@ParameterizedTest
	@MethodSource( "invalidMetadataValues" )
	void testInvalidMaxLengthRejected( Object maxLength ) {
		assertThrows( BoxCastException.class,
		    () -> QueryParameter.fromAny( Struct.of( Key.value, "ab", Key.sqltype, "varchar", Key.maxLength, maxLength ) ) );
	}

	@Test
	void testOptionalMaxLength() {
		assertThat( QueryParameter.fromAny( Struct.of( Key.value, "ab", Key.sqltype, "varchar" ) ).getScaleOrLength() ).isNull();
		assertThat( QueryParameter.fromAny( Struct.of( Key.value, "ab", Key.sqltype, "varchar", Key.maxLength, null ) ).getScaleOrLength() ).isNull();
		assertThat( QueryParameter.fromAny( Struct.of( Key.value, "", Key.sqltype, "varchar", Key.maxLength, 0.0D ) ).getScaleOrLength() ).isEqualTo( 0 );
	}

}
