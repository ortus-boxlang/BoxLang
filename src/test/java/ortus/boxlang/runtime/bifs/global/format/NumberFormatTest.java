
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

package ortus.boxlang.runtime.bifs.global.format;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.DecimalFormat;
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

public class NumberFormatTest {

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

	@DisplayName( "It tests the BIF NumberFormat with no mask" )
	@Test
	public void testDefault() {
		instance.executeSource(
		    """
		    result = numberFormat( 12345 );
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "12,345" );
	}

	@DisplayName( "It tests the BIF NumberFormat with number format placeholder Masks" )
	@Test
	public void testBif() {

		instance.executeSource(
		    """
		    result = numberFormat( 12345, "9.99");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "12345.00" );
		instance.executeSource(
		    """
		    result = numberFormat( 12345, "_.__");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "12345" );
		instance.executeSource(
		    """
		    result = numberFormat( 12345, "9.999");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "12345.000" );
	}

	@DisplayName( "It tests the BIF NumberFormat with common format masks" )
	@Test
	public void testBifCommonFormat() {
		instance.executeSource(
		    """
		    result = numberFormat( -12345, "()");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "(12345)" );
		instance.executeSource(
		    """
		    result = numberFormat( 12345, "()");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "12345" );
		instance.executeSource(
		    """
		    result = numberFormat( 12345, "_,9");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "12345.000000000" );
		instance.executeSource(
		    """
		    result = numberFormat( 12345, "+");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "+12345" );

		instance.executeSource(
		    """
		    result = numberFormat( -12345, "+");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "-12345" );

		instance.executeSource(
		    """
		    result = numberFormat( 12345, "-");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), " 12345" );

		instance.executeSource(
		    """
		    result = numberFormat( -12345, "-");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "-12345" );

		instance.executeSource(
		    """
		    result = numberFormat( 12345, "$");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "$12,345.00" );

		java.text.NumberFormat formatter = DecimalFormat.getCurrencyInstance( Locale.getDefault() );
		instance.executeSource(
		    """
		    result = numberFormat( 12345, "ls$");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), formatter.format( 12345D ) );
	}

	@DisplayName( "It tests the BIF NumberFormat with justification masks" )
	@Test
	public void testBifJustify() {
		instance.executeSource(
		    """
		    result = numberFormat( 1, "L000");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "001" );
		instance.executeSource(
		    """
		    result = numberFormat( 1, "C000");
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "1" );
	}

	@DisplayName( "It tests will throw an error with an unparseable date" )
	@Test
	public void testBifError() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        result = numberFormat( "Blah", "0.00");
		        """,
		        context )
		);
	}

}
