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
package ortus.boxlang.runtime.runnables;

import java.util.Map;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.ThisScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Property;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.meta.BoxMeta;

public interface IClassRunnable extends ITemplateRunnable, IReferenceable {

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the name
	 */
	public Key getName();

	/**
	 * Get the variables scope
	 */
	public VariablesScope getVariablesScope();

	/**
	 * Get the this scope
	 */
	public ThisScope getThisScope();

	/**
	 * Get annotations
	 */
	public Struct getAnnotations();

	/**
	 * Get documentation
	 */
	public Struct getDocumentation();

	/**
	 * Get the properties
	 */
	public Map<Key, Property> getProperties();

	/**
	 * Run the pseudo constructor
	 */
	public void pseudoConstructor( IBoxContext context );

	/**
	 * Get the combined metadata for this class and all it's functions
	 * This follows the format of Lucee and Adobe's "combined" metadata
	 * TODO: Move this to compat module
	 *
	 * @return The metadata as a struct
	 */
	public Struct getMetaData();

	// Duplicate from IType
	public BoxMeta getBoxMeta();

	/**
	 * A helper to look at the "output" annotation, caching the result
	 * 
	 * @return Whether the function can output
	 */
	public boolean canOutput();

	/**
	 * Get the super class. Null if there is none
	 */
	public IClassRunnable getSuper();

	/**
	 * Set the super class.
	 */
	public void setSuper( IClassRunnable _super );

	/**
	 * Get the child class. Null if there is none
	 */
	public IClassRunnable getChild();

	/**
	 * Set the child class.
	 */
	public void setChild( IClassRunnable child );

	/**
	 * Get the bottom class in the inheritance chain
	 */
	public IClassRunnable getBottomClass();

}
