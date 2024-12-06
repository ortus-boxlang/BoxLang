
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

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.DateTime;

public class DateConvertTest {

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
		context.getParentOfType( RequestBoxContext.class ).setTimezone( ZoneId.of( "America/Los_Angeles" ) );
	}

	@DisplayName( "It tests the BIF DateConvert local2UTC" )
	@Test
	public void testDateConvertLocal2Utc() {
		var	localZone	= ZoneId.of( "America/Los_Angeles" );
		var	dateRef		= new DateTime( localZone );
		assertEquals( dateRef.getWrapped().getZone(), localZone );
		var conversionRef = dateRef.convertToZone( ZoneId.of( "UTC" ) );
		variables.put( Key.of( "date" ), dateRef );
		instance.executeSource(
		    """
		    result = dateConvert( "local2Utc", date );
		    """,
		    context );
		DateTime result = DateTimeCaster.cast( variables.get( Key.of( "result" ) ) );
		assertNotEquals( result.getWrapped().getZone(), localZone );
		assertTrue( result.getWrapped().equals( conversionRef.getWrapped() ) );
	}

	@DisplayName( "It tests the BIF DateConvert with utc2Local" )
	@Test
	public void testDateConvertUtcToLocal() {
		var	utcZone		= ZoneId.of( "UTC" );
		var	localZone	= ZoneId.of( "America/Los_Angeles" );
		var	dateRef		= new DateTime( utcZone );

		assertEquals( dateRef.getWrapped().getZone(), utcZone );

		var conversionRef = dateRef.convertToZone( localZone );
		variables.put( Key.of( "date" ), dateRef );
		instance.executeSource(
		    """
		    result = dateConvert( "utc2local", date );
		    """,
		    context );

		DateTime result = DateTimeCaster.cast( variables.get( Key.of( "result" ) ) );
		assertNotEquals( result.getWrapped().getZone(), utcZone );
		assertTrue( result.getWrapped().equals( conversionRef.getWrapped() ) );
	}

	@DisplayName( "It tests the BIF DateConvert with utc2Local on epoch date" )
	@Test
	public void testDateConvertUtcToLocalEpoch() {
		var	utcZone		= ZoneId.of( "UTC" );
		var	localZone	= ZoneId.of( "America/Los_Angeles" );
		var	dateRef		= "1970-01-01T00:00";
		variables.put( Key.of( "date" ), dateRef );
		instance.executeSource(
		    """
		    setTimezone( "America/Los_Angeles" );
		       result = dateConvert( "utc2local", date );
		       """,
		    context );

		DateTime result = DateTimeCaster.cast( variables.get( Key.of( "result" ) ) );
		assertNotEquals( result.getWrapped().getZone(), utcZone );
		assertEquals( result.getWrapped().getZone(), localZone );
		assertEquals( "1969-12-31T16:00", result.format( "yyyy-MM-dd'T'HH:mm" ) );
	}

	@DisplayName( "It tests the BIF DateConvert with utc2Local on epoch date" )
	@Test
	public void testDateConvertLocalToUTCEpoch() {
		var	utcZone		= ZoneId.of( "UTC" );
		var	localZone	= ZoneId.of( "America/Los_Angeles" );
		var	dateRef		= "1969-12-31T16:00:00";
		variables.put( Key.of( "date" ), dateRef );
		instance.executeSource(
		    """
		    setTimezone( "America/Los_Angeles" );
		       result = dateConvert( "local2utc", date );
		       """,
		    context );

		DateTime result = variables.getAsDateTime( Key.of( "result" ) );
		assertNotEquals( result.getWrapped().getZone(), localZone );
		assertEquals( result.getWrapped().getZone(), utcZone );
		assertEquals( "1970-01-01T00:00", result.format( "yyyy-MM-dd'T'HH:mm" ) );
	}

}
