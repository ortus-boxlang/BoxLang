package ortus.boxlang.runtime.net;

import ortus.boxlang.runtime.jdbc.DataSource;
import ortus.boxlang.runtime.jdbc.DataSourceManager;
import ortus.boxlang.runtime.scopes.Key;

import java.net.http.HttpClient;
import java.util.HashMap;
import java.util.Map;

public class HttpManager {

	/**
	 * Singleton instance of the DataSourceManager.
	 */
	private static HttpClient instance;

	/**
	 * Private constructor. Use getInstance() instead.
	 */
	private HttpManager() {
	}

	/**
	 * Get the singleton instance of the DataSourceManager.
	 * <p>
	 * Will construct a new instance if one does not already exist, otherwise returns the existing instance.
	 *
	 * @return The DataSourceManager instance.
	 */
	public static HttpClient getClient() {
		if ( instance == null ) {
			instance = HttpClient.newHttpClient();
		}
		return instance;
	}

}
