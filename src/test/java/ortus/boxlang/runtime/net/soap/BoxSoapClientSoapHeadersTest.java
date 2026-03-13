
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

package ortus.boxlang.runtime.net.soap;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import java.lang.reflect.Method;
import java.nio.file.Paths;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.HttpService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class BoxSoapClientSoapHeadersTest {

	static BoxRuntime		instance;
	private IBoxContext		context;
	private HttpService		httpService;
	private static String	calculatorWsdlUrl;

	@BeforeAll
	public static void setUpAll() {
		instance			= BoxRuntime.getInstance( true );
		calculatorWsdlUrl	= Paths.get( "src/test/resources/wsdl/calculator.wsdl" ).toAbsolutePath().toUri().toString();
	}

	@BeforeEach
	public void setUp() {
		this.context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		this.httpService	= BoxRuntime.getInstance().getHttpService();
		this.httpService.clearAllSoapClients();
	}

	@AfterEach
	public void tearDown() {
		this.httpService.clearAllSoapClients();
	}

	// A
	@DisplayName( "withSoapHeaders accepts simple String header" )
	@Test
	void withSoapHeaders_string_accepts() {
		BoxSoapClient client = this.httpService.getOrCreateSoapClient( calculatorWsdlUrl, this.context );

		client.withSoapHeaders(
		    Struct.of( Key.of( "AuthToken" ), "abc123" )
		);

		// no exception = pass
		assertThat( client ).isNotNull();
	}

	// B
	@DisplayName( "withSoapHeaders throws on null headers" )
	@Test
	void withSoapHeaders_null_throws() {
		BoxSoapClient client = this.httpService.getOrCreateSoapClient( calculatorWsdlUrl, this.context );
		assertThrows( BoxRuntimeException.class, () -> client.withSoapHeaders( null ) );
	}

	// C
	@DisplayName( "withSoapHeaders throws on invalid XML Key" )
	@Test
	void withSoapHeaders_invalid_xml_key_throws() {
		BoxSoapClient	client	= this.httpService.getOrCreateSoapClient( calculatorWsdlUrl, this.context );
		IStruct			headers	= Struct.of( Key.of( "123" ), "x" );

		assertThrows( BoxRuntimeException.class, () -> client.withSoapHeaders( headers ) );

	}

	// D
	@DisplayName( "withSoapHeaders throws on illegal XML char in value" )
	@Test
	void withSoapHeaders_illegal_xml_char_in_value_throws() {
		BoxSoapClient	client	= this.httpService.getOrCreateSoapClient( calculatorWsdlUrl, this.context );
		IStruct			headers	= Struct.of( Key.of( "Token" ), "a\u0001b" );
		assertThrows( BoxRuntimeException.class, () -> client.withSoapHeaders( headers ) );
	}

	// E
	@DisplayName( "withSoapHeaders throws on non-scalar value" )
	@Test
	void withSoapHeaders_non_scalar_value_throws() {
		BoxSoapClient	client	= this.httpService.getOrCreateSoapClient( calculatorWsdlUrl, this.context );
		IStruct			headers	= Struct.of( Key.of( "Nested" ), new Struct() );
		assertThrows( BoxRuntimeException.class, () -> client.withSoapHeaders( headers ) );
	}

	// F
	@DisplayName( "withSoapHeaders accepts number value" )
	@Test
	void withSoapHeaders_number_accepts() {
		BoxSoapClient client = this.httpService.getOrCreateSoapClient( calculatorWsdlUrl, this.context );

		client.withSoapHeaders(
		    Struct.of( Key.of( "RetryCount" ), 3 ) );
		assertThat( client ).isNotNull();
	}

	// G
	@DisplayName( "withSoapHeaders accepts boolean value" )
	@Test
	void withSoapHeaders_boolean_accepts() {
		BoxSoapClient client = this.httpService.getOrCreateSoapClient( calculatorWsdlUrl, this.context );

		client.withSoapHeaders( Struct.of( Key.of( "Enabled" ), true ) );
		assertThat( client ).isNotNull();
	}

	// H
	@DisplayName( "withSoapHeaders accepts null as a header value" )
	@Test
	void withSoapHeaders_nullValue_accepts() {
		BoxSoapClient client = this.httpService.getOrCreateSoapClient( calculatorWsdlUrl, this.context );

		client.withSoapHeaders( Struct.of( Key.of( "OptionalNote" ), null ) );
		assertThat( client ).isNotNull();
	}

	// I
	@DisplayName( "withSoapHeaders accepts multiple header keys" )
	@Test
	void withSoapHeaders_multipleKeys_accepts() {
		BoxSoapClient client = this.httpService.getOrCreateSoapClient( calculatorWsdlUrl, this.context );
		client.withSoapHeaders( Struct.of(
		    Key.of( "AuthToken" ), "abc123",
		    Key.of( "RetryCount" ), 2,
		    Key.of( "Enabled" ), false
		) );
		assertThat( client ).isNotNull();
	}

	// J
	@DisplayName( "no headers set and withSoapHeaders never called will not throw" )
	@Test
	void noHeaders_withSoapHeadersNeverCalled_doesNotThrow() {
		BoxSoapClient client = this.httpService.getOrCreateSoapClient( calculatorWsdlUrl, this.context );
		// Just create the client and do not call withSoapHeaders
		assertThat( client ).isNotNull();
	}

	@DisplayName( "SOAP request contains exactly one header before the body for SOAP 1.1" )
	@Test
	void soapRequest_serializesSingleHeaderBeforeBody_forSoap11() throws Exception {
		BoxSoapClient	client		= this.httpService.getOrCreateSoapClient( calculatorWsdlUrl, this.context )
		    .withSoapHeaders( Struct.of( Key.of( "AuthToken" ), "abc123", Key.of( "RetryCount" ), 3 ) )
		    .soapVersion( "1.1" );

		Document		request		= parseRequest( buildSoapRequest( client ) );
		Element			envelope	= request.getDocumentElement();
		Element			header		= getFirstChildElement( envelope );

		assertThat( envelope.getNamespaceURI() ).isEqualTo( "http://schemas.xmlsoap.org/soap/envelope/" );
		assertThat( header.getLocalName() ).isEqualTo( "Header" );
		assertThat( request.getElementsByTagNameNS( envelope.getNamespaceURI(), "Header" ).getLength() ).isEqualTo( 1 );
		assertThat( getNextElementSibling( header ).getLocalName() ).isEqualTo( "Body" );
		assertThat( header.getElementsByTagName( "AuthToken" ).item( 0 ).getTextContent() ).isEqualTo( "abc123" );
		assertThat( header.getElementsByTagName( "RetryCount" ).item( 0 ).getTextContent() ).isEqualTo( "3" );
	}

	@DisplayName( "SOAP request contains exactly one header before the body for SOAP 1.2" )
	@Test
	void soapRequest_serializesSingleHeaderBeforeBody_forSoap12() throws Exception {
		BoxSoapClient	client		= this.httpService.getOrCreateSoapClient( calculatorWsdlUrl, this.context )
		    .withSoapHeaders( Struct.of( Key.of( "SessionId" ), "session-456" ) )
		    .soapVersion( "1.2" );

		Document		request		= parseRequest( buildSoapRequest( client ) );
		Element			envelope	= request.getDocumentElement();
		Element			header		= getFirstChildElement( envelope );

		assertThat( envelope.getNamespaceURI() ).isEqualTo( "http://www.w3.org/2003/05/soap-envelope" );
		assertThat( header.getLocalName() ).isEqualTo( "Header" );
		assertThat( request.getElementsByTagNameNS( envelope.getNamespaceURI(), "Header" ).getLength() ).isEqualTo( 1 );
		assertThat( getNextElementSibling( header ).getLocalName() ).isEqualTo( "Body" );
		assertThat( header.getElementsByTagName( "SessionId" ).item( 0 ).getTextContent() ).isEqualTo( "session-456" );
	}

	private String buildSoapRequest( BoxSoapClient client ) throws Exception {
		WsdlOperation	operation			= client.getWsdlDefinition().getOperation( Key.of( String.valueOf( client.getOperations().get( 0 ) ) ) );
		Method			buildSoapRequest	= BoxSoapClient.class.getDeclaredMethod( "buildSoapRequest", WsdlOperation.class, Object.class );
		buildSoapRequest.setAccessible( true );
		return ( String ) buildSoapRequest.invoke( client, operation, null );
	}

	private Document parseRequest( String requestXml ) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware( true );
		return factory.newDocumentBuilder().parse( new InputSource( new StringReader( requestXml ) ) );
	}

	private Element getFirstChildElement( Element parent ) {
		Node child = parent.getFirstChild();
		while ( child != null && child.getNodeType() != Node.ELEMENT_NODE ) {
			child = child.getNextSibling();
		}
		return ( Element ) child;
	}

	private Element getNextElementSibling( Element element ) {
		Node sibling = element.getNextSibling();
		while ( sibling != null && sibling.getNodeType() != Node.ELEMENT_NODE ) {
			sibling = sibling.getNextSibling();
		}
		return ( Element ) sibling;
	}
}
