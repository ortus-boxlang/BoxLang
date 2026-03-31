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
package ortus.boxlang.runtime.validation.dynamic;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.validation.Validatable;
import ortus.boxlang.runtime.validation.Validator;

public class MaxTest {

	private IBoxContext	context;
	private Max			validator;
	private Validatable	testRecord;
	private Key			caller;

	/**
	 * Simple test implementation of Validatable
	 */
	private static class TestValidatable implements Validatable {

		private final Key				name;
		private final String			type;
		private final Object			defaultValue;
		private final Set<Validator>	validators;

		public TestValidatable( Key name, String type, Object defaultValue, Set<Validator> validators ) {
			this.name			= name;
			this.type			= type;
			this.defaultValue	= defaultValue;
			this.validators		= validators != null ? validators : Set.of();
		}

		@Override
		public Key name() {
			return name;
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public Object getDefaultValue( IBoxContext context ) {
			return defaultValue;
		}

		@Override
		public boolean hasDefaultValue() {
			return defaultValue != null;
		}

		@Override
		public Set<Validator> validators() {
			return validators;
		}
	}

	@BeforeEach
	void setUp() {
		context		= new ScriptingRequestBoxContext( BoxRuntime.getInstance().getRuntimeContext() );
		validator	= new Max( 100 );
		testRecord	= new TestValidatable( Key.of( "testField" ), "numeric", null, null );
		caller		= Key.of( "testCaller" );
	}

	@Test
	@DisplayName( "Should pass validation when value is exactly at maximum" )
	void testExactMax() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), 100 );

		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should pass validation when value is below maximum" )
	void testBelowMax() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), 50 );

		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should throw BoxValidationException when value exceeds maximum" )
	void testExceedsMax() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), 150 );

		BoxValidationException exception = assertThrows( BoxValidationException.class,
		    () -> validator.validate( context, caller, testRecord, records ) );

		assertThat( exception.getMessage() ).contains( "cannot be greater than" );
		assertThat( exception.getMessage() ).contains( "100" );
	}

	@Test
	@DisplayName( "Should pass validation when record is not present (optional field)" )
	void testMissingRecord() {
		IStruct records = new Struct();
		// testField is not added to records

		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should pass validation when record value is null" )
	void testNullValue() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), null );

		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle integer values" )
	void testIntegerValues() {
		IStruct records = new Struct();

		records.put( Key.of( "testField" ), 99 );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), 101 );
		assertThrows( BoxValidationException.class,
		    () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle long values" )
	void testLongValues() {
		IStruct records = new Struct();

		records.put( Key.of( "testField" ), 99L );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), 101L );
		assertThrows( BoxValidationException.class,
		    () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle double values" )
	void testDoubleValues() {
		IStruct records = new Struct();

		records.put( Key.of( "testField" ), 99.5 );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), 100.1 );
		assertThrows( BoxValidationException.class,
		    () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle float values" )
	void testFloatValues() {
		IStruct records = new Struct();

		records.put( Key.of( "testField" ), 99.9f );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), 100.1f );
		assertThrows( BoxValidationException.class,
		    () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle BigDecimal values" )
	void testBigDecimalValues() {
		IStruct records = new Struct();

		records.put( Key.of( "testField" ), new BigDecimal( "99.999" ) );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), new BigDecimal( "100.001" ) );
		assertThrows( BoxValidationException.class,
		    () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle string representation of numbers" )
	void testStringNumbers() {
		IStruct records = new Struct();

		records.put( Key.of( "testField" ), "99" );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), "99.5" );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), "101" );
		assertThrows( BoxValidationException.class,
		    () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle negative numbers with negative maximum" )
	void testNegativeNumbers() {
		Max		negativeValidator	= new Max( -10 );
		IStruct	records				= new Struct();

		records.put( Key.of( "testField" ), -15 );
		assertDoesNotThrow( () -> negativeValidator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), -10 );
		assertDoesNotThrow( () -> negativeValidator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), -5 );
		assertThrows( BoxValidationException.class,
		    () -> negativeValidator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle zero values" )
	void testZeroValues() {
		Max		zeroValidator	= new Max( 0 );
		IStruct	records			= new Struct();

		records.put( Key.of( "testField" ), -1 );
		assertDoesNotThrow( () -> zeroValidator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), 0 );
		assertDoesNotThrow( () -> zeroValidator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), 1 );
		assertThrows( BoxValidationException.class,
		    () -> zeroValidator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle edge case with very large numbers" )
	void testLargeNumbers() {
		Max		largeValidator	= new Max( Long.MAX_VALUE );
		IStruct	records			= new Struct();

		records.put( Key.of( "testField" ), Long.MAX_VALUE - 1 );
		assertDoesNotThrow( () -> largeValidator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), Long.MAX_VALUE );
		assertDoesNotThrow( () -> largeValidator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle decimal maximum values" )
	void testDecimalMaximum() {
		Max		decimalValidator	= new Max( 10.5 );
		IStruct	records				= new Struct();

		records.put( Key.of( "testField" ), 10.4 );
		assertDoesNotThrow( () -> decimalValidator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), 10.5 );
		assertDoesNotThrow( () -> decimalValidator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), 10.6 );
		assertThrows( BoxValidationException.class,
		    () -> decimalValidator.validate( context, caller, testRecord, records ) );
	}
}