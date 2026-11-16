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

package ortus.boxlang.runtime.application;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.context.SessionBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Struct;

public class SessionCacheInvalidationTest {

	private static BoxRuntime	instance;

	@TempDir
	Path						cacheDirectory;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@ParameterizedTest( name = "{0} removes the old serialized session" )
	@CsvSource( { "sessionInvalidate(),false", "sessionRotate(),true" } )
	public void testSessionChangeRemovesPersistedSession( String operation, boolean preservesData ) {
		String			suffix			= UUID.randomUUID().toString();
		String			cacheName		= "session-invalidation-" + suffix;
		String			appName			= "session-app-" + suffix;
		String			otherAppName	= "other-session-app-" + suffix;
		Key				sessionID		= Key.of( UUID.randomUUID().toString() );
		ICacheProvider	cache			= instance.getCacheService().createCache(
		    cacheName,
		    "BoxCacheProvider",
		    Struct.of( "objectStore", "FileSystemStore", "directory", this.cacheDirectory.toString() )
		);
		try {
			ScriptingRequestBoxContext	context				= startSession( appName, cacheName, sessionID );
			SessionBoxContext			sessionContext		= context.getParentOfType( SessionBoxContext.class );
			Session						original			= sessionContext.getSession();
			String						originalCacheKey	= original.getCacheKey();
			sessionContext.persistSession( context );

			// Serialized stores return a separate object, so shutting down the live session cannot revoke this copy.
			Session persisted = ( Session ) cache.get( originalCacheKey ).get();
			assertThat( persisted ).isNotSameInstanceAs( original );
			assertThat( persisted.getSessionScope().get( "userId" ) ).isEqualTo( "member-123" );

			// The same browser ID in another application must remain untouched in the shared cache.
			ScriptingRequestBoxContext	otherContext		= startSession( otherAppName, cacheName, sessionID );
			SessionBoxContext			otherSessionContext	= otherContext.getParentOfType( SessionBoxContext.class );
			otherSessionContext.persistSession( otherContext );
			String otherCacheKey = otherSessionContext.getSession().getCacheKey();

			instance.executeSource( operation + ";", context );

			Session replacement = context.getParentOfType( SessionBoxContext.class ).getSession();
			assertThat( replacement.getID() ).isNotEqualTo( sessionID );
			assertThat( cache.get( originalCacheKey ).isEmpty() ).isTrue();
			assertThat( replacement.getSessionScope().containsKey( "userId" ) ).isEqualTo( preservesData );
			assertThat( cache.get( replacement.getCacheKey() ).isPresent() ).isTrue();
			assertThat( cache.get( otherCacheKey ).isPresent() ).isTrue();

			// A later request presenting the retired ID must receive a fresh, unauthenticated session.
			ScriptingRequestBoxContext replay = startSessionWithoutData( appName, cacheName, sessionID );
			assertThat( replay.getParentOfType( SessionBoxContext.class ).getSession().getSessionScope().containsKey( "userId" ) ).isFalse();
		} finally {
			try {
				instance.getApplicationService().shutdownApplication( Key.of( appName ) );
				instance.getApplicationService().shutdownApplication( Key.of( otherAppName ) );
			} finally {
				instance.getCacheService().shutdownCache( Key.of( cacheName ) );
			}
		}
	}

	private ScriptingRequestBoxContext startSession( String appName, String cacheName, Key sessionID ) {
		ScriptingRequestBoxContext context = startSessionWithoutData( appName, cacheName, sessionID );
		instance.executeSource( "session.userId = 'member-123';", context );
		return context;
	}

	private ScriptingRequestBoxContext startSessionWithoutData( String appName, String cacheName, Key sessionID ) {
		ScriptingRequestBoxContext context = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		context.setSessionID( sessionID );
		context.getScopeNearby( VariablesScope.name ).put( "appName", appName );
		context.getScopeNearby( VariablesScope.name ).put( "cacheName", cacheName );
		instance.executeSource(
		    "bx:application name=appName sessionManagement=true sessionStorage=cacheName sessionCluster=true;",
		    context
		);
		return context;
	}
}
