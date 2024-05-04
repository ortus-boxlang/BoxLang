package ortus.boxlang.runtime.net;

import java.net.http.HttpClient;

public class HttpManager {

	/**
	 * Singleton instance of the HttpManager.
	 */
	private static HttpClient instance;

	/**
	 * Private constructor. Use getInstance() instead.
	 */
	private HttpManager() {
	}

	/**
	 * Get the singleton instance of the HttpManager.
	 * <p>
	 * Will construct a new instance if one does not already exist, otherwise returns the existing instance.
	 *
	 * @return The HttpManager instance.
	 */
	public static HttpClient getClient() {
		if ( instance == null ) {
			instance = HttpClient.newHttpClient();
		}
		return instance;
	}

}
