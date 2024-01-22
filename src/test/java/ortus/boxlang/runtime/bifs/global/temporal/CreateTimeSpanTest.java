
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

import java.time.Duration;

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

public class CreateTimeSpanTest {

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

	@DisplayName( "It tests the BIF CreateTimeSpan with no duration" )
	@Test
	public void testCreateTimeSpanEmpty() {
		instance.executeSource(
		    """
		    result = createTimespan( 0, 0, 0, 0, 0 );
		    """,
		    context );
		Duration duration = ( Duration ) variables.get( Key.of( "result" ) );
		assertEquals( duration.toMillis(), 0l );
	}

	@DisplayName( "It tests the BIF CreateTimeSpan with a 1 hour duration" )
	@Test
	public void testCreateTimeSpanHours() {
		instance.executeSource(
		    """
		    result = createTimespan( 0, 1, 0, 0, 0 );
		    """,
		    context );
		Duration duration = ( Duration ) variables.get( Key.of( "result" ) );
		assertEquals( duration.toHours(), 1l );
	}

	@DisplayName( "It tests the BIF CreateTimeSpan with a 1 hour duration" )
	@Test
	public void testCreateTimeSpanMulti() {
		Duration refDuration = Duration.ofDays( 365l )
		    .plusHours( 1l )
		    .plusMinutes( 2l )
		    .plusSeconds( 3l )
		    .plusMillis( 4l );
		instance.executeSource(
		    """
		    result = createTimespan( 365, 1, 2, 3, 4 );
		    """,
		    context );
		Duration duration = ( Duration ) variables.get( Key.of( "result" ) );
		assertEquals( duration, refDuration );
	}

	@DisplayName( "It tests the BIF CreateTimeSpan will throw an error with a missing argument" )
	@Test
	public void testCreateTimeSpanErro() {
		assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        result = createTimespan( 365, 1, 2 );
		        """,
		        context )
		);
	}

}
