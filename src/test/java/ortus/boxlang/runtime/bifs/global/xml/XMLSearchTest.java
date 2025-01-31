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

package ortus.boxlang.runtime.bifs.global.xml;

import static com.google.common.truth.Truth.assertThat;

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
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.XML;

public class XMLSearchTest {

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

	@DisplayName( "It can search" )
	@Test
	public void testCanSearch() {
		instance.executeSource(
		    """
		       xml = XMLParse( '<users><user name="brad" /><user name="luis" /></users>' );
		    result = XMLSearch( xml, '/users/user' );
		       """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Array.class );
		Array arr = variables.getAsArray( result );
		assertThat( arr.size() ).isEqualTo( 2 );
		assertThat( ( ( Struct ) ( ( XML ) arr.get( 0 ) ).dereference( context, Key.XMLAttributes, false ) ).dereference( context, Key._NAME, false ) )
		    .isEqualTo( "brad" );
		assertThat( ( ( Struct ) ( ( XML ) arr.get( 1 ) ).dereference( context, Key.XMLAttributes, false ) ).dereference( context, Key._NAME, false ) )
		    .isEqualTo( "luis" );
	}

	@DisplayName( "It can search params" )
	@Test
	public void testCanSearchParams() {
		instance.executeSource(
		    """
		       xml = XMLParse( '<users><user name="brad" /><user name="luis" /></users>' );
		    result = XMLSearch( xml, "/users/user[@name=$param1]", { param1 : "brad" } );
		       """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Array.class );
		Array arr = variables.getAsArray( result );
		assertThat( arr.size() ).isEqualTo( 1 );
		assertThat( ( ( Struct ) ( ( XML ) arr.get( 0 ) ).dereference( context, Key.XMLAttributes, false ) ).dereference( context, Key._NAME, false ) )
		    .isEqualTo( "brad" );
	}

	@DisplayName( "It can search member" )
	@Test
	public void testCanSearchMember() {
		instance.executeSource(
		    """
		       xml = XMLParse( '<users><user name="brad" /><user name="luis" /></users>' );
		    result = xml.Search( '/users/user' );
		       """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Array.class );
		Array arr = variables.getAsArray( result );
		assertThat( arr.size() ).isEqualTo( 2 );
		assertThat( ( ( Struct ) ( ( XML ) arr.get( 0 ) ).dereference( context, Key.XMLAttributes, false ) ).dereference( context, Key._NAME, false ) )
		    .isEqualTo( "brad" );
		assertThat( ( ( Struct ) ( ( XML ) arr.get( 1 ) ).dereference( context, Key.XMLAttributes, false ) ).dereference( context, Key._NAME, false ) )
		    .isEqualTo( "luis" );
	}

	@DisplayName( "It can search string" )
	@Test
	public void testCanSearchString() {
		instance.executeSource(
		    """
		    xml = '<Conditions NotBefore="2018-08-24T10:54:19.464Z" NotOnOrAfter="2018-08-24T11:54:19.464Z"></Conditions>';

		    result = XmlSearch(xml, "string(/Conditions/@NotBefore)");
		         """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( String.class );
		assertThat( variables.get( result ) ).isEqualTo( "2018-08-24T10:54:19.464Z" );
	}

	@DisplayName( "It can search boolean" )
	@Test
	public void testCanSearchBoolean() {
		instance.executeSource(
		    """
		    xml = '<Conditions IsActive="true"></Conditions>';

		    result = XmlSearch(xml, "boolean(/Conditions/@IsActive)");
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Boolean.class );
		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "It can search numeric" )
	@Test
	public void testCanSearchNumeric() {
		instance.executeSource(
		    """
		    xml = '<Item Price="19.99"></Item>';

		    result = XmlSearch(xml, "number(/Item/@Price)");
		    """,
		    context );
		assertThat( variables.get( result ) ).isInstanceOf( Double.class );
		assertThat( variables.get( result ) ).isEqualTo( 19.99 );
	}

	@DisplayName( "It can search when namespaces are present" )
	@Test
	void testNamespaceSearching() {
		//@formatter:off
		instance.executeSource(
		    """
		    xmlObj = xmlParse( '
			   <SOAP-ENV:Envelope xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/"
			   xmlns:xsd="http://www.w3.org/2001/XMLSchema"
			   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
			   <SOAP-ENV:Header>
				   <ns1:MessageHeader xmlns:ns1="http://www.ebxml.org/namespaces/messageHeader">
					   <ns1:From>
						   <ns1:PartyId ns1:type="urn:x12.org:IO5:01">Ortus Solutions</ns1:PartyId>
					   </ns1:From>
					   <ns1:Action>Testing</ns1:Action>
				   </ns1:MessageHeader>
			   </SOAP-ENV:Header>
		   </SOAP-ENV:Envelope>  
			' );
			result = xmlSearch( xmlObj.xmlRoot, ".//SOAP-ENV:Header" )
		            """,
		    context );
			//@formatter:on

		assertThat( variables.get( result ) ).isInstanceOf( Array.class );
		assertThat( variables.getAsArray( result ).size() ).isEqualTo( 1 );
	}

}
