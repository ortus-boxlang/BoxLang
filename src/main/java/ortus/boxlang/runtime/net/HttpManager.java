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

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Base64;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class HttpManager {

	/**
	 * Singleton instance of the HttpManager.
	 */
	private static HttpClient	instance;

	public static Key			encodedCertKey	= Key.of( "clientCertEncoded" );

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
		// Configure client certificate if provided
		if ( attributes.containsKey( Key.clientCert ) ) {
			String	clientCert			= attributes.getAsString( Key.clientCert );
			String	clientCertPassword	= attributes.containsKey( Key.clientCertPassword )
			    ? attributes.getAsString( Key.clientCertPassword )
			    : null;

			try {
				// Load the client certificate in to a new keystore
				KeyStore keyStore = KeyStore.getInstance( "PKCS12" );
				try ( InputStream certInputStream = new FileInputStream( clientCert ) ) {
					keyStore.load( certInputStream, clientCertPassword != null ? clientCertPassword.toCharArray() : null );
				}

				// Debugging option for testability
				if ( attributes.containsKey( Key.debug ) ) {
					X509Certificate	storeCertificate	= ( X509Certificate ) keyStore.getCertificate( keyStore.aliases().nextElement() );
					String			encodedCert			= Base64.getEncoder().encodeToString( storeCertificate.getEncoded() );
					attributes.put( encodedCertKey, encodedCert );
				}

				KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance( KeyManagerFactory.getDefaultAlgorithm() );
				keyManagerFactory.init( keyStore, clientCertPassword != null ? clientCertPassword.toCharArray() : null );
				SSLContext sslContext = SSLContext.getInstance( "TLS" );
				sslContext.init( keyManagerFactory.getKeyManagers(), null, new SecureRandom() );

				// Set the SSL context in the builder
				builder.sslContext( sslContext );
			} catch ( Exception e ) {
				throw new BoxRuntimeException( "Failed to configure client certificate", e.getClass().getSimpleName(), e );
			}
		}
		return builder.build();
	}

}
