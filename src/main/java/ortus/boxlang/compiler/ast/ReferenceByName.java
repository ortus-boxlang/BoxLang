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
package ortus.boxlang.compiler.ast;

/**
 * Represent a reference by the name to a Node
 */
public class ReferenceByName {

	private final String	name;
	private Node			reference;

	/**
	 * Returns the name of the reference
	 *
	 * @return name of the reference
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the referenced node
	 *
	 * @return the Node referenced by the name
	 *
	 * @see Node
	 */
	public Node getReference() {
		return reference;
	}

	/**
	 * Set the referenced node referred by the name
	 *
	 * @param reference the Node referred
	 *
	 * @see Node
	 */
	public void setReference( Node reference ) {
		this.reference = reference;
	}

	/**
	 *
	 * @param name
	 */
	public ReferenceByName( String name ) {
		this.name = name;
	}
}
