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

		variables.put(
		    Key.of( "bigxmlString" ),
		    """
		    		    <samlp:Response ID="_74634feb-b473-4679-9aba-c3778ef35bfa" Version="2.0"
		        IssueInstant="2025-11-12T21:54:21.468Z" Destination="http://localhost:9181/cbsso/auth/entra"
		        InResponseTo="idACB318BD-F2B7-49E0-8340B56C81928DA0"
		        xmlns:samlp="urn:oasis:names:tc:SAML:2.0:protocol">
		        <Issuer xmlns="urn:oasis:names:tc:SAML:2.0:assertion">
		            https://sts.windows.net/d8f22d4c-22ab-4713-90d8-652b57f7d30f/</Issuer>
		        <samlp:Status>
		            <samlp:StatusCode Value="urn:oasis:names:tc:SAML:2.0:status:Success" />
		        </samlp:Status>
		        <Assertion ID="_28fd0c70-8240-43a8-b5d7-5a77386b3c00" IssueInstant="2025-11-12T21:54:21.465Z"
		            Version="2.0" xmlns="urn:oasis:names:tc:SAML:2.0:assertion">
		            <Issuer>https://sts.windows.net/d8f22d4c-22ab-4713-90d8-652b57f7d30f/</Issuer>
		            <Signature xmlns="http://www.w3.org/2000/09/xmldsig#">
		                <SignedInfo>
		                    <CanonicalizationMethod Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#" />
		                    <SignatureMethod Algorithm="http://www.w3.org/2001/04/xmldsig-more#rsa-sha256" />
		                    <Reference URI="#_28fd0c70-8240-43a8-b5d7-5a77386b3c00">
		                        <Transforms>
		                            <Transform Algorithm="http://www.w3.org/2000/09/xmldsig#enveloped-signature" />
		                            <Transform Algorithm="http://www.w3.org/2001/10/xml-exc-c14n#" />
		                        </Transforms>
		                        <DigestMethod Algorithm="http://www.w3.org/2001/04/xmlenc#sha256" />
		                        <DigestValue>g+8/KlVZ5JXko4kGfayO1JGscvpsb1Mp1rXZsWLE+SU=</DigestValue>
		                    </Reference>
		                </SignedInfo>
		                <SignatureValue>
		                    o2aU1QdJGqi6NZoK2cURVMpq6nj3zG4lWAcTpJ7UagEsqi3IRh/kPELk/F8iMkFQ1zcn/AQSLKlM2U5+UFarg7IVwurdU2NnYyynPfVsck/OrRIbIgE7D+qMayImm7kR09PCi9rxcdmx0lyBQauA3QE150z3Iy2UNgmlFRrcC/+bkK+BQoSS/NrIDPOC0GSwOFRtYDYy45HEmaxaiOKr82cT7xqeVBFOsOT1bglzSd/OUGsoBaOnXIcDuk5vkDLHe0VpeuVzBXLGiYmfYXl6cwwP606ytEr9EsBAPoyhVf/RvWxw/ym01TfgARmmBvQMsXd8fbO6Nk4MgR/f0OYX0w==</SignatureValue>
		                <KeyInfo>
		                    <X509Data>
		                        <X509Certificate>
		                            MIIC/jCCAeagAwIBAgIJAPJGEpowIhBNMA0GCSqGSIb3DQEBCwUAMC0xKzApBgNVBAMTImFjY291bnRzLmFjY2Vzc2NvbnRyb2wud2luZG93cy5uZXQwHhcNMjUxMDI2MTk0NjQ1WhcNMzAxMDI2MTk0NjQ1WjAtMSswKQYDVQQDEyJhY2NvdW50cy5hY2Nlc3Njb250cm9sLndpbmRvd3MubmV0MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAsE+vzm1BhzJJ5KKgJKPGX4M3GbeM0c25HOVQL1aLbOEHm92HBFk1djM9a8WLDfg/d8SLh3Ehta0i0ctATwU0CSeeodvsqL4mKEOXYEqIi1f8ixCX0c7vJ0ESNcyWeAm18F9WNtFKKDOM7gzCn0zuuAZR3m/rBaPDOkoX1AULrkMZjnantrw4z8hL344dLAneta5JiulJor2NiJGNU5EHcVjw7eMDunPTpC/IAxDKF5/hTQ0Hj4+R2AzuSBO0DZ3T2G7/6lmIguOIanfGoYGKev4JvumXahkVGf/tgZ3WuoUqB8KEIM8VGjS0MjBFgCtxX6GmvRD+H3F58x4bsBAZxwIDAQABoyEwHzAdBgNVHQ4EFgQU+A6C3/xdVe7vu2wezFXPLQE0nyMwDQYJKoZIhvcNAQELBQADggEBADAAoTCjqbO+Ku6E1nbOUkq513ETV+7iL6g7FnxY4ysl2qPAsgPcLOO/HoWGLNfu4fbqyBqtSpoHYQUEe2e4FNF9T0EB5B5NShFiSlLVcQyp23PcrcInQRnb7x9iX/ztxm1bpNnLXrQrh/RTsdev6LqiIfhC2XH70Avb6LTYcBMkUuo9Y2kxT3WtyklSl0Ogr3td/lPZne1vcPP4h64uzE9+GKcm+2iZRyWGMjtG6DnC1whmoetqDDmQ9pmHi2xlxSjcTS8oq/FwEA20sjNO4DdBN9tS2VMwVZldZ/Z594sRKOPo3kPVdKhJZud5Yt2nt+xiHcjKY48HmOXRnF8AOto=</X509Certificate>
		                    </X509Data>
		                </KeyInfo>
		            </Signature>
		            <Subject>
		                <NameID Format="urn:oasis:names:tc:SAML:2.0:nameid-format:transient">
		                    mMceg+Qeu9APYxQDW9LuA4GaTXm8u1BOinCPhAKrNrw=</NameID>
		                <SubjectConfirmation Method="urn:oasis:names:tc:SAML:2.0:cm:bearer">
		                    <SubjectConfirmationData InResponseTo="idACB318BD-F2B7-49E0-8340B56C81928DA0"
		                        NotOnOrAfter="2025-11-12T22:54:21.305Z"
		                        Recipient="http://localhost:9181/cbsso/auth/entra" />
		                </SubjectConfirmation>
		            </Subject>
		            <Conditions NotBefore="2025-11-12T21:49:21.305Z" NotOnOrAfter="2025-11-12T22:54:21.305Z">
		                <AudienceRestriction>
		                    <Audience>spn:caaa139f-4810-4355-a5b5-d8417b406909</Audience>
		                </AudienceRestriction>
		            </Conditions>
		            <AttributeStatement>
		                <Attribute Name="http://schemas.microsoft.com/identity/claims/tenantid">
		                    <AttributeValue>d8f22d4c-22ab-4713-90d8-652b57f7d30f</AttributeValue>
		                </Attribute>
		                <Attribute Name="http://schemas.microsoft.com/identity/claims/objectidentifier">
		                    <AttributeValue>ab143efb-18d5-4888-8005-97ae69a9f6f5</AttributeValue>
		                </Attribute>
		                <Attribute Name="http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname">
		                    <AttributeValue>Beers</AttributeValue>
		                </Attribute>
		                <Attribute Name="http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname">
		                    <AttributeValue>Jacob</AttributeValue>
		                </Attribute>
		                <Attribute Name="http://schemas.microsoft.com/identity/claims/displayname">
		                    <AttributeValue>Jacob Beers</AttributeValue>
		                </Attribute>
		                <Attribute Name="http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress">
		                    <AttributeValue>jbeers@ortussolutions.com</AttributeValue>
		                </Attribute>
		                <Attribute Name="http://schemas.microsoft.com/identity/claims/identityprovider">
		                    <AttributeValue>live.com</AttributeValue>
		                </Attribute>
		                <Attribute Name="http://schemas.microsoft.com/claims/authnmethodsreferences">
		                    <AttributeValue>
		                        http://schemas.microsoft.com/ws/2008/06/identity/authenticationmethod/password</AttributeValue>
		                    <AttributeValue>http://schemas.microsoft.com/claims/multipleauthn</AttributeValue>
		                    <AttributeValue>
		                        http://schemas.microsoft.com/ws/2008/06/identity/authenticationmethod/unspecified</AttributeValue>
		                </Attribute>
		            </AttributeStatement>
		            <AuthnStatement AuthnInstant="2025-11-12T20:40:34.778Z"
		                SessionIndex="_28fd0c70-8240-43a8-b5d7-5a77386b3c00">
		                <AuthnContext>
		                    <AuthnContextClassRef>urn:oasis:names:tc:SAML:2.0:ac:classes:Password</AuthnContextClassRef>
		                </AuthnContext>
		            </AuthnStatement>
		        </Assertion>
		    </samlp:Response>
		    		    """ );
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

	@DisplayName( "It works attribute Node" )
	@Test
	public void testSearchComplexXML() {
		instance.executeSource(
		    """
		     xml = XMLParse( bigxmlString.trim() );
		     search = xmlSearch( xml, "samlp:Response//samlp:StatusCode[@Value='urn:oasis:names:tc:SAML:2.0:status:Success']" )
		    result = search.len();
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( 1 );
	}

}
