/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except compliance with the License.
 * You may obtaa copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.config.util;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.DefaultExpression;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.validation.Validator;
import ortus.boxlang.runtime.validation.dynamic.Max;
import ortus.boxlang.runtime.validation.dynamic.Min;
import ortus.boxlang.runtime.validation.dynamic.MinLength;
import ortus.boxlang.runtime.validation.dynamic.ValueOneOf;

@DisplayName( "ConfigUtil Tests" )
class ConfigUtilTest {

	// Key constants
	private static final Key	stringValueKey			= Key.of( "stringValue" );
	private static final Key	emptyStringKey			= Key.of( "emptyString" );
	private static final Key	booleanTrueKey			= Key.of( "booleanTrue" );
	private static final Key	booleanFalseKey			= Key.of( "booleanFalse" );
	private static final Key	booleanStringTrueKey	= Key.of( "booleanStringTrue" );
	private static final Key	booleanStringFalseKey	= Key.of( "booleanStringFalse" );
	private static final Key	booleanStringYesKey		= Key.of( "booleanStringYes" );
	private static final Key	booleanStringNoKey		= Key.of( "booleanStringNo" );
	private static final Key	extraKey				= Key.of( "Extra" );
	private static final Key	intValueKey				= Key.of( "intValue" );
	private static final Key	intNegativeKey			= Key.of( "intNegative" );
	private static final Key	intZeroKey				= Key.of( "intZero" );
	private static final Key	intAsStringKey			= Key.of( "intAsString" );
	private static final Key	intNegativeAsStringKey	= Key.of( "intNegativeAsString" );
	private static final Key	longValueKey			= Key.of( "longValue" );
	private static final Key	longAsStringKey			= Key.of( "longAsString" );
	private static final Key	doubleValueKey			= Key.of( "doubleValue" );
	private static final Key	doubleNegativeKey		= Key.of( "doubleNegative" );
	private static final Key	doubleAsStringKey		= Key.of( "doubleAsString" );
	private static final Key	doubleAsIntStringKey	= Key.of( "doubleAsIntString" );
	private static final Key	arrayValueKey			= Key.of( "arrayValue" );
	private static final Key	structValueKey			= Key.of( "structValue" );
	private static final Key	nullValueKey			= Key.of( "nullValue" );
	private static final Key	nonExistentKeyKey		= Key.of( "nonExistentKey" );
	private static final Key	nestedKey				= Key.of( "nested" );
	private static final Key	countKey				= Key.of( "count" );
	private static final Key	keyKey					= Key.of( "key" );

	static BoxRuntime			runtime;
	IBoxContext					context;
	IStruct						config;

