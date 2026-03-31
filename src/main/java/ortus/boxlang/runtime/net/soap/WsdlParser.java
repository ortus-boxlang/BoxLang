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

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ortus.boxlang.runtime.net.BoxHttpClient;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Parses WSDL documents and extracts service definitions, operations, and metadata.
 * Uses DOM parsing to read WSDL 1.1 documents.
 */
public class WsdlParser {

	/**
	 * WSDL namespace constants
	 */
	private static final String	WSDL_NS								= "http://schemas.xmlsoap.org/wsdl/";
	private static final String	SOAP_NS								= "http://schemas.xmlsoap.org/wsdl/soap/";
	private static final String	SOAP12_NS							= "http://schemas.xmlsoap.org/wsdl/soap12/";

	private static final String	XSD_NS								= "http://www.w3.org/2001/XMLSchema";
	private static final String	DISALLOW_DOCTYPE_DECL_FEATURE		= "http://apache.org/xml/features/disallow-doctype-decl";
	private static final String	EXTERNAL_GENERAL_ENTITIES_FEATURE	= "http://xml.org/sax/features/external-general-entities";
	private static final String	EXTERNAL_PARAMETER_ENTITIES_FEATURE	= "http://xml.org/sax/features/external-parameter-entities";

	private static final String	XMLNS_PREFIX						= "xmlns:";

	/**
	 * --------------------------------------------------------------------------
	 * Parsing Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Parse a WSDL document from a URL or file path
	 *
	 * @param wsdlUrl The WSDL URL or file path to parse
	 *
	 * @return The parsed WSDL definition
	 *
	 * @throws BoxRuntimeException If parsing fails
	 */
	public static WsdlDefinition parse( String wsdlUrl ) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware( true );
			factory.setFeature( DISALLOW_DOCTYPE_DECL_FEATURE, true );
			factory.setFeature( EXTERNAL_GENERAL_ENTITIES_FEATURE, false );
			factory.setFeature( EXTERNAL_PARAMETER_ENTITIES_FEATURE, false );

			DocumentBuilder	builder	= factory.newDocumentBuilder();
			Document		document;
			URI				uri		= new URI( wsdlUrl );

			// Check if it's a file:// URI or a local path
			if ( uri.getScheme() == null || "file".equalsIgnoreCase( uri.getScheme() ) ) {
				// Local file path - use FileUtils or File
				java.nio.file.Path filePath;
				if ( uri.getScheme() != null && "file".equalsIgnoreCase( uri.getScheme() ) ) {
					filePath = java.nio.file.Paths.get( uri );
				} else {
					// No scheme, treat as local path
					filePath = java.nio.file.Paths.get( wsdlUrl );
				}

				try ( InputStream input = java.nio.file.Files.newInputStream( filePath ) ) {
					document = builder.parse( input );
				}
			} else {
				// HTTP/HTTPS URL - use HttpURLConnection with proper User-Agent header
				URL					url			= uri.toURL();
				HttpURLConnection	connection	= ( HttpURLConnection ) url.openConnection();
				connection.setRequestProperty( "User-Agent", BoxHttpClient.DEFAULT_USER_AGENT );
				connection.setConnectTimeout( BoxHttpClient.DEFAULT_CONNECTION_TIMEOUT * 1000 );
				connection.setReadTimeout( BoxHttpClient.DEFAULT_READ_TIMEOUT * 1000 );

				try ( InputStream input = connection.getInputStream() ) {
					document = builder.parse( input );
				} finally {
					connection.disconnect();
				}
			}

