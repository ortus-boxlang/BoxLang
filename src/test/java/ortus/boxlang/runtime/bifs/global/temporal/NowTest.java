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

import static com.google.common.truth.Truth.assertThat;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

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
import ortus.boxlang.runtime.types.DateTime;

public class NowTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;

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

	@DisplayName( "It can retrieve the current date" )
	@Test
	void testTrueConditions() {
		instance.executeSource(
		    """
		    result = now()
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( DateTime.class );
		ZonedDateTime		referenceNow		= ZonedDateTime.of( LocalDateTime.now(), ZoneId.systemDefault() );
		DateTimeFormatter	referenceFormatter	= DateTimeFormatter.ofPattern( DateTime.TS_FORMAT_MASK );
		assertThat( result.toString() ).isInstanceOf( String.class );
		assertThat( result.toString() ).isEqualTo( referenceFormatter.format( referenceNow ) );
	}

	@DisplayName( "It can retrieve the current date with a timezone" )
	@Test
	void testTimezone() {
		instance.executeSource(
		    """
		    result = now( "UTC" )
		    """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( DateTime.class );
		ZonedDateTime		referenceNow		= ZonedDateTime.of( LocalDateTime.now(), ZoneId.of( "UTC" ) );
		DateTimeFormatter	referenceFormatter	= DateTimeFormatter.ofPattern( DateTime.TS_FORMAT_MASK );
		assertThat( result.toString() ).isInstanceOf( String.class );
		assertThat( result.toString() ).isEqualTo( referenceFormatter.format( referenceNow ) );
	}

}
