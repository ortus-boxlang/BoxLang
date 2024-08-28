/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.type;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

@BoxBIF( documented = false )
// MOVE THIS TO THE COMPAT MODULE WHEN THE TRANSPILER IS MOVED THERE
public class TranspileCollectionKeySwap extends BIF {

	/**
	 * Constructor
	 */
	public TranspileCollectionKeySwap() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key.collection ),
		    new Argument( true, "Struct", Key.keyMap )
		};
	}

	/**
	 * This is used behind the scenes to help do runtime transpiling of argumentCollection and attributeCollection
	 * by swapping keys out. If the input is not a Struct, it will be returned as is. If the input is a Struct,
	 * and has the fromKey, it will be replaced with the toKey.
	 * MOVE THIS TO THE COMPAT MODULE WHEN THE TRANSPILER IS MOVED THERE
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.collection The collection to transpile
	 * 
	 * @argument.keyMap The map of keys to swap. The key is the key to replace, the value is the key to replace it with.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object					oCollection		= arguments.get( Key.collection );

		CastAttempt<IStruct>	structAttempt	= StructCaster.attempt( oCollection );
		if ( !structAttempt.wasSuccessful() ) {
			return oCollection;
		}
		IStruct	collection		= structAttempt.get();
		// Don't modify original struct
		IStruct	newCollection	= Struct.fromMap( collection );
		IStruct	keyMap			= arguments.getAsStruct( Key.keyMap );
		for ( var keySet : keyMap.entrySet() ) {
			Key	fromKey	= keySet.getKey();
			Key	toKey	= Key.of( keySet.getValue().toString() );
			if ( newCollection.containsKey( fromKey ) ) {
				Object value = newCollection.get( fromKey );
				newCollection.remove( fromKey );
				newCollection.put( toKey, value );
			}
		}
		return newCollection;
	}

}
