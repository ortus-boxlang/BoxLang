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

	@DisplayName( "It works on creditcards" )
	@Test
	public void testCreditcard() {
		instance.executeSource(
		    """
		    stripeTestCard = isValid( "creditcard","4111111111111111" );
		    valid1 = IsValid( "creditcard","4000000000006" );
		    valid2 = IsValid( "creditcard","378888888888858" );
		    valid3 = IsValid( "creditcard","4888888888888838" );
		    valid3 = IsValid( "creditcard","5588888888888838" );
		    valid4 = IsValid( "creditcard","6011222233334444" );
		    valid5 = IsValid( "creditcard","6011-2222-3333-4444" );
		    valid6 = IsValid( "creditcard","6011 2222 3333 4444" );
		    valid7 = IsValid( "creditcard","6011,2222,3333,4444" );

		    // falsies
		    stripeTestWithExtra4 = isValid( "creditcard","41111111111111114" );
		    alphachars           = IsValid( "creditcard","4111x1111x1111y11114" );
		    invalid3             = IsValid( "creditcard","6010222233334444" );
		    invalid4             = IsValid( "creditcard","4000000000007" );
		       """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "stripeTestCard" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "valid1" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "valid2" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "valid3" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "valid4" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "valid5" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "valid6" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "valid7" ) ) ).isTrue();

		assertThat( ( Boolean ) variables.get( Key.of( "stripeTestWithExtra4" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "alphachars" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "invalid3" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "invalid4" ) ) ).isFalse();
	}

	@DisplayName( "It works on components" )
	@Test
	public void testComponent() {
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'component', new src.test.java.TestCases.phase3.MyClass() )" ) ).isTrue();
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'component', {} )" ) ).isFalse();
	}

	@DisplayName( "It works on dates" )
	@Test
	public void testDate() {
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'date', '2024-01-26' )" ) ).isTrue();
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'date', '2024-01-26777' )" ) ).isFalse();
	}

	@DisplayName( "It works on times" )
	@Test
	public void testTime() {
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'time', '10:45' )" ) ).isTrue();
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'time', '' )" ) ).isFalse();
	}

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

	@DisplayName( "It works on telephones" )
	@Test
	public void testTelephone() {
		instance.executeSource(
		    """
		    // trues
		    dotdelimited  = isValid( 'telephone', '1.678.256.3011' );
		    pluscountry   = isValid( 'telephone', '+1 (678) 256-3011' );
		    noPunctuation = isValid( 'telephone', '16782563011' );

		    // falsies
		    tooshort           = isValid( 'telephone', '234-567' );
		    toolong            = isValid( 'telephone', '+1 1234-456-7890' );
		    missingCountryCode = isValid( 'telephone', '+ 1234-456-7890' );
		       """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "dotdelimited" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "pluscountry" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "noPunctuation" ) ) ).isTrue();

		assertThat( ( Boolean ) variables.get( Key.of( "tooshort" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "toolong" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "missingCountryCode" ) ) ).isFalse();
	}

	@DisplayName( "It works on URLs" )
	@Test
	public void testURL() {
		instance.executeSource(
		    """
		    // trues
		    httpScheme        = isValid( 'url', 'http://ortussolutions.com' );
		    httpsScheme      = isValid( 'url', 'https://ortussolutions.com' );
		    ftpScheme        = isValid( 'url', 'ftp://ortussolutions.com' );
		    fileScheme        = isValid( 'url', 'file://ortussolutions.com' );

		    doubleslash      = isValid( 'url', 'https://ortussolutions.com//' );
		    filename         = isValid( 'url', 'https://ortussolutions.com/foo.html' );
		    querystring      = isValid( 'url', 'https://ortussolutions.com/foo.html?page=brad' );
		    querystringfunky = isValid( 'url', 'https://www.ortussolutions.com/services/web-development?page=brad%20writes%20command.box&fo0+foo=foo!' );

		    // falsies
		    noscheme = isValid( 'url', 'ortussolutions.com' );
		    filepath = isValid( 'url', '../www/var/html' );
		       """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "httpScheme" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "httpsScheme" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "ftpScheme" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "fileScheme" ) ) ).isTrue();

		assertThat( ( Boolean ) variables.get( Key.of( "doubleslash" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "filename" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "querystring" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "querystringfunky" ) ) ).isTrue();

		assertThat( ( Boolean ) variables.get( Key.of( "noscheme" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "filepath" ) ) ).isFalse();
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

	@Disabled( "toBinary is not implemented" )
	@DisplayName( "It works on Binaries" )
	@Test
	public void testBinary() {
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'Binary', toBinary( toBase64( 'boxlang' ) )  )" ) ).isTrue();
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'Binary', toBase64( 'boxlang' )  )" ) ).isFalse();
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

	@DisplayName( "It works on Querys" )
	@Test
	public void testQuery() {
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'Query', queryNew( 'id,name', 'varchar,varchar' ) )" ) ).isTrue();
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

	@Disabled( "Unimplemented" )
	@DisplayName( "It works on variablenames" )
	@Test
	public void testVariablename() {
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'variablename','foo' )" ) ).isTrue();
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'variablename','new' )" ) ).isFalse();
	}

	@DisplayName( "It works on xmls" )
	@Test
	public void testXml() {
		instance.executeSource(
		    """
		    xmlNew = isValid( 'xml', XMLParse( '<xml></xml>' ) );
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

	@DisplayName( "It works on lambdas" )
	@Test
	public void testLambda() {
		instance.executeSource(
		    """
		    aLambdaIsLambda  = IsValid( "lambda", () -> {} );
		    """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "aLambdaIsLambda" ) ) ).isTrue();
	}

	@DisplayName( "It works on closures" )
	@Test
	public void testClosure() {
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'closure', function() {} )" ) ).isTrue();
		assertThat( ( Boolean ) instance.executeStatement( "isValid( 'closure', () => {} )" ) ).isTrue();
	}

	@DisplayName( "It works on custom functions" )
	@Test
	public void testFunctions() {
		instance.executeSource(
		    """
		    function myFunkyUDF() {};
		    aUDF       = IsValid( "function", myFunkyUDF );

		    // falsies
		    aClosure = IsValid( "function", function() {} );
		    aLambda  = IsValid( "function", () -> {} );
		    """,
		    context );
		assertThat( ( Boolean ) variables.get( Key.of( "aUDF" ) ) ).isTrue();
		assertThat( ( Boolean ) variables.get( Key.of( "aClosure" ) ) ).isFalse();
		assertThat( ( Boolean ) variables.get( Key.of( "aLambda" ) ) ).isFalse();
	}
}
