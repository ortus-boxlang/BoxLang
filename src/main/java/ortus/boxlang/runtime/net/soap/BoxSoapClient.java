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
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
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
	private static final String		XSI_TYPE_ATTR			= "xsi:type";

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
	 * SOAP version (1.1 or 1.2)
	 */
	private String					soapVersion				= "1.1";

	/**
	 * The creation timestamp
	 */
	private final Instant			createdAt				= Instant.now();

	/**
	 * The execution context
	 */
	private IBoxContext				executionContext;

	/**
	 * Statistics tracking
	 */
	private long					totalInvocations		= 0;
	private long					successfulInvocations	= 0;
	private long					failedInvocations		= 0;

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
	 * @param context        The BoxLang execution context
	 */
	private BoxSoapClient( WsdlDefinition wsdlDefinition, HttpService httpService, IBoxContext context ) {
		this.wsdlDefinition		= wsdlDefinition;
		this.httpService		= httpService;
		this.logger				= this.httpService.getLogger();
		// Initialize SOAP version from WSDL definition (can be overridden later)
		this.soapVersion		= wsdlDefinition.getSoapVersion();
		this.executionContext	= context;
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
	 * @param context     The BoxLang execution context
	 *
	 * @return A new SOAP client instance
	 *
	 * @throws BoxRuntimeException If WSDL parsing fails
	 */
	public static BoxSoapClient fromWsdl( String wsdlUrl, HttpService httpService, IBoxContext context ) {
		WsdlDefinition definition = WsdlParser.parse( wsdlUrl );
		return new BoxSoapClient( definition, httpService, context );
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
	public BoxSoapClient timeout( int timeout ) {
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
	public BoxSoapClient withBasicAuth( String username, String password ) {
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
	public BoxSoapClient header( String name, String value ) {
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
	public BoxSoapClient soapVersion( String version ) {
		if ( !"1.1".equals( version ) && !"1.2".equals( version ) ) {
			throw new BoxRuntimeException( "Invalid SOAP version: " + version + ". Must be '1.1' or '1.2'" );
		}
		this.soapVersion = version;
		return this;
	}

	/**
	 * Seed a new execution context for this client.
	 *
	 * @param context The BoxLang execution context
	 *
	 * @return This instance for chaining
	 */
	public BoxSoapClient withContext( IBoxContext context ) {
		this.executionContext = context;
		return this;
	}

	/**
	 * Set the timeout for HTTP requests.
	 * <p>
	 * The timeout is specified in seconds. If {@code null} is passed, a default of 30 seconds is used.
	 *
	 * @param timeout The timeout in seconds
	 *
	 * @return This instance for chaining
	 */
	public BoxSoapClient withTimeout( Integer timeout ) {
		this.timeout = timeout != null ? timeout : 30;
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
	 * Get all available operations on this SOAP service
	 *
	 * @return List of operation names
	 */
	public Array getOperations() {
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

	/**
	 * Convert this client to a BoxLang struct representation
	 *
	 * @return A struct with client information
	 */
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

	/**
	 * ------------------------------------------------------------------------------
	 * Operation Invocation Methods
	 * ------------------------------------------------------------------------------
	 */

	/**
	 * Invoke a SOAP operation without arguments
	 *
	 * @param operationName The operation to invoke
	 *
	 * @return The operation result
	 *
	 * @throws BoxRuntimeException If the operation fails
	 */
	public Object invoke( String operationName ) {
		return invoke( operationName, null );
	}

	/**
	 * Invoke a SOAP operation with positional arguments
	 *
	 * @param operationName The operation to invoke
	 * @param arguments     The arguments (array or struct)
	 *
	 * @return The operation result
	 *
	 * @throws BoxRuntimeException If the operation fails
	 */
	public Object invoke( String operationName, Object arguments ) {
		this.totalInvocations++;

		try {
			// Get the operation definition
			WsdlOperation operation = this.wsdlDefinition.getOperation( Key.of( operationName ) );
			if ( operation == null ) {
				throw new BoxRuntimeException(
				    "SOAP operation [" + operationName + "] not found in WSDL: "
				        + this.getWsdlUrl()
				        + ". Available operations: "
				        + this.wsdlDefinition.getOperationNames().toString()
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
			    true, // follow redirects
			    BoxHttpClient.DEFAULT_CONNECTION_TIMEOUT, // connect timeout
			    null, null, null, null, // no proxy
			    null, null // no client cert
			);

			// Build the request
			BoxHttpClient.BoxHttpRequest	request		= httpClient
			    .newRequest( this.getServiceEndpoint(), this.executionContext )
			    .post()
			    .timeout( this.timeout )
			    .header( "Accept", "application/soap+xml, text/xml, */*" )
			    .header(
			        "Content-Type",
			        "1.1".equals( this.soapVersion ) ? "text/xml; charset=utf-8" : "application/soap+xml; charset=utf-8"
			    )
			    .header( "SOAPAction", operation.getSoapAction() != null ? operation.getSoapAction() : "" )
			    .body( soapRequest );

			// Do we have any custom headers to add?
			for ( Map.Entry<String, String> header : this.customHeaders.entrySet() ) {
				request.header( header.getKey(), header.getValue() );
			}

			// Add authentication if provided
			if ( this.username != null && this.password != null ) {
				request.withBasicAuth( this.username, this.password );
			}

			// Execute the request
			IStruct	httpResult	= ( IStruct ) request.send();

			// Parse the SOAP response
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
		String methodName = name.getName();

		// Check if this is a known SoapClient method
		if ( isKnownMethod( methodName ) ) {
			// Return a reference to this client's method - the actual invocation will come via dereferenceAndInvoke
			return this;
		}

		// Check if this is a SOAP operation
		if ( this.wsdlDefinition.hasOperation( name ) ) {
			// Return a reference to this client - the actual invocation will come via dereferenceAndInvoke
			return this;
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
		if ( isKnownMethod( methodName ) ) {
			// Handle built-in methods like invoke(), getOperations(), etc.
			return handleBuiltInMethod( methodName, positionalArguments );
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
		if ( isKnownMethod( methodName ) ) {
			// Handle built-in methods like invoke(), getOperations(), etc.
			return handleBuiltInMethod( methodName, namedArguments );
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
		    || lower.equals( "tostruct" )
		    || lower.equals( "withbasicauth" )
		    || lower.equals( "withhttpsprotocol" )
		    || lower.equals( "withtimeout" )
		    || lower.equals( "withheaders" )
		    || lower.equals( "header" )
		    || lower.equals( "toString" )
		    || lower.equals( "equals" )
		    || lower.equals( "hashcode" )
		    || lower.equals( "getclass" );
	}

	/**
	 * Handle built-in method calls
	 *
	 * @param methodName The method name
	 * @param arguments  The arguments (either Object[] or Map<Key, Object>)
	 *
	 * @return The method result
	 */
	private Object handleBuiltInMethod( String methodName, Object arguments ) {
		String lower = methodName.toLowerCase();

		switch ( lower ) {
			case "invoke" :
				if ( arguments instanceof Object[] args ) {
					if ( args.length < 1 ) {
						throw new BoxRuntimeException( "invoke() requires at least 1 argument (operation name)" );
					}
					String	operationName	= StringCaster.cast( args[ 0 ] );
					Object	operationArgs	= args.length > 1 ? args[ 1 ] : null;
					return invoke( operationName, operationArgs );
				} else if ( arguments instanceof Map<?, ?> namedArgs ) {
					// Handle named arguments for invoke()
					@SuppressWarnings( "unchecked" )
					Map<Key, Object>	argMap			= ( Map<Key, Object> ) namedArgs;
					String				operationName	= StringCaster.cast( argMap.get( Key.of( "method" ) ) );
					if ( operationName == null ) {
						operationName = StringCaster.cast( argMap.get( Key.of( "operationName" ) ) );
					}
					Object operationArgs = argMap.get( Key.of( "arguments" ) );
					if ( operationArgs == null ) {
						operationArgs = argMap.get( Key.of( "args" ) );
					}
					return invoke( operationName, operationArgs );
				}
				throw new BoxRuntimeException( "Invalid arguments for invoke() method" );

			case "getoperations" :
			case "listoperations" :
				return getOperations();

			case "getstatistics" :
			case "getstats" :
				return getStatistics();

			case "getoperationinfo" :
				if ( arguments instanceof Object[] args && args.length > 0 ) {
					String operationName = StringCaster.cast( args[ 0 ] );
					return getOperationInfo( operationName );
				} else if ( arguments instanceof Map<?, ?> namedArgs ) {
					@SuppressWarnings( "unchecked" )
					Map<Key, Object>	argMap			= ( Map<Key, Object> ) namedArgs;
					String				operationName	= StringCaster.cast( argMap.get( Key.of( "operationName" ) ) );
					return getOperationInfo( operationName );
				}
				throw new BoxRuntimeException( "getOperationInfo() requires an operation name argument" );

			case "tostruct" :
				return toStruct();

			case "withbasicauth" :
				if ( arguments instanceof Object[] args && args.length >= 2 ) {
					String	username	= StringCaster.cast( args[ 0 ] );
					String	password	= StringCaster.cast( args[ 1 ] );
					return withBasicAuth( username, password );
				} else if ( arguments instanceof Map<?, ?> namedArgs ) {
					@SuppressWarnings( "unchecked" )
					Map<Key, Object>	argMap		= ( Map<Key, Object> ) namedArgs;
					String				username	= StringCaster.cast( argMap.get( Key.of( "username" ) ) );
					String				password	= StringCaster.cast( argMap.get( Key.of( "password" ) ) );
					return withBasicAuth( username, password );
				}
				throw new BoxRuntimeException( "withBasicAuth() requires username and password arguments" );

			case "header" :
				if ( arguments instanceof Object[] args && args.length >= 2 ) {
					String	headerName	= StringCaster.cast( args[ 0 ] );
					String	headerValue	= StringCaster.cast( args[ 1 ] );
					return header( headerName, headerValue );
				} else if ( arguments instanceof Map<?, ?> namedArgs ) {
					@SuppressWarnings( "unchecked" )
					Map<Key, Object>	argMap		= ( Map<Key, Object> ) namedArgs;
					String				headerName	= StringCaster.cast( argMap.get( Key.of( "name" ) ) );
					String				headerValue	= StringCaster.cast( argMap.get( Key.of( "value" ) ) );
					return header( headerName, headerValue );
				}
				throw new BoxRuntimeException( "header() requires name and value arguments" );

			case "withtimeout" :
				if ( arguments instanceof Object[] args && args.length >= 1 ) {
					Integer timeout = IntegerCaster.cast( args[ 0 ] );
					return withTimeout( timeout );
				} else if ( arguments instanceof Map<?, ?> namedArgs ) {
					@SuppressWarnings( "unchecked" )
					Map<Key, Object>	argMap	= ( Map<Key, Object> ) namedArgs;
					Integer				timeout	= IntegerCaster.cast( argMap.get( Key.of( "timeout" ) ) );
					return withTimeout( timeout );
				}
				throw new BoxRuntimeException( "withTimeout() requires a timeout value" );

			default :
				throw new BoxRuntimeException( "Unknown built-in method: " + methodName );
		}
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
			if ( logger.isTraceEnabled() ) {
				logger.trace( "Processing SOAP struct arguments with keys: " + struct.keySet() );
				logger.trace( "WSDL parameters: " + params.stream().map( p -> p.getName() ).collect( java.util.stream.Collectors.toList() ) );
			}

			// Struct arguments - use parameter names as keys
			for ( WsdlParameter param : params ) {
				String	paramName	= param.getName();
				Key		key			= Key.of( paramName );
				Object	value		= struct.get( key );

				// If exact match not found, try fuzzy matching
				if ( value == null ) {
					// If still no match, try partial matches (e.g., intA matches param 'a', intB matches param 'b')
					if ( value == null ) {
						for ( Key structKey : struct.keySet() ) {
							String structKeyName = structKey.getName();
							// Check if struct key ends with param name (intA -> a, intB -> b)
							if ( structKeyName.toLowerCase().endsWith( paramName.toLowerCase() ) ) {
								value = struct.get( structKey );
								logger.trace( "Found suffix match: " + structKeyName + " -> " + paramName );
								break;
							}
						}
					}
				}

				if ( value != null ) {
					argMap.put( paramName, value );
				} else {
					logger.trace( "No value found for WSDL param: " + paramName );
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

				// Add xsi:type attribute if type is available
				String type = param.getType();
				if ( type != null && !type.isEmpty() ) {
					String xsiType = convertWsdlTypeToXsiType( type );
					if ( xsiType != null ) {
						paramElement.setAttribute( XSI_TYPE_ATTR, xsiType );
					}
				}

				parentElement.appendChild( paramElement );
				if ( logger.isTraceEnabled() ) {
					logger.trace( "Added XML element to SOAP Request: <" + param.getName() +
					    ( type != null ? " xsi:type=\"" + convertWsdlTypeToXsiType( type ) + "\"" : "" ) +
					    ">" + value + "</" + param.getName() + ">" );
				}
			}
		}
	}

	/**
	 * Convert WSDL type to XSI type for the xsi:type attribute
	 *
	 * @param wsdlType The WSDL type (e.g., "xsd:double", "xsd:string", "xsd:int")
	 *
	 * @return The XSI type for the xsi:type attribute (e.g., "xsd:double")
	 */
	private String convertWsdlTypeToXsiType( String wsdlType ) {
		if ( wsdlType == null || wsdlType.isEmpty() ) {
			return null;
		}

		// Handle types that already have a namespace prefix (e.g., "xsd:double")
		if ( wsdlType.contains( ":" ) ) {
			return wsdlType;
		}

		// Handle simple type names - default to xsd namespace for common types
		switch ( wsdlType.toLowerCase() ) {
			case "string" :
				return "xsd:string";
			case "double" :
			case "float" :
				return "xsd:double";
			case "int" :
			case "integer" :
				return "xsd:int";
			case "long" :
				return "xsd:long";
			case "boolean" :
				return "xsd:boolean";
			case "datetime" :
			case "date" :
				return "xsd:dateTime";
			case "decimal" :
				return "xsd:decimal";
			case "byte" :
				return "xsd:byte";
			case "short" :
				return "xsd:short";
			case "anytype" :
				return "xsd:anyType";
			default :
				// For unknown types, assume they need the xsd namespace
				return "xsd:" + wsdlType;
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
			// Handle both String and byte[] responses from HTTP client
			Object	fileContent	= httpResult.get( Key.fileContent );
			String	responseBody;

			if ( fileContent instanceof String ) {
				responseBody = ( String ) fileContent;
			} else if ( fileContent instanceof byte[] castBytes ) {
				responseBody = new String( castBytes, java.nio.charset.StandardCharsets.UTF_8 );
			} else {
				throw new BoxRuntimeException( "Unexpected fileContent type: " +
				    ( fileContent != null ? fileContent.getClass().getName() : "null" ) );
			}

			if ( responseBody == null || responseBody.isEmpty() ) {
				throw new BoxRuntimeException( "Empty SOAP response received" );
			}

			// Log the response if debug enabled
			if ( this.logger.isTraceEnabled() ) {
				this.logger.trace( "SOAP Response: {}", responseBody );
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
			String errorMessage = "Failed to parse SOAP response. " + e.getMessage();
			if ( logger.isDebugEnabled() ) {
				errorMessage += " Response body: " + httpResult.get( Key.fileContent );
			}
			throw new BoxRuntimeException( errorMessage, e );
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
			// Leaf element - get text content and attempt type casting
			String	textContent	= element.getTextContent();

			// Check for xsi:type attribute for explicit type information
			String	xsiType		= element.getAttributeNS( XSI_NS, "type" );
			if ( xsiType != null && !xsiType.isEmpty() ) {
				return castByXsiType( textContent, xsiType );
			}

			// Attempt intelligent type casting based on content
			return castStringValue( textContent );
		}
	}

	/**
	 * Cast a string value to an appropriate BoxLang type using xsi:type information
	 *
	 * @param value   The string value to cast
	 * @param xsiType The xsi:type attribute value (e.g., "xsd:int", "xsd:boolean")
	 *
	 * @return The cast value
	 */
	private Object castByXsiType( Object value, String xsiType ) {
		if ( value == null ) {
			return value;
		}

		// Remove namespace prefix if present
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
					// Unknown type, return as string
					return StringCaster.cast( value );
			}
		} catch ( Exception e ) {
			// If casting fails, return as string
			this.logger.trace( "Failed to cast value '{}' using xsi-type '{}': {}", value, xsiType, e.getMessage() );
			return value;
		}
	}

	/**
	 * Attempt to intelligently cast a string value to an appropriate BoxLang type
	 *
	 * @param value The string value to cast
	 *
	 * @return The cast value, or original string if no casting applies
	 */
	private Object castStringValue( String value ) {
		if ( value == null || value.isEmpty() ) {
			return value;
		}

		// Try integer (before double to avoid losing precision)
		var intAttempt = ortus.boxlang.runtime.dynamic.casters.IntegerCaster.attempt( value );
		if ( intAttempt.wasSuccessful() ) {
			return intAttempt.get();
		}

		// Try double/float
		var doubleAttempt = ortus.boxlang.runtime.dynamic.casters.DoubleCaster.attempt( value );
		if ( doubleAttempt.wasSuccessful() ) {
			return doubleAttempt.get();
		}

		// Try datetime (ISO formats and common date patterns)
		var dateAttempt = ortus.boxlang.runtime.dynamic.casters.DateTimeCaster.attempt( value );
		if ( dateAttempt.wasSuccessful() ) {
			return dateAttempt.get();
		}

		// Return as string if no casting worked
		return value;
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
			return outputStream.toString( StandardCharsets.UTF_8 );
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
