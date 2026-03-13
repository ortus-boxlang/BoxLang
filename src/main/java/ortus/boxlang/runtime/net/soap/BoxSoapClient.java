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

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.interop.DynamicInteropService;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.logging.BoxLangLogger;
import ortus.boxlang.runtime.net.BoxHttpClient;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.HttpService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * A fluent SOAP web service client for BoxLang.
 * Provides human-friendly methods for invoking SOAP operations discovered from WSDL documents.
 *
 * Usage:
 *
 * <pre>
 *
 * soapClient = SoapClient.fromWsdl( "http://example.com/service.wsdl" );
 * Object result = soapClient.invoke( "methodName", args );
 * </pre>
 */
public class BoxSoapClient implements IReferenceable {

	/**
	 * ------------------------------------------------------------------------------
	 * Constants
	 * ------------------------------------------------------------------------------
	 */

	private static final String		SOAP_11_ENVELOPE_NS		= "http://schemas.xmlsoap.org/soap/envelope/";
	private static final String		SOAP_12_ENVELOPE_NS		= "http://www.w3.org/2003/05/soap-envelope";
	private static final String		XSI_NS					= "http://www.w3.org/2001/XMLSchema-instance";
	private static final String		XSD_NS					= "http://www.w3.org/2001/XMLSchema";

	/**
	 * ------------------------------------------------------------------------------
	 * Properties
	 * ------------------------------------------------------------------------------
	 */

	/**
	 * The WSDL definition for this client
	 */
	private final WsdlDefinition	wsdlDefinition;

	/**
	 * The HTTP service for making requests
	 */
	private final HttpService		httpService;

	/**
	 * The logger
	 */
	private final BoxLangLogger		logger;

	/**
	 * Request timeout in seconds (0 = no timeout)
	 */
	private int						timeout					= 30;

	/**
	 * Username for HTTP basic authentication
	 */
	private String					username;

	/**
	 * Password for HTTP basic authentication
	 */
	private String					password;

	/**
	 * Custom HTTP headers
	 */
	private Map<String, String>		customHeaders			= new HashMap<>();

	/**
	 * SOAP headers to include in the SOAP envelope.
	 *
	 * SOAP Spec:
	 * - <soap:Header> is OPTIONAL.
	 * - If present it must be the first child of <soap:Envelope> (before <soap:Body>).
	 * - There is only ONE <soap:Header> section per request.
	 *
	 * Storage and injection (implemented):
	 * - We store simple key/value headers using a BoxLang Struct (IStruct).
	 * - Example: { "AuthToken" : "abc123", "SessionId" : "session-456" }.
	 * - When building the SOAP request (e.g., in buildSoapRequest), stored headers are
	 *   injected into the <soap:Header> section of the SOAP envelope.
	 */

	private IStruct					soapHeaders;
	private String					soapVersion				= "1.1";
	private final Instant			createdAt				= Instant.now();
	private IBoxContext				executionContext;
	private long					totalInvocations		= 0;
	private long					successfulInvocations	= 0;
	private long					failedInvocations		= 0;

	private BoxSoapClient( WsdlDefinition wsdlDefinition, HttpService httpService, IBoxContext context ) {
		this.wsdlDefinition		= wsdlDefinition;
		this.httpService		= httpService;
		this.logger				= this.httpService.getLogger();
		// Initialize SOAP version from WSDL definition (can be overridden later)
		this.soapVersion		= wsdlDefinition.getSoapVersion();
		this.executionContext	= context;
		// Initialize SOAP headers storage as an empty Struct
		// This avoids NullPointerExceptions later when we add header
		this.soapHeaders		= Struct.of();
	}

	public static BoxSoapClient fromWsdl( String wsdlUrl, HttpService httpService, IBoxContext context ) {
		WsdlDefinition definition = WsdlParser.parse( wsdlUrl );
		return new BoxSoapClient( definition, httpService, context );
	}

	public BoxSoapClient timeout( int timeout ) {
		this.timeout = timeout;
		return this;
	}

