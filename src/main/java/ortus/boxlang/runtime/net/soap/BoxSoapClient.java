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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
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
 * SoapClient client = SoapClient.fromWsdl( "http://example.com/service.wsdl" );
 * Object result = client.invoke( "methodName", args );
 * </pre>
 *
 * Or using createObject:
 *
 * <pre>
 * ws = createObject( "webservice", "http://example.com/service.wsdl" );
 * result = ws.methodName( arg1, arg2 );
 * </pre>
 */
public class BoxSoapClient implements IReferenceable {

	/**
	 * ------------------------------------------------------------------------------
	 * Constants
	 * ------------------------------------------------------------------------------
	 */

	private static final String			SOAP_11_ENVELOPE_NS		= "http://schemas.xmlsoap.org/soap/envelope/";
	private static final String			SOAP_12_ENVELOPE_NS		= "http://www.w3.org/2003/05/soap-envelope";
	private static final String			XSI_NS					= "http://www.w3.org/2001/XMLSchema-instance";
	private static final String			XSD_NS					= "http://www.w3.org/2001/XMLSchema";

	/**
	 * ------------------------------------------------------------------------------
	 * Properties
	 * ------------------------------------------------------------------------------
	 */

	/**
	 * The WSDL definition for this client
	 */
	private final WsdlDefinition		wsdlDefinition;

	/**
	 * The HTTP service for making requests
	 */
	private final HttpService			httpService;

	/**
	 * The logger
	 */
	private final BoxLangLogger			logger;

	/**
	 * Request timeout in seconds (0 = no timeout)
	 */
	private int							timeout					= 30;

	/**
	 * Username for HTTP basic authentication
	 */
	private String						username;

	/**
	 * Password for HTTP basic authentication
	 */
	private String						password;

	/**
	 * Custom HTTP headers
	 */
	private final Map<String, String>	customHeaders			= new HashMap<>();

	/**
	 * SOAP version (1.1 or 1.2)
	 */
	private String						soapVersion				= "1.1";

	/**
	 * The creation timestamp
	 */
	private final Instant				createdAt;

	/**
	 * Statistics tracking
	 */
	private long						totalInvocations		= 0;
	private long						successfulInvocations	= 0;
	private long						failedInvocations		= 0;

	/**
	 * ------------------------------------------------------------------------------
	 * Constructors
	 * ------------------------------------------------------------------------------
	 */

	/**
	 * Private constructor - use static factory methods
	 *
	 * @param wsdlDefinition The WSDL definition
	 * @param httpService    The HTTP service
	 * @param logger         The logger
	 */
	private BoxSoapClient( WsdlDefinition wsdlDefinition, HttpService httpService, BoxLangLogger logger ) {
		this.wsdlDefinition	= wsdlDefinition;
		this.httpService	= httpService;
		this.logger			= logger;
		this.createdAt		= Instant.now();
		// Initialize SOAP version from WSDL definition (can be overridden later)
		this.soapVersion	= wsdlDefinition.getSoapVersion();
	}

	/**
	 * ------------------------------------------------------------------------------
	 * Static Factory Methods
	 * ------------------------------------------------------------------------------
	 */

	/**
	 * Create a new SOAP client from a WSDL URL
	 *
	 * @param wsdlUrl     The WSDL URL to parse
	 * @param httpService The HTTP service to use
	 * @param logger      The logger to use
	 *
	 * @return A new SOAP client instance
	 *
	 * @throws BoxRuntimeException If WSDL parsing fails
	 */
	public static BoxSoapClient fromWsdl( String wsdlUrl, HttpService httpService, BoxLangLogger logger ) {
		WsdlDefinition definition = WsdlParser.parse( wsdlUrl );
		return new BoxSoapClient( definition, httpService, logger );
	}

