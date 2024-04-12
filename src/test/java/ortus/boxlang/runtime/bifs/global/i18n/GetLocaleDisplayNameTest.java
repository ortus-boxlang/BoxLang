
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
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class GetLocaleDisplayNameTest {

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
		context.getParentOfType( RequestBoxContext.class ).setLocale( new Locale( "en", "US" ) );
	}

	@DisplayName( "It tests the BIF GetLocaleDisplayName with no arguments" )
	@Test
	public void testBifGetLocaleDisplayName() {
		Locale	contextLocale	= ( Locale ) context.getConfig().getAsStruct( Key.runtime ).get( Key.locale );
		String	refDisplayName	= contextLocale.getDisplayName( contextLocale );
		instance.executeSource(
		    """
		    result = GetLocaleDisplayName();
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertEquals( result, refDisplayName );
	}

	@DisplayName( "It tests the BIF GetLocaleDisplayName with only locale arg" )
	@Test
	public void testGetLocaleDisplayNameSingleArg() {
		Locale	contextLocale	= ( Locale ) context.getConfig().getAsStruct( Key.runtime ).get( Key.locale );
		String	refDisplayName	= contextLocale.getDisplayName( contextLocale );
		instance.executeSource(
		    """
		    result = GetLocaleDisplayName( "en-US" );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertEquals( result, refDisplayName );

	}

	@DisplayName( "It tests the BIF GetLocaleDisplayName" )
	@Test
	public void testGetLocaleDisplayNameBothArgs() {
		Locale	contextLocale	= ( Locale ) context.getConfig().getAsStruct( Key.runtime ).get( Key.locale );
		String	refDisplayName	= contextLocale.getDisplayName( contextLocale );
		// Set a different locale to test whether our explicit args are being honored
		context.getParentOfType( RequestBoxContext.class ).setLocale( Locale.GERMANY );
		instance.executeSource(
		    """
		    result = GetLocaleDisplayName( "en-US", "en-US" );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertEquals( result, refDisplayName );

	}

	@DisplayName( "It tests the BIF GetLocaleDisplayName with only dspLocale arg" )
	@Test
	public void testGetLocaleDisplayNameDspArg() {
		Locale	contextLocale	= ( Locale ) context.getConfig().getAsStruct( Key.runtime ).get( Key.locale );
		String	refDisplayName	= contextLocale.getDisplayName( Locale.GERMANY );
		instance.executeSource(
		    """
		    result = GetLocaleDisplayName( dspLocale="Germany" );
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertEquals( result, refDisplayName );

	}

}