	public BoxSoapClient withBasicAuth( String username, String password ) {
		this.username	= username;
		this.password	= password;
		return this;
	}

	public BoxSoapClient header( String name, String value ) {
		this.customHeaders.put( name, value );
		return this;
	}

	public BoxSoapClient soapVersion( String version ) {
		if ( !"1.1".equals( version ) && !"1.2".equals( version ) ) {
			throw new BoxRuntimeException( "Invalid SOAP version: " + version + ". Must be '1.1' or '1.2'" );
		}
		this.soapVersion = version;
		return this;
	}

	public BoxSoapClient withContext( IBoxContext context ) {
		this.executionContext = context;
		return this;
	}

	/**
	 * Set SOAP headers to be included in the SOAP envelope.
	 *
	 * @param headers A struct of simple key/value pairs
	 *
	 * @return This instance for chaining
	 */
	public BoxSoapClient withSoapHeaders( IStruct headers ) {
		validateSoapHeaders( headers );
		this.soapHeaders = headers;
		return this;
	}

	public String getSoapVersion() {
		return this.soapVersion;
	}

	public WsdlDefinition getWsdlDefinition() {
		return this.wsdlDefinition;
	}

	public String getWsdlUrl() {
		return this.wsdlDefinition.getWsdlUrl();
	}

	public String getServiceEndpoint() {
		return this.wsdlDefinition.getServiceEndpoint();
	}

	public Array getOperations() {
		return this.wsdlDefinition.getOperationNames();
	}

	public boolean hasOperation( String operationName ) {
		return this.wsdlDefinition.hasOperation( Key.of( operationName ) );
	}

	public IStruct getOperationInfo( String operationName ) {
		WsdlOperation operation = this.wsdlDefinition.getOperation( Key.of( operationName ) );
		return operation != null ? operation.toStruct() : null;
	}

	public IStruct getStatistics() {
		return Struct.ofNonConcurrent(
		    "totalInvocations", this.totalInvocations,
		    "successfulInvocations", this.successfulInvocations,
		    "failedInvocations", this.failedInvocations,
		    "wsdlUrl", this.wsdlDefinition.getWsdlUrl(),
		    "serviceEndpoint", this.wsdlDefinition.getServiceEndpoint(),
		    "operationCount", this.wsdlDefinition.getOperations().size(),
		    "createdAt", this.createdAt.toString()
		);
	}

	public IStruct toStruct() {
		return Struct.ofNonConcurrent(
		    "wsdlUrl", this.wsdlDefinition.getWsdlUrl(),
		    "serviceEndpoint", this.wsdlDefinition.getServiceEndpoint(),
		    "serviceName", this.wsdlDefinition.getServiceName(),
		    "targetNamespace", this.wsdlDefinition.getTargetNamespace(),
		    "bindingStyle", this.wsdlDefinition.getBindingStyle(),
		    "operations", this.wsdlDefinition.getOperationNames(),
		    "soapVersion", this.soapVersion,
		    "timeout", this.timeout,
		    "statistics", this.getStatistics()
		);
	}

	public Object invoke( String operationName ) {
		return invoke( operationName, null );
	}