	/**
	 * Create a new SOAP client from a cached WSDL definition
	 *
	 * @param wsdlDefinition The cached WSDL definition
	 * @param httpService    The HTTP service to use
	 * @param logger         The logger to use
	 *
	 * @return A new SOAP client instance
	 */
	public static BoxSoapClient fromDefinition( WsdlDefinition wsdlDefinition, HttpService httpService, BoxLangLogger logger ) {
		return new BoxSoapClient( wsdlDefinition, httpService, logger );
	}

	/**
	 * ------------------------------------------------------------------------------
	 * Configuration Methods (Fluent)
	 * ------------------------------------------------------------------------------
	 */

	/**
	 * Set the request timeout
	 *
	 * @param timeout The timeout in seconds
	 *
	 * @return This instance for chaining
	 */
	public BoxSoapClient setTimeout( int timeout ) {
		this.timeout = timeout;
		return this;
	}

	/**
	 * Set HTTP basic authentication credentials
	 *
	 * @param username The username
	 * @param password The password
	 *
	 * @return This instance for chaining
	 */
	public BoxSoapClient setAuthentication( String username, String password ) {
		this.username	= username;
		this.password	= password;
		return this;
	}

	/**
	 * Add a custom HTTP header
	 *
	 * @param name  The header name
	 * @param value The header value
	 *
	 * @return This instance for chaining
	 */
	public BoxSoapClient addHeader( String name, String value ) {
		this.customHeaders.put( name, value );
		return this;
	}

	/**
	 * Set the SOAP version to use (1.1 or 1.2).
	 * By default, the SOAP version is automatically detected from the WSDL binding.
	 * Use this method to override the detected version if needed.
	 *
	 * @param version The SOAP version ("1.1" or "1.2")
	 *
	 * @return This instance for chaining
	 */
	public BoxSoapClient setSoapVersion( String version ) {
		if ( !"1.1".equals( version ) && !"1.2".equals( version ) ) {
			throw new BoxRuntimeException( "Invalid SOAP version: " + version + ". Must be '1.1' or '1.2'" );
		}
		this.soapVersion = version;
		return this;
	}

	/**
	 * ------------------------------------------------------------------------------
	 * Information Methods
	 * ------------------------------------------------------------------------------
	 */

	/**
	 * Get the SOAP version being used
	 *
	 * @return The SOAP version ("1.1" or "1.2")
	 */
	public String getSoapVersion() {
		return this.soapVersion;
	}

	/**
	 * Get the WSDL definition
	 *
	 * @return The WSDL definition
	 */
	public WsdlDefinition getWsdlDefinition() {
		return this.wsdlDefinition;
	}

	/**
	 * Get the WSDL URL
	 *
	 * @return The WSDL URL
	 */
	public String getWsdlUrl() {
		return this.wsdlDefinition.getWsdlUrl();
	}

	/**
	 * Get the service endpoint URL
	 *
	 * @return The service endpoint URL
	 */
	public String getServiceEndpoint() {
		return this.wsdlDefinition.getServiceEndpoint();
	}

	/**
	 * Get all available operation names
	 *
	 * @return List of operation names
	 */
	public List<String> getOperationNames() {
		return this.wsdlDefinition.getOperationNames();
	}

	/**
	 * Check if an operation exists
	 *
	 * @param operationName The operation name
	 *
	 * @return True if the operation exists
	 */
	public boolean hasOperation( String operationName ) {
		return this.wsdlDefinition.hasOperation( Key.of( operationName ) );
	}

	/**
	 * Get information about a specific operation
	 *
	 * @param operationName The operation name
	 *
	 * @return A struct containing operation information, or null if not found
	 */
	public IStruct getOperationInfo( String operationName ) {
		WsdlOperation operation = this.wsdlDefinition.getOperation( Key.of( operationName ) );
		return operation != null ? operation.toStruct() : null;
	}

