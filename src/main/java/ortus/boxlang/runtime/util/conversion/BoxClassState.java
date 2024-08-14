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
package ortus.boxlang.runtime.util.conversion;

import java.io.ObjectStreamException;
import java.io.Serializable;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * This class represents a serialization of a BoxClass object
 */
public class BoxClassState implements Serializable {

	/**
	 * Serial version UID
	 */
	private static final long	serialVersionUID	= 1L;

	/**
	 * The variables scope to serialize
	 */
	public IStruct				variablesScope		= new Struct();

	/**
	 * The this scope to serialize
	 */
	public IStruct				thisScope			= new Struct();

	/**
	 * The BoxLang class path
	 */
	public Key					classPath;

	/**
	 * Constructor
	 */
	public BoxClassState( Key classPath, IStruct variablesScope, IStruct thisScope ) {
		this.classPath = classPath;

		variablesScope.entrySet()
		    .stream()
		    // Filter out any functions, we won't serialize those.
		    .filter( entry -> ! ( entry.getValue() instanceof Function ) )
		    .forEach( entry -> this.variablesScope.put( entry.getKey(), entry.getValue() ) );

		thisScope.entrySet()
		    .stream()
		    // Filter out any functions, we won't serialize those.
		    .filter( entry -> ! ( entry.getValue() instanceof Function ) )
		    .forEach( entry -> this.thisScope.put( entry.getKey(), entry.getValue() ) );
	}

	/**
	 * This method is called to return a new instance of Target Class after deserialization
	 *
	 * @return The deserialized class
	 *
	 * @throws ObjectStreamException
	 */
	private Object readResolve() throws ObjectStreamException {
		IBoxContext		context		= BoxRuntime.getInstance().getRuntimeContext();
		IClassRunnable	boxClass	= ( IClassRunnable ) ClassLocator
		    .getInstance()
		    .load(
		        context,
		        this.classPath.getName(),
		        ClassLocator.BX_PREFIX,
		        true,
		        context.getCurrentImports()
		    )
		    .invokeConstructor( context, Key.noInit )
		    .unWrapBoxLangClass();

		// Restore the state
		boxClass.getVariablesScope().putAll( this.variablesScope );
		boxClass.getThisScope().putAll( this.thisScope );

		// Fire away!
		return boxClass;
	}

}
