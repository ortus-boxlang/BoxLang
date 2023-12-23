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
package ortus.boxlang.runtime.bifs;

import java.util.Map;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.ArgumentUtil;

/**
 * This class is used to describe a BIF
 * as it can be a global BIF or a member BIF or both or coming from a module
 * It also lazily creates the BIF instance and caches it upon first use
 */
public class BIFDescriptor {

	/**
	 * BIF name
	 */
	public Key			name;

	/**
	 * BIF class
	 */
	public Class<?>		BIFClass;

	/**
	 * Module name, or null if global
	 */
	public String		module;

	/**
	 * Namespace name, or null if global
	 */
	public String		namespace;

	/**
	 * BIF instance, lazily created
	 */
	public volatile BIF	BIFInstance;

	/**
	 * Is this a global BIF?
	 */
	public Boolean		isGlobal;

	/**
	 * Constructor for a global BIF
	 *
	 * @param name        The name of the BIF
	 * @param BIFClass    The class of the BIF
	 * @param module      The module name, or null if global
	 * @param namespace   The namespace name, or null if global
	 * @param isGlobal    Is this a global BIF?
	 * @param BIFInstance The BIF instance or null by default
	 */
	public BIFDescriptor(
	    Key name,
	    Class<?> BIFClass,
	    String module,
	    String namespace,
	    Boolean isGlobal,
	    BIF BIFInstance ) {
		this.name			= name;
		this.BIFClass		= BIFClass;
		this.module			= module;
		this.namespace		= namespace;
		this.isGlobal		= isGlobal;
		this.BIFInstance	= BIFInstance;
	}

	/**
	 * Descriptor belongs to a modules or not
	 *
	 * @return True if the descriptor belongs to a module, false otherwise
	 */
	public Boolean hasModule() {
		return module != null;
	}

	/**
	 * Descriptor belongs to a namespace or not
	 *
	 * @return True if the descriptor belongs to a namespace, false otherwise
	 */
	public Boolean hasNamespace() {
		return namespace != null;
	}

	/**
	 * Get the BIF instance for this descriptor and lazily create it if needed
	 *
	 * @return The BIF instance
	 */
	public BIF getBIF() {
		if ( this.BIFInstance == null ) {
			synchronized ( this ) {
				// Double check inside lock
				if ( this.BIFInstance == null ) {
					this.BIFInstance = ( BIF ) DynamicObject.of( this.BIFClass ).invokeConstructor( ( IBoxContext ) null ).getTargetInstance();
				}
			}
		}
		return this.BIFInstance;
	}

	/**
	 * Invoke the BIF with no arguments
	 *
	 * @param context  The context
	 * @param isMember Is this a member BIF?
	 *
	 * @return The result of the invocation
	 */
	public Object invoke( IBoxContext context, boolean isMember ) {
		ArgumentsScope scope = new ArgumentsScope();
		scope.put( BIF.__isMemberExecution, isMember );
		return this.getBIF().invoke( context, scope );
	}

	/**
	 * Invoke the BIF with positional arguments
	 *
	 * @param context             The context
	 * @param positionalArguments The positional arguments
	 * @param isMember            Is this a member BIF?
	 *
	 * @return The result of the invocation
	 */
	public Object invoke( IBoxContext context, Object[] positionalArguments, boolean isMember, Key name ) {
		ArgumentsScope scope = ArgumentUtil.createArgumentsScope( positionalArguments, getBIF().getDeclaredArguments() );
		scope.put( BIF.__isMemberExecution, isMember );
		scope.put( BIF.__executionName, name );
		// Invoke it baby!
		return this.getBIF().invoke( context, scope );
	}

	/**
	 * Invoke the BIF with named arguments
	 *
	 * @param context        The context
	 * @param namedArguments The named arguments
	 * @param isMember       Is this a member BIF?
	 *
	 * @return The result of the invocation
	 */
	public Object invoke( IBoxContext context, Map<Key, Object> namedArguments, boolean isMember ) {
		ArgumentsScope scope = ArgumentUtil.createArgumentsScope( namedArguments, getBIF().getDeclaredArguments() );
		scope.put( BIF.__isMemberExecution, isMember );
		// Invoke it baby!
		return this.getBIF().invoke( context, scope );
	}

}
