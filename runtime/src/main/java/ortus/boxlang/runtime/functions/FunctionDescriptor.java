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

import java.util.Optional;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;

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

	public DynamicObject getBIF() throws ClassNotFoundException {
		if ( this.BIF == null ) {
			synchronized ( this ) {
				this.BIF = DynamicObject.of( Class.forName( this.className ) );
			}
		}
		return this.BIF;
	}

	public Optional<Object> invoke( Object... arguments ) throws Throwable, IllegalArgumentException {
		// Check first argument, it must be the context
		if ( arguments.length == 0 || ! ( arguments[ 0 ] instanceof IBoxContext ) ) {
			throw new IllegalArgumentException( "First argument must be an IBoxContext" );
		}
		// Invoke it baby!
		return this.getBIF().invoke( "invoke", arguments );
	}

}
