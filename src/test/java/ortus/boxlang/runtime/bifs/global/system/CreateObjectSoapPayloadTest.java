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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

/**
 * Test SOAP payload assembly and transmission for createObject webservice type using WireMock
 */
public class CreateObjectSoapPayloadTest {

	static BoxRuntime			instance;
	IBoxContext					context;
	IScope						variables;
	static Key					result		= new Key( "result" );

	@RegisterExtension
	static WireMockExtension	wireMock	= WireMockExtension.newInstance()
	    .options( wireMockConfig().dynamicPort() )
	    .build();

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
		instance.getClassLocator().clearClassLoaders();
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );

		// Reset WireMock for each test
		wireMock.resetAll();
	}

	@DisplayName( "Test SOAP request payload for webservice method invocation with parameters" )
	@Test
	void testSOAPPayloadNoParameters() {
		// Mock WSDL response
		wireMock.stubFor( get( urlEqualTo( "/calculator.wsdl" ) )
		    .willReturn( aResponse()
		        .withStatus( 200 )
		        .withHeader( "Content-Type", "text/xml" )
		        .withBody( getCalculatorWSDL( wireMock.baseUrl() ) ) ) );

		// Mock SOAP service endpoint
		wireMock.stubFor( post( urlEqualTo( "/calculator" ) )
		    .willReturn( aResponse()
		        .withStatus( 200 )
		        .withHeader( "Content-Type", "text/xml; charset=utf-8" )
		        .withBody( getEchoResponseXML( "test message" ) ) ) );

		String baseURL = wireMock.baseUrl();

		// Test creating webservice and calling method with parameters
		instance.executeSource(
		    String.format( """
		                   ws = createObject( "webservice", "%s/calculator.wsdl" )
		                   result = ws.echo( "test message" )
		                   """, baseURL ),
		    context );

		// Verify the SOAP request was made with correct structure
		wireMock.verify( postRequestedFor( urlEqualTo( "/calculator" ) )
		    .withHeader( "Content-Type", containing( "text/xml" ) )
		    .withHeader( "SOAPAction", equalTo( "" ) )
		    .withRequestBody( containing( "<soap:Envelope" ) )
		    .withRequestBody( containing( "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"" ) )
		    .withRequestBody( containing( "<soap:Body>" ) )
		    .withRequestBody( containing( "<echo" ) )
		    .withRequestBody( containing( "</soap:Body>" ) )
		    .withRequestBody( containing( "</soap:Envelope>" ) ) );

		assertThat( variables.get( result ) ).isEqualTo( "test message" );
	}

	@DisplayName( "Test SOAP request payload for webservice method with parameters" )
	@Test
	void testSOAPPayloadWithParameters() {
		// Mock WSDL response
		wireMock.stubFor( get( urlEqualTo( "/calculator.wsdl" ) )
		    .willReturn( aResponse()
		        .withStatus( 200 )
		        .withHeader( "Content-Type", "text/xml" )
		        .withBody( getCalculatorWSDL( wireMock.baseUrl() ) ) ) );

		// Mock SOAP service endpoint
		wireMock.stubFor( post( urlEqualTo( "/calculator" ) )
		    .willReturn( aResponse()
		        .withStatus( 200 )
		        .withHeader( "Content-Type", "text/xml; charset=utf-8" )
		        .withBody( getAddResponseXML( 30 ) ) ) );

		String baseURL = wireMock.baseUrl();

		// Test creating webservice and calling method with parameters
		instance.executeSource(
		    String.format( """
		                   ws = createObject( "webservice", "%s/calculator.wsdl" )
		                   result = ws.add( 10, 20 )
		                   """, baseURL ),
		    context );

		// Verify the SOAP request was made with correct structure and parameters
		wireMock.verify( postRequestedFor( urlEqualTo( "/calculator" ) )
		    .withHeader( "Content-Type", containing( "text/xml" ) )
		    .withHeader( "SOAPAction", equalTo( "" ) )
		    .withRequestBody( containing( "<soap:Envelope" ) )
		    .withRequestBody( containing( "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"" ) )
		    .withRequestBody( containing( "<soap:Body>" ) )
		    .withRequestBody( containing( "<add" ) )
		    .withRequestBody( containing( "<a xsi:type=\"xsd:double\">10</a>" ) )
		    .withRequestBody( containing( "<b xsi:type=\"xsd:double\">20</b>" ) )
		    .withRequestBody( containing( "</soap:Body>" ) )
		    .withRequestBody( containing( "</soap:Envelope>" ) ) );

		assertThat( variables.get( result ) ).isEqualTo( 30 );
	}

	@DisplayName( "Test SOAP request payload for webservice method with struct parameters" )
	@Test
	void testSOAPPayloadWithStructParameters() {
		// Mock WSDL response
		wireMock.stubFor( get( urlEqualTo( "/calculator.wsdl" ) )
		    .willReturn( aResponse()
		        .withStatus( 200 )
		        .withHeader( "Content-Type", "text/xml" )
		        .withBody( getCalculatorWSDL( wireMock.baseUrl() ) ) ) );

		// Mock SOAP service endpoint
		wireMock.stubFor( post( urlEqualTo( "/calculator" ) )
		    .willReturn( aResponse()
		        .withStatus( 200 )
		        .withHeader( "Content-Type", "text/xml; charset=utf-8" )
		        .withBody( getSubtractResponseXML( -5 ) ) ) );

		String baseURL = wireMock.baseUrl();

		// Test creating webservice and calling method with struct parameters
		instance.executeSource(
		    String.format( """
		                   ws = createObject( "webservice", "%s/calculator.wsdl" )
		                   params = { a: 15, b: 20 }
		                   result = ws.subtract( argumentCollection=params )
		                   """, baseURL ),
		    context );

		// Verify the SOAP request was made with correct structure and parameters from struct
		wireMock.verify( postRequestedFor( urlEqualTo( "/calculator" ) )
		    .withHeader( "Content-Type", containing( "text/xml" ) )
		    .withHeader( "SOAPAction", equalTo( "" ) )
		    .withRequestBody( containing( "<soap:Envelope" ) )
		    .withRequestBody( containing( "xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"" ) )
		    .withRequestBody( containing( "<soap:Body>" ) )
		    .withRequestBody( containing( "<subtract" ) )
		    .withRequestBody( containing( "<a xsi:type=\"xsd:double\">15</a>" ) )
		    .withRequestBody( containing( "<b xsi:type=\"xsd:double\">20</b>" ) )
		    .withRequestBody( containing( "</soap:Body>" ) )
		    .withRequestBody( containing( "</soap:Envelope>" ) ) );

		assertThat( variables.get( result ) ).isEqualTo( -5 );
	}

	@DisplayName( "Test SOAP 1.2 request payload format" )
	@Test
	void testSOAP12PayloadFormat() {
		// Mock WSDL response with SOAP 1.2 binding
		wireMock.stubFor( get( urlEqualTo( "/calculator12.wsdl" ) )
		    .willReturn( aResponse()
		        .withStatus( 200 )
		        .withHeader( "Content-Type", "text/xml" )
		        .withBody( getCalculatorWSDLSoap12( wireMock.baseUrl() ) ) ) );

		// Mock SOAP 1.2 service endpoint
		wireMock.stubFor( post( urlEqualTo( "/calculator12" ) )
		    .willReturn( aResponse()
		        .withStatus( 200 )
		        .withHeader( "Content-Type", "application/soap+xml; charset=utf-8" )
		        .withBody( getAddResponseXMLSoap12( 25 ) ) ) );

		String baseURL = wireMock.baseUrl();

		// Test creating webservice and calling method with SOAP 1.2
		instance.executeSource(
		    String.format( """
		                   ws = createObject( "webservice", "%s/calculator12.wsdl" )
		                   result = ws.add( 5, 20 )
		                   """, baseURL ),
		    context );

		// Verify SOAP 1.2 request format
		wireMock.verify( postRequestedFor( urlEqualTo( "/calculator12" ) )
		    .withHeader( "Content-Type", containing( "application/soap+xml" ) )
		    .withRequestBody( containing( "<soap:Envelope" ) )
		    .withRequestBody( containing( "xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\"" ) )
		    .withRequestBody( containing( "<soap:Body>" ) )
		    .withRequestBody( containing( "<add" ) )
		    .withRequestBody( containing( "<a xsi:type=\"xsd:double\">5</a>" ) )
		    .withRequestBody( containing( "<b xsi:type=\"xsd:double\">20</b>" ) )
		    .withRequestBody( containing( "</soap:Body>" ) )
		    .withRequestBody( containing( "</soap:Envelope>" ) ) );

		assertThat( variables.get( result ) ).isEqualTo( 25 );
	}

	@DisplayName( "Test SOAP request with custom headers and authentication" )
	@Test
	void testSOAPPayloadWithCustomHeaders() {
		// Mock WSDL response with authentication
		wireMock.stubFor( get( urlEqualTo( "/secure-calculator.wsdl" ) )
		    .willReturn( aResponse()
		        .withStatus( 200 )
		        .withHeader( "Content-Type", "text/xml" )
		        .withBody( getCalculatorWSDL( wireMock.baseUrl() ).replace( "/calculator", "/secure-calculator" ) ) ) );

		// Mock SOAP service endpoint with authentication
		wireMock.stubFor( post( urlEqualTo( "/secure-calculator" ) )
		    .withBasicAuth( "testuser", "testpass" )
		    .withHeader( "X-Custom-Header", equalTo( "test-value" ) )
		    .willReturn( aResponse()
		        .withStatus( 200 )
		        .withHeader( "Content-Type", "text/xml; charset=utf-8" )
		        .withBody( getAddResponseXML( 100 ) ) ) );

		String baseURL = wireMock.baseUrl();

		// Test creating webservice with authentication and custom headers
		instance.executeSource(
		    String.format( """
		                   ws = createObject( "webservice", "%s/secure-calculator.wsdl" )
		                       .withBasicAuth( "testuser", "testpass" )
		                       .header( "X-Custom-Header", "test-value" )
		                   result = ws.add( 40, 60 )
		                   """, baseURL ),
		    context );

		// Verify the SOAP request included authentication and custom headers
		wireMock.verify( postRequestedFor( urlEqualTo( "/secure-calculator" ) )
		    .withHeader( "Authorization", containing( "Basic" ) )
		    .withHeader( "X-Custom-Header", equalTo( "test-value" ) )
		    .withHeader( "Content-Type", containing( "text/xml" ) )
		    .withHeader( "SOAPAction", equalTo( "" ) )
		    .withRequestBody( containing( "<soap:Envelope" ) )
		    .withRequestBody( containing( "<add" ) )
		    .withRequestBody( containing( "<a xsi:type=\"xsd:double\">40</a>" ) )
		    .withRequestBody( containing( "<b xsi:type=\"xsd:double\">60</b>" ) ) );

		assertThat( variables.get( result ) ).isEqualTo( 100 );
	}

	/**
	 * Sample Calculator WSDL for testing - Axis format
	 */
	private String getCalculatorWSDL( String baseURL ) {
		return """
		       <?xml version="1.0" encoding="UTF-8" standalone="no"?>
		       <wsdl:definitions xmlns:apachesoap="http://xml.apache.org/xml-soap"
		           xmlns:impl="http://local.soaptest/Calculator.cfc"
		           xmlns:intf="http://local.soaptest/Calculator.cfc"
		           xmlns:soapenc="http://schemas.xmlsoap.org/soap/encoding/"
		           xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/"
		           xmlns:wsdlsoap="http://schemas.xmlsoap.org/wsdl/soap/"
		           xmlns:xsd="http://www.w3.org/2001/XMLSchema"
		           targetNamespace="http://local.soaptest/Calculator.cfc">
		           <wsdl:types>
		               <schema xmlns="http://www.w3.org/2001/XMLSchema" targetNamespace="http://xml.apache.org/xml-soap">
		                   <import namespace="http://local.soaptest/Calculator.cfc"/>
		                   <import namespace="http://schemas.xmlsoap.org/soap/encoding/"/>
		                   <complexType name="mapItem">
		                       <sequence>
		                           <element name="key" nillable="true" type="xsd:anyType"/>
		                           <element name="value" nillable="true" type="xsd:anyType"/>
		                       </sequence>
		                   </complexType>
		                   <complexType name="Map">
		                       <sequence>
		                           <element maxOccurs="unbounded" minOccurs="0" name="item" type="apachesoap:mapItem"/>
		                       </sequence>
		                   </complexType>
		               </schema>
		               <schema xmlns="http://www.w3.org/2001/XMLSchema" targetNamespace="http://local.soaptest/Calculator.cfc">
		                   <import namespace="http://xml.apache.org/xml-soap"/>
		                   <import namespace="http://schemas.xmlsoap.org/soap/encoding/"/>
		                   <complexType name="ArrayOf_xsd_anyType">
		                       <complexContent>
		                           <restriction base="soapenc:Array">
		                               <attribute ref="soapenc:arrayType" wsdl:arrayType="xsd:anyType[]"/>
		                           </restriction>
		                       </complexContent>
		                   </complexType>
		               </schema>
		           </wsdl:types>
		           <wsdl:message name="echoRequest">
		               <wsdl:part name="message" type="xsd:string"/>
		           </wsdl:message>
		           <wsdl:message name="echoResponse">
		               <wsdl:part name="echoReturn" type="xsd:string"/>
		           </wsdl:message>
		           <wsdl:message name="divideRequest">
		               <wsdl:part name="a" type="xsd:double"/>
		               <wsdl:part name="b" type="xsd:double"/>
		           </wsdl:message>
		           <wsdl:message name="divideResponse">
		               <wsdl:part name="divideReturn" type="xsd:double"/>
		           </wsdl:message>
		           <wsdl:message name="addRequest">
		               <wsdl:part name="a" type="xsd:double"/>
		               <wsdl:part name="b" type="xsd:double"/>
		           </wsdl:message>
		           <wsdl:message name="addResponse">
		               <wsdl:part name="addReturn" type="xsd:double"/>
		           </wsdl:message>
		           <wsdl:message name="subtractRequest">
		               <wsdl:part name="a" type="xsd:double"/>
		               <wsdl:part name="b" type="xsd:double"/>
		           </wsdl:message>
		           <wsdl:message name="subtractResponse">
		               <wsdl:part name="subtractReturn" type="xsd:double"/>
		           </wsdl:message>
		           <wsdl:message name="getUserRequest">
		               <wsdl:part name="userId" type="xsd:double"/>
		           </wsdl:message>
		           <wsdl:message name="getUserResponse">
		               <wsdl:part name="getUserReturn" type="apachesoap:Map"/>
		           </wsdl:message>
		           <wsdl:message name="getUsersRequest">
		               <wsdl:part name="limit" type="xsd:double"/>
		           </wsdl:message>
		           <wsdl:message name="getUsersResponse">
		               <wsdl:part name="getUsersReturn" type="impl:ArrayOf_xsd_anyType"/>
		           </wsdl:message>
		           <wsdl:portType name="Calculator_wrap">
		               <wsdl:operation name="echo" parameterOrder="message">
		                   <wsdl:input message="impl:echoRequest" name="echoRequest"/>
		                   <wsdl:output message="impl:echoResponse" name="echoResponse"/>
		               </wsdl:operation>
		               <wsdl:operation name="divide" parameterOrder="a b">
		                   <wsdl:input message="impl:divideRequest" name="divideRequest"/>
		                   <wsdl:output message="impl:divideResponse" name="divideResponse"/>
		               </wsdl:operation>
		               <wsdl:operation name="add" parameterOrder="a b">
		                   <wsdl:input message="impl:addRequest" name="addRequest"/>
		                   <wsdl:output message="impl:addResponse" name="addResponse"/>
		               </wsdl:operation>
		               <wsdl:operation name="subtract" parameterOrder="a b">
		                   <wsdl:input message="impl:subtractRequest" name="subtractRequest"/>
		                   <wsdl:output message="impl:subtractResponse" name="subtractResponse"/>
		               </wsdl:operation>
		               <wsdl:operation name="getUser" parameterOrder="userId">
		                   <wsdl:input message="impl:getUserRequest" name="getUserRequest"/>
		                   <wsdl:output message="impl:getUserResponse" name="getUserResponse"/>
		               </wsdl:operation>
		               <wsdl:operation name="getUsers" parameterOrder="limit">
		                   <wsdl:input message="impl:getUsersRequest" name="getUsersRequest"/>
		                   <wsdl:output message="impl:getUsersResponse" name="getUsersResponse"/>
		               </wsdl:operation>
		           </wsdl:portType>
		           <wsdl:binding name="calculator.cfcSoapBinding" type="impl:Calculator_wrap">
		               <wsdlsoap:binding style="rpc" transport="http://schemas.xmlsoap.org/soap/http"/>
		               <wsdl:operation name="echo">
		                   <wsdlsoap:operation soapAction=""/>
		                   <wsdl:input name="echoRequest">
		                       <wsdlsoap:body encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" namespace="http://DefaultNamespace" use="encoded"/>
		                   </wsdl:input>
		                   <wsdl:output name="echoResponse">
		                       <wsdlsoap:body encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" namespace="http://local.soaptest/Calculator.cfc" use="encoded"/>
		                   </wsdl:output>
		               </wsdl:operation>
		               <wsdl:operation name="divide">
		                   <wsdlsoap:operation soapAction=""/>
		                   <wsdl:input name="divideRequest">
		                       <wsdlsoap:body encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" namespace="http://DefaultNamespace" use="encoded"/>
		                   </wsdl:input>
		                   <wsdl:output name="divideResponse">
		                       <wsdlsoap:body encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" namespace="http://local.soaptest/Calculator.cfc" use="encoded"/>
		                   </wsdl:output>
		               </wsdl:operation>
		               <wsdl:operation name="add">
		                   <wsdlsoap:operation soapAction=""/>
		                   <wsdl:input name="addRequest">
		                       <wsdlsoap:body encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" namespace="http://DefaultNamespace" use="encoded"/>
		                   </wsdl:input>
		                   <wsdl:output name="addResponse">
		                       <wsdlsoap:body encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" namespace="http://local.soaptest/Calculator.cfc" use="encoded"/>
		                   </wsdl:output>
		               </wsdl:operation>
		               <wsdl:operation name="subtract">
		                   <wsdlsoap:operation soapAction=""/>
		                   <wsdl:input name="subtractRequest">
		                       <wsdlsoap:body encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" namespace="http://DefaultNamespace" use="encoded"/>
		                   </wsdl:input>
		                   <wsdl:output name="subtractResponse">
		                       <wsdlsoap:body encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" namespace="http://local.soaptest/Calculator.cfc" use="encoded"/>
		                   </wsdl:output>
		               </wsdl:operation>
		               <wsdl:operation name="getUser">
		                   <wsdlsoap:operation soapAction=""/>
		                   <wsdl:input name="getUserRequest">
		                       <wsdlsoap:body encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" namespace="http://DefaultNamespace" use="encoded"/>
		                   </wsdl:input>
		                   <wsdl:output name="getUserResponse">
		                       <wsdlsoap:body encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" namespace="http://local.soaptest/Calculator.cfc" use="encoded"/>
		                   </wsdl:output>
		               </wsdl:operation>
		               <wsdl:operation name="getUsers">
		                   <wsdlsoap:operation soapAction=""/>
		                   <wsdl:input name="getUsersRequest">
		                       <wsdlsoap:body encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" namespace="http://DefaultNamespace" use="encoded"/>
		                   </wsdl:input>
		                   <wsdl:output name="getUsersResponse">
		                       <wsdlsoap:body encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" namespace="http://local.soaptest/Calculator.cfc" use="encoded"/>
		                   </wsdl:output>
		               </wsdl:operation>
		           </wsdl:binding>
		           <wsdl:service name="Calculator_wrapService">
		               <wsdl:port binding="impl:calculator.cfcSoapBinding" name="calculator.cfc">
		                   <wsdlsoap:address location="%s/calculator"/>
		               </wsdl:port>
		           </wsdl:service>
		       </wsdl:definitions>
		       """
		    .replace( "%s", baseURL );
	}

	/**
	 * Sample Calculator WSDL for SOAP 1.2 testing
	 */
	private String getCalculatorWSDLSoap12( String baseURL ) {
		String wsdl = getCalculatorWSDL( baseURL )
		    // Add SOAP 1.2 namespace declaration
		    .replace(
		        "xmlns:wsdlsoap=\"http://schemas.xmlsoap.org/wsdl/soap/\"",
		        "xmlns:wsdlsoap=\"http://schemas.xmlsoap.org/wsdl/soap/\" xmlns:soap12=\"http://schemas.xmlsoap.org/wsdl/soap12/\""
		    )
		    // Replace SOAP 1.1 binding with SOAP 1.2 binding
		    .replace(
		        "<wsdlsoap:binding",
		        "<soap12:binding"
		    )
		    // Replace SOAP operations
		    .replace(
		        "<wsdlsoap:operation",
		        "<soap12:operation"
		    )
		    // Replace SOAP body elements
		    .replace(
		        "<wsdlsoap:body",
		        "<soap12:body"
		    )
		    // Replace SOAP address
		    .replace(
		        "<wsdlsoap:address location=\"",
		        "<soap12:address location=\""
		    )
		    // Update endpoint URLs
		    .replace( "/calculator", "/calculator12" );

		return wsdl;
	}

	/**
	 * Sample SOAP response for Add operation - Axis format
	 */
	private String getAddResponseXML( int result ) {
		return String.format(
		    """
		    <?xml version="1.0" encoding="utf-8"?>
		    <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
		        <soapenv:Body>
		            <ns1:addResponse soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" xmlns:ns1="http://local.soaptest/Calculator.cfc">
		                <addReturn xsi:type="xsd:double">%d</addReturn>
		            </ns1:addResponse>
		        </soapenv:Body>
		    </soapenv:Envelope>
		    """,
		    result );
	}

	/**
	 * Sample SOAP response for Echo operation - Axis format
	 */
	private String getEchoResponseXML( String message ) {
		return String.format(
		    """
		    <?xml version="1.0" encoding="utf-8"?>
		    <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
		        <soapenv:Body>
		            <ns1:echoResponse soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" xmlns:ns1="http://local.soaptest/Calculator.cfc">
		                <echoReturn xsi:type="xsd:string">%s</echoReturn>
		            </ns1:echoResponse>
		        </soapenv:Body>
		    </soapenv:Envelope>
		    """,
		    message );
	}

	/**
	 * Sample SOAP 1.2 response for Add operation - Axis format
	 */
	private String getAddResponseXMLSoap12( int result ) {
		return String.format(
		    """
		    <?xml version="1.0" encoding="utf-8"?>
		    <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
		        <soap:Body>
		            <ns1:addResponse soap:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" xmlns:ns1="http://local.soaptest/Calculator.cfc">
		                <addReturn xsi:type="xsd:double">%d</addReturn>
		            </ns1:addResponse>
		        </soap:Body>
		    </soap:Envelope>
		    """,
		    result );
	}

	/**
	 * Sample SOAP response for Subtract operation - Axis format
	 */
	private String getSubtractResponseXML( int result ) {
		return String.format(
		    """
		    <?xml version="1.0" encoding="utf-8"?>
		    <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
		        <soapenv:Body>
		            <ns1:subtractResponse soapenv:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" xmlns:ns1="http://local.soaptest/Calculator.cfc">
		                <subtractReturn xsi:type="xsd:double">%d</subtractReturn>
		            </ns1:subtractResponse>
		        </soapenv:Body>
		    </soapenv:Envelope>
		    """,
		    result );
	}
}