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
package TestCases.phase3;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Path;
import java.time.ZoneId;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.application.Application;
import ortus.boxlang.runtime.application.BaseApplicationListener;
import ortus.boxlang.runtime.async.tasks.IScheduler;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.context.ApplicationBoxContext;
import ortus.boxlang.runtime.context.BaseBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.ApplicationScope;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.SessionScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.services.CacheService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

public class ApplicationTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "application basics" )
	@Test
	public void testBasicApplication() {
		// @formatter:off
		instance.executeSource(
		    """
		        bx:application name="myAppsdfsdf" sessionmanagement="true";

				result = application;
				result2 = session;
				result3 = application.applicationName;
				result4 = application.applicationName.startsWith( "myApp" );
				startTime = ApplicationStartTime()
			""", context );
		// @formatter:on

		assertThat( variables.get( result ) ).isInstanceOf( ApplicationScope.class );
		assertThat( variables.get( Key.of( "result2" ) ) ).isInstanceOf( SessionScope.class );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( "myAppsdfsdf" );
		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( true );

		ApplicationBoxContext	appContext	= context.getParentOfType( ApplicationBoxContext.class );
		Application				app			= appContext.getApplication();

		assertThat( app.getName().getName() ).isEqualTo( "myAppsdfsdf" );
		assertThat( app.getSessionsCache() ).isNotNull();
		assertThat( app.getApplicationScope() ).isNotNull();
		assertThat( app.getApplicationScope().getName().getName() ).isEqualTo( "application" );
		assertThat( app.getClassLoaders() ).isNotNull();
		assertThat( app.hasStarted() ).isTrue();
	}

	@Test
	public void testGetAppMeta() {
		// @formatter:off
		instance.executeSource(
		    """
		        bx:application name="myAppsdfsdf2" sessionmanagement="true";
				result = GetApplicationMetadata();
			""", context );
		// @formatter:on

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
		assertThat( variables.getAsStruct( result ).get( "name" ) ).isEqualTo( "myAppsdfsdf2" );
		assertThat( variables.getAsStruct( result ).get( "sessionmanagement" ).toString() ).isEqualTo( "true" );
	}

	@Test
	public void testGetDefaultAppMeta() {
		// @formatter:off
		instance.executeSource(
		    """
				result = GetApplicationMetadata();
			""", context );
		// @formatter:on

		assertThat( variables.get( result ) ).isInstanceOf( IStruct.class );
		assertThat( variables.getAsStruct( result ).get( "name" ) ).isEqualTo( "" );
		assertThat( variables.getAsStruct( result ).get( "sessionmanagement" ).toString() ).isEqualTo( "false" );
	}

	@DisplayName( "java settings setup" )
	@Test
	public void testJavaSettings() {

		ClassLoader currentClassLoader = Thread.currentThread().getContextClassLoader();

		// @formatter:off
		instance.executeSource(
		    """
				import java.lang.Thread;

		        bx:application name="myJavaApp" javaSettings={
					loadPaths = [ "/src/test/resources/libs" ],
					reloadOnChange = true
				 };

				 import com.github.benmanes.caffeine.cache.Caffeine
				 targetInstance = Caffeine.newBuilder()

				 import org.apache.commons.lang3.ClassUtils
				 targetInstance2 = ClassUtils.getClass()

				 result = Thread.currentThread().getContextClassLoader()

			""", context );
		// @formatter:on

		ApplicationBoxContext	appContext	= context.getParentOfType( ApplicationBoxContext.class );
		Application				app			= appContext.getApplication();
		assertThat( app.getClassLoaderCount() ).isEqualTo( 1 );

		// ClassLoader newClassLoader = ( ClassLoader ) variables.get( result );
		// assertThat( newClassLoader ).isNotEqualTo( currentClassLoader );
		// assertThat( newClassLoader.getName() ).isEqualTo( "myJavaApp" );
	}

	@DisplayName( "Ad-hoc config override" )
	@Test
	public void testAdHocConfigOverride() {

		context.injectParentContext( new BaseBoxContext() {

			public IStruct getConfig() {
				IStruct config = super.getConfig();
				config.put( "adHocConfig", "adHocConfigValue" );
				return config;
			}
		} );

		assertThat( context.getConfigItem( Key.of( "adHocConfig" ) ) ).isEqualTo( "adHocConfigValue" );
	}

	@DisplayName( "Can resolve java settings paths with a full jar/class path" )
	@Test
	public void testJavaSettingsPaths() {
		// @formatter:off
		instance.executeSource(
		    """
		        bx:application name="myJavaApp" javaSettings={
					loadPaths = [ "/src/test/resources/libs/helloworld.jar" ],
					reloadOnChange = true
				 };
			""", context );
		// @formatter:on

		ApplicationBoxContext	appContext	= context.getParentOfType( ApplicationBoxContext.class );
		Application				app			= appContext.getApplication();
		assertThat( app.getClassLoaderCount() ).isEqualTo( 1 );
	}

	@DisplayName( "Can resolve java settings paths with a full jar/class path with bad pathing" )
	@Test
	public void testJavaSettingsBadPaths() {
		// @formatter:off
		instance.executeSource(
		    """
		        bx:application name="myJavaApp" javaSettings={
					loadPaths = [ "\\src\\test\\resources\\libs\\helloworld.jar" ],
					reloadOnChange = true
				 };
			""", context );
		// @formatter:on

		ApplicationBoxContext	appContext	= context.getParentOfType( ApplicationBoxContext.class );
		Application				app			= appContext.getApplication();

		assertThat( app.getClassLoaderCount() ).isEqualTo( 1 );
	}

	@DisplayName( "Can resolve relative paths" )
	@Test
	public void testJavaSettingsRelativePaths() {

		RequestBoxContext		requestContext	= context.getParentOfType( RequestBoxContext.class );
		BaseApplicationListener	listener		= requestContext.getApplicationListener();

		// Mock the relative path
		listener.getSettings()
		    .put( "source", Path.of( "src/test/resources/Application.bx" ).toAbsolutePath().toString() );

		// @formatter:off
		instance.executeSource(
		    """
		        bx:application name="myJavaApp" javaSettings={
					loadPaths = [ "libs/helloworld.jar" ],
					reloadOnChange = true
				 };
			""", context );
		// @formatter:on

		ApplicationBoxContext	appContext	= context.getParentOfType( ApplicationBoxContext.class );
		Application				app			= appContext.getApplication();
		assertThat( app.getClassLoaderCount() ).isEqualTo( 1 );
	}

	@DisplayName( "Can resolve mappings with java settings" )
	@Test
	public void testJavaSettingsMappings() {
		// @formatter:off
		instance.executeSource(
		    """
		        bx:application
					name="myJavaAppWithMappings"
					mappings = { "/javalib": "/src/test/resources/libs/" }
					javaSettings={
						loadPaths = [ "/javalib/helloworld.jar" ],
						reloadOnChange = true
					};
			""", context );
		// @formatter:on

		ApplicationBoxContext	appContext	= context.getParentOfType( ApplicationBoxContext.class );
		Application				app			= appContext.getApplication();
		assertThat( app.getClassLoaderCount() ).isEqualTo( 1 );
	}

	@DisplayName( "Datasources declared in App.cfc will be promoted" )
	@Test
	public void testDatasourceDeclaration() {
		// @formatter:off
		instance.executeSource(
		    """
		        bx:application
					name="myAppWithDatasource"
					datasources = {
						mysql = {
							database : "mysql",
							host : "localhost",
							port : "3306",
							driver : "MySQL",
							username : "root",
							password : "mysql"
						}
					};
			""", context );
		// @formatter:on

		IStruct config = context.getConfig();
		assertThat( config.getAsStruct( Key.datasources ) ).isNotEmpty();
	}

	@DisplayName( "Timezone declared in App.cfc will be promoted" )
	@Test
	public void testTimezoneDeclaration() {
		// @formatter:off
		instance.executeSource(
		    """
		        bx:application
					name="myAppWithDatasource"
					timezone="America/Los_Angeles";
			""", context );
		// @formatter:on

		assertThat( context.getConfig().get( Key.timezone ) ).isInstanceOf( ZoneId.class );
		ZoneId zone = ( ZoneId ) context.getConfig().get( Key.timezone );
		assertThat( zone.getId() ).isEqualTo( "America/Los_Angeles" );
	}

	@DisplayName( "Can update application without name" )
	@Test
	public void testUpdateApplicationWithoutName() {
		// @formatter:off
		instance.executeSource(
		    """
		        bx:application
					name="testUpdateApplicationWithoutName"
					sessionmanagement="true";

				firstSessionID = session.sessionID;

				newMappings = {
					"/UpdateApplicationWithoutName" : "/src/test/resources/libs/"
				}

				bx:application
					action        ="update"
					mappings      ="#newMappings#";

				secondSessionID = session.sessionID;

				result = GetApplicationMetadata();
			""", context );
		// @formatter:on

		IStruct result = variables.getAsStruct( Key.result );
		assertThat( result.get( Key._NAME ) ).isEqualTo( "testUpdateApplicationWithoutName" );
		assertThat( result.get( Key.mappings ) ).isNotNull();
		assertThat( result.get( Key.mappings ) ).isInstanceOf( IStruct.class );
		assertThat( result.getAsStruct( Key.mappings ).get( "/UpdateApplicationWithoutName" ) ).isEqualTo( "/src/test/resources/libs/" );
		assertThat( variables.get( Key.of( "firstSessionID" ) ) ).isEqualTo( variables.get( Key.of( "secondSessionID" ) ) );
	}

	@DisplayName( "Create this.caches for an application" )
	@Test
	public void testCreateCaches() {
		// @formatter:off
		instance.executeSource(
		    """
		        bx:application
					action = "update"
					name  = "cacheTestApp"
					caches = {
						cache1NoProvider = {
							properties : {
								"objectStore" = "ConcurrentSoftReferenceStore",
								"evictionPolicy" = "LFU"
							}
						},
						cache2 = {
							provider : "BoxCacheProvider",
							properties : {
								maxObjects : 100
							}
						}
					};

				println( getApplicationMetadata() );
				result = getApplicationMetadata().caches;

				cache1 = cache( "cache1NoProvider" );
				cache2 = cache( "cache2" );
			""", context );
		// @formatter:on

		IStruct caches = variables.getAsStruct( Key.result );
		assertThat( caches ).isNotNull();
		assertThat( caches.get( "cache1NoProvider" ) ).isNotNull();
		assertThat( caches.get( "cache2" ) ).isNotNull();

		ICacheProvider	cache1		= ( ICacheProvider ) variables.get( Key.of( "cache1" ) );
		Key				cache1Name	= Key.of( "cacheTestApp" + ":" + "cache1NoProvider" );
		ICacheProvider	cache2		= ( ICacheProvider ) variables.get( Key.of( "cache2" ) );
		Key				cache2Name	= Key.of( "cacheTestApp" + ":" + "cache2" );

		assertThat( cache1 ).isNotNull();
		assertThat( cache1.getName() ).isEqualTo( cache1Name );
		assertThat( cache2 ).isNotNull();
		assertThat( cache2.getName() ).isEqualTo( cache2Name );

		// Check the serviice now
		CacheService cacheService = instance.getCacheService();
		assertThat( cacheService.hasCache(
		    cache1Name
		) ).isTrue();
		assertThat( cacheService.hasCache(
		    cache2Name
		) ).isTrue();
	}

	@DisplayName( "Create this.schedulers for an application" )
	@Test
	public void testCreateSchedulers() {

		// @formatter:off
		instance.executeSource(
		    """
		        bx:application
					action = "update"
					name  = "schedulerTestApp"
					schedulers = [ "src.test.bx.Scheduler" ]
					;

					result = getApplicationMetadata().schedulers
					println( result )

					println( schedulerList() )

					scheduler = schedulerGet( "My-Scheduler" )
					started = scheduler.hasStarted()
					stats = schedulerStats( "My-Scheduler" )
					println( stats )
			""", context );
		// @formatter:on

		Array schedulers = variables.getAsArray( Key.result );
		assertThat( schedulers ).isNotNull();
		assertThat( schedulers ).hasSize( 1 );
		assertThat( ( IScheduler ) variables.get( Key.of( "scheduler" ) ) ).isNotNull();
		assertThat( variables.getAsBoolean( Key.of( "started" ) ) ).isTrue();
	}

}
