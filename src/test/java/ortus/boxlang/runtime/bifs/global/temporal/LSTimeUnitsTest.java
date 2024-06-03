
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZonedDateTime;
import java.time.temporal.WeekFields;
import java.util.Locale;

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
import ortus.boxlang.runtime.util.LocalizationUtil;

public class LSTimeUnitsTest {

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

	@DisplayName( "It tests the BIF LSWeek" )
	@Test
	public void testLSWeek() {
		Locale	locale			= LocalizationUtil.parseLocale( "es-SV" );
		Integer	refWeekOfYear	= ZonedDateTime.now().get( WeekFields.of( locale ).weekOfWeekBasedYear() );
		instance.executeSource(
		    """
		    now = now();
		       result = LSWeek( now, "es-SV" );
		       """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refWeekOfYear );

	}

	@DisplayName( "It tests the member function DateTime.LSWeek" )
	@Test
	public void testMemberLSWeek() {
		Locale	locale			= LocalizationUtil.parseLocale( "es-SV" );
		Integer	refWeekOfYear	= ZonedDateTime.now().get( WeekFields.of( locale ).weekOfWeekBasedYear() );
		instance.executeSource(
		    """
		    now = now();
		       result = now.LSWeek( "es-SV" );
		       """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refWeekOfYear );

	}

	@DisplayName( "It tests the BIF LSDayOfWeek" )
	@Test
	public void testLSDayOfWeek() {
		Locale	locale			= LocalizationUtil.parseLocale( "es-SV" );
		Integer	refWeekOfYear	= ZonedDateTime.now().get( WeekFields.of( locale ).dayOfWeek() );
		instance.executeSource(
		    """
		    now = now();
		       result = LSDayOfWeek( now, "es-SV" );
		       """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refWeekOfYear );

	}

	@DisplayName( "It tests the Member function DateTime.LSDayOfWeek" )
	@Test
	public void testMemberLSDayOfWeek() {
		Locale	locale			= LocalizationUtil.parseLocale( "es-SV" );
		Integer	refWeekOfYear	= ZonedDateTime.now().get( WeekFields.of( locale ).dayOfWeek() );
		instance.executeSource(
		    """
		    now = now();
		       result = now.LSDayOfWeek( "es-SV" );
		       """,
		    context );
		Integer result = ( Integer ) variables.get( Key.of( "result" ) );
		assertEquals( result, refWeekOfYear );

	}

}
