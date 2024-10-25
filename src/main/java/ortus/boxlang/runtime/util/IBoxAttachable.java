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

import ortus.boxlang.runtime.scopes.Key;

/**
 * This allows any key-value pair of attachable data to be attached to a Box object.
 */
public interface IBoxAttachable {

	/**
	 * Comput an attachment if it is not already present.
	 * If an attachment for this key was already set, return the original value.
	 *
	 * @param key             The key to attach the value to.
	 * @param mappingFunction The function to compute the value to attach.
	 *
	 */
	public <T> T computeAttachmentIfAbsent( Key key, java.util.function.Function<? super Key, ? extends T> mappingFunction );

	/**
	 * Put a key-value pair of attachable data to the Box object.
	 * If an attachment for this key was already set, return the original value.
	 *
	 * @param key The key to attach the value to.
	 * @param <T> The type of the value to attach.
	 */
	public <T> T putAttachment( Key key, T value );

	/**
	 * Get the attachment for the given key.
	 *
	 * @param key The key to get the attachment for.
	 *
	 * @return The attachment for the given key, or null if there is none.
	 */
	public <T> T getAttachment( Key key );

	/**
	 * Verify if an attachment is present for the given key.
	 *
	 * @param key
	 *
	 * @return true if an attachment is present for the given key, false otherwise.
	 */
	public boolean hasAttachment( Key key );

	/**
	 * Remove an attachment, returning its previous value.
	 *
	 * @param key The key to remove the attachment for.
	 *
	 * @return The previous value attached to the key, or null if there was none.
	 */
	public <T> T removeAttachment( Key key );

	/**
	 * Get the keys of all attachments.
	 */
	public Key[] getAttachmentKeys();

}
