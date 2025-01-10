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

package ortus.boxlang.runtime.bifs.global.system;

import static com.google.common.truth.Truth.assertThat;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.ConfigOverrideBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.RequestScope;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class GetBaseTemplatePathTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
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

	@DisplayName( "It gets base template path" )
	@Test
	public void testBaseTemplate() {

		context = getContext( "src/test/java/TestCases/applications/appClass/", "sub2/index.bxm" );
		instance.executeTemplate(
		    "sub2/index.bxm",
		    context );

		IScope	request		= context.getScopeNearby( RequestScope.name );
		String	actualPath	= new java.io.File( "src/test/java/TestCases/applications/appClass/sub2/index.bxm" ).getAbsolutePath();
		assertThat( request.get( "indexbxmBasePath" ) ).isEqualTo( actualPath );
		assertThat( request.get( "includexmBasePath" ) ).isEqualTo( actualPath );

	}

	private IBoxContext getContext( String rootPath, String template ) {
		try {
			return new ScriptingRequestBoxContext( new ConfigOverrideBoxContext( instance.getRuntimeContext(), config -> {
				config.getAsStruct( Key.mappings ).put( "/", new java.io.File( rootPath ).getAbsolutePath() );
				return config;
			} ), new URI( template ) );
		} catch ( URISyntaxException e ) {
			throw new RuntimeException( e );
		}
	}

}
