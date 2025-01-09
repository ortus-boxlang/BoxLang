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
		        application name="myAppsdfsdf" sessionmanagement="true";

				result = application;
				result2 = session;
				startTime = ApplicationStartTime()
			""", context );
		// @formatter:on

		assertThat( variables.get( result ) ).isInstanceOf( ApplicationScope.class );
		assertThat( variables.get( Key.of( "result2" ) ) ).isInstanceOf( SessionScope.class );

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
		        application name="myAppsdfsdf2" sessionmanagement="true";
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

		        application name="myJavaApp" javaSettings={
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
		        application name="myJavaApp" javaSettings={
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
		        application name="myJavaApp" javaSettings={
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
		        application name="myJavaApp" javaSettings={
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
		        application
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
		        application
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
		        application
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
		        application
					name="testUpdateApplicationWithoutName"
					sessionmanagement="true";

				firstSessionID = session.sessionID;

				newMappings = {
					"/UpdateApplicationWithoutName" : "/src/test/resources/libs/"
				}

				application
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

}
