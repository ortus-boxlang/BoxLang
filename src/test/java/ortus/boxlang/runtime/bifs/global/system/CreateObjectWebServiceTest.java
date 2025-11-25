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

package ortus.boxlang.runtime.bifs.global.system;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.net.soap.BoxSoapClient;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

public class CreateObjectWebServiceTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result					= new Key( "result" );

	// WSDL Service URLs
	static String		numberConversionWSDL	= "https://www.dataaccess.com/webservicesserver/NumberConversion.wso?wsdl";
	static String		calculatorWSDL			= "http://www.dneonline.com/calculator.asmx?wsdl";
	static String		countryInfoWSDL			= "http://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso?wsdl";
	static String		tempConvertWSDL			= "https://www.w3schools.com/xml/tempconvert.asmx?wsdl";

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
		instance.getClassLocator().clearClassLoaders();
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	// ========================================
	// Number Conversion Service Tests
	// ========================================

	@DisplayName( "Number Conversion: Convert number to words" )
	@Test
	public void testNumberConversionNumberToWords() {
		// @formatter:off
		instance.executeSource(
		    """
		    ws = createObject( "webservice", "%s" );
		    result = ws.NumberToWords( 123 );
		    """.formatted( numberConversionWSDL ),
		    context );
		// @formatter:on
		String result = variables.getAsString( Key.of( "result" ) );
		assertThat( result.toLowerCase() ).contains( "one hundred" );
		assertThat( result.toLowerCase() ).contains( "twenty" );
		assertThat( result.toLowerCase() ).contains( "three" );
	}

	@DisplayName( "Number Conversion: Convert number to dollars" )
	@Test
	public void testNumberConversionNumberToDollars() {
		// @formatter:off
		instance.executeSource(
		    """
		    ws = createObject( "webservice", "%s" );
		    result = ws.NumberToDollars( 1234.56 );
		    """.formatted( numberConversionWSDL ),
		    context );
		// @formatter:on
		String result = variables.getAsString( Key.of( "result" ) );
		assertThat( result.toLowerCase() ).contains( "one thousand" );
		assertThat( result.toLowerCase() ).contains( "two hundred" );
		assertThat( result.toLowerCase() ).contains( "dollars" );
		assertThat( result.toLowerCase() ).contains( "cents" );
	}

	// ========================================
	// Country Info Service Tests
	// ========================================

	@DisplayName( "Country Info: Get list of continents" )
	@Test
	public void testCountryInfoListOfContinents() {
		// @formatter:off
		instance.executeSource(
		    """
		    ws = createObject( "webservice", "%s" );
		    result = ws.ListOfContinentsByName();
		    """.formatted( countryInfoWSDL ),
		    context );
		// @formatter:on
		Array result = variables.getAsArray( Key.of( "result" ) );
		assertThat( result.size() ).isGreaterThan( 5 );

		// Check for expected continents
		boolean	foundAfrica	= false;
		boolean	foundAsia	= false;
		boolean	foundEurope	= false;
		for ( Object continent : result ) {
			IStruct	continentStruct	= ( IStruct ) continent;
			String	continentName	= continentStruct.get( Key.of( "sName" ) ).toString();
			if ( "Africa".equals( continentName ) ) {
				foundAfrica = true;
			}
			if ( "Asia".equals( continentName ) ) {
				foundAsia = true;
			}
			if ( "Europe".equals( continentName ) ) {
				foundEurope = true;
			}
		}
		assertThat( foundAfrica ).isTrue();
		assertThat( foundAsia ).isTrue();
		assertThat( foundEurope ).isTrue();
	}

	@DisplayName( "Country Info: Get country currency by country code" )
	@Test
	public void testCountryInfoCountryCurrency() {
		// @formatter:off
		instance.executeSource(
		    """
		    ws = createObject( "webservice", "%s" );
		    result = ws.CountryCurrency( "CA" );
		    """.formatted( countryInfoWSDL ),
		    context );
		// @formatter:on
		IStruct result = variables.getAsStruct( Key.of( "result" ) );
		assertThat( result.get( Key.of( "sISOCode" ) ).toString() ).isEqualTo( "CAD" );
		assertThat( result.get( Key.of( "sName" ) ).toString() ).isEqualTo( "Canadian Dollars" );
	}

	@DisplayName( "Country Info: Get full country information" )
	@Test
	public void testCountryInfoFullCountryInfo() {
		// @formatter:off
		instance.executeSource(
		    """
		    ws = createObject( "webservice", "%s" );
		    result = ws.FullCountryInfo( "GB" );
		    """.formatted( countryInfoWSDL ),
		    context );
		// @formatter:on
		IStruct result = variables.getAsStruct( Key.of( "result" ) );

		assertThat( result.get( Key.of( "sISOCode" ) ).toString() ).isEqualTo( "GB" );
		assertThat( result.get( Key.of( "sName" ) ).toString() ).isEqualTo( "United Kingdom" );
		assertThat( result.get( Key.of( "sCapitalCity" ) ).toString() ).isEqualTo( "London" );
		assertThat( result.get( Key.of( "sCurrencyISOCode" ) ).toString() ).isEqualTo( "GBP" );
	}

	@DisplayName( "Country Info: Get capital city by country code" )
	@Test
	public void testCountryInfoCapitalCity() {
		// @formatter:off
		instance.executeSource(
		    """
		    ws = createObject( "webservice", "%s" );
		    result = ws.CapitalCity( "FR" );
		    """.formatted( countryInfoWSDL ),
		    context );
		// @formatter:on
		String result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).isEqualTo( "Paris" );
	}

	// ========================================
	// Temperature Conversion Service Tests
	// ========================================

	@DisplayName( "Temp Convert: Celsius to Fahrenheit" )
	@Test
	public void testTempConvertCelsiusToFahrenheit() {
		// @formatter:off
		instance.executeSource(
		    """
		    ws = createObject( "webservice", "%s" );
		    result = ws.CelsiusToFahrenheit( 0 );
		    """.formatted( tempConvertWSDL ),
		    context );
		// @formatter:on
		String result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).isEqualTo( "32" );
	}

	@DisplayName( "Temp Convert: Fahrenheit to Celsius" )
	@Test
	public void testTempConvertFahrenheitToCelsius() {
		// @formatter:off
		instance.executeSource(
		    """
		    ws = createObject( "webservice", "%s" );
		    result = ws.FahrenheitToCelsius( 212 );
		    """.formatted( tempConvertWSDL ),
		    context );
		// @formatter:on
		String result = variables.getAsString( Key.of( "result" ) );
		assertThat( result ).isEqualTo( "100" );
	}

	// ========================================
	// Webservice Object Tests
	// ========================================

	@DisplayName( "CreateObject returns BoxSoapClient instance" )
	@Test
	public void testCreateObjectReturnsBoxSoapClient() {
		// @formatter:off
		instance.executeSource(
		    """
		    result = createObject( "webservice", "%s" );
		    """.formatted( countryInfoWSDL ),
		    context );
		// @formatter:on
		Object result = variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( BoxSoapClient.class );
	}

	@DisplayName( "Webservice object can be reused for multiple calls" )
	@Test
	public void testWebserviceObjectReuse() {
		// @formatter:off
		instance.executeSource(
		    """
		    ws = createObject( "webservice", "%s" );
		    result1 = ws.CapitalCity( "FR" );
		    result2 = ws.CapitalCity( "IT" );
		    result3 = ws.CapitalCity( "ES" );
		    """.formatted( countryInfoWSDL ),
		    context );
		// @formatter:on
		String	result1	= variables.getAsString( Key.of( "result1" ) );
		String	result2	= variables.getAsString( Key.of( "result2" ) );
		String	result3	= variables.getAsString( Key.of( "result3" ) );

		assertThat( result1 ).isEqualTo( "Paris" );
		assertThat( result2 ).isEqualTo( "Rome" );
		assertThat( result3 ).isEqualTo( "Madrid" );
	}

}
