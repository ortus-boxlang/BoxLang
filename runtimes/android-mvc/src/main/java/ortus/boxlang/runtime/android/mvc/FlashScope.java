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
package ortus.boxlang.runtime.android.mvc;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * A ColdBox-style flash scope: data that survives exactly one request hop (e.g. across a
 * {@code relocate()}), then is discarded. Used for post-redirect-get messages.
 * <p>
 * Two internal buckets are maintained: {@code now} (readable during the current request)
 * and {@code next} (staged for the following request). {@link #put} writes to {@code next};
 * {@link #keep} re-stages the surviving data; {@link #persist} rotates {@code next} into
 * {@code now} for the upcoming request.
 * <p>
 * On Android there is a single in-process runtime, so this lives in memory tied to the
 * runtime instance rather than an HTTP session.
 */
public class FlashScope {

	/**
	 * Data readable during the current request.
	 */
	private IStruct	now		= new Struct();

	/**
	 * Data staged to be readable on the next request.
	 */
	private IStruct	next	= new Struct();

	/**
	 * Stage a value to be available on the next request.
	 *
	 * @param key   The key
	 * @param value The value
	 *
	 * @return This flash scope for chaining
	 */
	public FlashScope put( String key, Object value ) {
		this.next.put( Key.of( key ), value );
		return this;
	}

	/**
	 * Read a value available during the current request.
	 *
	 * @param key The key
	 *
	 * @return The value, or {@code null} if absent
	 */
	public Object get( String key ) {
		return this.now.get( Key.of( key ) );
	}

	/**
	 * Read a value with a default fallback.
	 *
	 * @param key          The key
	 * @param defaultValue The fallback value
	 *
	 * @return The value, or {@code defaultValue} if absent
	 */
	public Object get( String key, Object defaultValue ) {
		Object value = this.now.get( Key.of( key ) );
		return value == null ? defaultValue : value;
	}

	/**
	 * @param key The key
	 *
	 * @return {@code true} if a value is available this request under the given key
	 */
	public boolean exists( String key ) {
		return this.now.containsKey( Key.of( key ) );
	}

	/**
	 * @return The current-request flash contents (live view)
	 */
	public IStruct getScope() {
		return this.now;
	}

	/**
	 * Re-stage all currently-readable values so they survive another request hop.
	 *
	 * @return This flash scope for chaining
	 */
	public FlashScope keep() {
		this.next.putAll( this.now );
		return this;
	}

	/**
	 * Rotate the staged data into the readable bucket for the upcoming request and clear
	 * the staging bucket. Called by the framework at the start of each request.
	 */
	public void persist() {
		this.now	= this.next;
		this.next	= new Struct();
	}

	/**
	 * Clear all flash data in both buckets.
	 */
	public void clear() {
		this.now.clear();
		this.next.clear();
	}
}
