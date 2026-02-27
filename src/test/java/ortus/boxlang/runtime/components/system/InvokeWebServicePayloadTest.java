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
package ortus.boxlang.runtime.components.system;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;

/**
 * Test the Invoke component with webservice functionality using WireMock to validate SOAP payloads
 */
public class InvokeWebServicePayloadTest {

	static BoxRuntime			instance			= BoxRuntime.getInstance( true );
	IBoxContext					context;
	IScope						variables;
	static Key					result				= new Key( "result" );

	// Mock Calculator WSDL content for testing - Axis format
	private static final String	CALCULATOR_WSDL		= """
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
	                                                      <wsdl:message name="addRequest">
	                                                          <wsdl:part name="a" type="xsd:double"/>
	                                                          <wsdl:part name="b" type="xsd:double"/>
	                                                      </wsdl:message>
	                                                      <wsdl:message name="addResponse">
	                                                          <wsdl:part name="addReturn" type="xsd:double"/>
	                                                      </wsdl:message>
	                                                      <wsdl:message name="divideRequest">
	                                                          <wsdl:part name="intA" type="xsd:double"/>
	                                                          <wsdl:part name="intB" type="xsd:double"/>
	                                                      </wsdl:message>
	                                                      <wsdl:message name="divideResponse">
	                                                          <wsdl:part name="divideReturn" type="xsd:double"/>
	                                                      </wsdl:message>
	                                                      <wsdl:portType name="Calculator_wrap">
	                                                          <wsdl:operation name="add" parameterOrder="intA intB">
	                                                              <wsdl:input message="impl:addRequest" name="addRequest"/>
	                                                              <wsdl:output message="impl:addResponse" name="addResponse"/>
	                                                          </wsdl:operation>
	                                                          <wsdl:operation name="divide" parameterOrder="intA intB">
	                                                              <wsdl:input message="impl:divideRequest" name="divideRequest"/>
	                                                              <wsdl:output message="impl:divideResponse" name="divideResponse"/>
	                                                          </wsdl:operation>
	                                                      </wsdl:portType>
	                                                      <wsdl:binding name="calculator.cfcSoapBinding" type="impl:Calculator_wrap">
	                                                          <wsdlsoap:binding style="rpc" transport="http://schemas.xmlsoap.org/soap/http"/>
	                                                          <wsdl:operation name="add">
	                                                              <wsdlsoap:operation soapAction=""/>
	                                                              <wsdl:input name="addRequest">
	                                                                  <wsdlsoap:body encodingStyle="http://schemas.xmlsoap.org/soap/encoding/" namespace="http://DefaultNamespace" use="encoded"/>
	                                                              </wsdl:input>
	                                                              <wsdl:output name="addResponse">
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
	                                                      </wsdl:binding>
	                                                      <wsdl:service name="Calculator_wrapService">
	                                                          <wsdl:port binding="impl:calculator.cfcSoapBinding" name="calculator.cfc">
	                                                              <wsdlsoap:address location="http://localhost:%d/calculator"/>
	                                                          </wsdl:port>
	                                                      </wsdl:service>
	                                                  </wsdl:definitions>
	                                                  """;