	public Object invoke( String operationName, Object arguments ) {
		this.totalInvocations++;

		try {
			WsdlOperation operation = this.wsdlDefinition.getOperation( Key.of( operationName ) );
			if ( operation == null ) {
				throw new BoxRuntimeException(
				    "SOAP operation [" + operationName + "] not found in WSDL: "
				        + this.getWsdlUrl()
				        + ". Available operations: "
				        + this.wsdlDefinition.getOperationNames().toString()
				);
			}

			String soapRequest = buildSoapRequest( operation, arguments );

			if ( this.logger.isDebugEnabled() ) {
				this.logger.trace( "SOAP Request to {}: {}", this.getServiceEndpoint(), soapRequest );
			}

			BoxHttpClient					httpClient	= this.httpService.getOrBuildClient(
			    "HTTP/2",
			    true,
			    BoxHttpClient.DEFAULT_CONNECTION_TIMEOUT,
			    null, null, null, null,
			    null, null
			);

			BoxHttpClient.BoxHttpRequest	request		= httpClient
			    .newRequest( this.getServiceEndpoint(), this.executionContext )
			    .post()
			    .timeout( this.timeout )
			    .header(
			        "Content-Type",
			        "1.1".equals( this.soapVersion ) ? "text/xml; charset=utf-8" : "application/soap+xml; charset=utf-8"
			    )
			    .header( "SOAPAction", operation.getSoapAction() != null ? operation.getSoapAction() : "" )
			    .body( soapRequest );

			for ( Map.Entry<String, String> header : this.customHeaders.entrySet() ) {
				request.header( header.getKey(), header.getValue() );
			}

			if ( this.username != null && this.password != null ) {
				request.withBasicAuth( this.username, this.password );
			}

			IStruct	httpResult	= ( IStruct ) request.send();
			Object	result		= parseSoapResponse( httpResult, operation );

			this.successfulInvocations++;
			return result;

		} catch ( Exception e ) {
			this.failedInvocations++;
			throw new BoxRuntimeException( "SOAP operation '" + operationName + "' failed: " + e.getMessage(), e );
		}
	}

	/**
	 * ------------------------------------------------------------------------------
	 * IReferenceable Implementation
	 * ------------------------------------------------------------------------------
	 */

	/**
	 * Dereference this object by a key and return the value, or throw exception
	 *
	 * @param context The context we're executing inside of
	 * @param name    The key to dereference
	 * @param safe    Whether to throw an exception if the key is not found
	 *
	 * @return The requested object
	 */
	@Override
	public Object dereference( IBoxContext context, Key name, Boolean safe ) {
		String			methodName		= name.getName();

		// Check if this is a known SoapClient method
		DynamicObject	dynamicClient	= DynamicObject.of( this );
		if ( dynamicClient.hasMethodNoCase( methodName ) ) {
			// Return a reference to this client's method - the actual invocation will come via dereferenceAndInvoke
			return dynamicClient.getMethod( methodName, true );
		}

		// Check if this is a SOAP operation
		if ( this.wsdlDefinition.hasOperation( name ) ) {
			return this.wsdlDefinition.getOperation( name );
		}

		if ( safe ) {
			return null;
		}

		throw new BoxRuntimeException(
		    "Property [" + methodName + "] not found on SOAP client. Available operations: "
		        + this.wsdlDefinition.getOperationNames().toString()
		);
	}

