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
import static org.junit.Assert.assertNotEquals;

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
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.util.LocalizationUtil;

public class NowTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;

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
		ZonedDateTime		referenceNow		= ZonedDateTime.of( LocalDateTime.now(), LocalizationUtil.parseZoneId( null, context ) );
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
		ZonedDateTime		referenceNow		= ZonedDateTime.now( ZoneId.of( "UTC" ) );
		DateTimeFormatter	referenceFormatter	= DateTimeFormatter.ofPattern( DateTime.TS_FORMAT_MASK );
		assertThat( result.toString() ).isInstanceOf( String.class );
		assertThat( result.toString() ).isEqualTo( referenceFormatter.format( referenceNow ) );
		assertThat( DateTimeCaster.cast( result ).getWrapped().getZone() ).isEqualTo( referenceNow.getZone() );
	}

	@DisplayName( "It can retrieve the current date which will be created with the assigned timezone" )
	@Test
	void testAssignedTimezone() {
		instance.executeSource(
		    """
		    setTimezone( "America/Los_Angeles" );
		       result = now()
		       """,
		    context );
		var result = variables.get( Key.of( "result" ) );
		assertThat( result ).isInstanceOf( DateTime.class );
		ZonedDateTime		referenceNow		= ZonedDateTime.now( ZoneId.of( "America/Los_Angeles" ) );
		DateTimeFormatter	referenceFormatter	= DateTimeFormatter.ofPattern( DateTime.TS_FORMAT_MASK );
		assertThat( result.toString() ).isInstanceOf( String.class );
		assertThat( result.toString() ).isEqualTo( referenceFormatter.format( referenceNow ) );
		assertThat( DateTimeCaster.cast( result ).getWrapped().getZone() ).isEqualTo( referenceNow.getZone() );
	}

	@DisplayName( "Current local time will translated in to the assigned timezone" )
	@Test
	void testNowWillConvertToTimezone() {
		instance.executeSource(
		    """
		       setTimezone( "America/Los_Angeles" );
		    localTime = now();
		       result = now( "UTC" )
		          """,
		    context );
		var local = variables.get( Key.of( "localTime" ) );
		assertThat( local ).isInstanceOf( DateTime.class );
		var				result		= variables.get( Key.of( "result" ) );
		ZonedDateTime	localTime	= DateTimeCaster.cast( local ).getWrapped();
		ZonedDateTime	resultTime	= DateTimeCaster.cast( result ).getWrapped();
		assertNotEquals( localTime.getZone(), resultTime.getZone() );
		assertNotEquals( localTime.getHour(), resultTime.getHour() );
	}

}
