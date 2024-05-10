
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

package ortus.boxlang.runtime.components.cache;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.cache.BoxCacheEntry;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.CacheService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent( allowsBody = true )
public class Cache extends Component {

	/**
	 * Enumeration of all possible `type` attribute values.
	 */
	public enum CacheAction {

		CACHE,
		OPTIMAL, // Alias to CACHE
		CONTENT,
		CLIENTCACHE,
		SERVERCACHE,
		FLUSH,
		DELETE,
		PUT,
		GET;

		public static CacheAction fromString( String type ) {
			return CacheAction.valueOf( type.trim().toUpperCase() );
		}
	}

	public static final double		secondsInDay		= 86400d;

	/**
	 * The interceptor service helper
	 */
	protected final CacheService	cacheService		= BoxRuntime.getInstance().getCacheService();

	protected ICacheProvider		defaultCache		= cacheService.getDefaultCache();

	private final String			defaultFileStore	= "FileSystemStore";

	/**
	 * Constructor
	 */
	public Cache() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.action,
		        "string",
		        "cache",
		        Set.of(
		            Validator.valueOneOf(
		                "cache",
		                "flush",
		                "clientcache",
		                "servercache",
		                "optimal",
		                "content",
		                "put",
		                "get" ) ) ),
		    new Attribute( Key.key, "string" ), // "object identifier"
		    new Attribute( Key.value, "any" ), // "value"
		    new Attribute( Key._NAME, "any" ), // "variable or variable name"
		    new Attribute( Key.cacheName, "string" ), // "true|false"
		    new Attribute( Key.metadata, "struct" ), // "true|false"
		    new Attribute( Key.directory, "string" ), // "directory path"
		    new Attribute( Key.timespan, "double" ), // "decimal number of days">
		    new Attribute( Key.idleTime, "double" ), // "decimal number of days"
		    new Attribute( Key.metadata, "struct" ), // "variable name"
		    new Attribute( Key.stripWhitespace, "boolean", false ), // "false|true"
		    new Attribute( Key.throwOnError, "boolean", false ), // "false|true"
		    new Attribute( Key.useCache, "boolean", true ), // "true|false"
		    // TODO: These are specfic to web connectivity and will need to be implemented with the web runtime
		    new Attribute( Key.expireURL, "string", Set.of( Validator.NOT_IMPLEMENTED ) ), // "wildcarded URL reference"
		    new Attribute( Key.password, "string", Set.of( Validator.NOT_IMPLEMENTED ) ), // "password"
		    new Attribute( Key.port, "integer", Set.of( Validator.NOT_IMPLEMENTED ) ), // "port number"
		    new Attribute( Key.protocol, "string", Set.of( Validator.NOT_IMPLEMENTED ) ), // "http://|https://"
		    new Attribute( Key.region, "string", Set.of( Validator.NOT_IMPLEMENTED ) ), // "region_name"
		    new Attribute( Key.useQueryString, "boolean", false, Set.of( Validator.NOT_IMPLEMENTED ) ), // "false|true"
		    new Attribute( Key.username, "string", Set.of( Validator.NOT_IMPLEMENTED ) ), // "username"
		    // TODO: Circle back and immplement find nearby checks for these variables - then make them part of the key
		    new Attribute( Key.dependsOn, "string", Set.of( Validator.NOT_IMPLEMENTED ) ),// "variable name list"
		};
	}

	/**
	 * Component which provides caching functionality, including content, individual entries, and HTTP headers
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 * @attribute.action The cache action - "flush", "clientcache", "servercache", "optimal", "content", "put", "get"
	 *
	 * @attribute.key Cache key - optional for GET, DELETE functions and when body content is present. If not provided, a unique identifier of the body
	 *                will provide the key
	 *
	 * @attribute.id Alias for key attribute
	 *
	 * @attribute.throw on error - when set to true will throw errors when CRUD actions on the cache fail
	 *
	 * @attribute.name - a variable name for the result of the cache action. Required for GET action
	 *
	 * @attribute.metadata - any additional metadata to store with the cache object
	 *
	 * @attribute.value - mandatory value for put action
	 *
	 * @attribute.cachename - optional cache name. If not provided the default cache will be used
	 *
	 * @attribute.timespan - The duration to cache the object, defaults to either the cache default or unlimited until a server restart
	 *
	 * @attribute.idletime - The maximum idle time for an object to remain in the cache, defaults to the timeout
	 *
	 * @attributes.directory - Optional directory attribute which implements a file storage cache
	 *
	 * @attributes.expireUrl - GLOB pattern or regex this string is found in the URL, the cache object will be invalidated.
	 *
	 * @attributes.protocol - Legacy CFML attribute. Not implemented
	 *
	 * @attributes.port - Legacy CFML attributes. Not implemented
	 *
	 */
	@SuppressWarnings( { "unchecked" } )
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		CacheAction			cacheAction			= CacheAction.fromString( attributes.getAsString( Key.action ) );
		String				key					= attributes.getAsString( Key.key );
		Object				value				= attributes.get( Key.value );
		String				variable			= attributes.getAsString( Key._NAME );
		String				cacheName			= attributes.getAsString( Key.cacheName );
		String				cacheDirectory		= attributes.getAsString( Key.directory );
		Boolean				useCache			= attributes.getAsBoolean( Key.useCache );
		Double				timespan			= attributes.getAsDouble( Key.timespan );
		Double				idleTime			= attributes.getAsDouble( Key.idleTime );
		Boolean				throwOnError		= attributes.getAsBoolean( Key.throwOnError );
		ICacheProvider		cacheProvider		= null;
		List<CacheAction>	namedCacheOps		= List.of(
		    CacheAction.GET,
		    CacheAction.PUT,
		    CacheAction.CACHE,
		    CacheAction.OPTIMAL,
		    CacheAction.CONTENT,
		    CacheAction.FLUSH,
		    CacheAction.DELETE
		);
		List<CacheAction>	keyRequiredActions	= List.of(
		    CacheAction.GET,
		    CacheAction.PUT
		);

		if ( key == null && attributes.containsKey( Key.id ) ) {
			key = attributes.getAsString( Key.id );
		}

		if ( key == null && keyRequiredActions.contains( cacheAction ) ) {
			throw new BoxRuntimeException(
			    String.format( "An explict key attribute is required for the cache action [%s]", cacheAction.toString().toLowerCase() )
			);
		}

		String cacheKeyName = key != null
		    ? key
		    : StringCaster.cast(
		        runtime.getFunctionService().getGlobalFunction( Key.hash40 ).invoke(
		            context,
		            ArgumentsScope.of(
		                Key.input, Struct.of(
		                    Key.attributes, attributes,
		                    Key.body, body
		                )
		            ),
		            false,
		            Key.hash40
		        )
		    );

		if ( timespan == null && idleTime != null ) {
			timespan = idleTime;
		}

		Object result = null;

		if ( !useCache ) {
			if ( cacheAction.equals( CacheAction.CLIENTCACHE ) || cacheAction.equals( CacheAction.SERVERCACHE ) ) {
				return DEFAULT_RETURN;
			} else {
				result = value == null ? processCacheBody( context, body ) : value;
			}
		}

		// Evalutions on cache directive
		if ( !namedCacheOps.contains( cacheAction ) ) {
			IStruct interceptorArgs = Struct.of(
			    Key.component, this,
			    Key.context, context,
			    Key.attributes, attributes,
			    Key.body, body,
			    Key.executionState, executionState,
			    Key.result, null
			);
			interceptorService.announce( BoxEvent.ON_CREATEOBJECT_REQUEST, interceptorArgs );
			if ( interceptorArgs.get( Key.result ) == null ) {
				throw new BoxRuntimeException(
				    String.format( "The specified cache action [%s] is is not valid in the current runtime", cacheAction.toString().toLowerCase() )
				);
			}
		} else {
			if ( cacheName != null ) {
				cacheProvider = cacheService.getCache( Key.of( cacheName ) );
			} else if ( cacheDirectory != null ) {
				Key directoryCacheKey = Key.of( cacheDirectory );
				if ( !cacheService.hasCache( directoryCacheKey ) ) {
					cacheService.createCache(
					    directoryCacheKey,
					    Key.boxCacheProvider,
					    Struct.of(
					        Key.objectStore, defaultFileStore,
					        Key.directory, cacheDirectory,
					        Key.useLastAccessTimeouts, true
					    )
					);
				}
				cacheProvider = cacheService.getCache( directoryCacheKey );
			} else {
				cacheProvider = cacheService.getDefaultCache();
			}

			long	timeout				= timespan != null ? DoubleCaster.cast( timespan * secondsInDay ).longValue() : 0l;
			long	lastAccessTimeout	= idleTime != null ? DoubleCaster.cast( idleTime * secondsInDay ).longValue() : 0l;

			switch ( cacheAction ) {
				case GET : {
					if ( variable == null ) {
						throw new BoxRuntimeException( "A variable name is required when specifying the cache action [get]" );
					}
					result = cacheProvider.getQuiet( cacheKeyName );
					break;
				}
				case PUT : {
					cacheProvider.set(
					    cacheKeyName,
					    new BoxCacheEntry(
					        cacheProvider.getName(),
					        timeout,
					        lastAccessTimeout,
					        Key.of( cacheKeyName ),
					        value == null ? processCacheBody( context, body ) : value,
					        new Struct()
					    )
					);
					break;
				}
				case CACHE :
				case OPTIMAL :
				case CONTENT : {
					// TODO: Need to figure out how to handle the full page caching
					Key cacheProviderName = cacheProvider.getName();
					result = cacheProvider.getOrSet(
					    cacheKeyName,
					    () -> new BoxCacheEntry(
					        cacheProviderName,
					        timeout,
					        lastAccessTimeout,
					        Key.of( cacheKeyName ),
					        value == null ? processCacheBody( context, body ) : value,
					        new Struct()
					    )
					).get();
					break;
				}
				case FLUSH : {
					if ( key != null ) {
						cacheProvider.clear( key );
					} else {
						cacheProvider.clearAll();
					}
					break;
				}
				case DELETE : {
					if ( key == null ) {
						throw new BoxRuntimeException( "The cache method [delete] was specified but no key was provided for deletion" );
					}
					cacheProvider.clear( key );
					break;
				}
				default : {
					throw new BoxRuntimeException(
					    String.format( "The cache action [%s] is unknown or is not implemented in this runtime", cacheAction.toString().toLowerCase() ) );
				}
			}

		}

		// TODO getOrSet and get currently return optionals and there is all sorts of casting and retrieval. Per Luis, we're going to change those to return
		// the object
		if ( result != null && result instanceof Optional ) {
			result = ( ( ( Optional<BoxCacheEntry> ) result ).get() ).rawValue();
		} else if ( result != null && result instanceof BoxCacheEntry ) {
			result = ( ( BoxCacheEntry ) result ).rawValue();
		}

		if ( result instanceof String && attributes.getAsBoolean( Key.stripWhitespace ) ) {
			result = StringCaster.cast( result ).trim();
		}

		if ( result instanceof BodyResult && ( ( BodyResult ) result ).isEarlyExit() ) {
			return ( ( BodyResult ) result );
		}

		if ( variable != null ) {
			ExpressionInterpreter.setVariable(
			    context,
			    variable,
			    result
			);
		}

		return DEFAULT_RETURN;

		// Replace this example component function body with your own implementation;
		// Example, passing through to a registered BIF
		// IStruct response = StructCaster.cast(
		// runtime.getFunctionService().getGlobalFunction( Key.Foo ).invoke( context,
		// attributes, false, Key.Foo ) );

		// Set the result(s) back into the page
		// ExpressionInterpreter.setVariable( context, attributes.getAsString(
		// Key.variable ), response.getAsString( Key.output ) );
	}

	private String processCacheBody( IBoxContext context, ComponentBody body ) {
		StringBuffer buffer = new StringBuffer();
		processBody( context, body, buffer );
		return buffer.toString();
	}

}
