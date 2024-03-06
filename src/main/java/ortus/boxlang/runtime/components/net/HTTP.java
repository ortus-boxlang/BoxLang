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
package ortus.boxlang.runtime.components.net;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.components.validators.Validator;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.util.HTTP.HTTPStatusReasons;
import ortus.boxlang.runtime.util.HTTP.URIBuilder;

@BoxComponent( allowsBody = true )
public class HTTP extends Component {

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 *
	 * @param name The name of the component
	 */
	public HTTP() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.URL, "string", Set.of(
		        Validator.REQUIRED,
		        Validator.NON_EMPTY,
		        ( cxt, comp, attr, attrs ) -> {
			        if ( !attrs.getAsString( attr.name() ).startsWith( "http" ) ) {
				        throw new BoxValidationException( comp, attr, "must start with 'http://' or 'https://'" );
			        }
		        }
		    ) ),
		    new Attribute( Key.result, "string" )
			/*
			 * TODO:
			 * port
			 * method
			 * proxyserver
			 * proxyport
			 * proxyuser
			 * proxypassword
			 * username
			 * password
			 * useragent
			 * charset
			 * resolveurl
			 * throwonerror
			 * redirect
			 * timeout
			 * getasbinary
			 * delimiter
			 * name
			 * columns
			 * firstrowasheaders
			 * textqualifier
			 * file
			 * multipart
			 * clientcertpassword
			 * clientcert
			 * path
			 * compression
			 * authType
			 * domain
			 * workstation
			 * cachedWithin
			 * encodeURL
			 */
		};
	}

	/**
	 * I make an HTTP call
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		executionState.put( Key.HTTPParams, new Array() );

		BodyResult bodyResult = processBody( context, body );
		// IF there was a return statement inside our body, we early exit now
		if ( bodyResult.isEarlyExit() ) {
			return bodyResult;
		}

		String	variableName	= StringCaster.cast( attributes.getOrDefault( Key.result, "cfhttp" ) );
		String	theURL			= attributes.getAsString( Key.URL );
		String	method			= StringCaster.cast( attributes.getOrDefault( Key.method, "GET" ) ).toUpperCase();
		Array	params			= executionState.getAsArray( Key.HTTPParams );
		Struct	HTTPResult		= new Struct();

		System.out.println( "Make HTTP call to: " + theURL );
		System.out.println( "Using the following HTTP Params: " );
		System.out.println( params.asString() );

		try {
			HttpRequest.Builder			builder			= HttpRequest.newBuilder();
			URIBuilder					uriBuilder		= new URIBuilder( theURL );
			HttpRequest.BodyPublisher	bodyPublisher	= HttpRequest.BodyPublishers.noBody();
			for ( Object p : params ) {
				IStruct	param	= StructCaster.cast( p );
				String	type	= param.getAsString( Key.type );
				switch ( type.toLowerCase() ) {
					case "header" -> builder.header( param.getAsString( Key._NAME ), param.getAsString( Key.value ) );
					case "body" -> bodyPublisher = HttpRequest.BodyPublishers.ofString( param.getAsString( Key.value ) );
					case "xml" -> {
						builder.header( "Content-Type", "text/xml" );
						bodyPublisher = HttpRequest.BodyPublishers.ofString( param.getAsString( Key.value ) );
					}
					// @TODO move this to a non-deprecated method
					case "cgi" -> builder.header( param.getAsString( Key._NAME ), java.net.URLEncoder.encode( param.getAsString( Key.value ) ) );
					case "file" -> throw new BoxRuntimeException( "Unhandled HTTPParam type: " + type );
					case "url" -> uriBuilder.addParameter( param.getAsString( Key._NAME ), param.getAsString( Key.value ) );
					default -> throw new BoxRuntimeException( "Unhandled HTTPParam type: " + type );
				}
			}
			builder.method( method, bodyPublisher );
			builder.uri( uriBuilder.build() );
			HttpRequest				request		= builder.build();
			HttpClient				client		= HttpClient.newHttpClient();
			HttpResponse<String>	response	= client.send( request, HttpResponse.BodyHandlers.ofString() );

			HTTPResult.put( Key.statusCode, response.statusCode() );
			HTTPResult.put( Key.statusText, HTTPStatusReasons.getReasonForStatus( response.statusCode() ) );
			HTTPResult.put( Key.fileContent, response.body() );

			// Set the result back into the page
			ExpressionInterpreter.setVariable( context, variableName, HTTPResult );

			return DEFAULT_RETURN;
		} catch ( URISyntaxException | IOException | InterruptedException e ) {
			throw new BoxRuntimeException( e.getMessage() );
		}
	}
}
