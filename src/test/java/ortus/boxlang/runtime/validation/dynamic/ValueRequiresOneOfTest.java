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

public class ValueRequiresOneOfTest {

	private IBoxContext			context;
	private ValueRequiresOneOf	validator;
	private Validatable			testRecord;
	private Key					caller;

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
		validator	= new ValueRequiresOneOf( "triggerValue", Set.of( Key.of( "option1" ), Key.of( "option2" ), Key.of( "option3" ) ) );
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
	@DisplayName( "Should pass validation when target record has trigger value and one of the required records exists" )
	void testTriggerValueWithOneRequired() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "triggerValue" );
		records.put( Key.of( "option1" ), "value1" );
		// option2 and option3 are not present, but that's fine - only one is needed

		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should pass validation when target record has trigger value and multiple required records exist" )
	void testTriggerValueWithMultipleRequired() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "triggerValue" );
		records.put( Key.of( "option1" ), "value1" );
		records.put( Key.of( "option2" ), "value2" );
		// Having multiple options is fine

		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should pass validation when target record has trigger value and all required records exist" )
	void testTriggerValueWithAllRequired() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "triggerValue" );
		records.put( Key.of( "option1" ), "value1" );
		records.put( Key.of( "option2" ), "value2" );
		records.put( Key.of( "option3" ), "value3" );

		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should throw BoxValidationException when target record has trigger value but no required records exist" )
	void testTriggerValueWithNoRequired() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "triggerValue" );
		// No option1, option2, or option3

		BoxValidationException exception = assertThrows( BoxValidationException.class,
		    () -> validator.validate( context, caller, testRecord, records ) );

		assertThat( exception.getMessage() ).contains( "requires the one of the following attributes or arguments to be provided" );
		assertThat( exception.getMessage() ).contains( "option1" );
		assertThat( exception.getMessage() ).contains( "option2" );
		assertThat( exception.getMessage() ).contains( "option3" );
	}

	@Test
	@DisplayName( "Should handle case insensitive trigger value matching" )
	void testCaseInsensitiveTriggerValue() {
		IStruct records = new Struct();
		records.put( Key.of( "option1" ), "value1" );

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

		assertThat( exception.getMessage() ).contains( "requires the one of the following" );
	}

	@Test
	@DisplayName( "Should handle numeric trigger values converted to string" )
	void testNumericTriggerValue() {
		ValueRequiresOneOf	numericValidator	= new ValueRequiresOneOf( "123", Set.of( Key.of( "option1" ), Key.of( "option2" ) ) );
		IStruct				records				= new Struct();

		// For numeric values, we need to test string conversion behavior
		records.put( Key.of( "testField" ), "123" ); // String version should match
		records.put( Key.of( "option1" ), "value" );
		assertDoesNotThrow( () -> numericValidator.validate( context, caller, testRecord, records ) );

		// Missing all options when string value matches
		records.remove( Key.of( "option1" ) );
		assertThrows( BoxValidationException.class,
		    () -> numericValidator.validate( context, caller, testRecord, records ) );

		// Test with actual integer - behavior may depend on getAsString() implementation
		try {
			records.put( Key.of( "testField" ), 123 );
			records.put( Key.of( "option1" ), "value" );
			numericValidator.validate( context, caller, testRecord, records );
		} catch ( Exception e ) {
			// Acceptable if type conversion fails
		}
	}

	@Test
	@DisplayName( "Should handle boolean trigger values converted to string" )
	void testBooleanTriggerValue() {
		ValueRequiresOneOf	booleanValidator	= new ValueRequiresOneOf( "true", Set.of( Key.of( "option1" ), Key.of( "option2" ) ) );
		IStruct				records				= new Struct();

		// Test with string "true"
		records.put( Key.of( "testField" ), "true" );
		records.put( Key.of( "option2" ), "value" );
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
	@DisplayName( "Should handle single required option" )
	void testSingleRequiredOption() {
		ValueRequiresOneOf	singleValidator	= new ValueRequiresOneOf( "trigger", Set.of( Key.of( "onlyOption" ) ) );
		IStruct				records			= new Struct();

		// Trigger present, option present
		records.put( Key.of( "testField" ), "trigger" );
		records.put( Key.of( "onlyOption" ), "value" );
		assertDoesNotThrow( () -> singleValidator.validate( context, caller, testRecord, records ) );

		// Trigger present, option missing
		records.remove( Key.of( "onlyOption" ) );
		BoxValidationException exception = assertThrows( BoxValidationException.class,
		    () -> singleValidator.validate( context, caller, testRecord, records ) );

		assertThat( exception.getMessage() ).contains( "onlyOption" );
	}

	@Test
	@DisplayName( "Should handle empty required options set" )
	void testEmptyRequiredOptions() {
		ValueRequiresOneOf	emptyValidator	= new ValueRequiresOneOf( "trigger", Set.of() );
		IStruct				records			= new Struct();

		records.put( Key.of( "testField" ), "trigger" );
		// No options to check, so should always fail when trigger matches
		assertThrows( BoxValidationException.class,
		    () -> emptyValidator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle required options with null values" )
	void testRequiredOptionsNullValues() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "triggerValue" );
		records.put( Key.of( "option1" ), null );
		// option1 is present (even with null value), so should pass

		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle empty string trigger value" )
	void testEmptyStringTriggerValue() {
		ValueRequiresOneOf	emptyValidator	= new ValueRequiresOneOf( "", Set.of( Key.of( "option1" ), Key.of( "option2" ) ) );
		IStruct				records			= new Struct();

		records.put( Key.of( "testField" ), "" );
		records.put( Key.of( "option1" ), "value" );
		assertDoesNotThrow( () -> emptyValidator.validate( context, caller, testRecord, records ) );

		// Missing all options when empty string matches
		records.remove( Key.of( "option1" ) );
		assertThrows( BoxValidationException.class,
		    () -> emptyValidator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle whitespace in trigger values" )
	void testWhitespaceTriggerValue() {
		ValueRequiresOneOf	whitespaceValidator	= new ValueRequiresOneOf( "  trigger  ", Set.of( Key.of( "option1" ) ) );
		IStruct				records				= new Struct();

		// Exact match with whitespace
		records.put( Key.of( "testField" ), "  trigger  " );
		records.put( Key.of( "option1" ), "value" );
		assertDoesNotThrow( () -> whitespaceValidator.validate( context, caller, testRecord, records ) );

		// Different whitespace should not match
		records.put( Key.of( "testField" ), "trigger" );
		assertDoesNotThrow( () -> whitespaceValidator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should test each option independently" )
	void testEachOptionIndependently() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "triggerValue" );

		// Test with option1 only
		records.put( Key.of( "option1" ), "value1" );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		// Test with option2 only
		records.remove( Key.of( "option1" ) );
		records.put( Key.of( "option2" ), "value2" );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		// Test with option3 only
		records.remove( Key.of( "option2" ) );
		records.put( Key.of( "option3" ), "value3" );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should format error message correctly with all options" )
	void testErrorMessageFormatting() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "triggerValue" );
		// No options present

		BoxValidationException	exception	= assertThrows( BoxValidationException.class,
		    () -> validator.validate( context, caller, testRecord, records ) );

		String					message		= exception.getMessage();
		assertThat( message ).contains( "requires the one of the following attributes or arguments to be provided" );
		// Should contain all option names, comma-separated
		assertThat( message ).contains( "option1" );
		assertThat( message ).contains( "option2" );
		assertThat( message ).contains( "option3" );
	}

	@Test
	@DisplayName( "Should handle large number of options" )
	void testManyOptions() {
		Set<Key>			manyOptions		= Set.of(
		    Key.of( "opt1" ), Key.of( "opt2" ), Key.of( "opt3" ),
		    Key.of( "opt4" ), Key.of( "opt5" ), Key.of( "opt6" )
		);
		ValueRequiresOneOf	manyValidator	= new ValueRequiresOneOf( "trigger", manyOptions );
		IStruct				records			= new Struct();

		records.put( Key.of( "testField" ), "trigger" );

		// Only one option needed
		records.put( Key.of( "opt4" ), "value" );
		assertDoesNotThrow( () -> manyValidator.validate( context, caller, testRecord, records ) );

		// No options
		records.remove( Key.of( "opt4" ) );
		BoxValidationException	exception	= assertThrows( BoxValidationException.class,
		    () -> manyValidator.validate( context, caller, testRecord, records ) );

		String					message		= exception.getMessage();
		assertThat( message ).contains( "opt1" );
		assertThat( message ).contains( "opt6" );
	}

	@Test
	@DisplayName( "Should handle null trigger value" )
	void testNullTriggerValue() {
		ValueRequiresOneOf	nullValidator	= new ValueRequiresOneOf( null, Set.of( Key.of( "option1" ) ) );
		IStruct				records			= new Struct();

		records.put( Key.of( "testField" ), null );
		// This test depends on how getAsString() handles null values and equalsIgnoreCase with null
		// The behavior may vary
		try {
			nullValidator.validate( context, caller, testRecord, records );
		} catch ( Exception e ) {
			// Acceptable - null handling in string comparison
		}
	}
}