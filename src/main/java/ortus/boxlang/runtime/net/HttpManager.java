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
package ortus.boxlang.runtime.net;

import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSession;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
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
			synchronized ( HttpManager.class ) {
				if ( instance == null ) {
					instance = HttpClient.newBuilder().followRedirects( HttpClient.Redirect.NORMAL ).build();
				}
			}
		}
		return instance;
	}

	/**
	 * Get a new HttpClient instance custom attributes including proxy client connection and redirect enforcement.
	 * 
	 * @param attributes
	 * 
	 * @return
	 */
	public static HttpClient getCustomClient( IStruct attributes ) {
		HttpClient.Builder builder = HttpClient.newBuilder();
		if ( attributes.containsKey( Key.proxyServer ) ) {
			builder.proxy( ProxySelector.of( new InetSocketAddress( attributes.getAsString( Key.proxyServer ), attributes.getAsInteger( Key.proxyPort ) ) ) ); // Set proxy host & port

			if ( attributes.containsKey( Key.proxyUser ) && attributes.containsKey( Key.proxyPassword ) ) {
				String	proxyUser		= attributes.getAsString( Key.proxyUser );
				String	proxyPassword	= attributes.getAsString( Key.proxyPassword );
				builder.authenticator( new Authenticator() {

					@Override
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication( proxyUser, proxyPassword.toCharArray() );
					}
				} );
			}
			if ( attributes.containsKey( Key.timeout ) ) {
				builder.connectTimeout( Duration.ofSeconds( attributes.getAsInteger( Key.timeout ) ) );
			}
		}
		if ( !attributes.getAsBoolean( Key.redirect ) ) {
			builder.followRedirects( HttpClient.Redirect.NEVER );
		}
		return builder.build();
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
			throw new BoxRuntimeException( "The request was interrupted", "InterruptedException", e );
		}
	}

}
