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
package ortus.boxlang.runtime.cache.policies;

import java.util.Comparator;

import ortus.boxlang.runtime.cache.ICacheEntry;

/**
 * The base interface for a cache policy in BoxLang.
 * Each policy must return a Comparator based on the policy's rules
 * and the {@link ICacheEntry} interface.
 *
 * This is also a functional interface, so you can use a lambda expression
 * to implement it.
 *
 * BoxLang ships with the following policies:
 * - Least Recently Used (LRU)
 * - Most Recently Used (MRU)
 * - Least Frequently Used (LFU)
 * - Most Frequently Used (MFU)
 * - First In First Out (FIFO)
 * - Last In First Out (LIFO)
 * - Random
 */
@FunctionalInterface
public interface ICachePolicy {

	/**
	 * Get the comparator for this policy
	 *
	 * @return The comparator for this policy
	 */
	public Comparator<ICacheEntry> getComparator();

}
