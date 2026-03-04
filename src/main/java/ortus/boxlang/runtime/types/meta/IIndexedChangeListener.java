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

import ortus.boxlang.runtime.scopes.Key;

/**
 * Extended change listener for indexed collections (arrays, queries, etc.) that need to
 * distinguish between an insert (shifting existing elements) and a set/replace operation.
 * <p>
 * Listeners that only need key/value change notifications can implement {@link IChangeListener}.
 * Listeners that need insert-vs-replace context should implement this interface instead.
 */
@FunctionalInterface
public interface IIndexedChangeListener<T> extends IChangeListener<T> {

	/**
	 * Call when a value is being changed in an indexed IListenable, with insert context.
	 * An insert means the value is being added at the index, shifting existing elements.
	 * A non-insert means the value is replacing an existing element at the index.
	 *
	 * @param key      The key of the value being changed (1-based for arrays)
	 * @param newValue The new value (null if being removed)
	 * @param oldValue The old value (null if being added)
	 * @param object   The object being listened to
	 * @param isInsert Whether this is an insert (true) or a set/replace (false)
	 *
	 * @return The new value to be set (you can override)
	 */
	Object notify( Key key, Object newValue, Object oldValue, T object, boolean isInsert );

	/**
	 * Default implementation of the 4-arg notify that delegates to the 5-arg version with isInsert=false.
	 */
	@Override
	default Object notify( Key key, Object newValue, Object oldValue, T object ) {
		return notify( key, newValue, oldValue, object, false );
	}

}
