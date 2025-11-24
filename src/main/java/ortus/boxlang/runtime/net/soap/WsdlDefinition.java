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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * Represents a parsed WSDL definition containing service endpoints, operations, and metadata.
 * This class encapsulates all information extracted from a WSDL document.
 */
public class WsdlDefinition {

	/**
	 * The original WSDL URL
	 */
	private final String					wsdlUrl;

	/**
	 * The target namespace of the WSDL
	 */
	private String							targetNamespace;

	/**
	 * The service name
	 */
	private String							serviceName;

	/**
	 * The service endpoint URL
	 */
	private String							serviceEndpoint;

	/**
	 * Map of operation names to operation definitions
	 */
	private final Map<Key, WsdlOperation>	operations	= new ConcurrentHashMap<>();

	/**
	 * Additional namespace mappings
	 */
	private final Map<String, String>		namespaces	= new ConcurrentHashMap<>();

	/**
	 * SOAP binding style (document or rpc)
	 */
	private String							bindingStyle;

	/**
	 * Timestamp when this definition was created
	 */
	private final java.time.Instant			createdAt;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Creates a new WSDL definition for the given URL
	 *
	 * @param wsdlUrl The WSDL URL
	 */
	public WsdlDefinition( String wsdlUrl ) {
		this.wsdlUrl	= wsdlUrl;
		this.createdAt	= java.time.Instant.now();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Getters and Setters
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the WSDL URL
	 *
	 * @return The WSDL URL
	 */
	public String getWsdlUrl() {
		return this.wsdlUrl;
	}

	/**
	 * Get the target namespace
	 *
	 * @return The target namespace
	 */
	public String getTargetNamespace() {
		return this.targetNamespace;
	}

	/**
	 * Set the target namespace
	 *
	 * @param targetNamespace The target namespace
	 *
	 * @return This instance for chaining
	 */
	public WsdlDefinition setTargetNamespace( String targetNamespace ) {
		this.targetNamespace = targetNamespace;
		return this;
	}

	/**
	 * Get the service name
	 *
	 * @return The service name
	 */
	public String getServiceName() {
		return this.serviceName;
	}

	/**
	 * Set the service name
	 *
	 * @param serviceName The service name
	 *
	 * @return This instance for chaining
	 */
	public WsdlDefinition setServiceName( String serviceName ) {
		this.serviceName = serviceName;
		return this;
	}

	/**
	 * Get the service endpoint URL
	 *
	 * @return The service endpoint URL
	 */
	public String getServiceEndpoint() {
		return this.serviceEndpoint;
	}

	/**
	 * Set the service endpoint URL
	 *
	 * @param serviceEndpoint The service endpoint URL
	 *
	 * @return This instance for chaining
	 */
	public WsdlDefinition setServiceEndpoint( String serviceEndpoint ) {
		this.serviceEndpoint = serviceEndpoint;
		return this;
	}

	/**
	 * Get the binding style
	 *
	 * @return The binding style (document or rpc)
	 */
	public String getBindingStyle() {
		return this.bindingStyle;
	}

	/**
	 * Set the binding style
	 *
	 * @param bindingStyle The binding style (document or rpc)
	 *
	 * @return This instance for chaining
	 */
	public WsdlDefinition setBindingStyle( String bindingStyle ) {
		this.bindingStyle = bindingStyle;
		return this;
	}

	/**
	 * Get the creation timestamp
	 *
	 * @return The creation timestamp
	 */
	public java.time.Instant getCreatedAt() {
		return this.createdAt;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Operation Management
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Add an operation to this definition
	 *
	 * @param operation The operation to add
	 *
	 * @return This instance for chaining
	 */
	public WsdlDefinition addOperation( WsdlOperation operation ) {
		this.operations.put( Key.of( operation.getName() ), operation );
		return this;
	}

	/**
	 * Get an operation by name
	 *
	 * @param operationName The operation name
	 *
	 * @return The operation, or null if not found
	 */
	public WsdlOperation getOperation( Key operationName ) {
		return this.operations.get( operationName );
	}

	/**
	 * Check if an operation exists
	 *
	 * @param operationName The operation name
	 *
	 * @return True if the operation exists
	 */
	public boolean hasOperation( Key operationName ) {
		return this.operations.containsKey( operationName );
	}

	/**
	 * Get all operations
	 *
	 * @return Unmodifiable map of operations
	 */
	public Map<Key, WsdlOperation> getOperations() {
		return Collections.unmodifiableMap( this.operations );
	}

	/**
	 * Get all operation names
	 *
	 * @return List of operation names
	 */
	public List<String> getOperationNames() {
		return this.operations.keySet().stream()
		    .map( Key::getName )
		    .sorted()
		    .toList();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Namespace Management
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Add a namespace mapping
	 *
	 * @param prefix The namespace prefix
	 * @param uri    The namespace URI
	 *
	 * @return This instance for chaining
	 */
	public WsdlDefinition addNamespace( String prefix, String uri ) {
		this.namespaces.put( prefix, uri );
		return this;
	}

	/**
	 * Get a namespace URI by prefix
	 *
	 * @param prefix The namespace prefix
	 *
	 * @return The namespace URI, or null if not found
	 */
	public String getNamespace( String prefix ) {
		return this.namespaces.get( prefix );
	}

	/**
	 * Get all namespaces
	 *
	 * @return Unmodifiable map of namespaces
	 */
	public Map<String, String> getNamespaces() {
		return Collections.unmodifiableMap( this.namespaces );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Utility Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Convert this definition to a BoxLang struct
	 *
	 * @return A struct representation
	 */
	public IStruct toStruct() {
		IStruct result = Struct.of(
		    "wsdlUrl", this.wsdlUrl,
		    "targetNamespace", this.targetNamespace,
		    "serviceName", this.serviceName,
		    "serviceEndpoint", this.serviceEndpoint,
		    "bindingStyle", this.bindingStyle,
		    "createdAt", this.createdAt.toString(),
		    "operationCount", this.operations.size(),
		    "operations", this.getOperationNames()
		);

		return result;
	}

	/**
	 * Get a string representation
	 *
	 * @return String representation
	 */
	@Override
	public String toString() {
		return "WsdlDefinition{" +
		    "wsdlUrl='" + this.wsdlUrl + '\'' +
		    ", serviceName='" + this.serviceName + '\'' +
		    ", operations=" + this.operations.size() +
		    '}';
	}
}
