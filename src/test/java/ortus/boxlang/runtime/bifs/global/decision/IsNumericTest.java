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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.DecimalFormat;
import java.util.Locale;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.util.LocalizationUtil;

public class IsNumericTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;

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

	@DisplayName( "It detects numeric values" )
	@Test
	public void testNumerics() {
		instance.executeSource(
		    """
		    int         = isnumeric( 123 );
		    stringInt   = isnumeric( "123" );
		    stringFloat = isnumeric( "123.4" );
		    """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "int" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "stringInt" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "stringFloat" ) ) ).isTrue();
	}

	@DisplayName( "It returns false for non-numeric values, including non-string values like a struct" )
	@Test
	public void testSimpleNumerics() {
		instance.executeSource(
		    """
		    int         = isnumeric( "abc83" );
		    hexadecimal = isnumeric( "3FA5" );
		    badFloat    = isnumeric( "123.xx" );
		    struct      = isnumeric( {} );
		    """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "int" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "hexadecimal" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "badFloat" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "struct" ) ) ).isFalse();
	}

	@DisplayName( "It tests the BIF IsNumeric with locale arguments" )
	@Test
	public void testWithLocale() {
		assertTrue( ( Boolean ) instance.executeStatement( "isNumeric( '1.50', 'en_US' )" ) );
		assertTrue( ( Boolean ) instance.executeStatement( "isNumeric( '1,50', 'de_AT' )" ) );
		assertFalse( ( Boolean ) instance.executeStatement( "isNumeric( 'blah' )" ) );
		// Test currencies which may contain symbol separators
		java.text.NumberFormat formatter = DecimalFormat.getNumberInstance( LocalizationUtil.buildLocale( "ar", "JO" ) );
		assertTrue( ( Boolean ) instance.executeStatement( "isNumeric( '" + formatter.format( 1000.51 ) + "', 'ar_JO' )" ) );
		formatter = DecimalFormat.getNumberInstance( Locale.CHINA );
		assertTrue( ( Boolean ) instance.executeStatement( "isNumeric( '1.50', 'China' )" ) );
		assertTrue( ( Boolean ) instance.executeStatement( "isNumeric( '" + formatter.format( 1000.51 ) + "', 'China' )" ) );
	}

	@DisplayName( "It tests that a space thousands separator is not valid in UK locale" )
	@Test
	public void testBifUKSeparator() {
		assertFalse( ( Boolean ) instance.executeStatement( "IsNumeric( '999#char(160)#999', 'en_UK' )" ) );
	}

	@DisplayName( "It tests a date" )
	@Test
	public void testADate() {
		instance.executeSource(
		    """
		    	myDate = createObject("java", "java.sql.Date").valueOf("2011-03-24")
		    	result = isNumeric(myDate)
		    """,
		    context );
		assertFalse( variables.getAsBoolean( Key.of( "result" ) ) );
	}

}