	/**
	 * Get client statistics
	 *
	 * @return A struct containing statistics
	 */
	public IStruct getStatistics() {
		return Struct.of(
		    "totalInvocations", this.totalInvocations,
		    "successfulInvocations", this.successfulInvocations,
		    "failedInvocations", this.failedInvocations,
		    "wsdlUrl", this.wsdlDefinition.getWsdlUrl(),
		    "serviceEndpoint", this.wsdlDefinition.getServiceEndpoint(),
		    "operationCount", this.wsdlDefinition.getOperations().size(),
		    "createdAt", this.createdAt.toString()
		);
	}

	/**
	 * Convert this client to a BoxLang struct representation
	 *
	 * @return A struct with client information
	 */
	public IStruct toStruct() {
		return Struct.of(
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

	/**
	 * ------------------------------------------------------------------------------
	 * Operation Invocation Methods
	 * ------------------------------------------------------------------------------
	 */

	/**
	 * Invoke a SOAP operation with positional arguments
	 *
	 * @param context       The BoxLang context
	 * @param operationName The operation to invoke
	 * @param arguments     The arguments (array or struct)
	 *
	 * @return The operation result
	 *
	 * @throws BoxRuntimeException If the operation fails
	 */
	public Object invoke( IBoxContext context, String operationName, Object arguments ) {
		this.totalInvocations++;

		try {
			// Get the operation definition
			WsdlOperation operation = this.wsdlDefinition.getOperation( Key.of( operationName ) );
			if ( operation == null ) {
				throw new BoxRuntimeException(
				    "Operation '" + operationName + "' not found in WSDL. Available operations: " +
				        String.join( ", ", this.getOperationNames() )
				);
			}

			// Build the SOAP request
			String soapRequest = buildSoapRequest( operation, arguments );

			// Log the request if debug enabled
			if ( this.logger.isDebugEnabled() ) {
				this.logger.debug( "SOAP Request to {}: {}", this.getServiceEndpoint(), soapRequest );
			}

			// Execute the HTTP request using a default HTTP client
			BoxHttpClient					httpClient	= this.httpService.getOrBuildClient(
			    "HTTP/2", // HTTP version
			    false, // follow redirects
			    30, // connect timeout
			    null, null, null, null, // no proxy
			    null, null // no client cert
			);

			// Build the request
			BoxHttpClient.BoxHttpRequest	request		= httpClient.newRequest( this.getServiceEndpoint(), context )
			    .post()
			    .timeout( this.timeout )
			    .header(
			        "Content-Type",
			        "1.1".equals( this.soapVersion ) ? "text/xml; charset=utf-8" : "application/soap+xml; charset=utf-8"
			    )
			    .header( "SOAPAction", operation.getSoapAction() != null ? operation.getSoapAction() : "" )
			    .body( soapRequest );

			// Debugging
			System.out.println( "SOAP Request: " + ":\n" + soapRequest );
			System.out.println( "HTTP Request: " + request.inspect().toString() );

			// Add authentication if provided
			if ( this.username != null && this.password != null ) {
				request.withBasicAuth( this.username, this.password );
			}

			// Execute the request
			IStruct httpResult = ( IStruct ) request.send();

			System.out.println( "SOAP Response: " + httpResult.toString() );

			// Parse the SOAP response
			Object result = parseSoapResponse( httpResult, operation );

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
	 * Dereference a key (used for property access or to check if method exists)
	 *
	 * @param context The BoxLang context
	 * @param name    The name to dereference
	 * @param safe    Whether to use safe navigation
	 *
	 * @return The dereferenced value (for properties) or the name itself (for methods)
	 */
	@Override
	public Object dereference( IBoxContext context, Key name, Boolean safe ) {
		String methodName = name.getName();

		// Check if this is an operation name or known method
		if ( this.hasOperation( methodName ) || isKnownMethod( methodName ) ) {
			// Just return the name - actual invocation happens in dereferenceAndInvoke
			return methodName;
		}

		// Check for built-in properties that can be accessed directly
		switch ( methodName.toLowerCase() ) {
			case "getoperations" :
			case "listoperations" :
				return this.getOperationNames();
			case "getstatistics" :
			case "getstats" :
				return this.getStatistics();
			case "tostruct" :
				return this.toStruct();
			default :
				if ( safe ) {
					return null;
				}
				throw new BoxRuntimeException( "Unknown method or operation: " + methodName );
		}
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
	 * Dereference and invoke a method
	 *
	 * @param context   The BoxLang context
	 * @param name      The method name
	 * @param arguments The arguments
	 * @param safe      Whether to use safe navigation
	 *
	 * @return The invocation result
	 */
	@Override
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Map<Key, Object> arguments, Boolean safe ) {
		String methodName = name.getName();

		// Check if this is a SOAP operation
		if ( this.hasOperation( methodName ) ) {
			return this.invoke( context, methodName, arguments );
		}

		// Handle built-in methods
		switch ( methodName.toLowerCase() ) {
			case "invoke" :
				String operationName = ( String ) arguments.get( Key.method );
				Object argCollection = arguments.get( Key.argumentCollection );
				return this.invoke( context, operationName, argCollection );

			case "getoperationinfo" :
				String opName = ( String ) arguments.get( Key.of( "operation" ) );
				return this.getOperationInfo( opName );

			case "getoperations" :
			case "listoperations" :
				return this.getOperationNames();

			case "getstatistics" :
			case "getstats" :
				return this.getStatistics();

			case "tostruct" :
				return this.toStruct();

			default :
				if ( safe ) {
					return null;
				}
				throw new BoxRuntimeException( "Unknown method or operation: " + methodName );
		}
	}

	/**
	 * Dereference and invoke with array of arguments
	 *
	 * @param context   The BoxLang context
	 * @param name      The method name
	 * @param arguments The arguments array
	 * @param safe      Whether to use safe navigation
	 *
	 * @return The invocation result
	 */
	@Override
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Object[] arguments, Boolean safe ) {
		String methodName = name.getName();

		// Check if this is a SOAP operation - pass first argument as params
		if ( this.hasOperation( methodName ) ) {
			return this.invoke( context, methodName, arguments.length > 0 ? arguments[ 0 ] : Struct.EMPTY );
		}

		// Handle built-in methods (most don't take args with positional style)
		switch ( methodName.toLowerCase() ) {
			case "getoperations" :
			case "listoperations" :
				return this.getOperationNames();

			case "getstatistics" :
			case "getstats" :
				return this.getStatistics();

			case "tostruct" :
				return this.toStruct();

			case "getoperationinfo" :
				if ( arguments.length > 0 ) {
					return this.getOperationInfo( arguments[ 0 ].toString() );
				}
				throw new BoxRuntimeException( "getOperationInfo requires an operation name argument" );

			default :
				if ( safe ) {
					return null;
				}
				throw new BoxRuntimeException( "Unknown method or operation: " + methodName );
		}
	}

	/**
	 * Assign a value to a key (not supported)
	 *
	 * @param context The BoxLang context
	 * @param name    The name to assign to
	 * @param value   The value to assign
	 *
	 * @return The assigned value
	 */
	@Override
	public Object assign( IBoxContext context, Key name, Object value ) {
		throw new BoxRuntimeException( "Cannot assign to web service properties" );
	}

	/**
	 * ------------------------------------------------------------------------------
	 * SOAP Message Building and Parsing
	 * ------------------------------------------------------------------------------
	 */

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

			// Determine SOAP namespace
			String			soapNS		= "1.1".equals( this.soapVersion ) ? SOAP_11_ENVELOPE_NS : SOAP_12_ENVELOPE_NS;

			// Create Envelope
			Element			envelope	= doc.createElementNS( soapNS, "soap:Envelope" );
			envelope.setAttribute( "xmlns:soap", soapNS );
			envelope.setAttribute( "xmlns:xsi", XSI_NS );
			envelope.setAttribute( "xmlns:xsd", XSD_NS );

			// Add target namespace if available
			if ( operation.getNamespace() != null ) {
				envelope.setAttribute( "xmlns:tns", operation.getNamespace() );
			}

			doc.appendChild( envelope );

			// Create Body
			Element body = doc.createElementNS( soapNS, "soap:Body" );
			envelope.appendChild( body );

			// Create operation element
			Element operationElement = doc.createElement( operation.getName() );
			if ( operation.getNamespace() != null ) {
				operationElement.setAttribute( "xmlns", operation.getNamespace() );
			}
			body.appendChild( operationElement );

			// Add parameters
			addParametersToRequest( doc, operationElement, operation, arguments );

			// Convert to string
			return documentToString( doc );

		} catch ( Exception e ) {
			throw new BoxRuntimeException( "Failed to build SOAP request", e );
		}
	}

	/**
	 * Add parameters to the SOAP request
	 *
	 * @param doc           The XML document
	 * @param parentElement The parent element to add parameters to
	 * @param operation     The operation definition
	 * @param arguments     The arguments
	 */
	private void addParametersToRequest( Document doc, Element parentElement, WsdlOperation operation, Object arguments ) {
		List<WsdlParameter> params = operation.getInputParameters();

		if ( arguments == null || params.isEmpty() ) {
			return;
		}

		// Convert arguments to a map for easy access
		Map<String, Object> argMap = new HashMap<>();

		if ( arguments instanceof IStruct struct ) {
			// Struct arguments - use parameter names as keys
			for ( WsdlParameter param : params ) {
				Key		key		= Key.of( param.getName() );
				Object	value	= struct.get( key );
				if ( value != null ) {
					argMap.put( param.getName(), value );
				}
			}
		} else if ( arguments instanceof Array array ) {
			// Array arguments - map by position
			for ( int i = 0; i < params.size() && i < array.size(); i++ ) {
				argMap.put( params.get( i ).getName(), array.get( i ) );
			}
		} else if ( arguments instanceof Object[] array ) {
			// Object array arguments - map by position
			for ( int i = 0; i < params.size() && i < array.length; i++ ) {
				argMap.put( params.get( i ).getName(), array[ i ] );
			}
		} else {
			// Single argument - assign to first parameter
			if ( !params.isEmpty() ) {
				argMap.put( params.get( 0 ).getName(), arguments );
			}
		}

		// Add each parameter as an element
		for ( WsdlParameter param : params ) {
			Object value = argMap.get( param.getName() );
			if ( value != null ) {
				Element paramElement = doc.createElement( param.getName() );
				paramElement.setTextContent( String.valueOf( value ) );
				parentElement.appendChild( paramElement );
			}
		}
	}

	/**
	 * Parse a SOAP response and extract the result
	 *
	 * @param httpResult The HTTP result struct
	 * @param operation  The operation that was invoked
	 *
	 * @return The parsed result
	 */
	private Object parseSoapResponse( IStruct httpResult, WsdlOperation operation ) {
		try {
			String responseBody = httpResult.getAsString( Key.fileContent );

			if ( responseBody == null || responseBody.isEmpty() ) {
				throw new BoxRuntimeException( "Empty SOAP response received" );
			}

			// Log the response if debug enabled
			if ( this.logger.isDebugEnabled() ) {
				this.logger.debug( "SOAP Response: {}", responseBody );
			}

			// Parse the XML response
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware( true );
			DocumentBuilder	builder	= factory.newDocumentBuilder();
			Document		doc		= builder.parse( new InputSource( new StringReader( responseBody ) ) );

			// Check for SOAP Fault
			NodeList		faults	= doc.getElementsByTagNameNS( "*", "Fault" );
			if ( faults.getLength() > 0 ) {
				return parseSoapFault( ( Element ) faults.item( 0 ) );
			}

			// Extract the response body
			NodeList bodies = doc.getElementsByTagNameNS( "*", "Body" );
			if ( bodies.getLength() > 0 ) {
				Element	body		= ( Element ) bodies.item( 0 );
				// The first child of Body should be the response element
				Element	response	= getFirstChildElement( body );
				if ( response != null ) {
					Object result = xmlElementToBoxLang( response );
					// Unwrap single-property structs (common SOAP pattern)
					return unwrapResponse( result );
				}
			}

			return responseBody; // Return raw body if we can't parse it

		} catch ( Exception e ) {
			throw new BoxRuntimeException( "Failed to parse SOAP response", e );
		}
	}

	/**
	 * Parse a SOAP fault into a BoxLang exception
	 *
	 * @param faultElement The Fault element
	 *
	 * @return Never returns, always throws
	 */
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
		    "SOAP Fault: [" + faultCode + "] " + faultString +
		        ( faultDetail.isEmpty() ? "" : " - " + faultDetail )
		);
	}

	/**
	 * Convert an XML element to a BoxLang type
	 *
	 * @param element The XML element
	 *
	 * @return The BoxLang value
	 */
	private Object xmlElementToBoxLang( Element element ) {
		// If the element has child elements, convert to a struct
		NodeList children = element.getChildNodes();
		if ( hasChildElements( element ) ) {
			IStruct result = Struct.of();

			for ( int i = 0; i < children.getLength(); i++ ) {
				Node child = children.item( i );
				if ( child.getNodeType() == Node.ELEMENT_NODE ) {
					Element	childElement	= ( Element ) child;
					String	childName		= childElement.getLocalName();
					Object	childValue		= xmlElementToBoxLang( childElement );

					// Handle multiple elements with the same name (convert to array)
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
			// Leaf element - return text content
			return element.getTextContent();
		}
	}

	/**
	 * Unwrap single-property structs from SOAP responses.
	 * Many SOAP services wrap the actual result in a container element.
	 * For example:
	 * { ListOfContinentsByNameResult: { tContinent: [...] } }
	 * becomes:
	 * { tContinent: [...] }
	 *
	 * This only unwraps if the struct has exactly ONE property, preserving
	 * multi-property responses unchanged.
	 *
	 * @param value The value to potentially unwrap
	 *
	 * @return The unwrapped value or the original value if not a single-property struct
	 */
	private Object unwrapResponse( Object value ) {
		if ( value instanceof IStruct struct ) {
			// Only unwrap if there's exactly one property
			if ( struct.size() == 1 ) {
				// Get the single value
				Object unwrapped = struct.values().iterator().next();
				// Recursively unwrap in case of nested single-property structs
				return unwrapResponse( unwrapped );
			}
		}
		return value;
	}

	/**
	 * Check if an element has child elements (not just text)
	 *
	 * @param element The element to check
	 *
	 * @return True if it has child elements
	 */
	private boolean hasChildElements( Element element ) {
		NodeList children = element.getChildNodes();
		for ( int i = 0; i < children.getLength(); i++ ) {
			if ( children.item( i ).getNodeType() == Node.ELEMENT_NODE ) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get the first child element of a node
	 *
	 * @param parent The parent node
	 *
	 * @return The first child element, or null
	 */
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

	/**
	 * Convert an XML document to a string
	 *
	 * @param doc The XML document
	 *
	 * @return The XML as a string
	 */
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

	/**
	 * Get a string representation
	 *
	 * @return String representation
	 */
	@Override
	public String toString() {
		return "SoapClient{" +
		    "wsdlUrl='" + this.wsdlDefinition.getWsdlUrl() + '\'' +
		    ", serviceName='" + this.wsdlDefinition.getServiceName() + '\'' +
		    ", operations=" + this.wsdlDefinition.getOperations().size() +
		    '}';
	}
}
