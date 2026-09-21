
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

package ortus.boxlang.runtime.bifs.global.format;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.LocalizationUtil;

public class NumberFormatTest {

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

	@DisplayName( "It tests the BIF NumberFormat with no mask" )
	@Test
	public void testDefault() {
		instance.executeSource(
		    """
		    result = numberFormat( 12345 );
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "12,345" );
	}

	@DisplayName( "It tests the BIF NumberFormat with number format placeholder Masks" )
	@Test
	public void testBif() {

		instance.executeSource(
		    """
		    result = numberFormat( 12345, "9.99");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "12345.00" );
		instance.executeSource(
		    """
		    result = numberFormat( 12345, "_.__");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "12345.00" );
		instance.executeSource(
		    """
		    result = numberFormat( 12345, "9.999");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "12345.000" );
		instance.executeSource(
		    """
		    result = numberFormat( 1, "__,___,___.__" );
		    """,
		    context );
		assertEquals( "1.00", variables.getAsString( result ) );
		instance.executeSource(
		    """
		    result = numberFormat( round( 110.647747663711, 1 ), "_,.0");
		    """,
		    context );
		assertEquals( "110.6", variables.getAsString( result ) );
		instance.executeSource(
		    """
		    result = numberFormat( 123456789,'_$,9.99' );
		    """,
		    context );
		assertEquals( "$123,456,789.00", variables.getAsString( result ) );

		instance.executeSource(
		    """
		    result = numberFormat( 432342, "$,.00" );
		    """,
		    context );
		assertEquals( "$432,342.00", variables.getAsString( result ) );

		instance.executeSource(
		    """
		    result = numberFormat( 1.2, '9999.0' );
		    """,
		    context );
		assertEquals( "1.2", variables.getAsString( result ) );

		instance.executeSource(
		    """
		    result = numberFormat( 0.2, '9999.0' );
		    """,
		    context );
		assertEquals( "0.2", variables.getAsString( result ) );
	}

	@DisplayName( "It tests the BIF NumberFormat with common format masks" )
	@Test
	public void testBifCommonFormat() {
		instance.executeSource(
		    """
		    result = numberFormat( -12345, "()");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "(12345)" );
		instance.executeSource(
		    """
		    result = numberFormat( 12345, "()");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "12345" );
		instance.executeSource(
		    """
		    result = numberFormat( 12345, "_,9");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "12345.000000000" );
		instance.executeSource(
		    """
		    result = numberFormat( 12345, "+");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "+12345" );

		instance.executeSource(
		    """
		    result = numberFormat( -12345, "+");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "-12345" );

		instance.executeSource(
		    """
		    result = numberFormat( 12345, "-");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), " 12345" );

		instance.executeSource(
		    """
		    result = numberFormat( -12345, "-");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "-12345" );

		instance.executeSource(
		    """
		    result = numberFormat( 12345, "$");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "$12,345.00" );

