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
package ortus.boxlang.runtime.functions;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.types.exceptions.ApplicationException;

public class FunctionDescriptor {

	public String			name;
	public String			className;
	public String			module;
	public String			namespace;
	public DynamicObject	BIF;
	public Boolean			isGlobal;

	public FunctionDescriptor(
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
					throw new ApplicationException( "Error loading class for BIF.", e );
				}
			}
		}
		return this.BIF;
	}

	public Object invoke( IBoxContext context, Object... arguments ) {
		Object[] combined = new Object[ 1 + arguments.length ];
		combined[ 0 ] = context;
		System.arraycopy( arguments, 0, combined, 1, arguments.length );

		// Invoke it baby!
		return this.getBIF().invoke( "invoke", combined ).get();
	}

}
