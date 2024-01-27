
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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.LocalizationUtil;

public class LSCurrencyFormatTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
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

	@DisplayName( "It tests the BIF LSCurrencyFormat USD" )
	@Test
	public void testBifCommonFormat() {
		java.text.NumberFormat formatter = LocalizationUtil.localizedCurrencyFormatter( new Locale( "en", "US" ), "local" );
		instance.executeSource(
		    """
		    result = LSCurrencyFormat( 12345, "local", "en_US" );
		    """,
		    context );
		assertEquals( variables.getAsString( result ), formatter.format( 12345D ) );
		formatter = LocalizationUtil.localizedCurrencyFormatter( new Locale( "en", "US" ), "international" );
		instance.executeSource(
		    """
		    result = LSCurrencyFormat( 12345, "international", "en_US" );
		    """,
		    context );
		assertEquals( variables.getAsString( result ), formatter.format( 12345D ) );
		formatter = LocalizationUtil.localizedCurrencyFormatter( new Locale( "en", "US" ), "none" );
		instance.executeSource(
		    """
		    result = LSCurrencyFormat( 12345, "none", "en_US" );
		    """,
		    context );
		assertEquals( variables.getAsString( result ), formatter.format( 12345D ) );
	}

	@DisplayName( "It tests the Numeric.LSCurrencyFormat member function" )
	@Test
	public void testMemberFunctions() {
		java.text.NumberFormat formatter = LocalizationUtil.localizedCurrencyFormatter( new Locale( "en", "US" ), "local" );
		instance.executeSource(
		    """
		    number = 12345;
		       result = number.LSCurrencyFormat( "local", "en_US" );
		       """,
		    context );
		assertEquals( variables.getAsString( result ), formatter.format( 12345D ) );
		formatter = LocalizationUtil.localizedCurrencyFormatter( new Locale( "en", "US" ), "international" );
		instance.executeSource(
		    """
		    number = 12345;
		       result = number.LSCurrencyFormat( "international", "en_US" );
		       """,
		    context );
		assertEquals( variables.getAsString( result ), formatter.format( 12345D ) );
		formatter = LocalizationUtil.localizedCurrencyFormatter( new Locale( "en", "US" ), "none" );
		instance.executeSource(
		    """
		    number = 12345;
		       result = number.LSCurrencyFormat( "none", "en_US" );
		       """,
		    context );
		assertEquals( variables.getAsString( result ), formatter.format( 12345D ) );
	}

	@DisplayName( "It tests the BIF LSCurrencyFormat EURO" )
	@Test
	public void testBifEuro() {
		java.text.NumberFormat formatter = LocalizationUtil.localizedCurrencyFormatter( new Locale( "de", "AT" ), "local" );
		instance.executeSource(
		    """
		    result = LSCurrencyFormat( 12345, "local", "de_AT" );
		    """,
		    context );
		assertEquals( variables.getAsString( result ), formatter.format( 12345D ) );
		formatter = LocalizationUtil.localizedCurrencyFormatter( new Locale( "de", "AT" ), "international" );
		instance.executeSource(
		    """
		    result = LSCurrencyFormat( 12345, "international", "de_AT" );
		    """,
		    context );
		assertEquals( variables.getAsString( result ), formatter.format( 12345D ) );
		formatter = LocalizationUtil.localizedCurrencyFormatter( new Locale( "de", "AT" ), "none" );
		instance.executeSource(
		    """
		    result = LSCurrencyFormat( 12345, "none", "de_AT" );
		    """,
		    context );
		assertEquals( variables.getAsString( result ), formatter.format( 12345D ) );
	}

	@DisplayName( "It tests the BIF LSCurrencyFormat Yen" )
	@Test
	public void testBifYen() {
		java.text.NumberFormat formatter = LocalizationUtil.localizedCurrencyFormatter( Locale.JAPAN, "local" );
		instance.executeSource(
		    """
		    result = LSCurrencyFormat( 12345, "local", "japan" );
		    """,
		    context );
		assertEquals( variables.getAsString( result ), formatter.format( 12345D ) );
		formatter = LocalizationUtil.localizedCurrencyFormatter( Locale.JAPAN, "international" );
		instance.executeSource(
		    """
		    result = LSCurrencyFormat( 12345, "international", "japan" );
		    """,
		    context );
		assertEquals( variables.getAsString( result ), formatter.format( 12345D ) );
		formatter = LocalizationUtil.localizedCurrencyFormatter( Locale.JAPAN, "none" );
		instance.executeSource(
		    """
		    result = LSCurrencyFormat( 12345, "none", "japan" );
		    """,
		    context );
		assertEquals( variables.getAsString( result ), formatter.format( 12345D ) );
	}

	@DisplayName( "It tests the BIF LSCurrencyFormat will error with an invalid type" )
	@Test
	public void testBifError() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        result = LSCurrencyFormat( 12345, "blah", "japan" );
		        """,
		        context )
		);
	}

}
