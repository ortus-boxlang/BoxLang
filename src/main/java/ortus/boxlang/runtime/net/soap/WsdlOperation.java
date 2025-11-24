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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * Represents a SOAP operation defined in a WSDL.
 * Contains information about the operation name, input/output parameters, and namespaces.
 */
public class WsdlOperation {

	/**
	 * The operation name
	 */
	private final String				name;

	/**
	 * The SOAP action URI
	 */
	private String						soapAction;

	/**
	 * The input message name
	 */
	private String						inputMessage;

	/**
	 * The output message name
	 */
	private String						outputMessage;

	/**
	 * List of input parameters
	 */
	private final List<WsdlParameter>	inputParameters		= new ArrayList<>();

	/**
	 * List of output parameters
	 */
	private final List<WsdlParameter>	outputParameters	= new ArrayList<>();

	/**
	 * The operation namespace
	 */
	private String						namespace;

	/**
	 * Documentation/description
	 */
	private String						documentation;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Creates a new WSDL operation
	 *
	 * @param name The operation name
	 */
	public WsdlOperation( String name ) {
		this.name = name;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Getters and Setters
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the operation name
	 *
	 * @return The operation name
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Get the SOAP action
	 *
	 * @return The SOAP action URI
	 */
	public String getSoapAction() {
		return this.soapAction;
	}

	/**
	 * Set the SOAP action
	 *
	 * @param soapAction The SOAP action URI
	 *
	 * @return This instance for chaining
	 */
	public WsdlOperation setSoapAction( String soapAction ) {
		this.soapAction = soapAction;
		return this;
	}

	/**
	 * Get the input message name
	 *
	 * @return The input message name
	 */
	public String getInputMessage() {
		return this.inputMessage;
	}

	/**
	 * Set the input message name
	 *
	 * @param inputMessage The input message name
	 *
	 * @return This instance for chaining
	 */
	public WsdlOperation setInputMessage( String inputMessage ) {
		this.inputMessage = inputMessage;
		return this;
	}

	/**
	 * Get the output message name
	 *
	 * @return The output message name
	 */
	public String getOutputMessage() {
		return this.outputMessage;
	}

	/**
	 * Set the output message name
	 *
	 * @param outputMessage The output message name
	 *
	 * @return This instance for chaining
	 */
	public WsdlOperation setOutputMessage( String outputMessage ) {
		this.outputMessage = outputMessage;
		return this;
	}

	/**
	 * Get the namespace
	 *
	 * @return The operation namespace
	 */
	public String getNamespace() {
		return this.namespace;
	}

	/**
	 * Set the namespace
	 *
	 * @param namespace The operation namespace
	 *
	 * @return This instance for chaining
	 */
	public WsdlOperation setNamespace( String namespace ) {
		this.namespace = namespace;
		return this;
	}

	/**
	 * Get the documentation
	 *
	 * @return The operation documentation
	 */
	public String getDocumentation() {
		return this.documentation;
	}

	/**
	 * Set the documentation
	 *
	 * @param documentation The operation documentation
	 *
	 * @return This instance for chaining
	 */
	public WsdlOperation setDocumentation( String documentation ) {
		this.documentation = documentation;
		return this;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Parameter Management
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Add an input parameter
	 *
	 * @param parameter The parameter to add
	 *
	 * @return This instance for chaining
	 */
	public WsdlOperation addInputParameter( WsdlParameter parameter ) {
		this.inputParameters.add( parameter );
		return this;
	}

	/**
	 * Add an output parameter
	 *
	 * @param parameter The parameter to add
	 *
	 * @return This instance for chaining
	 */
	public WsdlOperation addOutputParameter( WsdlParameter parameter ) {
		this.outputParameters.add( parameter );
		return this;
	}

	/**
	 * Get all input parameters
	 *
	 * @return Unmodifiable list of input parameters
	 */
	public List<WsdlParameter> getInputParameters() {
		return Collections.unmodifiableList( this.inputParameters );
	}

	/**
	 * Get all output parameters
	 *
	 * @return Unmodifiable list of output parameters
	 */
	public List<WsdlParameter> getOutputParameters() {
		return Collections.unmodifiableList( this.outputParameters );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Utility Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Convert this operation to a BoxLang struct
	 *
	 * @return A struct representation
	 */
	public IStruct toStruct() {
		return Struct.of(
		    "name", this.name,
		    "soapAction", this.soapAction,
		    "namespace", this.namespace,
		    "inputMessage", this.inputMessage,
		    "outputMessage", this.outputMessage,
		    "inputParameterCount", this.inputParameters.size(),
		    "outputParameterCount", this.outputParameters.size(),
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
		return "WsdlOperation{" +
		    "name='" + this.name + '\'' +
		    ", soapAction='" + this.soapAction + '\'' +
		    ", inputParams=" + this.inputParameters.size() +
		    ", outputParams=" + this.outputParameters.size() +
		    '}';
	}
}
