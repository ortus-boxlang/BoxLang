package ortus.boxlang.runtime.net;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSession;

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

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

	public static CompletableFuture<HttpResponse<String>> getTimeoutRequestAsync( int timeout ) {
		return CompletableFuture.supplyAsync( () -> getTimeoutRequest( timeout ) );
	}

	public static HttpResponse<String> getTimeoutRequest( int timeout ) {
		try {
			TimeUnit.SECONDS.sleep( timeout );
			return new HttpResponse<String>() {

				@Override
				public int statusCode() {
					return 408;
				}

				@Override
				public HttpRequest request() {
					return null;
				}

				@Override
				public Optional<HttpResponse<String>> previousResponse() {
					return Optional
					    .empty();
				}

				@Override
				public HttpHeaders headers() {
					return null;
				}

				@Override
				public String body() {
					return "Request timed out after " + timeout + ( timeout == 1 ? " second." : " seconds." );
				}

				@Override
				public Optional<SSLSession> sslSession() {
					return Optional
					    .empty();
				}

				@Override
				public URI uri() {
					return null;
				}

				@Override
				public HttpClient.Version version() {
					return null;
				}
			};
		} catch ( InterruptedException e ) {
			throw new BoxRuntimeException( e.getMessage(), e );
		}
	}

}
