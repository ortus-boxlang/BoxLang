
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class SetTimezoneTest {

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

	@DisplayName( "It tests the BIF SetTimezone with a full name" )
	@Test
	public void testSetTimezone() {
		ZoneId testZone = ZoneId.of( "America/Los_Angeles" );
		context.getParentOfType( RequestBoxContext.class ).setTimezone( ZoneId.of( "UTC" ) );
		assertNotEquals( context.getParentOfType( RequestBoxContext.class ).getTimezone(), testZone );
		instance.executeSource(
		    """
		    setTimezone( "America/Los_Angeles" );
		    """,
		    context );
		assertEquals( context.getParentOfType( RequestBoxContext.class ).getTimezone(), testZone );
	}

	@DisplayName( "It tests the BIF SetTimezone with a three letter alias zone code" )
	@Test
	public void testSetTimezoneCode() {
		ZoneId testZone = ZoneId.of( "America/Los_Angeles" );
		context.getParentOfType( RequestBoxContext.class ).setTimezone( ZoneId.of( "UTC" ) );
		assertNotEquals( context.getParentOfType( RequestBoxContext.class ).getTimezone(), testZone );
		instance.executeSource(
		    """
		    setTimezone( "PST" );
		    """,
		    context );
		assertEquals( context.getParentOfType( RequestBoxContext.class ).getTimezone(), testZone );
	}

	@DisplayName( "It tests the BIF SetTimezone with a GMT Offset" )
	@Test
	public void testSetTimezoneOffset() {
		ZoneId testZone = ZoneId.of( "GMT-05:00" );
		context.getParentOfType( RequestBoxContext.class ).setTimezone( ZoneId.of( "GMT" ) );
		assertNotEquals( context.getParentOfType( RequestBoxContext.class ).getTimezone(), testZone );
		instance.executeSource(
		    """
		    setTimezone( "GMT-05:00" );
		    """,
		    context );
		assertEquals( context.getParentOfType( RequestBoxContext.class ).getTimezone(), testZone );
	}

	@DisplayName( "It tests the BIF SetTimezone will throw an error with an invalid timezone" )
	@Test
	public void testSetTimezoneError() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        setTimezone( "XYZ" );
		        """,
		        context )
		);
	}

}
