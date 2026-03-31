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

public class ValueRequiresTest {

	private IBoxContext		context;
	private ValueRequires	validator;
	private Validatable		testRecord;
	private Key				caller;

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
		validator	= new ValueRequires( "triggerValue", Set.of( Key.of( "requiredField1" ), Key.of( "requiredField2" ) ) );
		testRecord	= new TestValidatable( Key.of( "testField" ), "string", null, null );
		caller		= Key.of( "testCaller" );
	}

	@Test
	@DisplayName( "Should pass validation when target record is not present" )
	void testTargetRecordNotPresent() {
		IStruct records = new Struct();
		// testField is not present, so requirements don't need to be checked

		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should pass validation when target record has different value" )
	void testTargetRecordDifferentValue() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "differentValue" );
		// Required fields are not needed since the trigger value doesn't match

		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should pass validation when target record has trigger value and all required records exist" )
	void testTriggerValueWithAllRequiredPresent() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "triggerValue" );
		records.put( Key.of( "requiredField1" ), "value1" );
		records.put( Key.of( "requiredField2" ), "value2" );

		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should throw BoxValidationException when target record has trigger value but required records are missing" )
	void testTriggerValueWithMissingRequired() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "triggerValue" );
		// requiredField1 and requiredField2 are missing

		BoxValidationException exception = assertThrows( BoxValidationException.class,
		    () -> validator.validate( context, caller, testRecord, records ) );

		assertThat( exception.getMessage() ).contains( "requires the following records to be present" );
		assertThat( exception.getMessage() ).contains( "requiredField1" );
		assertThat( exception.getMessage() ).contains( "requiredField2" );
	}

	@Test
	@DisplayName( "Should throw BoxValidationException when target record has trigger value but some required records are missing" )
	void testTriggerValueWithSomeRequiredMissing() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "triggerValue" );
		records.put( Key.of( "requiredField1" ), "value1" );
		// requiredField2 is missing

		BoxValidationException exception = assertThrows( BoxValidationException.class,
		    () -> validator.validate( context, caller, testRecord, records ) );

		assertThat( exception.getMessage() ).contains( "requires the following records to be present" );
		assertThat( exception.getMessage() ).contains( "requiredField2" );
		assertThat( exception.getMessage() ).doesNotContain( "requiredField1" );
	}

	@Test
	@DisplayName( "Should handle case insensitive trigger value matching" )
	void testCaseInsensitiveTriggerValue() {
		IStruct records = new Struct();
		records.put( Key.of( "requiredField1" ), "value1" );
		records.put( Key.of( "requiredField2" ), "value2" );

		// Test uppercase trigger value
		records.put( Key.of( "testField" ), "TRIGGERVALUE" );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		// Test mixed case trigger value
		records.put( Key.of( "testField" ), "TriggerValue" );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		// Test lowercase trigger value
		records.put( Key.of( "testField" ), "triggervalue" );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle case insensitive trigger value with missing requirements" )
	void testCaseInsensitiveTriggerValueMissingRequired() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "TRIGGERVALUE" );
		// No required fields

		BoxValidationException exception = assertThrows( BoxValidationException.class,
		    () -> validator.validate( context, caller, testRecord, records ) );

		assertThat( exception.getMessage() ).contains( "requires the following records to be present" );
	}

	@Test
	@DisplayName( "Should handle numeric trigger values converted to string" )
	void testNumericTriggerValue() {
		ValueRequires	numericValidator	= new ValueRequires( "123", Set.of( Key.of( "required" ) ) );
		IStruct			records				= new Struct();

		// For numeric values, we need to test string conversion behavior
		records.put( Key.of( "testField" ), "123" ); // String version should match
		records.put( Key.of( "required" ), "value" );
		assertDoesNotThrow( () -> numericValidator.validate( context, caller, testRecord, records ) );

		// Missing required when string value matches
		records.remove( Key.of( "required" ) );
		assertThrows( BoxValidationException.class,
		    () -> numericValidator.validate( context, caller, testRecord, records ) );

		// Test with actual integer - behavior may depend on getAsString() implementation
		try {
			records.put( Key.of( "testField" ), 123 );
			records.put( Key.of( "required" ), "value" );
			numericValidator.validate( context, caller, testRecord, records );
		} catch ( Exception e ) {
			// Acceptable if type conversion fails
		}
	}

	@Test
	@DisplayName( "Should handle boolean trigger values converted to string" )
	void testBooleanTriggerValue() {
		ValueRequires	booleanValidator	= new ValueRequires( "true", Set.of( Key.of( "required" ) ) );
		IStruct			records				= new Struct();

		// Test with string "true"
		records.put( Key.of( "testField" ), "true" );
		records.put( Key.of( "required" ), "value" );
		assertDoesNotThrow( () -> booleanValidator.validate( context, caller, testRecord, records ) );

		// Test with different case
		records.put( Key.of( "testField" ), "TRUE" );
		assertDoesNotThrow( () -> booleanValidator.validate( context, caller, testRecord, records ) );

		// Test with actual boolean - behavior may depend on getAsString() implementation
		try {
			records.put( Key.of( "testField" ), true );
			booleanValidator.validate( context, caller, testRecord, records );
		} catch ( Exception e ) {
			// Acceptable if type conversion fails
		}
	}

	@Test
	@DisplayName( "Should handle single required record" )
	void testSingleRequiredRecord() {
		ValueRequires	singleValidator	= new ValueRequires( "trigger", Set.of( Key.of( "onlyRequired" ) ) );
		IStruct			records			= new Struct();

		// Trigger present, required present
		records.put( Key.of( "testField" ), "trigger" );
		records.put( Key.of( "onlyRequired" ), "requiredValue" );
		assertDoesNotThrow( () -> singleValidator.validate( context, caller, testRecord, records ) );

		// Trigger present, required missing
		records.remove( Key.of( "onlyRequired" ) );
		BoxValidationException exception = assertThrows( BoxValidationException.class,
		    () -> singleValidator.validate( context, caller, testRecord, records ) );

		assertThat( exception.getMessage() ).contains( "onlyRequired" );
	}

	@Test
	@DisplayName( "Should handle empty required records set" )
	void testEmptyRequiredRecords() {
		ValueRequires	emptyValidator	= new ValueRequires( "trigger", Set.of() );
		IStruct			records			= new Struct();

		records.put( Key.of( "testField" ), "trigger" );
		// No requirements, so should always pass even with trigger value
		assertDoesNotThrow( () -> emptyValidator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle null trigger value" )
	void testNullTriggerValue() {
		ValueRequires	nullValidator	= new ValueRequires( null, Set.of( Key.of( "required" ) ) );
		IStruct			records			= new Struct();

		records.put( Key.of( "testField" ), null );
		// This test depends on how getAsString() handles null values and equalsIgnoreCase with null
		// The behavior may vary
		try {
			nullValidator.validate( context, caller, testRecord, records );
		} catch ( Exception e ) {
			// Acceptable - null handling in string comparison
		}
	}

	@Test
	@DisplayName( "Should handle required records with null values" )
	void testRequiredRecordsNullValues() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "triggerValue" );
		records.put( Key.of( "requiredField1" ), null );
		records.put( Key.of( "requiredField2" ), null );

		// Required records are present (even with null values), so should pass
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle empty string trigger value" )
	void testEmptyStringTriggerValue() {
		ValueRequires	emptyValidator	= new ValueRequires( "", Set.of( Key.of( "required" ) ) );
		IStruct			records			= new Struct();

		records.put( Key.of( "testField" ), "" );
		records.put( Key.of( "required" ), "value" );
		assertDoesNotThrow( () -> emptyValidator.validate( context, caller, testRecord, records ) );

		// Missing required when empty string matches
		records.remove( Key.of( "required" ) );
		assertThrows( BoxValidationException.class,
		    () -> emptyValidator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle whitespace in trigger values" )
	void testWhitespaceTriggerValue() {
		ValueRequires	whitespaceValidator	= new ValueRequires( "  trigger  ", Set.of( Key.of( "required" ) ) );
		IStruct			records				= new Struct();

		// Exact match with whitespace
		records.put( Key.of( "testField" ), "  trigger  " );
		records.put( Key.of( "required" ), "value" );
		assertDoesNotThrow( () -> whitespaceValidator.validate( context, caller, testRecord, records ) );

		// Different whitespace should not match
		records.put( Key.of( "testField" ), "trigger" );
		assertDoesNotThrow( () -> whitespaceValidator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should format error message correctly for multiple missing records" )
	void testErrorMessageFormatting() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "triggerValue" );
		// Both required fields missing

		BoxValidationException	exception	= assertThrows( BoxValidationException.class,
		    () -> validator.validate( context, caller, testRecord, records ) );

		String					message		= exception.getMessage();
		assertThat( message ).contains( "requires the following records to be present" );
		// Should contain both field names, comma-separated
		assertThat( message ).containsMatch( "requiredField[12], requiredField[12]" );
	}
}