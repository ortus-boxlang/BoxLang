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
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.validation.Validatable;
import ortus.boxlang.runtime.validation.Validator;

public class MinLengthTest {

	private IBoxContext	context;
	private MinLength	validator;
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
		validator	= new MinLength( 5 );
		testRecord	= new TestValidatable( Key.of( "testField" ), "string", null, null );
		caller		= Key.of( "testCaller" );
	}

	@Test
	@DisplayName( "Should pass validation when string length is exactly at minimum" )
	void testExactMinLength() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "12345" ); // 5 characters

		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should pass validation when string length is above minimum" )
	void testAboveMinLength() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "hello world" ); // 11 characters

		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should throw BoxValidationException when string length is below minimum" )
	void testBelowMinLength() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "hi" ); // 2 characters

		BoxValidationException exception = assertThrows( BoxValidationException.class,
		    () -> validator.validate( context, caller, testRecord, records ) );

		assertThat( exception.getMessage() ).contains( "cannot be shorter than" );
		assertThat( exception.getMessage() ).contains( "5" );
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
	@DisplayName( "Should handle empty string as below minimum" )
	void testEmptyString() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "" );

		assertThrows( BoxValidationException.class,
		    () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should validate array length" )
	void testArrayLength() {
		IStruct	records		= new Struct();

		// Array with exactly 5 elements
		Array	validArray	= new Array();
		validArray.add( "a" );
		validArray.add( "b" );
		validArray.add( "c" );
		validArray.add( "d" );
		validArray.add( "e" );

		records.put( Key.of( "testField" ), validArray );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		// Array with more than 5 elements
		validArray.add( "f" );
		records.put( Key.of( "testField" ), validArray );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		// Array with fewer than 5 elements
		Array shortArray = new Array();
		shortArray.add( "a" );
		shortArray.add( "b" );

		records.put( Key.of( "testField" ), shortArray );
		BoxValidationException exception = assertThrows( BoxValidationException.class,
		    () -> validator.validate( context, caller, testRecord, records ) );

		assertThat( exception.getMessage() ).contains( "cannot be shorter than" );
		assertThat( exception.getMessage() ).contains( "5" );
		assertThat( exception.getMessage() ).contains( "item" );
	}

	@Test
	@DisplayName( "Should validate struct size" )
	void testStructSize() {
		IStruct	records		= new Struct();

		// Struct with exactly 5 keys
		IStruct	validStruct	= new Struct();
		validStruct.put( Key.of( "a" ), 1 );
		validStruct.put( Key.of( "b" ), 2 );
		validStruct.put( Key.of( "c" ), 3 );
		validStruct.put( Key.of( "d" ), 4 );
		validStruct.put( Key.of( "e" ), 5 );

		records.put( Key.of( "testField" ), validStruct );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		// Struct with more than 5 keys
		validStruct.put( Key.of( "f" ), 6 );
		records.put( Key.of( "testField" ), validStruct );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		// Struct with fewer than 5 keys
		IStruct shortStruct = new Struct();
		shortStruct.put( Key.of( "a" ), 1 );
		shortStruct.put( Key.of( "b" ), 2 );

		records.put( Key.of( "testField" ), shortStruct );
		BoxValidationException exception = assertThrows( BoxValidationException.class,
		    () -> validator.validate( context, caller, testRecord, records ) );

		assertThat( exception.getMessage() ).contains( "cannot be shorter than" );
		assertThat( exception.getMessage() ).contains( "5" );
		assertThat( exception.getMessage() ).contains( "record" );
	}

	@Test
	@DisplayName( "Should handle numeric values converted to string" )
	void testNumericValues() {
		IStruct records = new Struct();

		records.put( Key.of( "testField" ), 12345 ); // "12345" = 5 characters
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), 123 ); // "123" = 3 characters
		assertThrows( BoxValidationException.class,
		    () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle boolean values converted to string" )
	void testBooleanValues() {
		MinLength	shortValidator	= new MinLength( 4 );
		IStruct		records			= new Struct();

		records.put( Key.of( "testField" ), true ); // "true" = 4 characters
		assertDoesNotThrow( () -> shortValidator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), false ); // "false" = 5 characters
		assertDoesNotThrow( () -> shortValidator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle Unicode characters correctly" )
	void testUnicodeCharacters() {
		IStruct records = new Struct();

		// Each Unicode character counts as 1 character
		records.put( Key.of( "testField" ), "café!" ); // 5 characters
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), "🔥🎉✨🚀💫" ); // 5 emoji characters
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), "🔥🎉" ); // 2 emoji characters
		assertThrows( BoxValidationException.class,
		    () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle special characters and whitespace" )
	void testSpecialCharacters() {
		IStruct records = new Struct();

		records.put( Key.of( "testField" ), "a\nb\tc" ); // 5 characters (including newline, tab)
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), "a\nb" ); // 3 characters
		assertThrows( BoxValidationException.class,
		    () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle zero minimum length" )
	void testZeroMinLength() {
		MinLength	zeroValidator	= new MinLength( 0 );
		IStruct		records			= new Struct();

		records.put( Key.of( "testField" ), "" );
		assertDoesNotThrow( () -> zeroValidator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), "a" );
		assertDoesNotThrow( () -> zeroValidator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle decimal minimum length values" )
	void testDecimalMinLength() {
		MinLength	decimalValidator	= new MinLength( 3.7 ); // Should be treated as 3.7 but compared as double
		IStruct		records				= new Struct();

		records.put( Key.of( "testField" ), "1234" ); // 4 characters (greater than 3.7)
		assertDoesNotThrow( () -> decimalValidator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), "123" ); // 3 characters (less than 3.7)
		assertThrows( BoxValidationException.class,
		    () -> decimalValidator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle error message formatting for singular vs plural items" )
	void testItemErrorMessageFormatting() {
		MinLength	singleValidator	= new MinLength( 1 );
		IStruct		records			= new Struct();

		Array		emptyArray		= new Array();
		records.put( Key.of( "testField" ), emptyArray );

		BoxValidationException exception = assertThrows( BoxValidationException.class,
		    () -> singleValidator.validate( context, caller, testRecord, records ) );

		// Should mention "1 item" (singular)
		assertThat( exception.getMessage() ).contains( "[1]" );
		assertThat( exception.getMessage() ).contains( "item" );
		assertThat( exception.getMessage() ).doesNotContain( "items" );
	}

	@Test
	@DisplayName( "Should handle error message formatting for plural items" )
	void testPluralItemErrorMessageFormatting() {
		MinLength	pluralValidator	= new MinLength( 3 );
		IStruct		records			= new Struct();

		Array		shortArray		= new Array();
		shortArray.add( "one" );
		records.put( Key.of( "testField" ), shortArray );

		BoxValidationException exception = assertThrows( BoxValidationException.class,
		    () -> pluralValidator.validate( context, caller, testRecord, records ) );

		// Should mention "3 items" (plural)
		assertThat( exception.getMessage() ).contains( "[3]" );
		assertThat( exception.getMessage() ).contains( "items" );
	}

	@Test
	@DisplayName( "Should handle error message formatting for singular vs plural records" )
	void testRecordErrorMessageFormatting() {
		MinLength	singleValidator	= new MinLength( 1 );
		IStruct		records			= new Struct();

		IStruct		emptyStruct		= new Struct();
		records.put( Key.of( "testField" ), emptyStruct );

		BoxValidationException exception = assertThrows( BoxValidationException.class,
		    () -> singleValidator.validate( context, caller, testRecord, records ) );

		// Should mention "1 record" (singular)
		assertThat( exception.getMessage() ).contains( "[1]" );
		assertThat( exception.getMessage() ).contains( "record" );
		assertThat( exception.getMessage() ).doesNotContain( "records" );
	}

	@Test
	@DisplayName( "Should handle error message formatting for plural records" )
	void testPluralRecordErrorMessageFormatting() {
		MinLength	pluralValidator	= new MinLength( 3 );
		IStruct		records			= new Struct();

		IStruct		shortStruct		= new Struct();
		shortStruct.put( Key.of( "one" ), 1 );
		records.put( Key.of( "testField" ), shortStruct );

		BoxValidationException exception = assertThrows( BoxValidationException.class,
		    () -> pluralValidator.validate( context, caller, testRecord, records ) );

		// Should mention "3 records" (plural)
		assertThat( exception.getMessage() ).contains( "[3]" );
		assertThat( exception.getMessage() ).contains( "records" );
	}
}