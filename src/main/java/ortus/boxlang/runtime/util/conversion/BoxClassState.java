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
import java.util.Optional;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
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
	 *
	 * @param target The target class to serialize
	 */
	public BoxClassState( IClassRunnable target ) {
		// Store the class path
		this.classPath = target.getName();
		// Get the metadata properties to see which ones
		// are NOT serializable
		Array aProperties = target.getBoxMeta().getMeta().getAsArray( Key.properties );

		// Serialize the variables and this scope
		target.getVariablesScope()
		    .entrySet()
		    .stream()
		    // Filter out any functions, we won't serialize those for now, unless
		    // We figure out how to recreate them.
		    .filter( entry -> ! ( entry.getValue() instanceof Function ) )
		    // Filter out non-serializable properties
		    .filter( entry -> isSerializable( aProperties, entry.getKey() ) )
		    .forEach( entry -> this.variablesScope.put( entry.getKey(), entry.getValue() ) );

		target.getThisScope()
		    .entrySet()
		    .stream()
		    .filter( entry -> ! ( entry.getValue() instanceof Function ) )
		    .forEach( entry -> this.thisScope.put( entry.getKey(), entry.getValue() ) );
	}

	/**
	 * Check if a property is serializable
	 *
	 * @param properties The metadata properties
	 * @param property   The property to check
	 *
	 * @return True if the property is serializable
	 */
	private Boolean isSerializable( Array properties, Key property ) {
		Optional<IStruct> propertyMetadata = properties
		    .stream()
		    .map( IStruct.class::cast )
		    .filter( prop -> prop.getAsKey( Key.nameAsKey ).equals( property ) )
		    .findFirst();

		if ( propertyMetadata.isPresent() ) {
			// Check if the property is serializable
			return BooleanCaster.cast(
			    propertyMetadata.get()
			        .getAsStruct( Key.annotations )
			        .getOrDefault( Key.serializable, true )
			);
		}
		return true;
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