			return parseDocument( wsdlUrl, document );
		} catch ( Exception e ) {
			throw new BoxRuntimeException( "Failed to parse WSDL from: " + wsdlUrl, e );
		}
	}

	/**
	 * Parse a WSDL document
	 *
	 * @param wsdlUrl  The WSDL URL (for reference)
	 * @param document The parsed XML document
	 *
	 * @return The WSDL definition
	 */
	private static WsdlDefinition parseDocument( String wsdlUrl, Document document ) {
		WsdlDefinition	definition		= new WsdlDefinition( wsdlUrl );
		Element			root			= document.getDocumentElement();

		// Extract target namespace
		String			targetNamespace	= root.getAttribute( "targetNamespace" );
		definition.setTargetNamespace( targetNamespace );

		// Extract namespace prefixes
		extractNamespaces( root, definition );

		// Parse service information
		parseService( root, definition );

		// Parse binding information
		parseBinding( root, definition );

		// Parse port type and operations
		parsePortType( root, definition );

		// Parse messages
		Map<String, Element> messages = parseMessages( root );

		// Link messages to operations
		linkMessagesToOperations( definition, messages, root );

		return definition;
	}

	/**
	 * Extract namespace declarations from the root element
	 *
	 * @param root       The root element
	 * @param definition The WSDL definition to populate
	 */
	private static void extractNamespaces( Element root, WsdlDefinition definition ) {
		org.w3c.dom.NamedNodeMap attributes = root.getAttributes();
		for ( int i = 0; i < attributes.getLength(); i++ ) {
			Node attr = attributes.item( i );
			if ( attr.getNodeName().startsWith( XMLNS_PREFIX ) ) {
				String prefix = attr.getNodeName().substring( XMLNS_PREFIX.length() );
				definition.addNamespace( prefix, attr.getNodeValue() );
			}
		}
	}

	/**
	 * Parse service element to extract endpoint information
	 *
	 * @param root       The root element
	 * @param definition The WSDL definition to populate
	 */
	private static void parseService( Element root, WsdlDefinition definition ) {
		NodeList services = root.getElementsByTagNameNS( WSDL_NS, "service" );
		if ( services.getLength() > 0 ) {
			Element service = ( Element ) services.item( 0 );
			definition.setServiceName( service.getAttribute( "name" ) );

			// Get the port
			NodeList ports = service.getElementsByTagNameNS( WSDL_NS, "port" );
			if ( ports.getLength() > 0 ) {
				Element		port			= ( Element ) ports.item( 0 );

				// Get SOAP address (try both SOAP 1.1 and 1.2)
				NodeList	soapAddresses	= port.getElementsByTagNameNS( SOAP_NS, "address" );
				if ( soapAddresses.getLength() == 0 ) {
					soapAddresses = port.getElementsByTagNameNS( SOAP12_NS, "address" );
				}

				if ( soapAddresses.getLength() > 0 ) {
					Element soapAddress = ( Element ) soapAddresses.item( 0 );
					definition.setServiceEndpoint( soapAddress.getAttribute( "location" ) );
				}
			}
		}
	}

	/**
	 * Parse binding element to extract SOAP binding style
	 *
	 * @param root       The root element
	 * @param definition The WSDL definition to populate
	 */
	private static void parseBinding( Element root, WsdlDefinition definition ) {
		NodeList bindings = root.getElementsByTagNameNS( WSDL_NS, "binding" );
		if ( bindings.getLength() > 0 ) {
			Element		binding			= ( Element ) bindings.item( 0 );

			// Get SOAP binding (try both SOAP 1.1 and 1.2)
			NodeList	soapBindings	= binding.getElementsByTagNameNS( SOAP_NS, "binding" );
			boolean		isSoap12		= false;
			if ( soapBindings.getLength() == 0 ) {
				soapBindings	= binding.getElementsByTagNameNS( SOAP12_NS, "binding" );
				isSoap12		= soapBindings.getLength() > 0;
			}

			if ( soapBindings.getLength() > 0 ) {
				Element	soapBinding	= ( Element ) soapBindings.item( 0 );
				String	style		= soapBinding.getAttribute( "style" );
				definition.setBindingStyle( style != null && !style.isEmpty() ? style : "document" );
				// Set SOAP version based on detected namespace
				definition.setSoapVersion( isSoap12 ? "1.2" : "1.1" );
			}
		}
	}

	/**
	 * Parse portType element to extract operations
	 *
	 * @param root       The root element
	 * @param definition The WSDL definition to populate
	 */
	private static void parsePortType( Element root, WsdlDefinition definition ) {
		NodeList portTypes = root.getElementsByTagNameNS( WSDL_NS, "portType" );
		if ( portTypes.getLength() > 0 ) {
			Element		portType	= ( Element ) portTypes.item( 0 );
			NodeList	operations	= portType.getElementsByTagNameNS( WSDL_NS, "operation" );

			for ( int i = 0; i < operations.getLength(); i++ ) {
				Element			operationElement	= ( Element ) operations.item( i );
				String			operationName		= operationElement.getAttribute( "name" );
				WsdlOperation	operation			= new WsdlOperation( operationName );

				// Get documentation if present
				NodeList		documentation		= operationElement.getElementsByTagNameNS( WSDL_NS, "documentation" );
				if ( documentation.getLength() > 0 ) {
					operation.setDocumentation( documentation.item( 0 ).getTextContent().trim() );
				}

				// Get input message
				NodeList input = operationElement.getElementsByTagNameNS( WSDL_NS, "input" );
				if ( input.getLength() > 0 ) {
					Element inputElement = ( Element ) input.item( 0 );
					operation.setInputMessage( getLocalName( inputElement.getAttribute( "message" ) ) );
				}

				// Get output message
				NodeList output = operationElement.getElementsByTagNameNS( WSDL_NS, "output" );
				if ( output.getLength() > 0 ) {
					Element outputElement = ( Element ) output.item( 0 );
					operation.setOutputMessage( getLocalName( outputElement.getAttribute( "message" ) ) );
				}

				// Get SOAP action from binding
				String soapAction = findSoapAction( root, operationName );
				operation.setSoapAction( soapAction );

				operation.setNamespace( definition.getTargetNamespace() );
				definition.addOperation( operation );
			}
		}
	}

	/**
	 * Parse all message elements
	 *
	 * @param root The root element
	 *
	 * @return Map of message names to elements
	 */
	private static Map<String, Element> parseMessages( Element root ) {
		Map<String, Element>	messages		= new HashMap<>();
		NodeList				messageElements	= root.getElementsByTagNameNS( WSDL_NS, "message" );

		for ( int i = 0; i < messageElements.getLength(); i++ ) {
			Element	messageElement	= ( Element ) messageElements.item( i );
			String	messageName		= messageElement.getAttribute( "name" );
			messages.put( messageName, messageElement );
		}

		return messages;
	}

	/**
	 * Link message definitions to operations and extract parameters
	 *
	 * @param definition The WSDL definition
	 * @param messages   Map of message elements
	 * @param root       The root element for schema lookup
	 */
	private static void linkMessagesToOperations( WsdlDefinition definition, Map<String, Element> messages, Element root ) {
		for ( WsdlOperation operation : definition.getOperations().values() ) {
			// Process input message
			if ( operation.getInputMessage() != null ) {
				Element messageElement = messages.get( operation.getInputMessage() );
				if ( messageElement != null ) {
					extractParameters( messageElement, operation, true, root );
				}
			}

			// Process output message
			if ( operation.getOutputMessage() != null ) {
				Element messageElement = messages.get( operation.getOutputMessage() );
				if ( messageElement != null ) {
					extractParameters( messageElement, operation, false, root );
				}
			}
		}
	}

	/**
	 * Extract parameters from a message element
	 *
	 * @param messageElement The message element
	 * @param operation      The operation to add parameters to
	 * @param isInput        True if this is an input message
	 * @param root           The root element for schema lookup
	 */
	private static void extractParameters( Element messageElement, WsdlOperation operation, boolean isInput, Element root ) {
		NodeList parts = messageElement.getElementsByTagNameNS( WSDL_NS, "part" );

		for ( int i = 0; i < parts.getLength(); i++ ) {
			Element	partElement	= ( Element ) parts.item( i );
			String	partName	= partElement.getAttribute( "name" );
			String	type		= partElement.getAttribute( "type" );
			String	element		= partElement.getAttribute( "element" );

			// For document/literal wrapped style, we need to resolve the element reference
			// and extract the actual parameters from the schema
			if ( element != null && !element.isEmpty() ) {
				// Extract the local name from the element reference (e.g., "tns:CountryCurrency" -> "CountryCurrency")
				String elementName = getLocalName( element );

				// Try to find the element definition in the schema
				if ( !extractParametersFromSchemaElement( root, elementName, operation, isInput ) ) {
					// Fallback: if we can't find schema elements, use the part name
					WsdlParameter parameter = new WsdlParameter( partName );
					parameter.setType( elementName );
					if ( isInput ) {
						operation.addInputParameter( parameter );
					} else {
						operation.addOutputParameter( parameter );
					}
				}
			} else if ( type != null && !type.isEmpty() ) {
				// RPC style - use the part name directly
				WsdlParameter parameter = new WsdlParameter( partName );
				parameter.setType( getLocalName( type ) );
				if ( isInput ) {
					operation.addInputParameter( parameter );
				} else {
					operation.addOutputParameter( parameter );
				}
			}
		}
	}

	/**
	 * Extract parameters from a schema element definition
	 *
	 * @param root        The root WSDL element
	 * @param elementName The element name to find
	 * @param operation   The operation to add parameters to
	 * @param isInput     True if this is an input message
	 *
	 * @return True if parameters were found and extracted
	 */
	private static boolean extractParametersFromSchemaElement( Element root, String elementName, WsdlOperation operation, boolean isInput ) {
		// Find the types section
		NodeList types = root.getElementsByTagNameNS( WSDL_NS, "types" );
		if ( types.getLength() == 0 ) {
			return false;
		}

		Element		typesElement	= ( Element ) types.item( 0 );

		// Find schema elements
		NodeList	schemas			= typesElement.getElementsByTagNameNS( XSD_NS, "schema" );
		for ( int i = 0; i < schemas.getLength(); i++ ) {
			Element		schema		= ( Element ) schemas.item( i );

			// Find the element definition with matching name
			NodeList	elements	= schema.getElementsByTagNameNS( XSD_NS, "element" );
			for ( int j = 0; j < elements.getLength(); j++ ) {
				Element	element	= ( Element ) elements.item( j );
				String	name	= element.getAttribute( "name" );

				if ( elementName.equals( name ) ) {
					// Found the element - now extract its children (the actual parameters)
					// Look for complexType/sequence/element children
					NodeList complexTypes = element.getElementsByTagNameNS( XSD_NS, "complexType" );
					if ( complexTypes.getLength() > 0 ) {
						Element		complexType	= ( Element ) complexTypes.item( 0 );
						NodeList	sequences	= complexType.getElementsByTagNameNS( XSD_NS, "sequence" );
						if ( sequences.getLength() > 0 ) {
							Element		sequence		= ( Element ) sequences.item( 0 );
							NodeList	paramElements	= sequence.getElementsByTagNameNS( XSD_NS, "element" );

							for ( int k = 0; k < paramElements.getLength(); k++ ) {
								Element			paramElement	= ( Element ) paramElements.item( k );
								String			paramName		= paramElement.getAttribute( "name" );
								String			paramType		= paramElement.getAttribute( "type" );

								WsdlParameter	parameter		= new WsdlParameter( paramName );
								if ( paramType != null && !paramType.isEmpty() ) {
									parameter.setType( getLocalName( paramType ) );
								}

								if ( isInput ) {
									operation.addInputParameter( parameter );
								} else {
									operation.addOutputParameter( parameter );
								}
							}

							return paramElements.getLength() > 0;
						}
					}
				}
			}
		}

		return false;
	}

	/**
	 * Find the SOAP action for an operation from the binding section.
	 * Supports both standard WSDL SOAP bindings and Axis-style bindings.
	 *
	 * @param root          The root element
	 * @param operationName The operation name
	 *
	 * @return The SOAP action, or empty string if not found
	 */
	private static String findSoapAction( Element root, String operationName ) {
		NodeList bindings = root.getElementsByTagNameNS( WSDL_NS, "binding" );
		if ( bindings.getLength() > 0 ) {
			Element		binding		= ( Element ) bindings.item( 0 );
			NodeList	operations	= binding.getElementsByTagNameNS( WSDL_NS, "operation" );

			for ( int i = 0; i < operations.getLength(); i++ ) {
				Element operationElement = ( Element ) operations.item( i );
				if ( operationName.equals( operationElement.getAttribute( "name" ) ) ) {
					// Try to find SOAP operation element from multiple possible namespaces
					String soapAction = extractSoapAction( operationElement );
					if ( soapAction != null ) {
						return soapAction;
					}
				}
			}
		}
		return "";
	}

	/**
	 * Extract soapAction from operation element, trying multiple SOAP namespace variants.
	 * This supports both standard WSDL generators and Axis-style generators.
	 *
	 * @param operationElement The operation element from the binding
	 *
	 * @return The soapAction value, or null if not found
	 */
	private static String extractSoapAction( Element operationElement ) {
		// Array of namespace URIs to check for SOAP operations
		String[] soapNamespaces = { SOAP_NS, SOAP12_NS };

		for ( String namespace : soapNamespaces ) {
			NodeList soapOperations = operationElement.getElementsByTagNameNS( namespace, "operation" );
			if ( soapOperations.getLength() > 0 ) {
				Element	soapOperation	= ( Element ) soapOperations.item( 0 );
				String	soapAction		= soapOperation.getAttribute( "soapAction" );
				if ( soapAction != null ) {
					return soapAction; // Return even if empty string - this is valid for Axis
				}
			}
		}

		// Also try looking for wsdlsoap:operation (some Axis generators use this pattern)
		NodeList wsdlSoapOps = operationElement.getElementsByTagName( "wsdlsoap:operation" );
		if ( wsdlSoapOps.getLength() > 0 ) {
			Element	soapOperation	= ( Element ) wsdlSoapOps.item( 0 );
			String	soapAction		= soapOperation.getAttribute( "soapAction" );
			if ( soapAction != null ) {
				return soapAction;
			}
		}

		return null;
	}

	/**
	 * Get the local name from a QName string (strip namespace prefix)
	 *
	 * @param qname The qualified name
	 *
	 * @return The local name
	 */
	private static String getLocalName( String qname ) {
		if ( qname == null || qname.isEmpty() ) {
			return qname;
		}
		int colonIndex = qname.indexOf( ':' );
		return colonIndex >= 0 ? qname.substring( colonIndex + 1 ) : qname;
	}
}
