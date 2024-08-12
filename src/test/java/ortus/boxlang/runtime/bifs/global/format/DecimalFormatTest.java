
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

public class DecimalFormatTest {

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

	@DisplayName( "It tests the BIF DecimalFormat" )
	@Test
	public void testDecimalFormat() {
		instance.executeSource(
		    """
		    result = decimalFormat(12345.67);
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "12,345.67" );
	}

	@DisplayName( "It tests the Member function Numeric.DecimalFormat" )
	@Test
	public void testDecimalMemberFunction() {
		instance.executeSource(
		    """
		    myDecimal = 12345.67;
		       result = myDecimal.decimalFormat();
		       """,
		    context );
		assertEquals( variables.getAsString( result ), "12,345.67" );
	}

	@DisplayName( "It tests the BIF will always provide at least two decimal places" )
	@Test
	public void testDecimalFormatPlaces() {
		instance.executeSource(
		    """
		    result = decimalFormat(12345);
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "12,345.00" );
	}

	@DisplayName( "It tests the BIF will truncate to two decimal places" )
	@Test
	public void testDecimalFormatTruncation() {
		instance.executeSource(
		    """
		    result = decimalFormat(12345.987654312);
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "12,345.99" );
	}

	@DisplayName( "It tests that an optional length will increase the number of decimal places" )
	@Test
	public void testDecimalFormatLength() {
		instance.executeSource(
		    """
		    result = decimalFormat(12345.987654312, 4 );
		    """,
		    context );
		assertEquals( variables.getAsString( result ), "12,345.9877" );
	}

	@DisplayName( "It tests that an an invalid input will throw an error" )
	@Test
	public void testDecimalFormatError() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        result = decimalFormat( "One, one french fry! Bwah! Ha! Ha!" );
		        """,
		        context )
		);
	}

}
