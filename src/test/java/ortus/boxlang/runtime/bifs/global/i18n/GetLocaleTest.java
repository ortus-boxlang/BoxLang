
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
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.util.LocalizationUtil;

public class GetLocaleTest {

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

	@DisplayName( "It tests the BIF GetLocale will find a known alias" )
	@Test
	public void testBif() {
		context.getParentOfType( RequestBoxContext.class ).setLocale( ( Locale ) LocalizationUtil.COMMON_LOCALES.get( "US" ) );
		instance.executeSource(
		    """
		    result = getLocale();
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertEquals( result, "English (US)" );

	}

	@DisplayName( "It tests the BIF GetLocale will return `Language (Country)` without a known alias" )
	@Test
	public void testBifNoAlias() {
		context.getParentOfType( RequestBoxContext.class ).setLocale( new Locale( "ar", "TR" ) );
		Locale	localeRef	= new Locale( "ar", "TR" );
		String	refResult	= String.format(
		    "%s (%s)",
		    localeRef.getDisplayLanguage( ( Locale ) LocalizationUtil.COMMON_LOCALES.get( "US" ) ),
		    BooleanCaster.cast( localeRef.getVariant().length() ) ? localeRef.getVariant()
		        : localeRef.getDisplayCountry( ( Locale ) LocalizationUtil.COMMON_LOCALES.get( "US" ) )
		);
		instance.executeSource(
		    """
		    result = getLocale();
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertEquals( result, refResult );

	}

}
