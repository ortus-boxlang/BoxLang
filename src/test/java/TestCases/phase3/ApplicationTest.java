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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.scopes.ApplicationScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.SessionScope;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class ApplicationTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	VariablesScope		variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingBoxContext( instance.getRuntimeContext() );
		variables	= ( VariablesScope ) context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "basic class" )
	@Test
	public void testBasicClass() {

		instance.executeSource(
		    """
		      import java:ortus.boxlang.runtime.context.ApplicationBoxContext;
		      import java:ortus.boxlang.runtime.context.SessionBoxContext;
		      import java:ortus.boxlang.runtime.scopes.Key;
		      	context = getBoxContext();
		    thisApp = context.getRuntime().getApplicationService().getApplication( Key.of( "myApp" ) )
		    // manually wire up application and session contexts
		                   context
		      		.injectParentContext(
		      				new java:ApplicationBoxContext(
		      					thisApp
		      				)
		      			)
		                   context
		      		.injectParentContext(
		      				new java:SessionBoxContext(
		      					thisApp.getSession( Key.of( "my-session-id" ) )
		      				)
		      			)
		      		result = application;
		      		result2 = session;

		                        """, context );

		assertThat( variables.get( result ) ).isInstanceOf( ApplicationScope.class );
		assertThat( variables.get( Key.of( "result2" ) ) ).isInstanceOf( SessionScope.class );
	}

}