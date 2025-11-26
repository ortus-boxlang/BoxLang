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
package ortus.boxlang.runtime.types.meta;

import java.io.Serializable;

import org.checkerframework.checker.nullness.qual.NonNull;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * This is a base class for all meta types
 */
public abstract class BoxMeta<T> implements Serializable {

	/**
	 * The key used for BoxLang meta data when dereferencing
	 */
	public static final Key key = Key.$bx;

	/**
	 * Get target object this metadata is for.
	 * Implementations should return the target object.
	 */
	@NonNull
	public abstract T getTarget();

	/**
	 * Get the meta data of the target object.
	 * Implementations should return the meta data.
	 */
	public abstract IStruct getMeta();

	/**
	 * So we can get a pretty print of the metadata
	 */
	public String toString() {
		return Struct.of(
		    "meta", getMeta().asString(),
		    "$class", getClass().getName()
		).asString();
	}

	/**
	 * Register a change listener
	 *
	 * @param listener The listener to register
	 */
	public T registerChangeListener( IChangeListener<T> listener ) {
		return ensureTargetListenable().registerChangeListener( listener );
	}

	/**
	 * Register a change listener for a specific key
	 *
	 * @param key      The key to listen for changes on
	 * @param listener The listener to register
	 */
	public T registerChangeListener( Key key, IChangeListener<T> listener ) {
		return ensureTargetListenable().registerChangeListener( key, listener );
	}

	/**
	 * Remove a change listener for a specific key
	 *
	 * @param key The listener to remove
	 */
	public T removeChangeListener( Key key ) {
		return ensureTargetListenable().removeChangeListener( key );
	}

	/**
	 * Ensure the target is listenable
	 *
	 * @return The target as a listenable
	 *
	 * @throws BoxRuntimeException If the target is not listenable
	 */
	@SuppressWarnings( { "unchecked", "null" } )
	private IListenable<T> ensureTargetListenable() {
		if ( getTarget() instanceof IListenable listenable ) {
			return listenable;
		} else {
			throw new BoxRuntimeException( "Target [" + getTarget().getClass().getName() + "] does not support change listeners." );
		}
	}

}
