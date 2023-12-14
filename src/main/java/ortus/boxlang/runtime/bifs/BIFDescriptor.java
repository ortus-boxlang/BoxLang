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
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.ArgumentUtil;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class BIFDescriptor {

	public String			name;
	public String			className;
	public String			module;
	public String			namespace;
	public DynamicObject	BIF;
	public Boolean			isGlobal;

	public BIFDescriptor(
	    String name,
	    String className,
	    String module,
	    String namespace,
	    Boolean isGlobal,
	    DynamicObject BIF ) {
		this.name		= name;
		this.className	= className;
		this.module		= module;
		this.namespace	= namespace;
		this.isGlobal	= isGlobal;
		this.BIF		= BIF;
	}

	public Boolean hasModule() {
		return module != null;
	}

	public Boolean hasNamespace() {
		return namespace != null;
	}

	public DynamicObject getBIF() {
		if ( this.BIF == null ) {
			synchronized ( this ) {
				try {
					this.BIF = DynamicObject.of( Class.forName( this.className ) );
				} catch ( ClassNotFoundException e ) {
					throw new BoxRuntimeException( "Error loading class for BIF.", e );
				}
			}
		}
		return this.BIF;
	}

	/**
	 * Invoke the BIF with no arguments
	 * 
	 * @param context
	 * 
	 * @return The result of the invocation
	 */
	public Object invoke( IBoxContext context ) {
		return this.getBIF().invoke( "invoke", context, new ArgumentsScope() );
	}

	/**
	 * Invoke the BIF with positional arguments
	 * 
	 * @param context
	 * 
	 * @return The result of the invocation
	 */
	public Object invoke( IBoxContext context, Object[] positionalArguments ) {
		ArgumentsScope scope = ArgumentUtil.createArgumentsScope( positionalArguments, ( Argument[] ) getBIF().getField( "arguments" ).get() );
		// Invoke it baby!
		return this.getBIF().invoke( "invoke", context, scope );
	}

	/**
	 * Invoke the BIF with named arguments
	 * 
	 * @param context
	 * 
	 * @return The result of the invocation
	 */
	public Object invoke( IBoxContext context, Map<Key, Object> namedArguments ) {
		ArgumentsScope scope = ArgumentUtil.createArgumentsScope( namedArguments, ( Argument[] ) getBIF().getField( "arguments" ).get() );
		// Invoke it baby!
		return this.getBIF().invoke( "invoke", context, scope );
	}

}