	// ObjectUpload WSDL with Apache SOAP Map and base64Binary support
	private static final String	OBJECT_UPLOAD_WSDL	= """
	                                                  <?xml version="1.0" encoding="UTF-8"?>
	                                                  <wsdl:definitions targetNamespace="http://ws.elend" xmlns:apachesoap="http://xml.apache.org/xml-soap"
	                                                      xmlns:impl="http://ws.elend" xmlns:intf="http://ws.elend" xmlns:tns1="http://rpc.xml.coldfusion"
	                                                      xmlns:wsdl="http://schemas.xmlsoap.org/wsdl/" xmlns:wsdlsoap="http://schemas.xmlsoap.org/wsdl/soap/"
	                                                      xmlns:xsd="http://www.w3.org/2001/XMLSchema">
	                                                      <wsdl:types>
	                                                          <schema elementFormDefault="qualified" targetNamespace="http://ws.elend" xmlns="http://www.w3.org/2001/XMLSchema">
	                                                              <import namespace="http://xml.apache.org/xml-soap"/>
	                                                              <import namespace="http://rpc.xml.coldfusion"/>
	                                                              <element name="SharedEntryPoint">
	                                                                  <complexType>
	                                                                      <sequence>
	                                                                          <element name="Inputs" type="apachesoap:Map"/>
	                                                                          <element name="InputFile" type="xsd:base64Binary"/>
	                                                                      </sequence>
	                                                                  </complexType>
	                                                              </element>
	                                                              <element name="SharedEntryPointResponse">
	                                                                  <complexType>
	                                                                      <sequence>
	                                                                          <element name="SharedEntryPointReturn" type="apachesoap:Map"/>
	                                                                      </sequence>
	                                                                  </complexType>
	                                                              </element>
	                                                              <element name="fault" type="tns1:CFCInvocationException"/>
	                                                          </schema>
	                                                          <schema elementFormDefault="qualified" targetNamespace="http://xml.apache.org/xml-soap" xmlns="http://www.w3.org/2001/XMLSchema">
	                                                              <import namespace="http://rpc.xml.coldfusion"/>
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
	                                                          <schema elementFormDefault="qualified" targetNamespace="http://rpc.xml.coldfusion" xmlns="http://www.w3.org/2001/XMLSchema">
	                                                              <import namespace="http://xml.apache.org/xml-soap"/>
	                                                              <complexType name="CFCInvocationException">
	                                                                  <sequence/>
	                                                              </complexType>
	                                                          </schema>
	                                                      </wsdl:types>
	                                                      <wsdl:message name="CFCInvocationException">
	                                                          <wsdl:part element="impl:fault" name="fault"/>
	                                                      </wsdl:message>
	                                                      <wsdl:message name="SharedEntryPointRequest">
	                                                          <wsdl:part element="impl:SharedEntryPoint" name="parameters"/>
	                                                      </wsdl:message>
	                                                      <wsdl:message name="SharedEntryPointResponse">
	                                                          <wsdl:part element="impl:SharedEntryPointResponse" name="parameters"/>
	                                                      </wsdl:message>
	                                                      <wsdl:portType name="objectUpload">
	                                                          <wsdl:operation name="SharedEntryPoint">
	                                                              <wsdl:input message="impl:SharedEntryPointRequest" name="SharedEntryPointRequest"/>
	                                                              <wsdl:output message="impl:SharedEntryPointResponse" name="SharedEntryPointResponse"/>
	                                                              <wsdl:fault message="impl:CFCInvocationException" name="CFCInvocationException"/>
	                                                          </wsdl:operation>
	                                                      </wsdl:portType>
	                                                      <wsdl:binding name="objectUpload.cfcSoapBinding" type="impl:objectUpload">
	                                                          <wsdlsoap:binding style="document" transport="http://schemas.xmlsoap.org/soap/http"/>
	                                                          <wsdl:operation name="SharedEntryPoint">
	                                                              <wsdlsoap:operation soapAction=""/>
	                                                              <wsdl:input name="SharedEntryPointRequest">
	                                                                  <wsdlsoap:body use="literal"/>
	                                                              </wsdl:input>
	                                                              <wsdl:output name="SharedEntryPointResponse">
	                                                                  <wsdlsoap:body use="literal"/>
	                                                              </wsdl:output>
	                                                              <wsdl:fault name="CFCInvocationException">
	                                                                  <wsdlsoap:fault name="CFCInvocationException" use="literal"/>
	                                                              </wsdl:fault>
	                                                          </wsdl:operation>
	                                                      </wsdl:binding>
	                                                      <wsdl:service name="File Upload Web Services">
	                                                          <wsdl:port binding="impl:objectUpload.cfcSoapBinding" name="objectUpload.cfc">
	                                                              <wsdlsoap:address location="http://localhost:%d/objectUpload"/>
	                                                          </wsdl:port>
	                                                      </wsdl:service>
	                                                  </wsdl:definitions>
	                                                  """;

