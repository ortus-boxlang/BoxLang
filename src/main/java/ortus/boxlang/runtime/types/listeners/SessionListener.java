package ortus.boxlang.runtime.types.listeners;

import ortus.boxlang.runtime.application.Session;
import ortus.boxlang.runtime.scopes.Key;

public class SessionListener {

	private final Session session;

	public SessionListener( Session session ) {
		this.session = session;
	}

	/**
	 * Call when a value is being changed in an IListenable
	 *
	 * @param key      The key of the value being changed. For arrays, the key will be 1-based
	 * @param newValue The new value (null if being removed)
	 * @param oldValue The old value (null if being added)
	 *
	 * @return The new value to be set (you can override)
	 */
	public Object notify( Key key, Object newValue, Object oldValue ) {
		// this.session.getApplication().getSessionsCache().set( this.session.getID().getName(), this.session );
		return null;
	}

}
