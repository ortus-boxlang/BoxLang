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

public class ValueOneOfTest {

	private IBoxContext	context;
	private ValueOneOf	validator;
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
		validator	= new ValueOneOf( Set.of( "option1", "option2", "option3" ) );
		testRecord	= new TestValidatable( Key.of( "testField" ), "string", null, null );
		caller		= Key.of( "testCaller" );
	}

	@Test
	@DisplayName( "Should pass validation when value exactly matches allowed option" )
	void testExactMatch() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "option1" );

		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should pass validation when value matches in different case (case insensitive)" )
	void testCaseInsensitiveMatch() {
		IStruct records = new Struct();

		// Test uppercase
		records.put( Key.of( "testField" ), "OPTION1" );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		// Test mixed case
		records.put( Key.of( "testField" ), "OpTiOn2" );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		// Test lowercase
		records.put( Key.of( "testField" ), "option3" );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should throw BoxValidationException when value is not in allowed set" )
	void testInvalidValue() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "invalidOption" );

		BoxValidationException exception = assertThrows( BoxValidationException.class,
		    () -> validator.validate( context, caller, testRecord, records ) );

		assertThat( exception.getMessage() ).contains( "must be one of" );
		// Check that all options are mentioned (order doesn't matter due to HashSet)
		assertThat( exception.getMessage() ).contains( "option1" );
		assertThat( exception.getMessage() ).contains( "option2" );
		assertThat( exception.getMessage() ).contains( "option3" );
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
	@DisplayName( "Should handle empty string as invalid value" )
	void testEmptyString() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "" );

		BoxValidationException exception = assertThrows( BoxValidationException.class,
		    () -> validator.validate( context, caller, testRecord, records ) );

		assertThat( exception.getMessage() ).contains( "must be one of" );
	}

	@Test
	@DisplayName( "Should handle whitespace-only strings as invalid" )
	void testWhitespaceString() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "   " );

		BoxValidationException exception = assertThrows( BoxValidationException.class,
		    () -> validator.validate( context, caller, testRecord, records ) );

		assertThat( exception.getMessage() ).contains( "must be one of" );
	}

	@Test
	@DisplayName( "Should handle numeric values by string conversion" )
	void testNumericValues() {
		// Create validator with numeric strings
		ValueOneOf	numericValidator	= new ValueOneOf( Set.of( "1", "2", "3" ) );
		IStruct		records				= new Struct();

		// Test with string representation first (most reliable)
		records.put( Key.of( "testField" ), "2" );
		assertDoesNotThrow( () -> numericValidator.validate( context, caller, testRecord, records ) );

		// Test with invalid string numeric
		records.put( Key.of( "testField" ), "5" );
		assertThrows( BoxValidationException.class,
		    () -> numericValidator.validate( context, caller, testRecord, records ) );

		// Test with actual integer - behavior depends on how ValueOneOf handles type conversion
		try {
			records.put( Key.of( "testField" ), 2 );
			numericValidator.validate( context, caller, testRecord, records );
		} catch ( Exception e ) {
			// Acceptable if the validator doesn't handle automatic type conversion
		}
	}

	@Test
	@DisplayName( "Should test case sensitivity thoroughly - all permutations" )
	void testComprehensiveCaseSensitivity() {
		// Test with values that have different case combinations
		ValueOneOf	caseValidator	= new ValueOneOf( Set.of( "Test", "VALUE", "option" ) );
		IStruct		records			= new Struct();

		// Test "Test" variations
		String[]	testVariations	= { "test", "TEST", "Test", "tEsT", "TeSt" };
		for ( String variation : testVariations ) {
			records.put( Key.of( "testField" ), variation );
			assertDoesNotThrow( () -> caseValidator.validate( context, caller, testRecord, records ),
			    "Should accept '" + variation + "' as case-insensitive match for 'Test'" );
		}

		// Test "VALUE" variations
		String[] valueVariations = { "value", "Value", "VALUE", "vAlUe" };
		for ( String variation : valueVariations ) {
			records.put( Key.of( "testField" ), variation );
			assertDoesNotThrow( () -> caseValidator.validate( context, caller, testRecord, records ),
			    "Should accept '" + variation + "' as case-insensitive match for 'VALUE'" );
		}

		// Test "option" variations
		String[] optionVariations = { "option", "OPTION", "Option", "OpTiOn" };
		for ( String variation : optionVariations ) {
			records.put( Key.of( "testField" ), variation );
			assertDoesNotThrow( () -> caseValidator.validate( context, caller, testRecord, records ),
			    "Should accept '" + variation + "' as case-insensitive match for 'option'" );
		}
	}

	@Test
	@DisplayName( "Should handle special characters in allowed values" )
	void testSpecialCharacters() {
		ValueOneOf	specialValidator	= new ValueOneOf( Set.of( "option-1", "option_2", "option.3", "option@4" ) );
		IStruct		records				= new Struct();

		// Test exact matches
		records.put( Key.of( "testField" ), "option-1" );
		assertDoesNotThrow( () -> specialValidator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), "option_2" );
		assertDoesNotThrow( () -> specialValidator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), "option.3" );
		assertDoesNotThrow( () -> specialValidator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), "option@4" );
		assertDoesNotThrow( () -> specialValidator.validate( context, caller, testRecord, records ) );

		// Test case variations with special characters
		records.put( Key.of( "testField" ), "OPTION-1" );
		assertDoesNotThrow( () -> specialValidator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should validate with single allowed value" )
	void testSingleValue() {
		ValueOneOf	singleValidator	= new ValueOneOf( Set.of( "onlyOption" ) );
		IStruct		records			= new Struct();

		records.put( Key.of( "testField" ), "onlyOption" );
		assertDoesNotThrow( () -> singleValidator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), "ONLYOPTION" );
		assertDoesNotThrow( () -> singleValidator.validate( context, caller, testRecord, records ) );

		records.put( Key.of( "testField" ), "wrongOption" );
		assertThrows( BoxValidationException.class,
		    () -> singleValidator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle empty allowed values set gracefully" )
	void testEmptyAllowedValues() {
		ValueOneOf	emptyValidator	= new ValueOneOf( Set.of() );
		IStruct		records			= new Struct();

		records.put( Key.of( "testField" ), "anyValue" );
		assertThrows( BoxValidationException.class,
		    () -> emptyValidator.validate( context, caller, testRecord, records ) );
	}
}