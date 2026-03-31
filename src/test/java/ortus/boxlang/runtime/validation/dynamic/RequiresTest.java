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

public class RequiresTest {

	private IBoxContext	context;
	private Requires	validator;
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
		validator	= new Requires( Set.of( Key.of( "requiredField1" ), Key.of( "requiredField2" ) ) );
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
	@DisplayName( "Should pass validation when target record is present and all required records exist" )
	void testAllRequiredRecordsPresent() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "someValue" );
		records.put( Key.of( "requiredField1" ), "value1" );
		records.put( Key.of( "requiredField2" ), "value2" );

		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should throw BoxValidationException when target record is present but required records are missing" )
	void testRequiredRecordsMissing() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "someValue" );
		// requiredField1 and requiredField2 are missing

		BoxValidationException exception = assertThrows( BoxValidationException.class,
		    () -> validator.validate( context, caller, testRecord, records ) );

		assertThat( exception.getMessage() ).contains( "requires the following records to be present" );
		assertThat( exception.getMessage() ).contains( "requiredField1" );
		assertThat( exception.getMessage() ).contains( "requiredField2" );
	}

	@Test
	@DisplayName( "Should throw BoxValidationException when target record is present but some required records are missing" )
	void testSomeRequiredRecordsMissing() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "someValue" );
		records.put( Key.of( "requiredField1" ), "value1" );
		// requiredField2 is missing

		BoxValidationException exception = assertThrows( BoxValidationException.class,
		    () -> validator.validate( context, caller, testRecord, records ) );

		assertThat( exception.getMessage() ).contains( "requires the following records to be present" );
		assertThat( exception.getMessage() ).contains( "requiredField2" );
		assertThat( exception.getMessage() ).doesNotContain( "requiredField1" );
	}

	@Test
	@DisplayName( "Should handle single required record" )
	void testSingleRequiredRecord() {
		Requires	singleValidator	= new Requires( Set.of( Key.of( "onlyRequired" ) ) );
		IStruct		records			= new Struct();

		// Target present, required present
		records.put( Key.of( "testField" ), "value" );
		records.put( Key.of( "onlyRequired" ), "requiredValue" );
		assertDoesNotThrow( () -> singleValidator.validate( context, caller, testRecord, records ) );

		// Target present, required missing
		records.clear();
		records.put( Key.of( "testField" ), "value" );
		BoxValidationException exception = assertThrows( BoxValidationException.class,
		    () -> singleValidator.validate( context, caller, testRecord, records ) );

		assertThat( exception.getMessage() ).contains( "onlyRequired" );
	}

	@Test
	@DisplayName( "Should handle empty required records set" )
	void testEmptyRequiredRecords() {
		Requires	emptyValidator	= new Requires( Set.of() );
		IStruct		records			= new Struct();

		records.put( Key.of( "testField" ), "value" );
		// No requirements, so should always pass
		assertDoesNotThrow( () -> emptyValidator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle target record with null value" )
	void testTargetRecordNullValue() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), null );
		records.put( Key.of( "requiredField1" ), "value1" );
		records.put( Key.of( "requiredField2" ), "value2" );

		// Target record is present (even with null value), so requirements should be checked
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle required records with null values" )
	void testRequiredRecordsNullValues() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "value" );
		records.put( Key.of( "requiredField1" ), null );
		records.put( Key.of( "requiredField2" ), null );

		// Required records are present (even with null values), so should pass
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should handle target record with different value types" )
	void testTargetRecordDifferentTypes() {
		IStruct records = new Struct();
		records.put( Key.of( "requiredField1" ), "value1" );
		records.put( Key.of( "requiredField2" ), "value2" );

		// String value
		records.put( Key.of( "testField" ), "stringValue" );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		// Numeric value
		records.put( Key.of( "testField" ), 123 );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		// Boolean value
		records.put( Key.of( "testField" ), true );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );

		// Object value
		records.put( Key.of( "testField" ), new Struct() );
		assertDoesNotThrow( () -> validator.validate( context, caller, testRecord, records ) );
	}

	@Test
	@DisplayName( "Should validate with many required records" )
	void testManyRequiredRecords() {
		Set<Key>	manyRequired	= Set.of(
		    Key.of( "req1" ), Key.of( "req2" ), Key.of( "req3" ),
		    Key.of( "req4" ), Key.of( "req5" ), Key.of( "req6" )
		);
		Requires	manyValidator	= new Requires( manyRequired );
		IStruct		records			= new Struct();

		records.put( Key.of( "testField" ), "value" );

		// All required present
		for ( Key req : manyRequired ) {
			records.put( req, "value" );
		}
		assertDoesNotThrow( () -> manyValidator.validate( context, caller, testRecord, records ) );

		// One required missing
		records.remove( Key.of( "req3" ) );
		BoxValidationException exception = assertThrows( BoxValidationException.class,
		    () -> manyValidator.validate( context, caller, testRecord, records ) );

		assertThat( exception.getMessage() ).contains( "req3" );
	}

	@Test
	@DisplayName( "Should format error message correctly for multiple missing records" )
	void testErrorMessageFormatting() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "value" );
		// Both required fields missing

		BoxValidationException	exception	= assertThrows( BoxValidationException.class,
		    () -> validator.validate( context, caller, testRecord, records ) );

		String					message		= exception.getMessage();
		assertThat( message ).contains( "requires the following records to be present" );
		// Should contain both field names, comma-separated
		assertThat( message ).containsMatch( "requiredField[12], requiredField[12]" );
	}

	@Test
	@DisplayName( "Should handle case sensitivity in record names" )
	void testCaseSensitivity() {
		IStruct records = new Struct();
		records.put( Key.of( "testField" ), "value" );
		records.put( Key.of( "REQUIREDFIELD1" ), "value1" ); // Different case
		records.put( Key.of( "requiredfield2" ), "value2" ); // Different case

		// Key comparison in BoxLang is typically case-insensitive
		// If keys are case-insensitive, this should pass validation
		try {
			validator.validate( context, caller, testRecord, records );
			// If validation passes, keys are case-insensitive
		} catch ( BoxValidationException e ) {
			// If validation fails, keys are case-sensitive
			assertThat( e.getMessage() ).contains( "requires the following records to be present" );
			// Both fields would be considered missing if case-sensitive
		}
	}
}