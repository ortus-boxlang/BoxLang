
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

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZoneId;

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

public class ClearTimezoneTest {

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

	@DisplayName( "It tests the BIF ClearTimezone" )
	@Test
	public void testClearTimezone() {
		ZoneId testZone = ZoneId.of( "America/Los_Angeles" );
		context.getParentOfType( RequestBoxContext.class ).setTimezone( testZone );
		assertEquals( context.getParentOfType( RequestBoxContext.class ).getTimezone(), testZone );
		assertEquals( ( ZoneId ) context.getConfigItem( Key.timezone ), testZone );
		instance.executeSource(
		    """
		    clearTimezone();
		    """,
		    context );
		assertNull( context.getParentOfType( RequestBoxContext.class ).getTimezone() );
		assertNull( context.getConfigItem( Key.timezone ) );
	}

	@DisplayName( "It tests the ClearTimezone works even if a default is not set" )
	@Test
	public void testClearNull() {
		assertNull( context.getParentOfType( RequestBoxContext.class ).getTimezone() );
		assertNull( context.getConfigItem( Key.timezone ) );
		instance.executeSource(
		    """
		    clearTimezone();
		    """,
		    context );
		assertNull( context.getParentOfType( RequestBoxContext.class ).getTimezone() );
		assertNull( context.getConfigItem( Key.timezone ) );
	}

}
