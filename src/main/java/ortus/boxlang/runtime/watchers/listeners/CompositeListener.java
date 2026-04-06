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
package ortus.boxlang.runtime.watchers.listeners;

import java.util.List;

import ortus.boxlang.runtime.watchers.WatcherContext;
import ortus.boxlang.runtime.watchers.WatcherEvent;

/**
 * A composite listener that fans out every event and error notification to an ordered list of
 * delegate {@link IWatcherListener} instances.
 * <p>
 * Each delegate's {@code onEvent} is invoked in sequence. If a delegate throws, the error
 * is forwarded to that same delegate's {@code onError}, and iteration continues to the next
 * delegate rather than aborting the fan-out.
 * </p>
 */
public class CompositeListener implements IWatcherListener {

	private final List<IWatcherListener> delegates;

	/**
	 * Construct from an ordered list of delegates.
	 *
	 * @param delegates the list of listeners to fan out to
	 */
	public CompositeListener( List<IWatcherListener> delegates ) {
		this.delegates = List.copyOf( delegates );
	}

	/**
	 * {@inheritDoc}
	 * Invokes each delegate's {@code onEvent} in order; errors in one delegate do not
	 * prevent subsequent delegates from being called.
	 */
	@Override
	public void onEvent( WatcherEvent event, WatcherContext context ) {
		for ( IWatcherListener delegate : delegates ) {
			try {
				delegate.onEvent( event, context );
			} catch ( Exception e ) {
				try {
					delegate.onError( e, context );
				} catch ( Exception ignored ) {
					// Swallow — WatcherInstance will log this at the loop level
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * Fans out the error notification to all delegates in order.
	 */
	@Override
	public void onError( Exception exception, WatcherContext context ) {
		for ( IWatcherListener delegate : delegates ) {
			try {
				delegate.onError( exception, context );
			} catch ( Exception ignored ) {
				// Swallow
			}
		}
	}

	/**
	 * Return an unmodifiable view of the delegates.
	 *
	 * @return the delegate list
	 */
	public List<IWatcherListener> getDelegates() {
		return delegates;
	}

}
