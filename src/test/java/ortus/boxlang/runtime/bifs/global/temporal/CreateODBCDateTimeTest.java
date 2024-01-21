
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
import ortus.boxlang.runtime.types.DateTime;

public class CreateODBCDateTimeTest {

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

	@DisplayName( "It tests the BIF CreateODBCDateTime" )
	@Test
	public void testCreateODBCDateTime() {
		DateTime	dateRef				= new DateTime( "2024-01-14T00:00:01.0001Z" );
		String		refFormattedDate	= dateRef.format( DateTime.ODBC_DATE_TIME_FORMAT_MASK );
		variables.put( Key.of( "date" ), dateRef );
		instance.executeSource(
		    """
		    result = CreateODBCDateTime( date ).toString();
		    """,
		    context );
		String result = variables.getAsString( Key.of( "result" ) );
		assertEquals( refFormattedDate, result );
	}

	@DisplayName( "It tests the Member function toODBCDateTime" )
	@Test
	public void testMemberToODBCDateTime() {
		DateTime	dateRef				= new DateTime( "2024-01-14T00:00:01.0001Z" );
		String		refFormattedDate	= dateRef.format( DateTime.ODBC_DATE_TIME_FORMAT_MASK );
		variables.put( Key.of( "date" ), dateRef );
		instance.executeSource(
		    """
		    result = date.toODBCDateTime().toString();
		    """,
		    context );
		String result = variables.getAsString( Key.of( "result" ) );
		assertEquals( refFormattedDate, result );
	}

	@DisplayName( "It tests the BIF CreateODBCDate" )
	@Test
	public void testCreateODBCDate() {
		DateTime	dateRef				= new DateTime( "2024-01-14" );
		String		refFormattedDate	= dateRef.format( DateTime.ODBC_DATE_FORMAT_MASK );
		variables.put( Key.of( "date" ), dateRef );
		instance.executeSource(
		    """
		    result = CreateODBCDate( date ).toString();
		    """,
		    context );
		String result = variables.getAsString( Key.of( "result" ) );
		assertEquals( refFormattedDate, result );
	}

	@DisplayName( "It tests the Member function DateTime.toODBCDate" )
	@Test
	public void testMemberToODBCDate() {
		DateTime	dateRef				= new DateTime( "2024-01-14" );
		String		refFormattedDate	= dateRef.format( DateTime.ODBC_DATE_FORMAT_MASK );
		variables.put( Key.of( "date" ), dateRef );
		instance.executeSource(
		    """
		    result = date.toODBCDate().toString();
		    """,
		    context );
		String result = variables.getAsString( Key.of( "result" ) );
		assertEquals( refFormattedDate, result );
	}

	@DisplayName( "It tests the BIF CreateODBCTime" )
	@Test
	public void testCreateODBCTime() {
		DateTime	dateRef				= new DateTime( "23:59:59" );
		String		refFormattedDate	= dateRef.format( DateTime.ODBC_TIME_FORMAT_MASK );
		variables.put( Key.of( "date" ), dateRef );
		instance.executeSource(
		    """
		    result = CreateODBCTime( date ).toString();
		    """,
		    context );
		String result = variables.getAsString( Key.of( "result" ) );
		assertEquals( refFormattedDate, result );
	}

	@DisplayName( "It tests the Member function DateTime.toODBCTime" )
	@Test
	public void testMemberToODBCTime() {
		DateTime	dateRef				= new DateTime( "23:59:59" );
		String		refFormattedDate	= dateRef.format( DateTime.ODBC_TIME_FORMAT_MASK );
		variables.put( Key.of( "date" ), dateRef );
		instance.executeSource(
		    """
		    result = date.toODBCTime().toString();
		    """,
		    context );
		String result = variables.getAsString( Key.of( "result" ) );
		assertEquals( refFormattedDate, result );
	}

}
