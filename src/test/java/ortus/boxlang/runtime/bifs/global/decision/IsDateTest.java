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

package ortus.boxlang.runtime.bifs.global.decision;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class IsDateTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {

	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It detects date parseable values" )
	@Test
	public void testTrueConditions() {
		instance.executeSource(
		    """
		    aDateStringWithDashes  = isDate( "2023-12-21" );
		    aDateStringWithPeriods = isDate( "2024.01.01" );
		    earlyDate              = isDate( "1100-12-21" );
		    leapDay                = isDate( "2024-02-29" );

		    // FYI: ACF 23 returns false, Lucee returns true.
		    anISO8601String        = isDate( '2023-12-21T14:22:32Z' );
		      """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "aDateStringWithDashes" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "aDateStringWithPeriods" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "earlyDate" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "leapDay" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "anISO8601String" ) ) ).isTrue();
	}

	@DisplayName( "It detects date objects returned from date functions" )
	@Test
	public void testDateFunctionCalls() {
		instance.executeSource(
		    """
		    aNowCall               = isDate( now() );
		    aCreateDateTimeCall = isDateObject( createDateTime( 2024, 01, 22, 7, 15, 1, 999 ) );
		      """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "aNowCall" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "aCreateDateTimeCall" ) ) ).isTrue();
	}

	@DisplayName( "It tests for numeric dates" )
	@Test
	public void testNumericDateFunctionCalls() {
		instance.executeSource(
		    """
		       isDateNumericDate               = isNumericDate( now() );
		       isStringNumericDate             = isNumericDate( "blah" );
		    isNegativeNumberNumericDate     = isNumericDate( -1 );
		    isPositiveNumberNumericDate     = isNumericDate( 1 );
		    isDecimalNumericDate			= isNumericDate( 1111111111.1 );
		         """,
		    context );
		assertFalse( variables.getAsBoolean( Key.of( "isDateNumericDate" ) ) );
		assertFalse( variables.getAsBoolean( Key.of( "isStringNumericDate" ) ) );
		assertTrue( variables.getAsBoolean( Key.of( "isNegativeNumberNumericDate" ) ) );
		assertTrue( variables.getAsBoolean( Key.of( "isPositiveNumberNumericDate" ) ) );
		assertTrue( variables.getAsBoolean( Key.of( "isDecimalNumericDate" ) ) );
	}

	@DisplayName( "It returns false for non-date values" )
	@Test
	public void testFalseConditions() {
		instance.executeSource(
		    """
		    aString = isDate( "abc" );
		    aNumericString = isDate( "2023" );
		    anInteger = isDate( 2024 );
		    aFloat = isDate( 2024.01 );
		    gibberishAfterValidDate = isDate( "2023-12-21xyz" );

		    // FYI: ACF 23 returns false, Lucee returns true.
		    aTimespan = isDate( createTimespan( 0, 24, 0, 0 ) );
		      """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "aString" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "aNumericString" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "anInteger" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "aFloat" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "gibberishAfterValidDate" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "aTimespan" ) ) ).isFalse();
	}

	@Disabled( "Not yet implemented." )
	@DisplayName( "It returns false for date-like values which are not valid dates" )
	@Test
	public void testInvalidDates() {
		instance.executeSource(
		    """
		    invalidMonth = isDate( "2023-31-31" );
		    invalidDayNumber = isDate( "2023-12-2100" );
		    invalidLeapDay = isDate( "2023-02-30" );
		        """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "invalidLeapDay" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "invalidMonth" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "invalidDayNumber" ) ) ).isFalse();
	}

	/**
	 * Localized Formats tests
	 */
	@DisplayName( "It tests the BIF IsDate with a full ISO including offset and locale argument" )
	@Test
	public void testIsDateFullISOLocale() {
		instance.executeSource(
		    """
		    result = IsDate( date="2024-01-14T00:00:01.0001Z", locale="en-US" );
		    """,
		    context );
		assertTrue( variables.getAsBoolean( result ) );
	}

	@DisplayName( "It tests the BIF IsDate using a localized russian format" )
	@Test
	public void testIsDateRussian() {
		instance.executeSource(
		    """
		    result = IsDate( date="14.01.2024", locale="ru_RU" );
		    """,
		    context );
		assertTrue( variables.getAsBoolean( result ) );
	}

	@DisplayName( "It tests the BIF IsDate using a localized, Spanish long-form format" )
	@Test
	public void testIsDateSpain() {

		instance.executeSource(
		    """
		    result = IsDate( date="14 de enero de 2024", locale="es-ES" );
		    """,
		    context );
		assertTrue( variables.getAsBoolean( result ) );
	}

	@DisplayName( "It tests the BIF IsDate using traditional chinese format" )
	@Test
	public void testIsDateChinese() {
		instance.executeSource(
		    """
		    result = IsDate( date="2024年1月14日", locale="zh-CN" );
		    """,
		    context );
		assertTrue( variables.getAsBoolean( result ) );
	}

	@DisplayName( "It tests the BIF IsDate returns false with an invalid date" )
	@Test
	public void testIsDateFalseChinese() {
		instance.executeSource(
		    """
		    result = IsDate( date="12345", locale="zh-CN" );
		    """,
		    context );
		assertFalse( variables.getAsBoolean( result ) );
	}

	@DisplayName( "It tests the BIF IsDate returns false if the date is from another locale" )
	@Test
	public void testIsDateFalseWrongLocale() {
		instance.executeSource(
		    """
		    result = IsDate( date="2024年1月14日", locale="en-US" );
		    """,
		    context );
		assertFalse( variables.getAsBoolean( result ) );
	}

	@DisplayName( "It tests the BIF IsDate will return false an invalid timezone" )
	@Test
	public void testIsDateTimezoneError() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        result = IsDate( date="2024-01-14", locale="en-US", timezone="Blah" );
		        """,
		        context )
		);
	}

}
