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
package ortus.boxlang.runtime.operators;

import static com.google.common.truth.Truth.assertThat;

import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

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

/**
 * BL-2758: date compares keep the calling context all the way down to the DateTimeCaster,
 * so they use the request's settings (and its cached config) instead of the runtime context.
 */
public class CompareContextTest {

	static BoxRuntime	instance;
	static final ZoneId	TOKYO	= ZoneId.of( "Asia/Tokyo" );
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
		context.getRequestContext().setTimezone( TOKYO );
	}

	@DisplayName( "Compiled comparison operators parse date strings in the request timezone" )
	@Test
	public void testOperatorsUseRequestTimezone() {
		// @formatter:off
		instance.executeSource(
		    """
		    tokyo  = createDateTime( 2024, 1, 15, 10, 30, 0, 0, "Asia/Tokyo" );
		    result = [
		        tokyo == "2024-01-15 10:30:00",
		        tokyo != "2024-01-15 10:30:00",
		        tokyo <  "2024-01-15 10:31:00",
		        tokyo <= "2024-01-15 10:30:00",
		        tokyo >  "2024-01-15 10:29:00",
		        tokyo >= "2024-01-15 10:30:00"
		    ];
		    """,
		    context );
		// @formatter:on
		assertThat( variables.getAsArray( result ) ).containsExactly( true, false, true, true, true, true ).inOrder();
	}

	@DisplayName( "structSort on a date path compares in the request timezone" )
	@Test
	public void testStructSortUsesRequestTimezone() {
		// "b" is 10:30 Tokyo time, "a" is 10:00 UTC (19:00 Tokyo): only the request timezone puts b first
		// @formatter:off
		instance.executeSource(
		    """
		    pool = {
		        a : { lastAccessed : createDateTime( 2024, 1, 15, 10, 0, 0, 0, "UTC" ) },
		        b : { lastAccessed : "2024-01-15 10:30:00" }
		    };
		    result = structSort( pool, "numeric", "asc", "lastAccessed" );
		    """,
		    context );
		// @formatter:on
		assertThat( variables.getAsArray( result ) ).containsExactly( "b", "a" ).inOrder();
	}

	@DisplayName( "DateTimeCaster.attempt( Object ) uses the current request when there is one" )
	@Test
	public void testAttemptUsesCurrentRequest() {
		RequestBoxContext.setCurrent( context );
		try {
			assertThat( DateTimeCaster.attempt( "2024-01-15 10:30:00" ).get().getZone() ).isEqualTo( TOKYO );
		} finally {
			RequestBoxContext.removeCurrent();
		}
	}

	@DisplayName( "The deprecated context-less Compare.invoke uses the current request when there is one" )
	@Test
	@SuppressWarnings( "deprecation" )
	public void testDeprecatedInvokeUsesCurrentRequest() {
		DateTime tokyo = new DateTime( java.time.ZonedDateTime.of( 2024, 1, 15, 10, 30, 0, 0, TOKYO ) );
		RequestBoxContext.setCurrent( context );
		try {
			assertThat( Compare.invoke( tokyo, "2024-01-15 10:30:00" ) ).isEqualTo( 0 );
		} finally {
			RequestBoxContext.removeCurrent();
		}
	}

	@DisplayName( "Compare.invoke with an explicit context uses that context" )
	@Test
	public void testExplicitContext() {
		DateTime tokyo = new DateTime( java.time.ZonedDateTime.of( 2024, 1, 15, 10, 30, 0, 0, TOKYO ) );
		// Not the current request: the context is only passed in
		assertThat( Compare.invoke( context, tokyo, "2024-01-15 10:30:00" ) ).isEqualTo( 0 );
		assertThat( Compare.attempt( context, tokyo, "2024-01-15 10:31:00", false ) ).isEqualTo( -1 );
		assertThat( LessThan.invoke( context, tokyo, "2024-01-15 10:31:00" ) ).isTrue();
		assertThat( EqualsEquals.invoke( context, tokyo, "2024-01-15 10:30:00" ) ).isTrue();
	}

	@DisplayName( "With no request, context-less casts and compares fall back to the runtime default" )
	@Test
	public void testNoRequestUsesRuntimeDefault() throws InterruptedException {
		AtomicReference<ZoneId>	zone	= new AtomicReference<>();
		// A fresh thread has no current request
		Thread					thread	= new Thread( () -> zone.set( DateTimeCaster.attempt( "2024-01-15 10:30:00" ).get().getZone() ) );
		thread.start();
		thread.join();
		assertThat( zone.get() ).isEqualTo( instance.getConfiguration().timezone );
	}
}
