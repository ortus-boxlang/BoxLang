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
 * distributed under the License is distribu ted on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ortus.boxlang.runtime.bifs.global.decision;

import static com.google.common.truth.Truth.assertThat;

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

public class IsValidTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@AfterAll
	public static void teardown() {
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		variables.clear();
	}

	@DisplayName( "It works on any" )
	@Test
	public void testAny() {
		instance.executeSource(
		    """
		    aStruct   = isValid( 'Any', {} );
		    aBool     = isValid( 'Any', true );
		    aDate     = isValid( 'Any', now() );
		    anInt     = isValid( 'Any', 12345 );
		    namedArgs = isValid( type = 'Any', value = 12345 );
		    """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "aStruct" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "aBool" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "aDate" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "anInt" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "namedArgs" ) ) ).isTrue();
	}

	@DisplayName( "It works on Arrays" )
	@Test
	public void testArray() {
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'Array', [] )" ) ).isTrue();
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'Array', {} )" ) ).isFalse();
	}

	@Disabled( "toBinary is not implemented" )
	@DisplayName( "It works on Binaries" )
	@Test
	public void testBinary() {
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'Binary', toBinary( toBase64( 'boxlang' ) )  )" ) ).isTrue();
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'Binary', toBase64( 'boxlang' )  )" ) ).isFalse();
	}

	@DisplayName( "It works on booleans" )
	@Test
	public void testBoolean() {
		instance.executeSource(
		    """
		    aTrue = isValid( 'boolean', true );
		    aYes = isValid( 'boolean', "yes" );
		    aStringFalse = isValid( 'boolean', "false" );
		    """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "aTrue" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "aYes" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "aStringFalse" ) ) ).isTrue();
	}

	@Disabled( "Unimplemented" )
	@DisplayName( "It works on creditcards" )
	@Test
	public void testCreditcard() {
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'creditcard','4111111111111111' )" ) ).isTrue();
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'creditcard','41111111111111114' )" ) ).isFalse();
	}

	@Disabled( "Unimplemented" )
	@DisplayName( "It works on components" )
	@Test
	public void testComponent() {
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'component',new FunkyComponent() )" ) ).isTrue();
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'component',{} )" ) ).isFalse();
	}

	@Disabled( "Unimplemented" )
	@DisplayName( "It works on dates" )
	@Test
	public void testDate() {
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'date', '2024-01-26' )" ) ).isTrue();
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'date', '2024-01-26777' )" ) ).isFalse();
	}

	@Disabled( "Unimplemented" )
	@DisplayName( "It works on times" )
	@Test
	public void testTime() {
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'time', '10:45' )" ) ).isTrue();
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'time', '' )" ) ).isFalse();
	}

	@Disabled( "Unimplemented" )
	@DisplayName( "It works on emails" )
	@Test
	public void testEmail() {
		instance.executeSource(
		    """
		    singlechar   = isValid( 'email', 'a@ortus.solutions.com' );
		    specialchars = isValid( 'email', 'a-sw.ell-brad+foo_09@gm.ail.com' );

		    // falsies
		    doubleat         = IsValid( "email", 'brad@a@bc-_brad.box.lang' );
		    nouser           = IsValid( "email", '@abc-_brad.box.lang' );
		    domainendwithdot = IsValid( "email", 'dddd@abc-_brad.box.' );
		    userendwithdot   = isValid( 'email', 'brad.@ortussolutions.xom' );
		    userstartwithdot = isValid( 'email', '.foo@ortussolutions.xom' );
		       """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "singlechar" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "specialchars" ) ) ).isTrue();

		assertThat( ( Boolean ) variables.get( Key.of( "doubleat" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "nouser" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "domainendwithdot" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "userendwithdot" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "userstartwithdot" ) ) ).isFalse();

	}

	@Disabled( "Unimplemented" )
	@DisplayName( "It works on floats" )
	@Test
	public void testFloat() {
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'float', 1.23 )" ) ).isTrue();
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'float', 'xyz' )" ) ).isFalse();
		instance.executeSource(
		    """
		    // trues
		    float       = isValid( 'numeric', 123.45 );
		    stringFloat = isValid( 'numeric', "123.45" );

		    // falsies
		    int       = isValid( 'numeric', 123 );
		    bool      = isValid( 'numeric', true );
		    stringval = isValid( 'numeric', '3x' );
		    """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "float" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "stringFloat" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "int" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "bool" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "stringval" ) ) ).isFalse();
	}

	@Disabled( "Unimplemented" )
	@DisplayName( "It works on guids" )
	@Test
	public void testGuid() {
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'guid', createGUID() )" ) ).isTrue();
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'guid', createUUID() )" ) ).isFalse();
	}

	@DisplayName( "It works on integers" )
	@Test
	public void testInteger() {
		instance.executeSource(
		    """
		    // trues
		    int       = isValid( 'integer', 123 );
		    // falsies
		    bool      = isValid( 'integer', true );
		    float     = isValid( 'integer', 123.45 );
		    stringval = isValid( 'integer', '3x' );
		    """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "int" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "bool" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "float" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "stringval" ) ) ).isFalse();
	}

	@DisplayName( "It works on Numerics" )
	@Test
	public void testNumeric() {
		instance.executeSource(
		    """
		    // trues
		    int         = isValid( 'numeric', 123 );
		    float       = isValid( 'numeric', 123.45 );
		    stringInt   = isValid( 'numeric', "123" );
		    stringFloat = isValid( 'numeric', "123.45" );

		    // falsies
		    bool      = isValid( 'numeric', true );
		    stringval = isValid( 'numeric', '3x' );
		    """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "int" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "float" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "stringInt" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "stringFloat" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "bool" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "stringval" ) ) ).isFalse();
	}

	@Disabled( "QueryNew() is not implemented" )
	@DisplayName( "It works on Querys" )
	@Test
	public void testQuery() {
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'Query', queryNew( 'id,name' ) )" ) ).isTrue();
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'Query', {} )" ) ).isFalse();
	}

	@Disabled( "Unimplemented" )
	@DisplayName( "It works on ranges" )
	@Test
	public void testRange() {
		instance.executeSource(
		    """
		    // trues
		    is3in1through5 = IsValid( "range", '3',1,5);
		    is1in1through5 = IsValid( "range", '1',1,5);
		    is5in1through5 = IsValid( "range", '5',1,5);

		    // falses
		    is6in1through5  = IsValid( "range", '6',1,5);
		    is0in1through5  = IsValid( "range", '0',1,5);
		    is10in1through5 = IsValid( "range", '10',10,5);
		    isXin1through5  = IsValid( "range", 'x',1,5);
		    """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "is3in1through5" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "is1in1through5" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "is5in1through5" ) ) ).isTrue();

		assertThat( ( Boolean ) variables.get( Key.of( "is6in1through5" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "is0in1through5" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "is10in1through5" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "is3in1through5" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "isXin1through5" ) ) ).isFalse();
	}

	@Disabled( "Unimplemented" )
	@DisplayName( "It works on Regexs" )
	@Test
	public void testRegex() {
		instance.executeSource(
		    """
		    // trues
		    singlechar      = IsValid( "regex", 'abc', '...' );
		    plusquantifier  = IsValid( "regex", 'abc', '.+' );
		    curlyquantifier = IsValid( "regex", 'abc', '[abc]{3}' );

		    // falses
		    wrongCasing = IsValid( "regex", 'ABC', '[abc]{3}' );
		    mismatch = IsValid( "regex", '(abc', '[abc]{3}' );
		    """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "singlechar" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "is3in1through5" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "plusquantifier" ) ) ).isTrue();

		assertThat( ( Boolean ) variables.get( Key.of( "wrongCasing" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "mismatch" ) ) ).isFalse();
	}

	@Disabled( "Unimplemented" )
	@DisplayName( "It works on regular_expressions" )
	@Test
	public void testRegular_expression() {
		instance.executeSource(
		    """
		    // trues
		    singlechar      = IsValid( "regex", 'abc', '...' );
		    plusquantifier  = IsValid( "regex", 'abc', '.+' );
		    curlyquantifier = IsValid( "regex", 'abc', '[abc]{3}' );

		    // falses
		    wrongCasing = IsValid( "regex", 'ABC', '[abc]{3}' );
		    mismatch = IsValid( "regex", '(abc', '[abc]{3}' );
		    """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "singlechar" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "is3in1through5" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "plusquantifier" ) ) ).isTrue();

		assertThat( ( Boolean ) variables.get( Key.of( "wrongCasing" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "mismatch" ) ) ).isFalse();
	}

	@DisplayName( "It works on ssns" )
	@Test
	public void testSSN() {
		instance.executeSource(
		    """
		    // trues
		    validwithdashes = isValid( 'ssn', '123-45-6789' );
		    validnodashes   = isValid( 'ssn', '123-45-6789' );

		    // falsies
		    toomanychars  = isValid( 'ssn', '1234567891' );
		    zeros         = isValid( 'ssn', '123-00-6789' );
		    woolworth     = isValid( 'ssn', '078-05-1120' );
		    ssaDisallowed = isValid( 'ssn', '219-09-9999' );
		       """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "validwithdashes" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "validnodashes" ) ) ).isTrue();

		assertThat( ( Boolean ) variables.get( Key.of( "toomanychars" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "zeros" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "woolworth" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "ssaDisallowed" ) ) ).isFalse();
	}

	@DisplayName( "It works on social_security_numbers" )
	@Test
	public void testSocial_security_number() {
		instance.executeSource(
		    """
		    // trues
		    validwithdashes = isValid( 'ssn', '123-45-6789' );
		    validnodashes   = isValid( 'ssn', '123-45-6789' );

		    // falsies
		    toomanychars  = isValid( 'ssn', '1234567891' );
		    zeros         = isValid( 'ssn', '123-00-6789' );
		    woolworth     = isValid( 'ssn', '078-05-1120' );
		    ssaDisallowed = isValid( 'ssn', '219-09-9999' );
		       """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "validwithdashes" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "validnodashes" ) ) ).isTrue();

		assertThat( ( Boolean ) variables.get( Key.of( "toomanychars" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "zeros" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "woolworth" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "ssaDisallowed" ) ) ).isFalse();
	}

	@DisplayName( "It works on Strings" )
	@Test
	public void testString() {
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'String', 'aString' )" ) ).isTrue();
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'String', {} )" ) ).isFalse();
	}

	@DisplayName( "It works on Structs" )
	@Test
	public void testStruct() {
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'Struct', {} )" ) ).isTrue();
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'Struct', [] )" ) ).isFalse();
	}

	@Disabled( "Unimplemented" )
	@DisplayName( "It works on telephones" )
	@Test
	public void testTelephone() {
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'telephone', '1.678.256.3011' )" ) ).isTrue();
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'telephone', '+1 (678) 256-3011' )" ) ).isTrue();
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'telephone', '16782563011' )" ) ).isFalse();
	}

	@Disabled( "Unimplemented" )
	@DisplayName( "It works on URLs" )
	@Test
	public void testURL() {
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'URL', goodValue )" ) ).isTrue();
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'URL', badValue )" ) ).isFalse();
	}

	@DisplayName( "It works on UUIDs" )
	@Test
	public void testUUID() {
		instance.executeSource(
		    """
		    // trues
		    createuuid = isValid( 'uuid', createUUID() );
		    result     = isValid( 'uuid', '8BC22B08-53A4-4876-A4E08CD9690DBF2C' );

		    // falsies
		    // guid         = isValid( 'uuid', createGUID() );
		    toomanychars = isValid( 'uuid', '8BC22B08-53A4-4876-A4E08CD9690DBF2C111' );
		    """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "createuuid" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "result" ) ) ).isTrue();

		// assertThat( ( Boolean ) variables.get( Key.of( "guid" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "toomanychars" ) ) ).isFalse();
	}

	@DisplayName( "It works on usdates" )
	@Test
	public void testUsdate() {
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'usdate', '1/31/2024' )" ) ).isTrue();
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'usdate', '31/1/2024' )" ) ).isFalse();
	}

	@Disabled( "Unimplemented" )
	@DisplayName( "It works on variablenames" )
	@Test
	public void testVariablename() {
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'variablename','foo' )" ) ).isTrue();
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'variablename','new' )" ) ).isFalse();
	}

	@Disabled( "Unimplemented" )
	@DisplayName( "It works on xmls" )
	@Test
	public void testXml() {
		instance.executeSource(
		    """
		    xmlNew = isValid( 'xml', xmlNew() );
		    emptyXMLNode = isValid( 'xml', '<xml></xml>' );

		    // falsies
		    invalidchildnode = isValid( 'xml', '<xml><funky node</xml>' );
		    emptybrackets    = isValid( 'xml', '<>' );
		    """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "xmlNew" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "emptyXMLNode" ) ) ).isTrue();

		assertThat( ( Boolean ) variables.get( Key.of( "invalidchildnode" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "emptybrackets" ) ) ).isFalse();
	}

	@DisplayName( "It works on zipcodes" )
	@Test
	public void testZipcode() {
		instance.executeSource(
		    """
		    fivedigit     = IsValid( "zipcode", '12345' );
		    ninewithdash  = IsValid( "zipcode", '12345-6789' );
		    ninewithspace = IsValid( "zipcode", '12345 6789' );

		    // falsies
		    fourdigit     = IsValid( "zipcode", '1234' );
		    eightwithdash = IsValid( "zipcode", '1234-12345' );
		    """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "fivedigit" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "ninewithdash" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "ninewithspace" ) ) ).isTrue();

		assertThat( ( Boolean ) variables.get( Key.of( "fourdigit" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "eightwithdash" ) ) ).isFalse();
	}
}
