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
package ortus.boxlang.runtime.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import ortus.boxlang.runtime.scopes.Key;

/**
 * This allows any key-value pair of attachable data to be attached to a Box object.
 */
public class Attachable implements IBoxAttachable {

	/**
	 * The map of attachments.
	 */
	private Map<Key, Object> attachments = new ConcurrentHashMap<>();

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	public Attachable() {
		// Empty constructor
	}

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Put a key-value pair of attachable data to the Box object.
	 * If an attachment for this key was already set, return the original value.
	 *
	 * @param key The key to attach the value to.
	 * @param <T> The type of the value to attach.
	 */
	@SuppressWarnings( "unchecked" )
	public <T> T putAttachment( Key key, T value ) {
		return ( T ) this.attachments.put( key, value );
	}

	/**
	 * Get the attachment for the given key.
	 *
	 * @param key The key to get the attachment for.
	 *
	 * @return The attachment for the given key, or null if there is none.
	 */
	@SuppressWarnings( "unchecked" )
	public <T> T getAttachment( Key key ) {
		return ( T ) this.attachments.get( key );
	}

	/**
	 * Verify if an attachment is present for the given key.
	 *
	 * @param key
	 *
	 * @return true if an attachment is present for the given key, false otherwise.
	 */
	public boolean hasAttachment( Key key ) {
		return this.attachments.containsKey( key );
	}

	/**
	 * Remove an attachment, returning its previous value.
	 *
	 * @param key The key to remove the attachment for.
	 *
	 * @return The previous value attached to the key, or null if there was none.
	 */
	@SuppressWarnings( "unchecked" )
	public <T> T removeAttachment( Key key ) {
		return ( T ) this.attachments.remove( key );
	}

	/**
	 * Get the keys of all attachments.
	 */
	public Key[] getAttachmentKeys() {
		return this.attachments.keySet().toArray( new Key[ 0 ] );
	}

}
