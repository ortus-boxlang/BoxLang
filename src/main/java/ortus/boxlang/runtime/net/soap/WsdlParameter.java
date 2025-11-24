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

import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * Represents a parameter in a WSDL operation.
 * Contains information about the parameter name, type, and metadata.
 */
public class WsdlParameter {

	/**
	 * The parameter name
	 */
	private final String	name;

	/**
	 * The parameter type (xsd:string, xsd:int, etc.)
	 */
	private String			type;

	/**
	 * The parameter namespace
	 */
	private String			namespace;

	/**
	 * Whether this parameter is required
	 */
	private boolean			required	= false;

	/**
	 * Whether this parameter is an array
	 */
	private boolean			isArray		= false;

	/**
	 * Documentation/description
	 */
	private String			documentation;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Creates a new WSDL parameter
	 *
	 * @param name The parameter name
	 */
	public WsdlParameter( String name ) {
		this.name = name;
	}

	/**
	 * Creates a new WSDL parameter with type
	 *
	 * @param name The parameter name
	 * @param type The parameter type
	 */
	public WsdlParameter( String name, String type ) {
		this.name	= name;
		this.type	= type;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Getters and Setters
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the parameter name
	 *
	 * @return The parameter name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Get the parameter type
	 *
	 * @return The parameter type
	 */
	public String getType() {
		return this.type;
	}

	/**
	 * Set the parameter type
	 *
	 * @param type The parameter type
	 *
	 * @return This instance for chaining
	 */
	public WsdlParameter setType( String type ) {
		this.type = type;
		return this;
	}

	/**
	 * Get the namespace
	 *
	 * @return The parameter namespace
	 */
	public String getNamespace() {
		return this.namespace;
	}

	/**
	 * Set the namespace
	 *
	 * @param namespace The parameter namespace
	 *
	 * @return This instance for chaining
	 */
	public WsdlParameter setNamespace( String namespace ) {
		this.namespace = namespace;
		return this;
	}

	/**
	 * Check if this parameter is required
	 *
	 * @return True if required
	 */
	public boolean isRequired() {
		return this.required;
	}

	/**
	 * Set whether this parameter is required
	 *
	 * @param required True if required
	 *
	 * @return This instance for chaining
	 */
	public WsdlParameter setRequired( boolean required ) {
		this.required = required;
		return this;
	}

	/**
	 * Check if this parameter is an array
	 *
	 * @return True if an array
	 */
	public boolean isArray() {
		return this.isArray;
	}

	/**
	 * Set whether this parameter is an array
	 *
	 * @param isArray True if an array
	 *
	 * @return This instance for chaining
	 */
	public WsdlParameter setArray( boolean isArray ) {
		this.isArray = isArray;
		return this;
	}

	/**
	 * Get the documentation
	 *
	 * @return The parameter documentation
	 */
	public String getDocumentation() {
		return this.documentation;
	}

	/**
	 * Set the documentation
	 *
	 * @param documentation The parameter documentation
	 *
	 * @return This instance for chaining
	 */
	public WsdlParameter setDocumentation( String documentation ) {
		this.documentation = documentation;
		return this;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Utility Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Convert this parameter to a BoxLang struct
	 *
	 * @return A struct representation
	 */
	public IStruct toStruct() {
		return Struct.of(
		    "name", this.name,
		    "type", this.type,
		    "namespace", this.namespace,
		    "required", this.required,
		    "isArray", this.isArray,
		    "documentation", this.documentation
		);
	}

	/**
	 * Get a string representation
	 *
	 * @return String representation
	 */
	@Override
	public String toString() {
		return "WsdlParameter{" +
		    "name='" + this.name + '\'' +
		    ", type='" + this.type + '\'' +
		    ", required=" + this.required +
		    '}';
	}
}
