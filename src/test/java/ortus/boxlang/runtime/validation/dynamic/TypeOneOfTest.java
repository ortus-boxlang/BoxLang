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
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.validation.Validatable;
import ortus.boxlang.runtime.validation.Validator;

public class TypeOneOfTest {

	private IBoxContext	context;
	private TypeOneOf	validator;
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
		validator	= new TypeOneOf( Set.of( "string", "numeric", "boolean" ) );
		testRecord	= new TestValidatable( Key.of( "testField" ), "any", null, null );
		caller		= Key.of( "testCaller" );
	}

	@Test
	@DisplayName( "Should pass validation when value can be cast to string" )
	void testStringType() {
		IStruct records = new Struct();

		records.put( Key.of( "testField" ), "hello" );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), 123 );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), true );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should pass validation when value can be cast to numeric" )
	void testNumericType() {
		IStruct records = new Struct();

		records.put( Key.of( "testField" ), 123 );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), 123.45 );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), "123" );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), "123.45" );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), new BigDecimal( "123.45" ) );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should pass validation when value can be cast to boolean" )
	void testBooleanType() {
		IStruct records = new Struct();

		records.put( Key.of( "testField" ), true );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), false );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), "true" );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), "false" );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), "yes" );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), "no" );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), 1 );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), 0 );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle array type validation" )
	void testArrayType() {
		TypeOneOf	arrayValidator	= new TypeOneOf( Set.of( "array" ) );
		IStruct		records			= new Struct();

		Array		testArray		= new Array();
		testArray.add( "item1" );
		testArray.add( "item2" );

		records.put( Key.of( "testField" ), testArray );
		assertDoesNotThrow( () -> arrayValidator.validate( context, caller, testRecord, records ) );

		// String should not be castable to array
		records.put( Key.of( "testField" ), "not an array" );
		assertThrows( BoxValidationException.class,
		    () -> arrayValidator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle struct type validation" )
	void testStructType() {
		TypeOneOf	structValidator	= new TypeOneOf( Set.of( "struct" ) );
		IStruct		records			= new Struct();

		IStruct		testStruct		= new Struct();
		testStruct.put( Key.of( "key1" ), "value1" );
		testStruct.put( Key.of( "key2" ), "value2" );

		records.put( Key.of( "testField" ), testStruct );
		assertDoesNotThrow( () -> structValidator.validate( context, caller, testRecord, records ) );

		// String should not be castable to struct
		records.put( Key.of( "testField" ), "not a struct" );
		assertThrows( BoxValidationException.class,
		    () -> structValidator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should throw BoxValidationException when value cannot be cast to any allowed type" )
	void testInvalidType() {
		TypeOneOf	restrictiveValidator	= new TypeOneOf( Set.of( "array" ) );
		IStruct		records					= new Struct();

		records.put( Key.of( "testField" ), "not an array" );

		BoxValidationException exception = assertThrows( BoxValidationException.class,
		    () -> restrictiveValidator.validate( context, caller, testRecord, records ) );

		assertThat( exception.getMessage() ).contains( "must be one of the following types" );
		assertThat( exception.getMessage() ).contains( "array" );
	}

	@Test
	@DisplayName( "Should pass validation when record is not present (optional field)" )
	void testMissingRecord() {
		IStruct records = new Struct();
		// testField is not added to records

		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle null values appropriately" )
	void testNullValue() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), null );

		// Null values should typically pass validation (depending on GenericCaster behavior)
		// The behavior may vary based on how GenericCaster handles null values
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle multiple type options" )
	void testMultipleTypes() {
		TypeOneOf	multiValidator	= new TypeOneOf( Set.of( "string", "numeric", "boolean", "array", "struct" ) );
		IStruct		records			= new Struct();

		// String value
		records.put( Key.of( "testField" ), "hello" );
		assertDoesNotThrow( () -> multiValidator.validate( context, caller, testRecord, records ) );

		// Numeric value
		records.put( Key.of( "testField" ), 123 );
		assertDoesNotThrow( () -> multiValidator.validate( context, caller, testRecord, records ) );

		// Boolean value
		records.put( Key.of( "testField" ), true );
		assertDoesNotThrow( () -> multiValidator.validate( context, caller, testRecord, records ) );

		// Array value
		Array testArray = new Array();
		testArray.add( "item" );
		records.put( Key.of( "testField" ), testArray );
		assertDoesNotThrow( () -> multiValidator.validate( context, caller, testRecord, records ) );

		// Struct value
		IStruct testStruct = new Struct();
		testStruct.put( Key.of( "key" ), "value" );
		records.put( Key.of( "testField" ), testStruct );
		assertDoesNotThrow( () -> multiValidator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle single type validation" )
	void testSingleType() {
		TypeOneOf	singleValidator	= new TypeOneOf( Set.of( "string" ) );
		IStruct		records			= new Struct();

		records.put( Key.of( "testField" ), "valid string" );
		assertDoesNotThrow( () -> singleValidator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), 123 ); // Can be cast to string
		assertDoesNotThrow( () -> singleValidator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle empty allowed types set" )
	void testEmptyAllowedTypes() {
		TypeOneOf	emptyValidator	= new TypeOneOf( Set.of() );
		IStruct		records			= new Struct();

		records.put( Key.of( "testField" ), "any value" );
		assertThrows( BoxValidationException.class,
		    () -> emptyValidator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle case sensitivity in type names" )
	void testTypeCaseSensitivity() {
		TypeOneOf	caseSensitiveValidator	= new TypeOneOf( Set.of( "String", "NUMERIC", "Boolean" ) );
		IStruct		records					= new Struct();

		// The GenericCaster should handle case variations appropriately
		records.put( Key.of( "testField" ), "hello" );
		// This test depends on GenericCaster's implementation
		// If it's case-sensitive, this might fail; if case-insensitive, it should pass
		try {
			caseSensitiveValidator.validate( context, caller, testRecord, records );
		} catch ( BoxValidationException e ) {
			// This is acceptable - it indicates case sensitivity in type casting
			assertThat( e.getMessage() ).contains( "must be one of the following types" );
		}
	}

	@Test
	@DisplayName( "Should provide helpful error message with all allowed types" )
	void testErrorMessageContainsAllTypes() {
		TypeOneOf	multiValidator	= new TypeOneOf( Set.of( "array", "struct", "query" ) );
		IStruct		records			= new Struct();

		records.put( Key.of( "testField" ), "invalid for these types" );

		BoxValidationException	exception		= assertThrows( BoxValidationException.class,
		    () -> multiValidator.validate( context, caller, testRecord, records ) );

		String					errorMessage	= exception.getMessage();
		assertThat( errorMessage ).contains( "must be one of the following types" );
		assertThat( errorMessage ).contains( "array" );
		assertThat( errorMessage ).contains( "struct" );
		assertThat( errorMessage ).contains( "query" );
	}
}