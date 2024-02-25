
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

package ortus.boxlang.runtime.components.jdbc;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;

public class DBInfoTest {

	static BoxRuntime	instance;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {

	}

	@DisplayName( "It requires a non-null `type` argument matching a valid type" )
	@Test
	public void requiredTypeValidation() {
		assertThrows( BoxValidationException.class, () -> {
			instance.executeStatement( "CFDBInfo();" );
		} );
		assertThrows( BoxValidationException.class, () -> {
			instance.executeStatement( "CFDBInfo( type='foo' );" );
		} );
		// assertDoesNotThrow( () -> {
		// instance.executeStatement( "CFDBInfo( type='version', name='result' );" );
		// } );
	}

	@DisplayName( "It requires the `table` argument on column, foreignkeys, and index types`" )
	@Test
	public void typeRequiresTableValidation() {
		assertThrows( BoxValidationException.class, () -> {
			instance.executeStatement( "CFDBInfo( type='columns' );" );
		} );
		assertThrows( BoxValidationException.class, () -> {
			instance.executeStatement( "CFDBInfo( type='foreignkeys' );" );
		} );
		assertThrows( BoxValidationException.class, () -> {
			instance.executeStatement( "CFDBInfo( type='index' );" );
		} );
	}

	@Disabled( "Unimplemented" )
	@DisplayName( "Can get JDBC driver version info" )
	@Test
	public void testVersion() {
		Object result = instance.executeStatement( "cfdbinfo( type='version' )" );
		assertTrue( result instanceof Query );
	}

}