	/**
	 * Dereference this object by a key and invoke the result as an invokable using positional arguments
	 *
	 * @param context             The context we're executing inside of
	 * @param name                The key to dereference
	 * @param positionalArguments The positional arguments to pass to the invokable
	 * @param safe                Whether to throw an exception if the key is not found
	 *
	 * @return The requested object
	 */
	@Override
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Object[] positionalArguments, Boolean safe ) {
		String methodName = name.getName();

		// Check if this is a known SoapClient method
		if ( DynamicObject.of( this ).hasMethodNoCase( methodName ) ) {
			// Handle built-in methods like invoke(), getOperations(), etc.
			return DynamicInteropService.invoke( context, this, methodName, safe, positionalArguments );
		}

		// Check if this is a SOAP operation
		if ( this.wsdlDefinition.hasOperation( name ) ) {
			// Convert positional arguments to the format expected by invoke()
			Object arguments = null;
			if ( positionalArguments != null && positionalArguments.length > 0 ) {
				if ( positionalArguments.length == 1 ) {
					arguments = positionalArguments[ 0 ];
				} else {
					arguments = Array.fromArray( positionalArguments );
				}
			}
			return invoke( methodName, arguments );
		}

		if ( safe ) {
			return null;
		}

		throw new BoxRuntimeException(
		    "Method [" + methodName + "] not found on SOAP client. Available operations: "
		        + this.wsdlDefinition.getOperationNames().toString()
		);
	}

	/**
	 * Dereference this object by a key and invoke the result as an invokable using named arguments
	 *
	 * @param context        The context we're executing inside of
	 * @param name           The key to dereference
	 * @param namedArguments The named arguments to pass to the invokable
	 * @param safe           Whether to throw an exception if the key is not found
	 *
	 * @return The requested object
	 */
	@Override
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Map<Key, Object> namedArguments, Boolean safe ) {
		String methodName = name.getName();

		// Check if this is a known SoapClient method
		if ( DynamicObject.of( this ).hasMethodNoCase( methodName ) ) {
			return DynamicInteropService.invoke( context, this, methodName, safe, namedArguments );
		}

		// Check if this is a SOAP operation
		if ( this.wsdlDefinition.hasOperation( name ) ) {
			// Handle argument collection properly
			Object arguments = null;
			if ( namedArguments != null && !namedArguments.isEmpty() ) {
				// Check if this is an argumentCollection call
				Object argumentCollection = namedArguments.get( Key.argumentCollection );
				if ( argumentCollection != null ) {
					// Use the argumentCollection value as the actual arguments
					arguments = argumentCollection;
				} else {
					// Convert named arguments to a struct for invoke()
					arguments = Struct.fromMap( namedArguments );
				}
			}
			return invoke( methodName, arguments );
		}

		if ( safe ) {
			return null;
		}

		throw new BoxRuntimeException(
		    "Method [" + methodName + "] not found on SOAP client. Available operations: "
		        + this.wsdlDefinition.getOperationNames().toString()
		);
	}

	/**
	 * Assign a value to a key in this object
	 *
	 * @param context The context we're executing inside of
	 * @param name    The name of the key to assign to
	 * @param value   The value to assign
	 *
	 * @return The value that was assigned
	 */
	@Override
	public Object assign( IBoxContext context, Key name, Object value ) {
		throw new BoxRuntimeException( "Cannot assign properties to SOAP client objects" );
	}

	/**
	 * Check if a method name is a known SoapClient method
	 *
	 * @param methodName The method name to check
	 *
	 * @return true if this is a known method
	 */
	private boolean isKnownMethod( String methodName ) {
		String lower = methodName.toLowerCase();
		return lower.equals( "invoke" )
		    || lower.equals( "getoperations" )
		    || lower.equals( "listoperations" )
		    || lower.equals( "getstatistics" )
		    || lower.equals( "getstats" )
		    || lower.equals( "getoperationinfo" )
		    || lower.equals( "tostruct" );
	}

	/**
	 * ------------------------------------------------------------------------------
	 * SOAP Message Building and Parsing
	 * ------------------------------------------------------------------------------
	 */

	/**
	 * Validate SOAP header input.
	 * Only simple key/value pairs are allowed.
	 */
	private void validateSoapHeaders( IStruct headers ) {
		if ( headers == null ) {
			throw new BoxRuntimeException( "SOAP headers cannot be null" );
		}

		for ( Key key : headers.keySet() ) {
			Object value = headers.get( key );

			if ( key.getName() == null || key.getName().isEmpty() ) {
				throw new BoxRuntimeException(
				    "SOAP header keys must be non-empty strings"
				);
			}
			if ( value == null ) {
				continue;
			}
			String valueStr = StringCaster.cast( value );
			if ( !isValidXMLCharData( valueStr ) ) {
				throw new BoxRuntimeException(
				    "This is an Invalid SOAP header value for key '" + key.getName() +
				        "'. Only simple scalar values are allowed."
				);
			}

			if ( value instanceof String
			    || value instanceof Number
			    || value instanceof Boolean
			    || value instanceof java.time.temporal.Temporal ) {
				continue;
			}

			throw new BoxRuntimeException(
			    "Invalid SOAP header value for key '" + key.getName() +
			        "'. Only simple scalar values are allowed."
			);
		}
	}
	private static boolean isValidXMLCharData( String s ) {
		if ( s == null ) {
			return true;
		}
		for ( int i = 0; i < s.length(); i++ ) {
			char c = s.charAt( i );
			if ( c >= 0x00 && c <= 0x1F && c != 0x09 && c != 0x0A && c != 0x0D ) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Build a SOAP request envelope for an operation
	 *
	 * @param operation The operation to invoke
	 * @param arguments The arguments (Array, Struct, or Object[])
	 *
	 * @return The SOAP XML request as a string
	 */
	private String buildSoapRequest( WsdlOperation operation, Object arguments ) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware( true );
			DocumentBuilder	builder		= factory.newDocumentBuilder();
			Document		doc			= builder.newDocument();

			String			soapNS		= "1.1".equals( this.soapVersion ) ? SOAP_11_ENVELOPE_NS : SOAP_12_ENVELOPE_NS;

			Element			envelope	= doc.createElementNS( soapNS, "soap:Envelope" );
			envelope.setAttribute( "xmlns:soap", soapNS );
			envelope.setAttribute( "xmlns:xsi", XSI_NS );
			envelope.setAttribute( "xmlns:xsd", XSD_NS );

			if ( operation.getNamespace() != null ) {
				envelope.setAttribute( "xmlns:tns", operation.getNamespace() );
			}

			doc.appendChild( envelope );

			if ( this.soapHeaders != null && !this.soapHeaders.isEmpty() ) {
				Element header = doc.createElementNS( soapNS, "soap:Header" );
				envelope.appendChild( header );
				addSoapHeadersToRequest( doc, header, this.soapHeaders );
			}

			Element body = doc.createElementNS( soapNS, "soap:Body" );
			envelope.appendChild( body );

			Element operationElement = doc.createElement( operation.getName() );
			if ( operation.getNamespace() != null ) {
				operationElement.setAttribute( "xmlns", operation.getNamespace() );
			}
			body.appendChild( operationElement );

			addParametersToRequest( doc, operationElement, operation, arguments );

			return documentToString( doc );

		} catch ( BoxRuntimeException e ) {
			throw e;
		} catch ( Exception e ) {
			throw new BoxRuntimeException( "Failed to build SOAP request", e );
		}
	}

	private void addSoapHeadersToRequest( Document doc, Element headerElement, IStruct headers ) {
		for ( Key key : headers.keySet() ) {
			String	headerName	= key.getName();
			Object	headerValue	= headers.get( key );

			Element	headerChild;
			try {
				headerChild = doc.createElement( headerName );
			} catch ( DOMException e ) {
				throw new BoxRuntimeException( "SOAP header keys must be a valid XML name: " + headerName, e );
			}

			if ( headerValue != null ) {
				headerChild.setTextContent( StringCaster.cast( headerValue ) );
			}

			headerElement.appendChild( headerChild );

			if ( this.logger.isTraceEnabled() ) {
				this.logger.trace(
				    "Added SOAP header element: <{}>{}</{}>",
				    headerName,
				    headerValue != null ? headerValue : "",
				    headerName
				);
			}
		}
	}

	private void addParametersToRequest( Document doc, Element parentElement, WsdlOperation operation, Object arguments ) {
		List<WsdlParameter> params = operation.getInputParameters();

		if ( arguments == null || params.isEmpty() ) {
			return;
		}

		Map<String, Object> argMap = new HashMap<>();

		if ( arguments instanceof IStruct struct ) {
			for ( WsdlParameter param : params ) {
				Key		key		= Key.of( param.getName() );
				Object	value	= struct.get( key );
				if ( value != null ) {
					argMap.put( param.getName(), value );
				}
			}
		} else if ( arguments instanceof Array array ) {
			for ( int i = 0; i < params.size() && i < array.size(); i++ ) {
				argMap.put( params.get( i ).getName(), array.get( i ) );
			}
		} else if ( arguments instanceof Object[] array ) {
			for ( int i = 0; i < params.size() && i < array.length; i++ ) {
				argMap.put( params.get( i ).getName(), array[ i ] );
			}
		} else {
			if ( !params.isEmpty() ) {
				argMap.put( params.get( 0 ).getName(), arguments );
			}
		}

		for ( WsdlParameter param : params ) {
			Object value = argMap.get( param.getName() );
			if ( value != null ) {
				Element paramElement = doc.createElement( param.getName() );
				paramElement.setTextContent( String.valueOf( value ) );
				parentElement.appendChild( paramElement );
			}
		}
	}

	private Object parseSoapResponse( IStruct httpResult, WsdlOperation operation ) {
		try {
			String responseBody = httpResult.getAsString( Key.fileContent );

			if ( responseBody == null || responseBody.isEmpty() ) {
				throw new BoxRuntimeException( "Empty SOAP response received" );
			}

			if ( this.logger.isTraceEnabled() ) {
				this.logger.trace( "SOAP Response: {}", responseBody );
			}

			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware( true );
			DocumentBuilder	builder	= factory.newDocumentBuilder();
			Document		doc		= builder.parse( new InputSource( new StringReader( responseBody ) ) );

			NodeList		faults	= doc.getElementsByTagNameNS( "*", "Fault" );
			if ( faults.getLength() > 0 ) {
				return parseSoapFault( ( Element ) faults.item( 0 ) );
			}

			NodeList bodies = doc.getElementsByTagNameNS( "*", "Body" );
			if ( bodies.getLength() > 0 ) {
				Element	body		= ( Element ) bodies.item( 0 );
				Element	response	= getFirstChildElement( body );
				if ( response != null ) {
					Object result = xmlElementToBoxLang( response );
					return unwrapResponse( result );
				}
			}

			return responseBody;

		} catch ( Exception e ) {
			throw new BoxRuntimeException( "Failed to parse SOAP response", e );
		}
	}

	private Object parseSoapFault( Element faultElement ) {
		String		faultCode	= "";
		String		faultString	= "";
		String		faultDetail	= "";

		NodeList	children	= faultElement.getChildNodes();
		for ( int i = 0; i < children.getLength(); i++ ) {
			Node child = children.item( i );
			if ( child.getNodeType() == Node.ELEMENT_NODE ) {
				Element	element		= ( Element ) child;
				String	localName	= element.getLocalName();

				if ( "faultcode".equalsIgnoreCase( localName ) || "Code".equalsIgnoreCase( localName ) ) {
					faultCode = element.getTextContent();
				} else if ( "faultstring".equalsIgnoreCase( localName ) || "Reason".equalsIgnoreCase( localName ) ) {
					faultString = element.getTextContent();
				} else if ( "detail".equalsIgnoreCase( localName ) || "Detail".equalsIgnoreCase( localName ) ) {
					faultDetail = element.getTextContent();
				}
			}
		}

		throw new BoxRuntimeException(
		    "SOAP Fault: [" + faultCode + "] " + faultString
		        + ( faultDetail.isEmpty() ? "" : " - " + faultDetail )
		);
	}

	private Object xmlElementToBoxLang( Element element ) {
		NodeList children = element.getChildNodes();
		if ( hasChildElements( element ) ) {
			IStruct result = Struct.of();

			for ( int i = 0; i < children.getLength(); i++ ) {
				Node child = children.item( i );
				if ( child.getNodeType() == Node.ELEMENT_NODE ) {
					Element	childElement	= ( Element ) child;
					String	childName		= childElement.getLocalName();
					Object	childValue		= xmlElementToBoxLang( childElement );

					if ( result.containsKey( Key.of( childName ) ) ) {
						Object existing = result.get( Key.of( childName ) );
						if ( existing instanceof Array array ) {
							array.add( childValue );
						} else {
							result.put( Key.of( childName ), Array.of( existing, childValue ) );
						}
					} else {
						result.put( Key.of( childName ), childValue );
					}
				}
			}

			return result;
		} else {
			String	textContent	= element.getTextContent();
			String	xsiType		= element.getAttributeNS( XSI_NS, "type" );
			if ( xsiType != null && !xsiType.isEmpty() ) {
				return castByXsiType( textContent, xsiType );
			}
			return castStringValue( textContent );
		}
	}

	private Object castByXsiType( Object value, String xsiType ) {
		if ( value == null ) {
			return value;
		}

		String type = xsiType.contains( ":" ) ? xsiType.substring( xsiType.indexOf( ':' ) + 1 ) : xsiType;
		type = type.toLowerCase();

		try {
			switch ( type ) {
				case "boolean" :
					return ortus.boxlang.runtime.dynamic.casters.BooleanCaster.cast( value, false );
				case "int" :
				case "integer" :
					return ortus.boxlang.runtime.dynamic.casters.IntegerCaster.cast( value, false );
				case "short" :
					return ortus.boxlang.runtime.dynamic.casters.ShortCaster.cast( value, false );
				case "byte" :
					return ortus.boxlang.runtime.dynamic.casters.ByteCaster.cast( value, false );
				case "long" :
					return ortus.boxlang.runtime.dynamic.casters.LongCaster.cast( value, false );
				case "float" :
				case "double" :
				case "decimal" :
					return ortus.boxlang.runtime.dynamic.casters.DoubleCaster.cast( value, false );
				case "datetime" :
				case "date" :
				case "time" :
					return ortus.boxlang.runtime.dynamic.casters.DateTimeCaster.cast( value );
				default :
					return StringCaster.cast( value );
			}
		} catch ( Exception e ) {
			this.logger.trace( "Failed to cast value '{}' using xsi-type '{}': {}", value, xsiType, e.getMessage() );
			return value;
		}
	}

	private Object castStringValue( String value ) {
		if ( value == null || value.isEmpty() ) {
			return value;
		}

		var intAttempt = ortus.boxlang.runtime.dynamic.casters.IntegerCaster.attempt( value );
		if ( intAttempt.wasSuccessful() ) {
			return intAttempt.get();
		}

		var doubleAttempt = ortus.boxlang.runtime.dynamic.casters.DoubleCaster.attempt( value );
		if ( doubleAttempt.wasSuccessful() ) {
			return doubleAttempt.get();
		}

		var dateAttempt = ortus.boxlang.runtime.dynamic.casters.DateTimeCaster.attempt( value );
		if ( dateAttempt.wasSuccessful() ) {
			return dateAttempt.get();
		}

		return value;
	}

	private Object unwrapResponse( Object value ) {
		if ( value instanceof IStruct struct ) {
			if ( struct.size() == 1 ) {
				Object unwrapped = struct.values().iterator().next();
				return unwrapResponse( unwrapped );
			}
		}
		return value;
	}

	private boolean hasChildElements( Element element ) {
		NodeList children = element.getChildNodes();
		for ( int i = 0; i < children.getLength(); i++ ) {
			if ( children.item( i ).getNodeType() == Node.ELEMENT_NODE ) {
				return true;
			}
		}
		return false;
	}

	private Element getFirstChildElement( Node parent ) {
		NodeList children = parent.getChildNodes();
		for ( int i = 0; i < children.getLength(); i++ ) {
			Node child = children.item( i );
			if ( child.getNodeType() == Node.ELEMENT_NODE ) {
				return ( Element ) child;
			}
		}
		return null;
	}

	private String documentToString( Document doc ) {
		try {
			TransformerFactory	transformerFactory	= TransformerFactory.newInstance();
			Transformer			transformer			= transformerFactory.newTransformer();
			transformer.setOutputProperty( OutputKeys.OMIT_XML_DECLARATION, "no" );
			transformer.setOutputProperty( OutputKeys.INDENT, "no" );

			ByteArrayOutputStream	outputStream	= new ByteArrayOutputStream();
			DOMSource				source			= new DOMSource( doc );
			StreamResult			result			= new StreamResult( outputStream );

			transformer.transform( source, result );
			return outputStream.toString( StandardCharsets.UTF_8.name() );
		} catch ( Exception e ) {
			throw new BoxRuntimeException( "Failed to convert document to string", e );
		}
	}

	@Override
	public String toString() {
		return "SoapClient{"
		    + "wsdlUrl='" + this.wsdlDefinition.getWsdlUrl() + '\''
		    + ", serviceName='" + this.wsdlDefinition.getServiceName() + '\''
		    + ", operations=" + this.wsdlDefinition.getOperations().size()
		    + '}';
	}
}
