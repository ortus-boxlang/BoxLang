
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

package ortus.boxlang.runtime.bifs.global.temporal;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class LSIsDateTest {

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

	@DisplayName( "It tests the BIF LSIsDate with a full ISO including offset" )
	@Test
	public void testLSIsDateFullISO() {
		instance.executeSource(
		    """
		    result = LSIsDate( "2024-01-14T00:00:01.0001Z" );
		    """,
		    context );
		assertTrue( variables.getAsBoolean( result ) );
	}

	@DisplayName( "It tests the BIF LSIsDate with a full ISO without offset" )
	@Test
	public void testLSIsDateNoOffset() {
		instance.executeSource(
		    """
		    result = LSIsDate( "2024-01-14T00:00:01.0001" );
		    """,
		    context );
		assertTrue( variables.getAsBoolean( result ) );
	}

	@DisplayName( "It tests the BIF LSIsDate with without any time" )
	@Test
	public void testLSIsDateNoTime() {
		instance.executeSource(
		    """
		    result = LSIsDate( "2024-01-14" );
		    """,
		    context );
		assertTrue( variables.getAsBoolean( result ) );
	}

	@DisplayName( "It tests the BIF LSIsDate with a full ISO including offset and locale argument" )
	@Test
	public void testLSIsDateFullISOLocale() {
		instance.executeSource(
		    """
		    result = LSIsDate( "2024-01-14T00:00:01.0001Z", "en-US" );
		    """,
		    context );
		assertTrue( variables.getAsBoolean( result ) );
	}

	@DisplayName( "It tests the BIF LSIsDate using a localized russian format" )
	@Test
	public void testLSIsDateRussian() {
		instance.executeSource(
		    """
		    result = LSIsDate( "14.01.2024", "ru_RU" );
		    """,
		    context );
		assertTrue( variables.getAsBoolean( result ) );
	}

	@DisplayName( "It tests the BIF LSIsDate using a localized, Spanish long-form format" )
	@Test
	public void testLSIsDateSpain() {

		instance.executeSource(
		    """
		    result = LSIsDate( "14 de enero de 2024", "es-ES" );
		    """,
		    context );
		assertTrue( variables.getAsBoolean( result ) );
	}

	@DisplayName( "It tests the BIF LSIsDate using traditional chinese format" )
	@Test
	public void testLSIsDateChinese() {
		instance.executeSource(
		    """
		    result = LSIsDate( "2024年1月14日", "zh-Hant" );
		    """,
		    context );
		assertTrue( variables.getAsBoolean( result ) );
	}

	@DisplayName( "It tests the BIF LSIsDate returns false with an invalid date" )
	@Test
	public void testLSIsDateFalseChinese() {
		instance.executeSource(
		    """
		    result = LSIsDate( "12345", "zh-Hant" );
		    """,
		    context );
		assertFalse( variables.getAsBoolean( result ) );
	}

	@DisplayName( "It tests the BIF LSIsDate returns false if the date is from another locale" )
	@Test
	public void testLSIsDateFalseWrongLocale() {
		instance.executeSource(
		    """
		    result = LSIsDate( "2024年1月14日", "en-US" );
		    """,
		    context );
		assertFalse( variables.getAsBoolean( result ) );
	}

	@DisplayName( "It tests the BIF LSIsDate will throw an error with an invalid timezone" )
	@Test
	public void testLSIsDateTimezoneError() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        result = LSIsDate( "2024-01-14", "en-US", "Blah" );
		        """,
		        context )
		);
	}

}
