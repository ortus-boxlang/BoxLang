
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

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.DecimalFormat;

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

public class LSNumberFormatTest {

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

	@DisplayName( "It tests the BIF LSNumberFormat with default mask and locale" )
	@Test
	public void testDefault() {
		instance.executeSource(
		    """
		    result = LSnumberFormat( 12345 );
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "12,345" );
	}

	@DisplayName( "It tests the BIF LSNumberFormat with number format placeholder and locale" )
	@Test
	public void testBif() {

		instance.executeSource(
		    """
		    result = LSnumberFormat( 12345, "0.00", "German (Austrian)");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "12345,00" );
		instance.executeSource(
		    """
		    result = LSnumberFormat( 12345, "_.__", "German (Austrian)");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "12345" );
		instance.executeSource(
		    """
		    result = LSnumberFormat( 12345, "9.999", "German (Austrian)");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "12345,000" );
		String refGrouped = new DecimalFormat( "#,##0.00", LocalizationUtil.localizedDecimalSymbols( LocalizationUtil.buildLocale( "de", "AT" ) ) )
		    .format( 12345D );
		instance.executeSource(
		    """
		    result = LSnumberFormat( 12345, "_,__0.00", "German (Austrian)");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), refGrouped );
	}

	@DisplayName( "It tests the BIF LSNumberFormat localized masks" )
	@Test
	public void testBifCommonFormat() {
		java.text.NumberFormat formatter = DecimalFormat.getCurrencyInstance( LocalizationUtil.buildLocale( "de", "AT" ) );
		instance.executeSource(
		    """
		    result = LSnumberFormat( 12345, "ls$", "German (Austrian)" );
		    """,
		    context );
		assertEquals( variables.getAsString( result ), formatter.format( 12345D ) );
	}

	@DisplayName( "It tests will throw an error with an unparseable date" )
	@Test
	public void testBifError() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        result = LSnumberFormat( "Blah", "0.00");
		        """,
		        context )
		);
	}

}
