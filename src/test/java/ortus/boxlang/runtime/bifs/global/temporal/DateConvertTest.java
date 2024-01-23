
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
		context.getParentOfType( RequestBoxContext.class ).setTimezone( ZoneId.of( "America/Los_Angeles" ) );
		variables.clear();
	}

	@DisplayName( "It tests the BIF DateConvert local2UTC" )
	@Test
	public void testDateCompareLocal2Utc() {
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
	public void testDateCompareUtcToLocal() {
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
		System.out.println( result.getWrapped().toString() );
		System.out.println( conversionRef.getWrapped().toString() );
		assertTrue( result.getWrapped().equals( conversionRef.getWrapped() ) );
	}

}