	@RegisterExtension
	static WireMockExtension	wireMock			= WireMockExtension.newInstance()
	    .options( wireMockConfig().dynamicPort() )
	    .build();

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );

		// Reset WireMock for each test
		wireMock.resetAll();

		// Set up common WSDL endpoint
		wireMock.stubFor( get( urlEqualTo( "/calculator?wsdl" ) )
		    .willReturn( aResponse()
		        .withStatus( 200 )
		        .withHeader( "Content-Type", "text/xml" )
		        .withBody( String.format( CALCULATOR_WSDL, wireMock.getPort() ) ) ) );
	}

	@DisplayName( "Test invoke webservice SOAP payload without parameters" )
	@Test
	public void testInvokeWebServiceSOAPPayloadParameters() {
		// Mock the SOAP operation response
		wireMock.stubFor( post( urlEqualTo( "/calculator" ) )
		    .willReturn( aResponse()
		        .withStatus( 200 )
		        .withHeader( "Content-Type", "text/xml" )
		        .withBody( """
		                   <?xml version="1.0" encoding="UTF-8"?>
		                   <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
		                       <soap:Body>
		                           <tns:AddResponse xmlns:tns="http://calculator.example.com/">
		                               <tns:AddResult>30</tns:AddResult>
		                           </tns:AddResponse>
		                       </soap:Body>
		                   </soap:Envelope>
		                   """ ) ) );

		String wsdlUrl = "http://localhost:" + wireMock.getPort() + "/calculator?wsdl";

		// Execute invoke webservice call
		instance.executeSource(
		    String.format( """
		                   bx:invoke
		                       webservice="%s"
		                       method="Add"
		                       argumentCollection={ intA : 10, intB : 20 }
		                       returnVariable="result";
		                   """, wsdlUrl ),
		    context );

		// Verify the result
		assertThat( variables.get( result ) ).isEqualTo( 30 );

		// Verify the SOAP request was made correctly
		RequestPatternBuilder requestPattern = postRequestedFor( urlEqualTo( "/calculator" ) )
		    .withHeader( "Content-Type", matching( "text/xml.*" ) )
		    .withHeader( "SOAPAction", equalTo( "" ) );

		wireMock.verify( requestPattern );

		// Verify the SOAP envelope contains the expected XML structure
		wireMock.verify( postRequestedFor( urlEqualTo( "/calculator" ) )
		    .withRequestBody( containing( "<soap:Envelope" ) )
		    .withRequestBody( containing( "<soap:Body>" ) )
		    .withRequestBody( containing( "<add" ) )
		    .withRequestBody( containing( "<a xsi:type=\"xsd:double\">10</a>" ) )
		    .withRequestBody( containing( "<b xsi:type=\"xsd:double\">20</b>" ) ) );
	}

	@DisplayName( "Test invoke webservice SOAP payload with struct parameters" )
	@Test
	public void testInvokeWebServiceSOAPPayloadWithStructParameters() {
		// Mock the SOAP operation response
		wireMock.stubFor( post( urlEqualTo( "/calculator" ) )
		    .willReturn( aResponse()
		        .withStatus( 200 )
		        .withHeader( "Content-Type", "text/xml" )
		        .withBody( """
		                   <?xml version="1.0" encoding="UTF-8"?>
		                   <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
		                       <soap:Body>
		                           <tns:DivideResponse xmlns:tns="http://calculator.example.com/">
		                               <tns:DivideResult>2.5</tns:DivideResult>
		                           </tns:DivideResponse>
		                       </soap:Body>
		                   </soap:Envelope>
		                   """ ) ) );

		String wsdlUrl = "http://localhost:" + wireMock.getPort() + "/calculator?wsdl";

		// Execute invoke webservice call with struct parameters
		instance.executeSource(
		    String.format( """
		                   params = { intA : 10, intB : 4 };
		                   bx:invoke
		                       webservice="%s"
		                       method="divide"
		                       argumentCollection=params
		                       returnVariable="result";
		                   """, wsdlUrl ),
		    context );

		// Verify the result
		assertThat( variables.get( result ) ).isEqualTo( 2.5 );
		// Verify the SOAP request was made correctly
		wireMock.verify( postRequestedFor( urlEqualTo( "/calculator" ) )
		    .withHeader( "Content-Type", matching( "text/xml.*" ) )
		    .withHeader( "SOAPAction", equalTo( "" ) )
		    .withRequestBody( containing( "<divide" ) )
		    .withRequestBody( containing( "<intA xsi:type=\"xsd:double\">10</intA>" ) )
		    .withRequestBody( containing( "<intB xsi:type=\"xsd:double\">4</intB>" ) ) );
	}

	@DisplayName( "Test invoke webservice SOAP 1.2 format" )
	@Test
	public void testInvokeWebServiceSOAP12PayloadFormat() {
		// Mock the SOAP 1.2 operation response
		wireMock.stubFor( post( urlEqualTo( "/calculator" ) )
		    .willReturn( aResponse()
		        .withStatus( 200 )
		        .withHeader( "Content-Type", "application/soap+xml" )
		        .withBody( """
		                   <?xml version="1.0" encoding="UTF-8"?>
		                   <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope">
		                       <soap:Body>
		                           <tns:AddResponse xmlns:tns="http://calculator.example.com/">
		                               <tns:AddResult>100</tns:AddResult>
		                           </tns:AddResponse>
		                       </soap:Body>
		                   </soap:Envelope>
		                   """ ) ) );

		String wsdlUrl = "http://localhost:" + wireMock.getPort() + "/calculator?wsdl";

		// Execute invoke webservice call - BoxLang should auto-detect SOAP version from WSDL
		instance.executeSource(
		    String.format( """
		                   <bx:set params = { intA : 50, intB : 50 } />
		                   <bx:invoke
		                       webservice="%s"
		                       method="Add"
		                       argumentCollection="#params#"
		                       returnVariable="result" />
		                   """, wsdlUrl ),
		    context, BoxSourceType.BOXTEMPLATE );

		// Verify the result
		assertThat( variables.get( result ) ).isEqualTo( 100 );

		// Verify SOAP request structure (SOAP 1.1 or 1.2 format)
		wireMock.verify( postRequestedFor( urlEqualTo( "/calculator" ) )
		    .withRequestBody( containing( "soap:Envelope" ) )
		    .withRequestBody( containing( "soap:Body" ) )
		    .withRequestBody( containing( "<add" ) ) );
	}

	@DisplayName( "Test invoke webservice with custom attributes" )
	@Test
	public void testInvokeWebServiceWithCustomAttributes() {
		// Mock the SOAP operation response
		wireMock.stubFor( post( urlEqualTo( "/secure-calculator" ) )
		    .willReturn( aResponse()
		        .withStatus( 200 )
		        .withHeader( "Content-Type", "text/xml" )
		        .withBody( """
		                   <?xml version="1.0" encoding="UTF-8"?>
		                   <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
		                       <soap:Body>
		                           <tns:AddResponse xmlns:tns="http://calculator.example.com/">
		                               <tns:AddResult>75</tns:AddResult>
		                           </tns:AddResponse>
		                       </soap:Body>
		                   </soap:Envelope>
		                   """ ) ) );

		// Set up secure calculator WSDL endpoint
		wireMock.stubFor( get( urlEqualTo( "/secure-calculator?wsdl" ) )
		    .willReturn( aResponse()
		        .withStatus( 200 )
		        .withHeader( "Content-Type", "text/xml" )
		        .withBody( String.format( CALCULATOR_WSDL.replace( "/calculator", "/secure-calculator" ), wireMock.getPort() ) ) ) );

		String secureWsdlUrl = "http://localhost:" + wireMock.getPort() + "/secure-calculator?wsdl";

		// Execute invoke webservice call with authentication
		instance.executeSource(
		    String.format( """
		                   <bx:set authParams = { intA : 25, intB : 50 } />
		                   <bx:invoke
		                       webservice="%s"
		                       method="Add"
		                       argumentCollection="#authParams#"
		                       username="admin"
		                       password="secret"
		                       timeout="30"
		                       returnVariable="result" />
		                   """, secureWsdlUrl ),
		    context, BoxSourceType.BOXTEMPLATE );

		// Verify the result
		assertThat( variables.get( result ) ).isEqualTo( 75 );

		// Verify the SOAP request includes authentication
		wireMock.verify( postRequestedFor( urlEqualTo( "/secure-calculator" ) )
		    .withHeader( "Authorization", containing( "Basic" ) ) );
	}

	@DisplayName( "Test invoke webservice returning complex structure" )
	@Test
	public void testInvokeWebServiceComplexStructure() {
		// Mock a complex SOAP response
		wireMock.stubFor( post( urlEqualTo( "/calculator" ) )
		    .willReturn( aResponse()
		        .withStatus( 200 )
		        .withHeader( "Content-Type", "text/xml" )
		        .withBody( """
		                   <?xml version="1.0" encoding="UTF-8"?>
		                   <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
		                       <soap:Body>
		                           <tns:ComplexResponse xmlns:tns="http://calculator.example.com/">
		                               <tns:Result>
		                                   <tns:Value>42</tns:Value>
		                                   <tns:Status>Success</tns:Status>
		                                   <tns:Details>
		                                       <tns:Operation>Addition</tns:Operation>
		                                       <tns:Timestamp>2026-02-16T14:30:00Z</tns:Timestamp>
		                                   </tns:Details>
		                               </tns:Result>
		                           </tns:ComplexResponse>
		                       </soap:Body>
		                   </soap:Envelope>
		                   """ ) ) );

		String wsdlUrl = "http://localhost:" + wireMock.getPort() + "/calculator?wsdl";

		// Execute invoke webservice call
		instance.executeSource(
		    String.format( """
		                   bx:invoke
		                       webservice="%s"
		                       method="Add"
		                       argumentCollection={ intA : 20, intB : 22 }
		                       returnVariable="result";
		                   """, wsdlUrl ),
		    context );

		// Verify the result is a complex structure
		IStruct resultStruct = variables.getAsStruct( result );
		assertThat( resultStruct ).isNotNull();

		// The exact structure depends on how BoxLang parses the SOAP response
		// This test validates that complex XML structures are handled properly
		wireMock.verify( postRequestedFor( urlEqualTo( "/calculator" ) )
		    .withRequestBody( containing( "<soap:Envelope" ) )
		    .withRequestBody( containing( "<add" ) ) );
	}

	@DisplayName( "Test invoke webservice returning array structure" )
	@Test
	public void testInvokeWebServiceArrayStructure() {
		// Mock a SOAP response with complex nested structure using the Divide operation
		wireMock.stubFor( post( urlEqualTo( "/calculator" ) )
		    .willReturn( aResponse()
		        .withStatus( 200 )
		        .withHeader( "Content-Type", "text/xml" )
		        .withBody( """
		                   <?xml version="1.0" encoding="UTF-8"?>
		                   <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
		                       <soap:Body>
		                           <tns:DivideResponse xmlns:tns="http://calculator.example.com/">
		                               <tns:DivideResult>
		                                   <tns:QuotientResult>5.0</tns:QuotientResult>
		                                   <tns:RemainderResult>0</tns:RemainderResult>
		                                   <tns:Items>
		                                       <tns:Item>
		                                           <tns:Id>1</tns:Id>
		                                           <tns:Value>First</tns:Value>
		                                       </tns:Item>
		                                       <tns:Item>
		                                           <tns:Id>2</tns:Id>
		                                           <tns:Value>Second</tns:Value>
		                                       </tns:Item>
		                                   </tns:Items>
		                               </tns:DivideResult>
		                           </tns:DivideResponse>
		                       </soap:Body>
		                   </soap:Envelope>
		                   """ ) ) );

		String wsdlUrl = "http://localhost:" + wireMock.getPort() + "/calculator?wsdl";

		// Execute invoke webservice call using valid Divide operation
		instance.executeSource(
		    String.format( """
		                   bx:invoke
		                       webservice="%s"
		                       method="Divide"
		                       argumentCollection={ intA : 10, intB : 2 }
		                       returnVariable="result";
		                   """, wsdlUrl ),
		    context );

		// Verify the request was made with proper SOAP envelope and divide operation
		wireMock.verify( postRequestedFor( urlEqualTo( "/calculator" ) )
		    .withRequestBody( containing( "<soap:Envelope" ) )
		    .withRequestBody( containing( "<divide" ) )
		    .withRequestBody( containing( "<intA xsi:type=\"xsd:double\">10</intA>" ) )
		    .withRequestBody( containing( "<intB xsi:type=\"xsd:double\">2</intB>" ) ) );

		// The result structure should be parsed as a complex structure
		Object resultObj = variables.get( result );
		assertThat( resultObj ).isNotNull();
	}

	@DisplayName( "Test invoke webservice using CF template syntax" )
	@Test
	public void testInvokeWebServiceCFTemplate() {
		// Mock the SOAP operation response
		wireMock.stubFor( post( urlEqualTo( "/calculator" ) )
		    .willReturn( aResponse()
		        .withStatus( 200 )
		        .withHeader( "Content-Type", "text/xml" )
		        .withBody( """
		                   <?xml version="1.0" encoding="UTF-8"?>
		                   <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
		                       <soap:Body>
		                           <tns:AddResponse xmlns:tns="http://calculator.example.com/">
		                               <tns:AddResult>150</tns:AddResult>
		                           </tns:AddResponse>
		                       </soap:Body>
		                   </soap:Envelope>
		                   """ ) ) );

		String wsdlUrl = "http://localhost:" + wireMock.getPort() + "/calculator?wsdl";

		// Execute invoke webservice using CF template syntax
		instance.executeSource(
		    String.format( """
		                   <cfinvoke
		                       webservice="%s"
		                       method="Add"
		                       returnVariable="result">
		                       <cfinvokeargument name="intA" value="75" />
		                       <cfinvokeargument name="intB" value="75" />
		                   </cfinvoke>
		                   """, wsdlUrl ),
		    context, BoxSourceType.CFTEMPLATE );

		// Verify the result
		assertThat( variables.get( result ) ).isEqualTo( 150 );

		// Verify the SOAP request was made correctly
		wireMock.verify( postRequestedFor( urlEqualTo( "/calculator" ) )
		    .withHeader( "SOAPAction", equalTo( "" ) )
		    .withRequestBody( containing( "<a xsi:type=\"xsd:double\">75</a>" ) )
		    .withRequestBody( containing( "<b xsi:type=\"xsd:double\">75</b>" ) ) );
	}

	@DisplayName( "Test invoke webservice with mixed argument types" )
	@Test
	public void testInvokeWebServiceMixedArgumentTypes() {
		// Mock the SOAP operation response
		wireMock.stubFor( post( urlEqualTo( "/calculator" ) )
		    .willReturn( aResponse()
		        .withStatus( 200 )
		        .withHeader( "Content-Type", "text/xml" )
		        .withBody( """
		                   <?xml version="1.0" encoding="UTF-8"?>
		                   <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
		                       <soap:Body>
		                           <tns:AddResponse xmlns:tns="http://calculator.example.com/">
		                               <tns:AddResult>200</tns:AddResult>
		                           </tns:AddResponse>
		                       </soap:Body>
		                   </soap:Envelope>
		                   """ ) ) );

		String wsdlUrl = "http://localhost:" + wireMock.getPort() + "/calculator?wsdl";

		// Set up variables to pass to the template
		context.getScopeNearby( VariablesScope.name ).put( "intAValue", 100 );
		context.getScopeNearby( VariablesScope.name ).put( "intBValue", 100 );

		// Execute invoke webservice using BoxLang template with mixed arguments
		instance.executeSource(
		    String.format( """
		                   <bx:invoke
		                       webservice="%s"
		                       method="Add"
		                       returnVariable="result">
		                       <bx:invokeArgument name="intA" value="#variables.intAValue#" />
		                       <bx:invokeArgument name="intB" value="#variables.intBValue#" />
		                   </bx:invoke>
		                   """, wsdlUrl ),
		    context, BoxSourceType.BOXTEMPLATE );

		// Verify the result
		assertThat( variables.get( result ) ).isEqualTo( 200 );

		// Verify the SOAP request contains both arguments
		wireMock.verify( postRequestedFor( urlEqualTo( "/calculator" ) )
		    .withRequestBody( containing( "<a xsi:type=\"xsd:double\">100</a>" ) )
		    .withRequestBody( containing( "<b xsi:type=\"xsd:double\">100</b>" ) ) );
	}

	@DisplayName( "Test invoke webservice with Apache SOAP Map type" )
	@Test
	public void testInvokeWebServiceApacheSoapMap() {
		// Set up objectUpload WSDL endpoint
		wireMock.stubFor( get( urlEqualTo( "/objectUpload?wsdl" ) )
		    .willReturn( aResponse()
		        .withStatus( 200 )
		        .withHeader( "Content-Type", "text/xml" )
		        .withBody( String.format( OBJECT_UPLOAD_WSDL, wireMock.getPort() ) ) ) );

		// Mock the SOAP operation response
		wireMock.stubFor( post( urlEqualTo( "/objectUpload" ) )
		    .willReturn( aResponse()
		        .withStatus( 200 )
		        .withHeader( "Content-Type", "text/xml" )
		        .withBody( """
		                   <?xml version="1.0" encoding="UTF-8"?>
		                   <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
		                       <soap:Body>
		                           <tns:SharedEntryPointResponse xmlns:tns="http://ws.elend">
		                               <tns:SharedEntryPointReturn>
		                                   <item>
		                                       <key>status</key>
		                                       <value>success</value>
		                                   </item>
		                                   <item>
		                                       <key>id</key>
		                                       <value>12345</value>
		                                   </item>
		                               </tns:SharedEntryPointReturn>
		                           </tns:SharedEntryPointResponse>
		                       </soap:Body>
		                   </soap:Envelope>
		                   """ ) ) );

		String wsdlUrl = "http://localhost:" + wireMock.getPort() + "/objectUpload?wsdl";

		// Execute invoke webservice with Map parameter and base64Binary file
		instance.executeSource(
		    String.format( """
		                   inputs = { fileName: "test.txt", fileType: "text/plain", uploadDate: "2026-02-20" };
		                   fileContent = "This is test file content";
		                   bx:invoke
		                       webservice="%s"
		                       method="SharedEntryPoint"
		                       argumentCollection={ Inputs: inputs, InputFile: fileContent }
		                       returnVariable="result";
		                   """, wsdlUrl ),
		    context );

		// Verify the SOAP request contains Apache SOAP Map structure with proper type attributes
		wireMock.verify( postRequestedFor( urlEqualTo( "/objectUpload" ) )
		    .withRequestBody( containing( "<Inputs xsi:type=\"apachesoap:Map\">" ) )
		    .withRequestBody( containing( "<item xsi:type=\"apachesoap:mapItem\">" ) )
		    .withRequestBody( containing( "<key xsi:type=\"xsd:string\">fileName</key>" ) )
		    .withRequestBody( containing( "<value xsi:type=\"xsd:string\">test.txt</value>" ) )
		    .withRequestBody( containing( "<key xsi:type=\"xsd:string\">fileType</key>" ) )
		    .withRequestBody( containing( "<value xsi:type=\"xsd:string\">text/plain</value>" ) )
		    .withRequestBody( containing( "<key xsi:type=\"xsd:string\">uploadDate</key>" ) )
		    .withRequestBody( containing( "<value xsi:type=\"xsd:string\">2026-02-20</value>" ) )
		    .withRequestBody( containing( "</item>" ) )
		    .withRequestBody( containing( "</Inputs>" ) ) );

		// Verify base64Binary encoding
		wireMock.verify( postRequestedFor( urlEqualTo( "/objectUpload" ) )
		    .withRequestBody( containing( "<InputFile" ) ) // Match opening tag (with or without attributes)
		    .withRequestBody( containing( "xsi:type=\"xsd:base64Binary\"" ) ) // Verify type attribute
		    .withRequestBody( containing( "VGhpcyBpcyB0ZXN0IGZpbGUgY29udGVudA==" ) ) // base64 of "This is test file content"
		    .withRequestBody( containing( "</InputFile>" ) ) );

		// Verify the result
		assertThat( variables.get( result ) ).isNotNull();
	}

	@DisplayName( "Test invoke webservice with base64Binary byte array" )
	@Test
	public void testInvokeWebServiceBase64Binary() {
		// Set up objectUpload WSDL endpoint
		wireMock.stubFor( get( urlEqualTo( "/objectUpload?wsdl" ) )
		    .willReturn( aResponse()
		        .withStatus( 200 )
		        .withHeader( "Content-Type", "text/xml" )
		        .withBody( String.format( OBJECT_UPLOAD_WSDL, wireMock.getPort() ) ) ) );

		// Mock the SOAP operation response
		wireMock.stubFor( post( urlEqualTo( "/objectUpload" ) )
		    .willReturn( aResponse()
		        .withStatus( 200 )
		        .withHeader( "Content-Type", "text/xml" )
		        .withBody( """
		                   <?xml version="1.0" encoding="UTF-8"?>
		                   <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
		                       <soap:Body>
		                           <tns:SharedEntryPointResponse xmlns:tns="http://ws.elend">
		                               <tns:SharedEntryPointReturn>
		                                   <item>
		                                       <key>uploaded</key>
		                                       <value>true</value>
		                                   </item>
		                               </tns:SharedEntryPointReturn>
		                           </tns:SharedEntryPointResponse>
		                       </soap:Body>
		                   </soap:Envelope>
		                   """ ) ) );

		String wsdlUrl = "http://localhost:" + wireMock.getPort() + "/objectUpload?wsdl";

		// Execute invoke webservice with binary data
		instance.executeSource(
		    String.format( """
		                   inputs = { action: "upload", documentType: "PDF" };
		                   // Simulate binary file content
		                   binaryData = toBinary( toBase64( "Binary file content here" ) );
		                   bx:invoke
		                       webservice="%s"
		                       method="SharedEntryPoint"
		                       argumentCollection={ Inputs: inputs, InputFile: binaryData }
		                       returnVariable="result";
		                   """, wsdlUrl ),
		    context );

		// Verify Apache SOAP Map structure with proper type attributes
		wireMock.verify( postRequestedFor( urlEqualTo( "/objectUpload" ) )
		    .withRequestBody( containing( "<Inputs xsi:type=\"apachesoap:Map\">" ) )
		    .withRequestBody( containing( "<item xsi:type=\"apachesoap:mapItem\">" ) )
		    .withRequestBody( containing( "<key xsi:type=\"xsd:string\">action</key>" ) )
		    .withRequestBody( containing( "<value xsi:type=\"xsd:string\">upload</value>" ) ) );

		// Verify base64Binary element exists (specific encoding may vary)
		wireMock.verify( postRequestedFor( urlEqualTo( "/objectUpload" ) )
		    .withRequestBody( containing( "<InputFile" ) ) // Match opening tag (with or without attributes)
		    .withRequestBody( containing( "</InputFile>" ) ) );

		// Verify the result
		assertThat( variables.get( result ) ).isNotNull();
	}

	@DisplayName( "Test invoke webservice with empty Apache SOAP Map" )
	@Test
	public void testInvokeWebServiceEmptyApacheSoapMap() {
		// Set up objectUpload WSDL endpoint
		wireMock.stubFor( get( urlEqualTo( "/objectUpload?wsdl" ) )
		    .willReturn( aResponse()
		        .withStatus( 200 )
		        .withHeader( "Content-Type", "text/xml" )
		        .withBody( String.format( OBJECT_UPLOAD_WSDL, wireMock.getPort() ) ) ) );

		// Mock the SOAP operation response
		wireMock.stubFor( post( urlEqualTo( "/objectUpload" ) )
		    .willReturn( aResponse()
		        .withStatus( 200 )
		        .withHeader( "Content-Type", "text/xml" )
		        .withBody( """
		                   <?xml version="1.0" encoding="UTF-8"?>
		                   <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
		                       <soap:Body>
		                           <tns:SharedEntryPointResponse xmlns:tns="http://ws.elend">
		                               <tns:SharedEntryPointReturn/>
		                           </tns:SharedEntryPointResponse>
		                       </soap:Body>
		                   </soap:Envelope>
		                   """ ) ) );

		String wsdlUrl = "http://localhost:" + wireMock.getPort() + "/objectUpload?wsdl";

		// Execute invoke webservice with empty Map parameter
		instance.executeSource(
		    String.format( """
		                   emptyInputs = {};
		                   fileContent = "";
		                   bx:invoke
		                       webservice="%s"
		                       method="SharedEntryPoint"
		                       argumentCollection={ Inputs: emptyInputs, InputFile: fileContent }
		                       returnVariable="result";
		                   """, wsdlUrl ),
		    context );

		// Verify the SOAP request contains empty Map structure (self-closing tag with type attribute)
		wireMock.verify( postRequestedFor( urlEqualTo( "/objectUpload" ) )
		    .withRequestBody( containing( "<Inputs xsi:type=\"apachesoap:Map\"" ) ) ); // Match opening tag (may be self-closing for empty map)

		// Verify empty base64Binary element
		wireMock.verify( postRequestedFor( urlEqualTo( "/objectUpload" ) )
		    .withRequestBody( containing( "<InputFile" ) ) ); // Match opening tag (with or without attributes)

		// Verify the result
		assertThat( variables.get( result ) ).isNotNull();
	}
}
