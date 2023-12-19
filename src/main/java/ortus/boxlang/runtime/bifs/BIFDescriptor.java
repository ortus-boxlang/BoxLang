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

public class BIFDescriptor {

	public Key		name;
	public Class<?>	BIFClass;
	public String	module;
	public String	namespace;
	public BIF		BIFInstance;
	public Boolean	isGlobal;

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

	public Boolean hasModule() {
		return module != null;
	}

	public Boolean hasNamespace() {
		return namespace != null;
	}

	public BIF getBIF() {
		if ( this.BIFInstance == null ) {
			synchronized ( this ) {
				// Double check inside lock
				if ( this.BIFInstance != null ) {
					return this.BIFInstance;
				}
				this.BIFInstance = ( BIF ) DynamicObject.of( this.BIFClass ).invokeConstructor().getTargetInstance();
			}
		}
		return this.BIFInstance;
	}

	/**
	 * Invoke the BIF with no arguments
	 * 
	 * @param context
	 * 
	 * @return The result of the invocation
	 */
	public Object invoke( IBoxContext context, boolean isMember ) {
		ArgumentsScope scope = new ArgumentsScope();
		scope.put( BIF.__isMemberExectution, isMember );
		return this.getBIF().invoke( context, scope );
	}

	/**
	 * Invoke the BIF with positional arguments
	 * 
	 * @param context
	 * 
	 * @return The result of the invocation
	 */
	public Object invoke( IBoxContext context, Object[] positionalArguments, boolean isMember ) {
		ArgumentsScope scope = ArgumentUtil.createArgumentsScope( positionalArguments, getBIF().getArguments() );
		scope.put( BIF.__isMemberExectution, isMember );
		// Invoke it baby!
		return this.getBIF().invoke( context, scope );
	}

	/**
	 * Invoke the BIF with named arguments
	 * 
	 * @param context
	 * 
	 * @return The result of the invocation
	 */
	public Object invoke( IBoxContext context, Map<Key, Object> namedArguments, boolean isMember ) {
		ArgumentsScope scope = ArgumentUtil.createArgumentsScope( namedArguments, getBIF().getArguments() );
		scope.put( BIF.__isMemberExectution, isMember );
		// Invoke it baby!
		return this.getBIF().invoke( context, scope );
	}

}