		java.text.NumberFormat formatter = DecimalFormat.getCurrencyInstance( Locale.getDefault() );
		instance.executeSource(
		    """
		    result = numberFormat( 12345, "ls$");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), formatter.format( 12345D ) );
	}

	@DisplayName( "It tests the BIF NumberFormat with justification masks" )
	@Test
	public void testBifJustify() {
		instance.executeSource(
		    """
		    result = numberFormat( 1, "L000");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "001" );
		instance.executeSource(
		    """
		    result = numberFormat( 1, "C000");
		    """,
		    context );
		assertEquals( "001", variables.getAsString( result ) );
		instance.executeSource(
		    """
		    result = numberFormat( 3.21, "C(_^_)");
		    """,
		    context );
		assertEquals( "( 3.21 )", variables.getAsString( result ) );
	}

	@DisplayName( "It tests will throw an error with an unparseable date" )
	@Test
	public void testBifError() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        result = numberFormat( "Blah", "0.00");
		        """,
		        context )
		);
	}

	@DisplayName( "It tests the BIF LSNumberFormat with number format placeholder and locale" )
	@Test
	public void testLocaleBif() {

		instance.executeSource(
		    """
		    result = numberFormat( 12345, "0.00", "German (Austrian)");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "12345,00" );
		instance.executeSource(
		    """
		    result = numberFormat( 12345, "_.__", "German (Austrian)");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "12345,00" );
		instance.executeSource(
		    """
		    result = numberFormat( 12345, "9.999", "German (Austrian)");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "12345,000" );
		String refGrouped = new DecimalFormat( "#,##0.00", LocalizationUtil.localizedDecimalSymbols( LocalizationUtil.buildLocale( "de", "AT" ) ) )
		    .format( 12345D );
		instance.executeSource(
		    """
		    result = numberFormat( 12345, "_,__0.00", "German (Austrian)");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), refGrouped );
	}

	@DisplayName( "It tests the BIF LSNumberFormat localized masks" )
	@Test
	public void testLocaleBifCommonFormat() {
		java.text.NumberFormat formatter = DecimalFormat.getCurrencyInstance( LocalizationUtil.buildLocale( "de", "AT" ) );
		instance.executeSource(
		    """
		    result = numberFormat( 12345, "ls$", "German (Austrian)" );
		    """,
		    context );
		assertEquals( variables.getAsString( result ), formatter.format( 12345D ) );
	}

	@DisplayName( "It tests will throw an error with an unparseable date" )
	@Test
	public void testLocaleBifError() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        result = LSnumberFormat( "Blah", "0.00");
		        """,
		        context )
		);
	}

	@DisplayName( "It tests the BIF LSNumberFormat with default mask and locale" )
	@Test
	public void testLocaleDefault() {
		instance.executeSource(
		    """
		    result = LSnumberFormat( 12345 );
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "12,345" );
	}

	// https://ortussolutions.atlassian.net/browse/BL-899
	@DisplayName( "It tests the BIF LSNumberFormat will not add leading zeroes to int < 10" )
	@Test
	public void testLeadingZeros() {
		instance.executeSource(
		    """
		    result = LSnumberFormat( 1 );
		    """,
		    context );
		assertEquals( "1", variables.getAsString( result ) );
	}

	@DisplayName( "It treats empty strings as zero" )
	@Test
	public void testEmptyStringAsZero() {
		instance.executeSource(
		    """
		    result = numberFormat( "" );
		    """,
		    context );
		assertEquals( "0", variables.getAsString( result ) );
	}

	@DisplayName( "It includes leading zero before decimal" )
	@Test
	public void testEmptyStringAsZerod() {
		instance.executeSource(
		    """
		    result = numberFormat( 0, "_.00" );
		    """,
		    context );
		assertEquals( "0.00", variables.getAsString( result ) );
	}

	@DisplayName( "It will rearrange incorrect mask orders" )
	@Test
	public void testRearrangeIncorrectMaskOrders() {
		instance.executeSource(
		    """
		    result = numberFormat(1.99,"_$,.99");
		    """,
		    context );
		assertEquals( "$1.99", variables.getAsString( result ) );
	}

	@DisplayName( "It will trim extraneous optionals" )
	@Test
	public void testTrimExtraneousOptionals() {
		instance.executeSource(
		    """
		    result = numberFormat(2, "999,999,999");
		    """,
		    context );
		assertEquals( "2", variables.getAsString( result ) );
	}

	@DisplayName( "It will fix incorrect thousands separators" )
	@Test
	public void testFixIncorrectThousandsSeparators() {
		instance.executeSource(
		    """
		    result = numberFormat(1234,",9");
		    """,
		    context );
		assertEquals( "1,234", variables.getAsString( result ) );
	}

	@DisplayName( "It will fix malformed pattern" )
	@Test
	public void testFixMalformedPattern() {
		instance.executeSource(
		    """
		    result = numberFormat(1234,",.99") ;
		    """,
		    context );
		assertEquals( "1,234.00", variables.getAsString( result ) );
	}

	@DisplayName( "It will correctly round decimal values" )
	@Test
	public void testCorrectlyRoundDecimalValues() {
		instance.executeSource(
		    """
		       result = numberFormat( 0.005, '0.00' )
		    result2 = numberFormat( 12.5, '0' )
		    result3 = numberFormat( -5.5, '0' )
		    result4 = numberFormat( 2.675, '0.00' )
		    result5 = lsNumberFormat( 0.005, '0.00', 'de_DE' )
		       """,
		    context );
		assertEquals( "0.01", variables.getAsString( result ) );
		assertEquals( "13", variables.getAsString( Key.of( "result2" ) ) );
		assertEquals( "-5", variables.getAsString( Key.of( "result3" ) ) );
		assertEquals( "2.68", variables.getAsString( Key.of( "result4" ) ) );
		assertEquals( "0,01", variables.getAsString( Key.of( "result5" ) ) );
	}

	@DisplayName( "It rounds to an integer when no mask is specified" )
	@Test
	public void testDefaultMaskNoDecimals() {
		instance.executeSource(
		    """
		       result = numberFormat( 1234.5 )
		    result2 = numberFormat( 3.9 )
		       """,
		    context );
		assertEquals( "1,235", variables.getAsString( result ) );
		assertEquals( "4", variables.getAsString( Key.of( "result2" ) ) );
	}

	@DisplayName( "It retains the leading zero for fraction-only masks" )
	@Test
	public void testFractionOnlyMaskLeadingZero() {
		instance.executeSource(
		    """
		       result = numberFormat( 0, '.__' )
		    result2 = numberFormat( 0.5, '.00' )
		       """,
		    context );
		assertEquals( "0.00", variables.getAsString( result ) );
		assertEquals( "0.50", variables.getAsString( Key.of( "result2" ) ) );
	}

	@DisplayName( "It does not follow the request locale for numberFormat" )
	@Test
	public void testNumberFormatLocaleIndependent() {
		instance.executeSource(
		    """
		       setLocale( 'de_DE' )
		    result = numberFormat( 3.9, '0.00' )
		    result2 = lsNumberFormat( 3.9, '0.00' )
		       """,
		    context );
		assertEquals( "3.90", variables.getAsString( result ) );
		assertEquals( "3,90", variables.getAsString( Key.of( "result2" ) ) );
	}

	// BL-2693 (D): numberFormat is locale independent, lsNumberFormat follows the request locale.
	@DisplayName( "It uses US locale for numberFormat and request locale for lsNumberFormat with grouped masks" )
	@Test
	public void testLocaleIndependenceGroupedMask() {
		instance.executeSource(
		    """
		       setLocale( 'de_DE' )
		    result = numberFormat( 1234.5, ',0.00' )
		    result2 = lsNumberFormat( 1234.5, ',0.00' )
		    result3 = val( numberFormat( 3.9, '0.00' ) )
		       """,
		    context );
		assertEquals( "1,234.50", variables.getAsString( result ) );
		assertEquals( "1.234,50", variables.getAsString( Key.of( "result2" ) ) );
		assertEquals( 3.9, variables.getAsNumber( Key.of( "result3" ) ).doubleValue(), 0.0001 );
	}

	// BL-2694: Adobe CF and Lucee left-pad to the mask width, but core BoxLang returns trimmed values
	// ( no justification padding ). These cover the example masks from the story.
	@DisplayName( "It returns trimmed values without padding to the mask width" )
	@Test
	public void testNoPaddingToMaskWidth() {
		instance.executeSource(
		    """
		       result = numberFormat( 7, '_,___.__' )
		    result2 = numberFormat( 7, '999.99' )
		    result3 = numberFormat( 12, '____.__' )
		    result4 = numberFormat( -5.5, ',___.__' )
		    result5 = numberFormat( 5, '___' )
		    result6 = numberFormat( 1234567.891, '_,___.__' )
		       """,
		    context );
		assertEquals( "7.00", variables.getAsString( result ) );
		assertEquals( "7.00", variables.getAsString( Key.of( "result2" ) ) );
		assertEquals( "12.00", variables.getAsString( Key.of( "result3" ) ) );
		assertEquals( "-5.50", variables.getAsString( Key.of( "result4" ) ) );
		assertEquals( "5", variables.getAsString( Key.of( "result5" ) ) );
		assertEquals( "1,234,567.89", variables.getAsString( Key.of( "result6" ) ) );
	}

}
