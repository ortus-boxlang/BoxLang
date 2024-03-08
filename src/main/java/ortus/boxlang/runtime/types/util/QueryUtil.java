package ortus.boxlang.runtime.types.util;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Query;

public class QueryUtil {

	/**
	 * Checks if a column exists in a query.
	 *
	 * @param query  the query to check
	 * @param column the name of the column to check
	 * 
	 * @return true if the column exists, false otherwise
	 */
	public static Boolean columnExists( Query query, String column ) {
		return query.hasColumn( Key.of( column ) );
	}

}
