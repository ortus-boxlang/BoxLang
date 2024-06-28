package ortus.boxlang.runtime.cache.filters;

import ortus.boxlang.runtime.application.Session;

public class PrefixFilter extends WildcardFilter {

	/**
	 * Create a new widlcard filter with a case-insensitive widlcard
	 *
	 * @param prefix The widlcard to use
	 */
	public PrefixFilter( String prefix ) {
		this( prefix, true );
	}

	/**
	 * Create a new prefix filter
	 *
	 * @param prefix     The prefix to use
	 * @param ignoreCase Whether the prefix should be case-sensitive
	 */
	public PrefixFilter( String prefix, boolean ignoreCase ) {
		super( prefix + Session.ID_CONCATENATOR + "*", ignoreCase );
	}

}