	@BeforeAll
	public static void setUp() {
		runtime = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {
		runtime.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		context	= new ScriptingRequestBoxContext( runtime.getRuntimeContext() );
		config	= Struct.of(
		    // Basic string
		    stringValueKey, "hello world",
		    emptyStringKey, "",

		    // Boolean values
		    booleanTrueKey, true,
		    booleanFalseKey, false,
		    booleanStringTrueKey, "true",
		    booleanStringFalseKey, "false",
		    booleanStringYesKey, "yes",
		    booleanStringNoKey, "no",

		    // Integer values
		    intValueKey, 42,
		    intNegativeKey, -100,
		    intZeroKey, 0,
		    intAsStringKey, "123",
		    intNegativeAsStringKey, "-456",

		    // Long values
		    longValueKey, 9999999999L,
		    longAsStringKey, "9999999999",

		    // Double values
		    doubleValueKey, 3.14159,
		    doubleNegativeKey, -2.5,
		    doubleAsStringKey, "3.14159",
		    doubleAsIntStringKey, "42",

		    // Array values
		    arrayValueKey, Array.of( "one", "two", "three" ),

		    // Struct values
		    structValueKey, Struct.of( nestedKey, "value", countKey, 5, extraKey, "extraValue" ),

		    // Null value
		    nullValueKey, null
		);
	}

	@DisplayName( "getAs canonical - string values" )
	@Test
	void testGetAsCanonicalStringValues() {
		// Basic string
		String result = ConfigUtil.getAs( String.class, "string", stringValueKey, config, false, null, "config struct", context );
		assertThat( result ).isEqualTo( "hello world" );

		// Empty string
		result = ConfigUtil.getAs( String.class, "string", emptyStringKey, config, true, null, "config struct", context );
		assertThat( result ).isEqualTo( "" );

		// Integer as string
		result = ConfigUtil.getAs( String.class, "string", intValueKey, config, false, null, "config struct", context );
		assertThat( result ).isEqualTo( "42" );

		// Boolean as string
		result = ConfigUtil.getAs( String.class, "string", booleanTrueKey, config, true, null, "config struct", context );
		assertThat( result ).isEqualTo( "true" );
	}

	@DisplayName( "getAs canonical - boolean values" )
	@Test
	void testGetAsCanonicalBooleanValues() {
		// Native boolean true
		Boolean result = ConfigUtil.getAs( Boolean.class, "boolean", booleanTrueKey, config, false, null, "config struct", context );
		assertThat( result ).isTrue();

		// Native boolean false
		result = ConfigUtil.getAs( Boolean.class, "boolean", booleanFalseKey, config, true, null, "config struct", context );
		assertThat( result ).isFalse();

		// String "true"
		result = ConfigUtil.getAs( Boolean.class, "boolean", booleanStringTrueKey, config, false, null, "config struct", context );
		assertThat( result ).isTrue();

		// String "false"
		result = ConfigUtil.getAs( Boolean.class, "boolean", booleanStringFalseKey, config, true, null, "config struct", context );
		assertThat( result ).isFalse();

		// String "yes"
		result = ConfigUtil.getAs( Boolean.class, "boolean", booleanStringYesKey, config, false, null, "config struct", context );
		assertThat( result ).isTrue();

		// String "no"
		result = ConfigUtil.getAs( Boolean.class, "boolean", booleanStringNoKey, config, true, null, "config struct", context );
		assertThat( result ).isFalse();
	}

	@DisplayName( "getAs canonical - integer values" )
	@Test
	void testGetAsCanonicalIntegerValues() {
		// Native integer
		Integer result = ConfigUtil.getAs( Integer.class, "integer", intValueKey, config, false, null, "config struct", context );
		assertThat( result ).isEqualTo( 42 );

		// Negative integer
		result = ConfigUtil.getAs( Integer.class, "integer", intNegativeKey, config, true, null, "config struct", context );
		assertThat( result ).isEqualTo( -100 );

		// Zero
		result = ConfigUtil.getAs( Integer.class, "integer", intZeroKey, config, false, null, "config struct", context );
		assertThat( result ).isEqualTo( 0 );

		// Integer as string
		result = ConfigUtil.getAs( Integer.class, "integer", intAsStringKey, config, true, null, "config struct", context );
		assertThat( result ).isEqualTo( 123 );

		// Negative integer as string
		result = ConfigUtil.getAs( Integer.class, "integer", intNegativeAsStringKey, config, false, null, "config struct", context );
		assertThat( result ).isEqualTo( -456 );
	}

	@DisplayName( "getAs canonical - long values" )
	@Test
	void testGetAsCanonicalLongValues() {
		// Native long
		Long result = ConfigUtil.getAs( Long.class, "long", longValueKey, config, false, null, "config struct", context );
		assertThat( result ).isEqualTo( 9999999999L );

		// Long as string
		result = ConfigUtil.getAs( Long.class, "long", longAsStringKey, config, true, null, "config struct", context );
		assertThat( result ).isEqualTo( 9999999999L );
	}

	@DisplayName( "getAs canonical - double values" )
	@Test
	void testGetAsCanonicalDoubleValues() {
		// Native double
		Double result = ConfigUtil.getAs( Double.class, "double", doubleValueKey, config, false, null, "config struct", context );
		assertThat( result ).isWithin( 0.00001 ).of( 3.14159 );

		// Negative double
		result = ConfigUtil.getAs( Double.class, "double", doubleNegativeKey, config, true, null, "config struct", context );
		assertThat( result ).isWithin( 0.00001 ).of( -2.5 );

		// Double as string
		result = ConfigUtil.getAs( Double.class, "double", doubleAsStringKey, config, false, null, "config struct", context );
		assertThat( result ).isWithin( 0.00001 ).of( 3.14159 );

		// Integer string to double
		result = ConfigUtil.getAs( Double.class, "double", doubleAsIntStringKey, config, true, null, "config struct", context );
		assertThat( result ).isWithin( 0.00001 ).of( 42.0 );
	}

	@DisplayName( "getAs canonical - array values" )
	@Test
	void testGetAsCanonicalArrayValues() {
		Array result = ConfigUtil.getAs( Array.class, "array", arrayValueKey, config, false, null, "config struct", context );
		assertThat( result ).hasSize( 3 );
		assertThat( result.get( 0 ) ).isEqualTo( "one" );
		assertThat( result.get( 1 ) ).isEqualTo( "two" );
		assertThat( result.get( 2 ) ).isEqualTo( "three" );

	}

	@DisplayName( "getAs canonical - struct values" )
	@Test
	void testGetAsCanonicalStructValues() {
		IStruct result = ConfigUtil.getAs( IStruct.class, "struct", structValueKey, config, true, null, "config struct", context );
		assertThat( result.get( nestedKey ) ).isEqualTo( "value" );
		assertThat( result.get( countKey ) ).isEqualTo( 5 );

	}

	@DisplayName( "getAs canonical - type coercion edge cases" )
	@Test
	void testGetAsCanonicalTypeCoercion() {
		// Double to integer (truncation)
		Integer intResult = ConfigUtil.getAs( Integer.class, "integerTruncate", doubleValueKey, config, false, null, "config struct", context );
		assertThat( intResult ).isEqualTo( 3 );

		// Integer to double
		Double doubleResult = ConfigUtil.getAs( Double.class, "double", intValueKey, config, true, null, "config struct", context );
		assertThat( doubleResult ).isWithin( 0.00001 ).of( 42.0 );
	}

	@DisplayName( "getAs canonical - null value returns null" )
	@Test
	void testGetAsCanonicalNullValue() {
		// Getting a null value with no default and not required should return null
		Object result = ConfigUtil.getAs( null, "any", nullValueKey, config, false, null, "config struct", context );
		assertThat( result ).isNull();
	}

	@DisplayName( "getAs canonical - default expressions" )
	@Test
	void testGetAsCanonicalDefaultExpressions() {
		// String default
		String stringResult = ConfigUtil.getAs( String.class, "string", nonExistentKeyKey, config, false, ctx -> "default string",
		    "config struct", context );
		assertThat( stringResult ).isEqualTo( "default string" );

		// Integer default
		Integer intResult = ConfigUtil.getAs( Integer.class, "integer", nonExistentKeyKey, config, false, ctx -> 999, "config struct", context );
		assertThat( intResult ).isEqualTo( 999 );

		// Boolean default
		Boolean boolResult = ConfigUtil.getAs( Boolean.class, "boolean", nonExistentKeyKey, config, false, ctx -> true, "config struct",
		    context );
		assertThat( boolResult ).isTrue();

		// Double default
		Double doubleResult = ConfigUtil.getAs( Double.class, "double", nonExistentKeyKey, config, false, ctx -> 3.14, "config struct",
		    context );
		assertThat( doubleResult ).isWithin( 0.00001 ).of( 3.14 );

		// Array default
		Array arrayResult = ConfigUtil.getAs( Array.class, "array", nonExistentKeyKey, config, false, ctx -> Array.of( "default" ),
		    "config struct", context );
		assertThat( arrayResult ).hasSize( 1 );
		assertThat( arrayResult.get( 0 ) ).isEqualTo( "default" );

		// Struct default
		IStruct structResult = ConfigUtil.getAs( IStruct.class, "struct", nonExistentKeyKey, config, false, ctx -> Struct.of( "key", "value" ),
		    "config struct", context );
		assertThat( structResult.get( keyKey ) ).isEqualTo( "value" );

		// Null value with default expression
		String nullWithDefault = ConfigUtil.getAs( String.class, "string", nullValueKey, config, false, ctx -> "fallback", "config struct",
		    context );
		assertThat( nullWithDefault ).isEqualTo( "fallback" );
	}

	// ==================== INFERRED CAST TYPE TESTS ====================

	@DisplayName( "getAs inferred - string values" )
	@Test
	void testGetAsInferredStringValues() {
		// Basic string
		String result = ConfigUtil.getAs( String.class, stringValueKey, config, context );
		assertThat( result ).isEqualTo( "hello world" );

		// Empty string
		result = ConfigUtil.getAs( String.class, emptyStringKey, config, context );
		assertThat( result ).isEqualTo( "" );

		// Integer as string
		result = ConfigUtil.getAs( String.class, intValueKey, config, context );
		assertThat( result ).isEqualTo( "42" );

		// Boolean as string
		result = ConfigUtil.getAs( String.class, booleanTrueKey, config, context );
		assertThat( result ).isEqualTo( "true" );
	}

	@DisplayName( "getAs inferred - boolean values" )
	@Test
	void testGetAsInferredBooleanValues() {
		// Native boolean true
		Boolean result = ConfigUtil.getAs( Boolean.class, booleanTrueKey, config, context );
		assertThat( result ).isTrue();

		// Native boolean false
		result = ConfigUtil.getAs( Boolean.class, booleanFalseKey, config, context );
		assertThat( result ).isFalse();

		// String "true"
		result = ConfigUtil.getAs( Boolean.class, booleanStringTrueKey, config, context );
		assertThat( result ).isTrue();

		// String "false"
		result = ConfigUtil.getAs( Boolean.class, booleanStringFalseKey, config, context );
		assertThat( result ).isFalse();

		// String "yes"
		result = ConfigUtil.getAs( Boolean.class, booleanStringYesKey, config, context );
		assertThat( result ).isTrue();

		// String "no"
		result = ConfigUtil.getAs( Boolean.class, booleanStringNoKey, config, context );
		assertThat( result ).isFalse();
	}

	@DisplayName( "getAs inferred - integer values" )
	@Test
	void testGetAsInferredIntegerValues() {
		// Native integer
		Integer result = ConfigUtil.getAs( Integer.class, intValueKey, config, context );
		assertThat( result ).isEqualTo( 42 );

		// Negative integer
		result = ConfigUtil.getAs( Integer.class, intNegativeKey, config, context );
		assertThat( result ).isEqualTo( -100 );

		// Zero
		result = ConfigUtil.getAs( Integer.class, intZeroKey, config, context );
		assertThat( result ).isEqualTo( 0 );

		// Integer as string
		result = ConfigUtil.getAs( Integer.class, intAsStringKey, config, context );
		assertThat( result ).isEqualTo( 123 );

		// Negative integer as string
		result = ConfigUtil.getAs( Integer.class, intNegativeAsStringKey, config, context );
		assertThat( result ).isEqualTo( -456 );
	}

	@DisplayName( "getAs inferred - long values" )
	@Test
	void testGetAsInferredLongValues() {
		// Native long
		Long result = ConfigUtil.getAs( Long.class, longValueKey, config, context );
		assertThat( result ).isEqualTo( 9999999999L );

		// Long as string
		result = ConfigUtil.getAs( Long.class, longAsStringKey, config, context );
		assertThat( result ).isEqualTo( 9999999999L );
	}

	@DisplayName( "getAs inferred - double values" )
	@Test
	void testGetAsInferredDoubleValues() {
		// Native double
		Double result = ConfigUtil.getAs( Double.class, doubleValueKey, config, context );
		assertThat( result ).isWithin( 0.00001 ).of( 3.14159 );

		// Negative double
		result = ConfigUtil.getAs( Double.class, doubleNegativeKey, config, context );
		assertThat( result ).isWithin( 0.00001 ).of( -2.5 );

		// Double as string
		result = ConfigUtil.getAs( Double.class, doubleAsStringKey, config, context );
		assertThat( result ).isWithin( 0.00001 ).of( 3.14159 );

		// Integer string to double
		result = ConfigUtil.getAs( Double.class, doubleAsIntStringKey, config, context );
		assertThat( result ).isWithin( 0.00001 ).of( 42.0 );
	}

	@DisplayName( "getAs inferred - array values" )
	@Test
	void testGetAsInferredArrayValues() {
		Array result = ConfigUtil.getAs( Array.class, arrayValueKey, config, context );
		assertThat( result ).hasSize( 3 );
		assertThat( result.get( 0 ) ).isEqualTo( "one" );
		assertThat( result.get( 1 ) ).isEqualTo( "two" );
		assertThat( result.get( 2 ) ).isEqualTo( "three" );
	}

	@DisplayName( "getAs inferred - struct values" )
	@Test
	void testGetAsInferredStructValues() {
		IStruct result = ConfigUtil.getAs( IStruct.class, structValueKey, config, context );
		assertThat( result.get( nestedKey ) ).isEqualTo( "value" );
		assertThat( result.get( countKey ) ).isEqualTo( 5 );
	}

	@DisplayName( "getAs inferred - type coercion edge cases" )
	@Test
	void testGetAsInferredTypeCoercion() {
		// Integer to double
		Double doubleResult = ConfigUtil.getAs( Double.class, intValueKey, config, context );
		assertThat( doubleResult ).isWithin( 0.00001 ).of( 42.0 );
	}

	@DisplayName( "getAs - remaining overloads coverage" )
	@Test
	void testGetAsRemainingOverloads() {
		// Overload: getAs(String castType, Key, IStruct, IBoxContext) - returns Object
		Object result1 = ConfigUtil.getAs( "integer", intValueKey, config, context );
		assertThat( result1 ).isEqualTo( 42 );

		// Overload: getAs(String castType, Key, IStruct, boolean required, DefaultExpression, String description, IBoxContext) - returns Object
		Object result2 = ConfigUtil.getAs( "integer", intValueKey, config, true, null, "config struct", context );
		assertThat( result2 ).isEqualTo( 42 );

		// Overload: getAs(Class<T>, Key, IStruct, boolean required, DefaultExpression, String description, IBoxContext) - inferred with full params
		Integer result3 = ConfigUtil.getAs( Integer.class, intValueKey, config, true, null, "config struct", context );
		assertThat( result3 ).isEqualTo( 42 );
	}

	// ==================== CONFIG ITEM TESTS ====================

	@DisplayName( "getAs ConfigItem - string values" )
	@Test
	void testGetAsConfigItemStringValues() {
		// Basic string - not required
		String result = ConfigUtil.getAs( String.class, ConfigItem.of( stringValueKey, false, "string", "config struct" ), config, context );
		assertThat( result ).isEqualTo( "hello world" );

		// Empty string - required
		result = ConfigUtil.getAs( String.class, ConfigItem.of( emptyStringKey, true, "string", "config struct" ), config, context );
		assertThat( result ).isEqualTo( "" );

		// Integer as string - not required
		result = ConfigUtil.getAs( String.class, ConfigItem.of( intValueKey, false, "string", "config struct" ), config, context );
		assertThat( result ).isEqualTo( "42" );

		// Boolean as string - required
		result = ConfigUtil.getAs( String.class, ConfigItem.of( booleanTrueKey, true, "string", "config struct" ), config, context );
		assertThat( result ).isEqualTo( "true" );
	}

	@DisplayName( "getAs ConfigItem - boolean values" )
	@Test
	void testGetAsConfigItemBooleanValues() {
		// Native boolean true - not required
		Boolean result = ConfigUtil.getAs( Boolean.class, ConfigItem.of( booleanTrueKey, false, "boolean", "config struct" ), config, context );
		assertThat( result ).isTrue();

		// Native boolean false - required
		result = ConfigUtil.getAs( Boolean.class, ConfigItem.of( booleanFalseKey, true, "boolean", "config struct" ), config, context );
		assertThat( result ).isFalse();

		// String "true" - not required
		result = ConfigUtil.getAs( Boolean.class, ConfigItem.of( booleanStringTrueKey, false, "boolean", "config struct" ), config, context );
		assertThat( result ).isTrue();

		// String "false" - required
		result = ConfigUtil.getAs( Boolean.class, ConfigItem.of( booleanStringFalseKey, true, "boolean", "config struct" ), config, context );
		assertThat( result ).isFalse();

		// String "yes" - not required
		result = ConfigUtil.getAs( Boolean.class, ConfigItem.of( booleanStringYesKey, false, "boolean", "config struct" ), config, context );
		assertThat( result ).isTrue();

		// String "no" - required
		result = ConfigUtil.getAs( Boolean.class, ConfigItem.of( booleanStringNoKey, true, "boolean", "config struct" ), config, context );
		assertThat( result ).isFalse();
	}

	@DisplayName( "getAs ConfigItem - integer values" )
	@Test
	void testGetAsConfigItemIntegerValues() {
		// Native integer - not required
		Integer result = ConfigUtil.getAs( Integer.class, ConfigItem.of( intValueKey, false, "integer", "config struct" ), config, context );
		assertThat( result ).isEqualTo( 42 );

		// Negative integer - required
		result = ConfigUtil.getAs( Integer.class, ConfigItem.of( intNegativeKey, true, "integer", "config struct" ), config, context );
		assertThat( result ).isEqualTo( -100 );

		// Zero - not required
		result = ConfigUtil.getAs( Integer.class, ConfigItem.of( intZeroKey, false, "integer", "config struct" ), config, context );
		assertThat( result ).isEqualTo( 0 );

		// Integer as string - required
		result = ConfigUtil.getAs( Integer.class, ConfigItem.of( intAsStringKey, true, "integer", "config struct" ), config, context );
		assertThat( result ).isEqualTo( 123 );

		// Negative integer as string - not required
		result = ConfigUtil.getAs( Integer.class, ConfigItem.of( intNegativeAsStringKey, false, "integer", "config struct" ), config, context );
		assertThat( result ).isEqualTo( -456 );
	}

	@DisplayName( "getAs ConfigItem - long values" )
	@Test
	void testGetAsConfigItemLongValues() {
		// Native long - not required
		Long result = ConfigUtil.getAs( Long.class, ConfigItem.of( longValueKey, false, "long", "config struct" ), config, context );
		assertThat( result ).isEqualTo( 9999999999L );

		// Long as string - required
		result = ConfigUtil.getAs( Long.class, ConfigItem.of( longAsStringKey, true, "long", "config struct" ), config, context );
		assertThat( result ).isEqualTo( 9999999999L );
	}

	@DisplayName( "getAs ConfigItem - double values" )
	@Test
	void testGetAsConfigItemDoubleValues() {
		// Native double - not required
		Double result = ConfigUtil.getAs( Double.class, ConfigItem.of( doubleValueKey, false, "double", "config struct" ), config, context );
		assertThat( result ).isWithin( 0.00001 ).of( 3.14159 );

		// Negative double - required
		result = ConfigUtil.getAs( Double.class, ConfigItem.of( doubleNegativeKey, true, "double", "config struct" ), config, context );
		assertThat( result ).isWithin( 0.00001 ).of( -2.5 );

		// Double as string - not required
		result = ConfigUtil.getAs( Double.class, ConfigItem.of( doubleAsStringKey, false, "double", "config struct" ), config, context );
		assertThat( result ).isWithin( 0.00001 ).of( 3.14159 );

		// Integer string to double - required
		result = ConfigUtil.getAs( Double.class, ConfigItem.of( doubleAsIntStringKey, true, "double", "config struct" ), config, context );
		assertThat( result ).isWithin( 0.00001 ).of( 42.0 );
	}

	@DisplayName( "getAs ConfigItem - array values" )
	@Test
	void testGetAsConfigItemArrayValues() {
		// Array - not required
		Array result = ConfigUtil.getAs( Array.class, ConfigItem.of( arrayValueKey, false, "array", "config struct" ), config, context );
		assertThat( result ).hasSize( 3 );
		assertThat( result.get( 0 ) ).isEqualTo( "one" );
		assertThat( result.get( 1 ) ).isEqualTo( "two" );
		assertThat( result.get( 2 ) ).isEqualTo( "three" );
	}

	@DisplayName( "getAs ConfigItem - struct values" )
	@Test
	void testGetAsConfigItemStructValues() {
		// Struct - required
		IStruct result = ConfigUtil.getAs( IStruct.class, ConfigItem.of( structValueKey, true, "struct", "config struct" ), config, context );
		assertThat( result.get( nestedKey ) ).isEqualTo( "value" );
		assertThat( result.get( countKey ) ).isEqualTo( 5 );
	}

	@DisplayName( "getAs ConfigItem - type coercion edge cases" )
	@Test
	void testGetAsConfigItemTypeCoercion() {
		// Double to integer (truncation) - not required
		Integer intResult = ConfigUtil.getAs( Integer.class, ConfigItem.of( doubleValueKey, false, "integerTruncate", "config struct" ), config,
		    context );
		assertThat( intResult ).isEqualTo( 3 );

		// Integer to double - required
		Double doubleResult = ConfigUtil.getAs( Double.class, ConfigItem.of( intValueKey, true, "double", "config struct" ), config, context );
		assertThat( doubleResult ).isWithin( 0.00001 ).of( 42.0 );
	}

	@DisplayName( "getAs ConfigItem - null value returns null" )
	@Test
	void testGetAsConfigItemNullValue() {
		// Getting a null value with no default and not required should return null
		Object result = ConfigUtil.getAs( null, ConfigItem.of( nullValueKey, false, "any", "config struct" ), config, context );
		assertThat( result ).isNull();
	}

	@DisplayName( "getAs ConfigItem - default expressions" )
	@Test
	void testGetAsConfigItemDefaultExpressions() {
		// String default - not required
		String stringResult = ConfigUtil.getAs( String.class,
		    ConfigItem.of( nonExistentKeyKey, false, "string", ctx -> "default string", "config struct" ), config, context );
		assertThat( stringResult ).isEqualTo( "default string" );

		// Integer default - not required
		Integer intResult = ConfigUtil.getAs( Integer.class, ConfigItem.of( nonExistentKeyKey, false, "integer", ctx -> 999, "config struct" ),
		    config, context );
		assertThat( intResult ).isEqualTo( 999 );

		// Boolean default - not required
		Boolean boolResult = ConfigUtil.getAs( Boolean.class, ConfigItem.of( nonExistentKeyKey, false, "boolean", ctx -> true, "config struct" ),
		    config, context );
		assertThat( boolResult ).isTrue();

		// Double default - not required
		Double doubleResult = ConfigUtil.getAs( Double.class, ConfigItem.of( nonExistentKeyKey, false, "double", ctx -> 3.14, "config struct" ),
		    config, context );
		assertThat( doubleResult ).isWithin( 0.00001 ).of( 3.14 );

		// Array default - not required
		Array arrayResult = ConfigUtil.getAs( Array.class,
		    ConfigItem.of( nonExistentKeyKey, false, "array", ctx -> Array.of( "default" ), "config struct" ), config, context );
		assertThat( arrayResult ).hasSize( 1 );
		assertThat( arrayResult.get( 0 ) ).isEqualTo( "default" );

		// Struct default - not required
		IStruct structResult = ConfigUtil.getAs( IStruct.class,
		    ConfigItem.of( nonExistentKeyKey, false, "struct", ctx -> Struct.of( "key", "value" ), "config struct" ), config, context );
		assertThat( structResult.get( keyKey ) ).isEqualTo( "value" );

		// Null value with default expression - not required
		String nullWithDefault = ConfigUtil.getAs( String.class,
		    ConfigItem.of( nullValueKey, false, "string", ctx -> "fallback", "config struct" ), config, context );
		assertThat( nullWithDefault ).isEqualTo( "fallback" );
	}

	// ==================== GET CONFIG SET TESTS ====================

	@DisplayName( "getConfigSet - all factory method overloads" )
	@Test
	void testGetConfigSetAllFactoryOverloads() {
		// Create a set of ConfigItems using every of() factory method overload
		Set<ConfigItem>	configItems	= Set.of(
		    // 1. of(Key name) - name only
		    ConfigItem.of( stringValueKey ),

		    // 2. of(Key name, String type, String description) - name, type, description
		    ConfigItem.of( emptyStringKey, "string", "config struct" ),

		    // 3. of(Key name, String type) - name, type
		    ConfigItem.of( booleanTrueKey, "boolean" ),

		    // 4. of(Key name, boolean required) - name, required
		    ConfigItem.of( booleanFalseKey, true ),

		    // 5. of(Key name, boolean required, String type) - name, required, type
		    ConfigItem.of( intValueKey, false, "integer" ),

		    // 6. of(Key name, boolean required, String type, String description) - name, required, type, description
		    ConfigItem.of( intNegativeKey, true, "integer", "config struct" ),

		    // 7. of(Key name, boolean required, String type, Set<Validator> validators) - with validators (Min, Max)
		    ConfigItem.of( longValueKey, false, "long", Set.<Validator>of( new Min( 0 ), new Max( 10000000000L ) ) ),

		    // 8. of(Key name, boolean required, String type, Set<Validator> validators, String description) - validators + description (Min)
		    ConfigItem.of( doubleValueKey, true, "double", Set.<Validator>of( new Min( 0 ) ), "config struct" ),

		    // 9. of(Key name, boolean required, String type, DefaultExpression defaultExpression) - with default
		    ConfigItem.of( arrayValueKey, false, "array", ( DefaultExpression ) null ),

		    // 10. of(Key name, boolean required, String type, DefaultExpression defaultExpression, String description) - default + description
		    ConfigItem.of( structValueKey, true, "struct", ( DefaultExpression ) null, Set.<Validator>of( Validator.configSet(
		        ConfigItem.of( nestedKey, false, "string", Set.<Validator>of( new MinLength( 1 ) ), "nested struct in config struct" ),
		        ConfigItem.of( countKey, false, "integer", Set.<Validator>of( new Min( 0 ) ), "nested struct in config struct"
		        ) ) ), "config struct" ),

		    // 11. of(Key name, boolean required, String type, DefaultExpression defaultExpression, Set<Validator> validators, String description) - canonical (ValueOneOf)
		    ConfigItem.of( booleanStringYesKey, false, "string", ( DefaultExpression ) null,
		        Set.<Validator>of( new ValueOneOf( Set.of( "yes", "no", "true", "false" ) ) ),
		        "config struct" )
		);

		// Call getConfigSet
		IStruct			result		= ConfigUtil.getConfigSet( configItems, config, context );

		// Assert all keys are present
		assertThat( result.containsKey( stringValueKey ) ).isTrue();
		assertThat( result.containsKey( emptyStringKey ) ).isTrue();
		assertThat( result.containsKey( booleanTrueKey ) ).isTrue();
		assertThat( result.containsKey( booleanFalseKey ) ).isTrue();
		assertThat( result.containsKey( intValueKey ) ).isTrue();
		assertThat( result.containsKey( intNegativeKey ) ).isTrue();
		assertThat( result.containsKey( longValueKey ) ).isTrue();
		assertThat( result.containsKey( doubleValueKey ) ).isTrue();
		assertThat( result.containsKey( arrayValueKey ) ).isTrue();
		assertThat( result.containsKey( structValueKey ) ).isTrue();
		assertThat( result.containsKey( booleanStringYesKey ) ).isTrue();

		// Assert the values are correctly retrieved and cast
		assertThat( result.get( stringValueKey ) ).isEqualTo( "hello world" );
		assertThat( result.get( emptyStringKey ) ).isEqualTo( "" );
		assertThat( result.get( booleanTrueKey ) ).isEqualTo( true );
		assertThat( result.get( booleanFalseKey ) ).isEqualTo( false );
		assertThat( result.get( intValueKey ) ).isEqualTo( 42 );
		assertThat( result.get( intNegativeKey ) ).isEqualTo( -100 );
		assertThat( result.get( longValueKey ) ).isEqualTo( 9999999999L );
		assertThat( ( Double ) result.get( doubleValueKey ) ).isWithin( 0.00001 ).of( 3.14159 );
		assertThat( ( Array ) result.get( arrayValueKey ) ).hasSize( 3 );
		assertThat( ( ( IStruct ) result.get( structValueKey ) ).get( nestedKey ) ).isEqualTo( "value" );
		assertThat( ( ( IStruct ) result.get( structValueKey ) ).get( countKey ) ).isEqualTo( 5 );
		assertThat( ( ( IStruct ) result.get( structValueKey ) ).get( extraKey ) ).isEqualTo( "extraValue" );
		assertThat( result.get( booleanStringYesKey ) ).isEqualTo( "yes" );  // Now returned as string since type="string"

		// Assert the result struct has exactly the expected number of keys
		assertThat( result.size() ).isEqualTo( 22 );
	}

	@DisplayName( "getAs - required null value throws exception" )
	@Test
	void testGetAsRequiredNullValueThrows() {
		ConfigItem ci = ConfigItem.of( nullValueKey, true, "string", "config struct" );
		// A required config item with a null value should throw
		assertThrows( BoxValidationException.class, () -> ConfigUtil.getAs( ci, config, context ) );
	}

	// ==================== VALIDATOR TESTS ====================

	@DisplayName( "getAs ConfigItem - Mvalidator throws on value below minimum" )
	@Test
	void testGetAsConfigItemMinValidatorThrows() {
		// intNegative is -100, Min(0) should throw
		ConfigItem ci = ConfigItem.of( intNegativeKey, false, "integer", Set.<Validator>of( new Min( 0 ) ) );
		assertThrows( BoxValidationException.class, () -> ConfigUtil.getAs( Integer.class, ci, config, context ) );
	}

	@DisplayName( "getAs ConfigItem - Max validator throws on value above maximum" )
	@Test
	void testGetAsConfigItemMaxValidatorThrows() {
		// longValue is 9999999999, Max(1000) should throw
		ConfigItem ci = ConfigItem.of( longValueKey, false, "long", Set.<Validator>of( new Max( 1000 ) ) );
		assertThrows( BoxValidationException.class, () -> ConfigUtil.getAs( Long.class, ci, config, context ) );
	}

	@DisplayName( "getAs ConfigItem - MinLength validator throws on string too short" )
	@Test
	void testGetAsConfigItemMinLengthValidatorThrows() {
		// emptyString is "", MinLength(1) should throw
		ConfigItem ci = ConfigItem.of( emptyStringKey, false, "string", Set.<Validator>of( new MinLength( 1 ) ) );
		assertThrows( BoxValidationException.class, () -> ConfigUtil.getAs( String.class, ci, config, context ) );
	}

	@DisplayName( "getAs ConfigItem - ValueOneOf validator throws on invalid value" )
	@Test
	void testGetAsConfigItemValueOneOfValidatorThrows() {
		// stringValue is "hello world", not allowed set
		ConfigItem ci = ConfigItem.of( stringValueKey, false, "string", Set.<Validator>of( new ValueOneOf( Set.of( "foo", "bar", "baz" ) ) ) );
		assertThrows( BoxValidationException.class, () -> ConfigUtil.getAs( String.class, ci, config, context ) );
	}

	@DisplayName( "getAs ConfigItem - Multiple validators all pass" )
	@Test
	void testGetAsConfigItemMultipleValidatorsPass() {
		// intValue is 42, should pass Min(0), Max(100)
		ConfigItem	ci		= ConfigItem.of( intValueKey, false, "integer", Set.<Validator>of( new Min( 0 ), new Max( 100 ) ) );
		Integer		result	= ConfigUtil.getAs( Integer.class, ci, config, context );
		assertThat( result ).isEqualTo( 42 );
	}

	@DisplayName( "getAs ConfigItem - Multiple validators with one failing throws" )
	@Test
	void testGetAsConfigItemMultipleValidatorsOneFails() {
		// intValue is 42, should pass Min(0) but fail Max(10)
		ConfigItem ci = ConfigItem.of( intValueKey, false, "integer", Set.<Validator>of( new Min( 0 ), new Max( 10 ) ) );
		assertThrows( BoxValidationException.class, () -> ConfigUtil.getAs( Integer.class, ci, config, context ) );
	}

}
