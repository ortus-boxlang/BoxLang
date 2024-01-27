
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

import java.text.DecimalFormat;
import java.util.Locale;

import org.junit.Ignore;
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

public class LSParseNumberTest {

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

	@DisplayName( "It tests the BIF LSParseNumber" )
	@Test
	@Ignore
	public void testBif() {
		assertEquals( ( Double ) instance.executeStatement( "LSParseNumber( '1.50', 'en_US' )" ), 1.50D );
		assertEquals( ( Double ) instance.executeStatement( "LSParseNumber( '1,50', 'de_AT' )" ), 1.50D );
		// Test currencies which may contain unicode numerics
		java.text.NumberFormat formatter = DecimalFormat.getNumberInstance( new Locale( "ar", "JO" ) );
		assertEquals( ( Double ) instance.executeStatement( "LSParseNumber( '" + formatter.format( 1000.51 ) + "', 'ar_JO' )" ), 1000.51D );
		assertEquals( ( Double ) instance.executeStatement( "LSParseNumber( 1000.51, 'ar_JO' )" ), 1000.51D );
		formatter = DecimalFormat.getNumberInstance( Locale.CHINA );
		assertEquals( ( Double ) instance.executeStatement( "LSParseNumber( '" + formatter.format( 1000.51 ) + "', 'China' )" ), 1000.51D );
		assertEquals( ( Double ) instance.executeStatement( "LSParseNumber( 1000.51, 'China' )" ), 1000.51D );
		assertThrows( BoxRuntimeException.class, () -> instance.executeStatement( "LSParseNumber( 'blah', 'en_US' )" ) );
	}

}
