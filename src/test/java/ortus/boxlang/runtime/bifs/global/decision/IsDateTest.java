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

package ortus.boxlang.runtime.bifs.global.decision;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

@Disabled( "Unimplemented" )
public class IsDateTest {

	static BoxRuntime	instance;
	static IBoxContext	context;
	static IScope		variables;

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

	@DisplayName( "It detects date parseable values" )
	@Test
	public void testTrueConditions() {
		instance.executeSource(
		    """
		    aNowCall               = isDate( now() );
		    aCreateTimeCall        = isDate( createTime( 3, 2, 1 ) );
		    aCreateDateCall        = isDate( createDate( 2023, 12, 21 ) );
		    aDateStringWithDashes  = isDate( "2023-12-21" );
		    aDateStringWithPeriods = isDate( "2024.01.01" );
		    earlyDate              = isDate( "1100-12-21" );
		    leapDay                = isDate( "2024-02-29" );

		    // FYI: ACF 23 returns false, Lucee returns true.
		    anISO8601String        = isDate( '2023-12-21T14:22:32Z' );
		      """,
		    context );
		assertThat( ( Boolean ) variables.dereference( Key.of( "aNowCall" ), false ) ).isTrue();
		assertThat( ( Boolean ) variables.dereference( Key.of( "aCreateTimeCall" ), false ) ).isTrue();
		assertThat( ( Boolean ) variables.dereference( Key.of( "aCreateDateCall" ), false ) ).isTrue();
		assertThat( ( Boolean ) variables.dereference( Key.of( "aDateStringWithDashes" ), false ) ).isTrue();
		assertThat( ( Boolean ) variables.dereference( Key.of( "aDateStringWithPeriods" ), false ) ).isTrue();
		assertThat( ( Boolean ) variables.dereference( Key.of( "earlyDate" ), false ) ).isTrue();
		assertThat( ( Boolean ) variables.dereference( Key.of( "leapDay" ), false ) ).isTrue();
		assertThat( ( Boolean ) variables.dereference( Key.of( "anISO8601String" ), false ) ).isTrue();
	}

	@DisplayName( "It returns false for non-date values" )
	@Test
	public void testFalseConditions() {
		instance.executeSource(
		    """
		    aString = isDate( "abc" );
		    aNumericString = isDate( "2023" );
		    anInteger = isDate( 2024 );
		    aFloat = isDate( 2024.01 );

		    // FYI: ACF 23 returns false, Lucee returns true.
		    aTimespan = isDate( createTimespan( 0, 24, 0, 0 ) );

		    invalidLeapDay = isDate( "2023-02-29" );
		    invalidDayNumber = isDate( "2023-12-2100" );
		    gibberishAfterValidDate = isDate( "2023-12-21xyz" );
		      """,
		    context );
		assertThat( ( Boolean ) variables.dereference( Key.of( "aString" ), false ) ).isFalse();
		assertThat( ( Boolean ) variables.dereference( Key.of( "aNumericString" ), false ) ).isFalse();
		assertThat( ( Boolean ) variables.dereference( Key.of( "anInteger" ), false ) ).isFalse();
		assertThat( ( Boolean ) variables.dereference( Key.of( "aFloat" ), false ) ).isFalse();
		assertThat( ( Boolean ) variables.dereference( Key.of( "aTimespan" ), false ) ).isFalse();
		assertThat( ( Boolean ) variables.dereference( Key.of( "invalidLeapDay" ), false ) ).isFalse();
		assertThat( ( Boolean ) variables.dereference( Key.of( "invalidDayNumber" ), false ) ).isFalse();
		assertThat( ( Boolean ) variables.dereference( Key.of( "gibberishAfterValidDate" ), false ) ).isFalse();
	}

}
