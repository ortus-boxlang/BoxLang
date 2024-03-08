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
package ortus.boxlang.runtime.cache.filters;

import com.google.common.base.Predicate;

import ortus.boxlang.runtime.scopes.Key;

/**
 * This interface can be used to filter cache keys when using the cache provider.
 * It can be used to filter keys based on a pattern or a regular expression.
 *
 * It is functional and can be used as a lambda or method reference.
 *
 * <pre>
 * apply( Key t )
 * </pre>
 */
@FunctionalInterface
public interface ICacheKeyFilter extends Predicate<Key> {
	// No methods to implement since it's a functional interface and only has one method
	// apply(T t);
}
