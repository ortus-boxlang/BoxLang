
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

package ortus.boxlang.runtime.bifs.global.i18n;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class GetLocaleDisplayNameTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@AfterAll
	public static void teardown() {
		instance.shutdown();
	}

	@BeforeEach
	public void setupEach() {
		variables.clear();
	}

	@DisplayName( "It tests the BIF GetLocaleDisplayName with no arguments" )
	@Test
	public void testBifGetLocaleDisplayName() {
		context.setConfigItem( Key.locale, Locale.US );
		instance.executeSource(
		    """
		    result = GetLocaleDisplayName();
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertEquals( result, "English (United States)" );

	}

	@DisplayName( "It tests the BIF GetLocaleDisplayName with only locale arg" )
	@Test
	public void testGetLocaleDisplayNameSingleArg() {
		instance.executeSource(
		    """
		    result = GetLocaleDisplayName( "en-US" );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertEquals( result, "English (United States)" );

	}

	@DisplayName( "It tests the BIF GetLocaleDisplayName" )
	@Test
	public void testGetLocaleDisplayNameBothArgs() {
		instance.executeSource(
		    """
		    result = GetLocaleDisplayName( "en-US", "en-US" );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertEquals( result, "English (United States)" );

	}

	@DisplayName( "It tests the BIF GetLocaleDisplayName with only dspLocale arg" )
	@Test
	public void testGetLocaleDisplayNameDspArg() {
		instance.executeSource(
		    """
		    result = GetLocaleDisplayName( dspLocale="Germany" );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertEquals( result, "Englisch (Vereinigte Staaten)" );

	}

}
