
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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.LocalizationUtil;

public class SetLocaleTest {

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
		context.getParentOfType( RequestBoxContext.class ).setLocale( null );
	}

	@DisplayName( "It tests the BIF SetLocale" )
	@Test
	public void testBif() {
		assertNull( context.getParentOfType( RequestBoxContext.class ).getLocale() );
		instance.executeSource(
		    """
		    result = setLocale( "en-US" );
		    """,
		    context );
		assertEquals( context.getParentOfType( RequestBoxContext.class ).getLocale(), LocalizationUtil.COMMON_LOCALES.get( "US" ) );
		var result = variables.get( Key.of( "result" ) );
		assertEquals( "English (United States)", result );
	}

	@DisplayName( "It tests the BIF SetLocale on a common locale" )
	@Test
	public void testBifCommon() {
		assertNull( context.getParentOfType( RequestBoxContext.class ).getLocale() );
		instance.executeSource(
		    """
		    setLocale( "United States" );
		    """,
		    context );
		assertEquals( context.getParentOfType( RequestBoxContext.class ).getLocale(), LocalizationUtil.COMMON_LOCALES.get( "US" ) );
	}

	@DisplayName( "It tests the BIF SetLocale on an aliased locale" )
	@Test
	public void testBifAlias() {
		assertNull( context.getParentOfType( RequestBoxContext.class ).getLocale() );
		instance.executeSource(
		    """
		    setLocale( "english (us)" );
		    """,
		    context );
		assertEquals( context.getParentOfType( RequestBoxContext.class ).getLocale(), LocalizationUtil.COMMON_LOCALES.get( "US" ) );
	}

	@DisplayName( "It tests the BIF SetLocale will throw an error on an invalid locale" )
	@Test
	public void testBifError() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        setLocale( "blah-Blah" );
		        """,
		        context )
		);
	}

	@DisplayName( "It can set the locale, test it, and then clear it" )
	@Test
	public void testBifClear() {
		assertNull( context.getParentOfType( RequestBoxContext.class ).getLocale() );
		// @formatter:off
		instance.executeSource(
		    """
				setLocale( "es_SV" );
				result = getLocale();
				clearLocale();
				result2 = getLocale();
		    """,
		    context );
		// @formatter:on
		assertEquals( context.getParentOfType( RequestBoxContext.class ).getLocale(), null );
		var result = variables.get( Key.of( "result" ) );
		System.out.println( "set locale -=---- > " + result );
		assertThat( result ).isNotNull();
		var result2 = variables.get( Key.of( "result2" ) );
		// Should be the default
		assertThat( result ).isNotEqualTo( result2 );
	}

}
