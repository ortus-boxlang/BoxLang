
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

import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
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
import org.w3c.dom.NodeList;
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
		this.context = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		this.httpService = BoxRuntime.getInstance().getHttpService();
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
		BoxSoapClient client = this.httpService.getOrCreateSoapClient( calculatorWsdlUrl, context );

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
		BoxSoapClient client = this.httpService.getOrCreateSoapClient( calculatorWsdlUrl, context );
		org.junit.jupiter.api.Assertions.assertThrows( BoxRuntimeException.class, () -> client.withSoapHeaders( null ) );
	}

	// C
	@DisplayName( "withSoapHeaders throws on invalid XML Key" )
	@Test
	void withSoapHeaders_invalid_xml_key_throws() {
		BoxSoapClient	client	= this.httpService.getOrCreateSoapClient( calculatorWsdlUrl, context );
		IStruct			headers	= Struct.of( Key.of( "123" ), "x" );

		client.withSoapHeaders( headers );
		org.junit.jupiter.api.Assertions.assertThrows( BoxRuntimeException.class, () -> buildSoapRequest( client, "Add", null ) );

	}

	// D
	@DisplayName( "withSoapHeaders throws on illegal XML char in value" )
	@Test
	void withSoapHeaders_illegal_xml_char_in_value_throws() {
		BoxSoapClient	client	= this.httpService.getOrCreateSoapClient( calculatorWsdlUrl, context );
		IStruct			headers	= Struct.of( Key.of( "Token" ), "a\u0001b" );
		org.junit.jupiter.api.Assertions.assertThrows( BoxRuntimeException.class, () -> client.withSoapHeaders( headers ) );
	}

	// E
	@DisplayName( "withSoapHeaders throws on non-scalar value" )
	@Test
	void withSoapHeaders_non_scalar_value_throws() {
		BoxSoapClient	client	= this.httpService.getOrCreateSoapClient( calculatorWsdlUrl, context );
		IStruct			headers	= Struct.of( Key.of( "Nested" ), new Struct() );
		org.junit.jupiter.api.Assertions.assertThrows( BoxRuntimeException.class, () -> client.withSoapHeaders( headers ) );
	}

	// F
	@DisplayName( "withSoapHeaders accepts number value" )
	@Test
	void withSoapHeaders_number_accepts() {
		BoxSoapClient client = this.httpService.getOrCreateSoapClient( calculatorWsdlUrl, context );

		client.withSoapHeaders(
		    Struct.of( Key.of( "RetryCount" ), 3 ) );
		assertThat( client ).isNotNull();
	}

	// G
	@DisplayName( "withSoapHeaders accepts boolean value" )
	@Test
	void withSoapHeaders_boolean_accepts() {
		BoxSoapClient client = this.httpService.getOrCreateSoapClient( calculatorWsdlUrl, context );

		client.withSoapHeaders( Struct.of( Key.of( "Enabled" ), true ) );
		assertThat( client ).isNotNull();
	}

	// H
	@DisplayName( "withSoapHeaders accepts null as a header value" )
	@Test
	void withSoapHeaders_nullValue_accepts() {
		BoxSoapClient client = this.httpService.getOrCreateSoapClient( calculatorWsdlUrl, context );

		client.withSoapHeaders( Struct.of( Key.of( "OptionalNote" ), null ) );
		assertThat( client ).isNotNull();
	}

	// I
	@DisplayName( "withSoapHeaders accepts multiple header keys" )
	@Test
	void withSoapHeaders_multipleKeys_accepts() {
		BoxSoapClient client = this.httpService.getOrCreateSoapClient( calculatorWsdlUrl, context );
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
		BoxSoapClient client = this.httpService.getOrCreateSoapClient( calculatorWsdlUrl, context );
		// Just create the client and do not call withSoapHeaders
		assertThat( client ).isNotNull();
	}

	// K
	@DisplayName( "buildSoapRequest creates a single soap:Header before soap:Body for SOAP 1.1" )
	@Test
	void buildSoapRequest_singleHeader_beforeBody_soap11() {
		BoxSoapClient client = this.httpService.getOrCreateSoapClient( calculatorWsdlUrl, context );
		client.soapVersion( "1.1" );
		client.withSoapHeaders( Struct.of( Key.of( "AuthToken" ), "abc123" ) );

		String soapRequest = buildSoapRequest( client, "Add", null );
		assertSingleHeaderBeforeBody( soapRequest );
	}

	// L
	@DisplayName( "buildSoapRequest creates a single soap:Header before soap:Body for SOAP 1.2" )
	@Test
	void buildSoapRequest_singleHeader_beforeBody_soap12() {
		BoxSoapClient client = this.httpService.getOrCreateSoapClient( calculatorWsdlUrl, context );
		client.soapVersion( "1.2" );
		client.withSoapHeaders( Struct.of( Key.of( "AuthToken" ), "abc123" ) );

		String soapRequest = buildSoapRequest( client, "Add", null );
		assertSingleHeaderBeforeBody( soapRequest );
	}

	private String buildSoapRequest( BoxSoapClient client, String operationName, Object arguments ) {
		WsdlOperation operation = client.getWsdlDefinition().getOperation( Key.of( operationName ) );
		assertThat( operation ).isNotNull();

		try {
			Method method = BoxSoapClient.class.getDeclaredMethod( "buildSoapRequest", WsdlOperation.class, Object.class );
			method.setAccessible( true );
			return ( String ) method.invoke( client, operation, arguments );
		} catch ( InvocationTargetException e ) {
			Throwable target = e.getTargetException();
			if ( target instanceof RuntimeException runtime ) {
				throw runtime;
			}
			throw new RuntimeException( target );
		} catch ( Exception e ) {
			throw new RuntimeException( e );
		}
	}

	private void assertSingleHeaderBeforeBody( String soapRequest ) {
		Document doc = parseSoapRequest( soapRequest );
		Element envelope = doc.getDocumentElement();
		String envelopeNamespace = envelope.getNamespaceURI();

		NodeList headers = doc.getElementsByTagNameNS( envelopeNamespace, "Header" );
		assertThat( headers.getLength() ).isEqualTo( 1 );

		NodeList bodies = doc.getElementsByTagNameNS( envelopeNamespace, "Body" );
		assertThat( bodies.getLength() ).isEqualTo( 1 );

		Integer headerIndex = null;
		Integer bodyIndex = null;
		int elementIndex = 0;

		NodeList children = envelope.getChildNodes();
		for ( int i = 0; i < children.getLength(); i++ ) {
			Node child = children.item( i );
			if ( child.getNodeType() != Node.ELEMENT_NODE ) {
				continue;
			}
			String localName = child.getLocalName();
			if ( "Header".equals( localName ) ) {
				headerIndex = elementIndex;
			} else if ( "Body".equals( localName ) ) {
				bodyIndex = elementIndex;
			}
			elementIndex++;
		}

		assertThat( headerIndex ).isNotNull();
		assertThat( bodyIndex ).isNotNull();
		assertThat( headerIndex ).isLessThan( bodyIndex );
	}

	private Document parseSoapRequest( String soapRequest ) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware( true );
			return factory.newDocumentBuilder().parse( new InputSource( new StringReader( soapRequest ) ) );
		} catch ( Exception e ) {
			throw new RuntimeException( e );
		}
	}
}
