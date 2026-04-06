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
package ortus.boxlang.runtime.watchers.exceptions;

import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.watchers.WatcherEvent;

/**
 * Exception thrown by the WatcherService when an error occurs during file watching.
 * <p>
 * Extends {@link BoxRuntimeException} so it integrates with BoxLang's standard
 * exception handling and can be serialized via {@code ExceptionUtil.throwableToStruct()}.
 * </p>
 */
public class WatcherException extends BoxRuntimeException {

	/**
	 * The filesystem event that was being processed when the error occurred.
	 * May be {@code null} for loop-level errors not tied to a specific event.
	 */
	private final WatcherEvent event;

	/**
	 * Constructor for loop-level errors with no associated event.
	 *
	 * @param message the error message
	 * @param cause   the underlying cause
	 */
	public WatcherException( String message, Throwable cause ) {
		this( message, null, cause );
	}

	/**
	 * Constructor with an associated filesystem event.
	 *
	 * @param message the error message
	 * @param event   the event being processed when the error occurred (may be null)
	 * @param cause   the underlying cause
	 */
	public WatcherException( String message, WatcherEvent event, Throwable cause ) {
		super( message, null, "WatcherException", event != null ? event.toStruct() : "", cause );
		this.event = event;
	}

	/**
	 * Get the filesystem event associated with this error, or {@code null} if none.
	 *
	 * @return the associated WatcherEvent, or null
	 */
	public WatcherEvent getEvent() {
		return event;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public IStruct dataAsStruct() {
		IStruct result = super.dataAsStruct();
		if ( this.event != null ) {
			result.put( ortus.boxlang.runtime.scopes.Key.event, this.event.toStruct() );
		}
		return result;
	}

}
