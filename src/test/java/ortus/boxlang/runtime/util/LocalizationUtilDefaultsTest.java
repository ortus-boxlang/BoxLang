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
package ortus.boxlang.runtime.util;

import static com.google.common.truth.Truth.assertThat;

import java.time.ZoneId;
import java.util.Locale;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.Configuration;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.operators.Compare;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.DateTime;

/**
 * BL-2758: request and application timezone and locale overrides still win over the runtime default,
 * and the runtime default is read live.
 */
public class LocalizationUtilDefaultsTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "The runtime default timezone is read live, so config changes are seen at once" )
	@Test
	public void testRuntimeTimezoneChangeIsSeen() {
		Configuration	config		= instance.getConfiguration();
		ZoneId			original	= config.timezone;
		ZoneId			tokyo		= ZoneId.of( "Asia/Tokyo" );
		try {
			config.timezone = tokyo;
			assertThat( LocalizationUtil.parseZoneId( null, instance.getRuntimeContext() ) ).isEqualTo( tokyo );
			assertThat( DateTimeCaster.attempt( "2024-01-15 10:30:00", instance.getRuntimeContext() ).get().getZone() ).isEqualTo( tokyo );
		} finally {
			config.timezone = original;
		}
		assertThat( LocalizationUtil.parseZoneId( null, instance.getRuntimeContext() ) ).isEqualTo( original );
	}

	@DisplayName( "Date compares give the same results" )
	@Test
	public void testDateCompareResults() {
		DateTime	earlier	= DateTimeCaster.cast( "2024-01-15 10:30:00", true, context );
		DateTime	later	= DateTimeCaster.cast( "2024-01-15 10:31:00", true, context );

		assertThat( Compare.invoke( context, earlier, later ) ).isEqualTo( -1 );
		assertThat( Compare.invoke( context, later, earlier ) ).isEqualTo( 1 );
		assertThat( Compare.invoke( context, earlier, earlier.clone() ) ).isEqualTo( 0 );
		assertThat( Compare.invoke( context, earlier, "2024-01-15 10:31:00" ) ).isEqualTo( -1 );
	}

	@DisplayName( "setTimezone() on the request still wins over the runtime default" )
	@Test
	public void testRequestTimezoneOverrideWins() {
		ZoneId zone = ZoneId.of( "America/Los_Angeles" );
		context.getRequestContext().setTimezone( zone );

		assertThat( LocalizationUtil.parseZoneId( null, context ) ).isEqualTo( zone );
		assertThat( DateTimeCaster.cast( "2024-01-15 10:30:00", false, context ).getZone() ).isEqualTo( zone );

		instance.executeSource(
		    """
		    setTimezone( "Europe/Paris" );
		    result = getTimezone();
		    """,
		    context );
		assertThat( LocalizationUtil.parseZoneId( null, context ) ).isEqualTo( ZoneId.of( "Europe/Paris" ) );
	}

	@DisplayName( "setLocale() on the request still wins over the runtime default" )
	@Test
	public void testRequestLocaleOverrideWins() {
		context.getRequestContext().setLocale( Locale.GERMANY );
		assertThat( LocalizationUtil.parseLocaleFromContext( context, new ArgumentsScope() ) ).isEqualTo( Locale.GERMANY );
	}

	@DisplayName( "this.timezone in the application still wins over the runtime default" )
	@Test
	public void testApplicationTimezoneOverrideWins() {
		// @formatter:off
		instance.executeSource(
		    """
		    bx:application
		        name="BL2758TimezoneApp"
		        timezone="Asia/Kolkata";
		    """, context );
		// @formatter:on

		ZoneId zone = ZoneId.of( "Asia/Kolkata" );
		assertThat( LocalizationUtil.parseZoneId( null, context ) ).isEqualTo( zone );
		assertThat( DateTimeCaster.cast( "2024-01-15 10:30:00", false, context ).getZone() ).isEqualTo( zone );
	}
}
