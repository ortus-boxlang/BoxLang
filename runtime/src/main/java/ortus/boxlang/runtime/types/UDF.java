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
package ortus.boxlang.runtime.types;

import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.GenericCaster;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Represents a UDF. A UDF is specifically a function that is defined with the "function name()" syntax.
 * UDFs have names, access, hints, etc which closures do not have.
 */
public abstract class UDF extends Function {

	/**
	 * The access modifier of the function
	 */
	private Access	access;

	/**
	 * The return type of the function
	 */
	private String	returnType;

	/**
	 * The hint of the function
	 */
	private String	hint;

	/**
	 * Whether the function outputs
	 */
	private boolean	output;

	// TODO: cachedwithin, modifier, localmode, return format

	/**
	 * Constructor
	 */
	protected UDF( Access access, Key name, String returnType, Argument[] arguments, String hint, boolean output ) {
		super( name, arguments );
		this.access		= access;
		this.returnType	= returnType;
		this.hint		= hint;
		this.output		= output;
	}

	public Access getAccess() {
		return access;
	}

	public String getReturnType() {
		return returnType;
	}

	public String getHint() {
		return hint;
	}

	public boolean isOutput() {
		return output;
	}

	protected Object ensureReturnType( Object value ) {
		CastAttempt<Object> typeCheck = GenericCaster.attempt( value, getReturnType(), true );
		if ( !typeCheck.wasSuccessful() ) {
			throw new RuntimeException(
			    String.format( "The return value of the function [%s] does not match the declared type of [%s]",
			        value.getClass().getName(), getReturnType() )
			);
		}
		// Should we actually return the casted value??? Not CFML Compat!
		return value;
	}
}
