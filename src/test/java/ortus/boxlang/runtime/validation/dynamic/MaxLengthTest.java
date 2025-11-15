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

public class MaxLengthTest {

	private IBoxContext	context;
	private MaxLength	validator;
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
		public Object defaultValue() {
			return defaultValue;
		}

		@Override
		public Set<Validator> validators() {
			return validators;
		}
	}

	@BeforeEach
	void setUp() {
		context		= new ScriptingRequestBoxContext( BoxRuntime.getInstance().getRuntimeContext() );
		validator	= new MaxLength( 10 );
		testRecord	= new TestValidatable( Key.of( "testField" ), "string", null, null );
		caller		= Key.of( "testCaller" );
	}

	@Test
	@DisplayName( "Should pass validation when string length is exactly at maximum" )
	void testExactMaxLength() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "1234567890" ); // 10 characters

		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should pass validation when string length is below maximum" )
	void testBelowMaxLength() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "hello" ); // 5 characters

		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should throw BoxValidationException when string length exceeds maximum" )
	void testExceedsMaxLength() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "12345678901" ); // 11 characters

		BoxValidationException exception = assertThrows( BoxValidationException.class,
		    () -> validator.validate( context, caller, testRecord, records ) );

		assertThat( exception.getMessage() ).contains( "cannot be longer than" );
		assertThat( exception.getMessage() ).contains( "10" );
		assertThat( exception.getMessage() ).contains( "character" );
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
	@DisplayName( "Should handle empty string" )
	void testEmptyString() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "" );

		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle numeric values converted to string" )
	void testNumericValues() {
		IStruct records = new Struct();

		records.put( Key.of( "testField" ), 123456 ); // "123456" = 6 characters
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), 12345678901L ); // "12345678901" = 11 characters
		assertThrows( BoxValidationException.class,
		    () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle boolean values converted to string" )
	void testBooleanValues() {
		IStruct records = new Struct();

		records.put( Key.of( "testField" ), true ); // "true" = 4 characters
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), false ); // "false" = 5 characters
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle Unicode characters correctly" )
	void testUnicodeCharacters() {
		IStruct records = new Struct();

		// Each Unicode character counts as 1 character
		records.put( Key.of( "testField" ), "café" ); // 4 characters
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		// Test with fewer emoji characters that should fit within the limit
		records.put( Key.of( "testField" ), "🔥🎉✨🚀💫" ); // 5 emoji characters
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		// Test with more emoji characters that exceed the limit (10 char max)
		records.put( Key.of( "testField" ), "🔥🎉✨🚀💫🌟⭐🎯🎪🎨🎭" ); // 11 emoji characters
		assertThrows( BoxValidationException.class,
		    () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle special characters and whitespace" )
	void testSpecialCharacters() {
		IStruct records = new Struct();

		records.put( Key.of( "testField" ), "a\nb\tc d" ); // 6 characters (including newline, tab, space)
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), "a!@#$%^&*()" ); // 11 characters
		assertThrows( BoxValidationException.class,
		    () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle zero maximum length" )
	void testZeroMaxLength() {
		MaxLength	zeroValidator	= new MaxLength( 0 );
		IStruct		records			= new Struct();

		records.put( Key.of( "testField" ), "" );
		assertDoesNotThrow( () -> zeroValidator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), "a" );
		assertThrows( BoxValidationException.class,
		    () -> zeroValidator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle large maximum length" )
	void testLargeMaxLength() {
		MaxLength		largeValidator	= new MaxLength( 1000 );
		IStruct			records			= new Struct();

		StringBuilder	longString		= new StringBuilder();
		for ( int i = 0; i < 999; i++ ) {
			longString.append( "a" );
		}

		records.put( Key.of( "testField" ), longString.toString() );
		assertDoesNotThrow( () -> largeValidator.validate( context, caller, testRecord, records ) );

		longString.append( "a" ); // Now 1000 characters
		records.put( Key.of( "testField" ), longString.toString() );
		assertDoesNotThrow( () -> largeValidator.validate( context, caller, testRecord, records ) );

		longString.append( "a" ); // Now 1001 characters
		records.put( Key.of( "testField" ), longString.toString() );
		assertThrows( BoxValidationException.class,
		    () -> largeValidator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle decimal maximum length values" )
	void testDecimalMaxLength() {
		MaxLength	decimalValidator	= new MaxLength( 5.7 ); // Should be treated as 5.7 but compared as double
		IStruct		records				= new Struct();

		records.put( Key.of( "testField" ), "12345" ); // 5 characters
		assertDoesNotThrow( () -> decimalValidator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), "123456" ); // 6 characters (greater than 5.7)
		assertThrows( BoxValidationException.class,
		    () -> decimalValidator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle error message formatting for singular vs plural" )
	void testErrorMessageFormatting() {
		MaxLength	singleValidator	= new MaxLength( 1 );
		IStruct		records			= new Struct();
		records.put( Key.of( "testField" ), "ab" );

		BoxValidationException exception = assertThrows( BoxValidationException.class,
		    () -> singleValidator.validate( context, caller, testRecord, records ) );

		// Should mention "1 character" (singular)
		assertThat( exception.getMessage() ).contains( "[1]" );
		assertThat( exception.getMessage() ).contains( "character(s)" );
	}
}