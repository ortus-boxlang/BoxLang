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

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.application.Application;
import ortus.boxlang.runtime.context.ApplicationBoxContext;
import ortus.boxlang.runtime.context.BaseBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
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

	@AfterAll
	public static void teardown() {

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

		ApplicationBoxContext	appContext			= context.getParentOfType( ApplicationBoxContext.class );
		Application				app					= appContext.getApplication();
		Instant					actual				= DateTimeCaster.cast( variables.get( Key.of( "startTime" ) ) ).getWrapped().toInstant();
		Instant					now					= Instant.now();
		long					differenceInSeconds	= ChronoUnit.SECONDS.between( actual, now );

		assertThat( app.getName().getName() ).isEqualTo( "myAppsdfsdf" );
		assertThat( app.getSessionsCache() ).isNotNull();
		assertThat( app.getInterceptorPool() ).isNotNull();
		assertThat( app.getApplicationScope() ).isNotNull();
		assertThat( app.getApplicationScope().getName().getName() ).isEqualTo( "application" );
		assertThat( app.getClassLoaders() ).isNotNull();
		assertThat( app.hasStarted() ).isTrue();
		assertThat( differenceInSeconds ).isAtMost( 1L );
	}

	@DisplayName( "java settings setup" )
	@Test
	public void testJavaSettings() {
		// @formatter:off
		instance.executeSource(
		    """
		        application name="myJavaApp" javaSettings={
					loadPaths = [ "/src/test/resources/libs" ],
					reloadOnChange = true
				 };
			""", context );
		// @formatter:on

		ApplicationBoxContext	appContext	= context.getParentOfType( ApplicationBoxContext.class );
		Application				app			= appContext.getApplication();
		assertThat( app.getClassLoaderCount() ).isEqualTo( 1 );
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

}
