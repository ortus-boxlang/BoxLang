
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

import java.time.ZoneId;
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
import ortus.boxlang.runtime.util.LocalizationUtil;

public class LSDateTimeFormatTest {

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

	@DisplayName( "It tests the BIF LSDateTimeFormat using a localized, Spanish long-form format" )
	@Test
	public void testLSDateTimeFormatSpain() {
		DateTime dateRef = new DateTime( ZoneId.of( "UTC" ) );
		variables.put( Key.date, dateRef );
		instance.executeSource(
		    """
		    result = LSDateTimeFormat( date, "long", "es-ES", "UTC" );
		    """,
		    context );
		String				result		= variables.getAsString( Key.of( "result" ) );
		DateTimeFormatter	formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "longDateTime" );
		assertEquals( result, dateRef.format( formatter.withLocale( LocalizationUtil.parseLocale( "es-ES" ) ) ) );
	}

	@DisplayName( "It tests the Member function DateTime.LSDateTimeFormat" )
	@Test
	public void testLSDateTimeFormatMember() {
		DateTime dateRef = new DateTime( ZoneId.of( "UTC" ) );
		variables.put( Key.date, dateRef );
		instance.executeSource(
		    """
		    result = date.LSDateTimeFormat( "long", "es-ES", "UTC" );
		    """,
		    context );
		String				result		= variables.getAsString( Key.of( "result" ) );
		DateTimeFormatter	formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "longDateTime" );
		assertEquals( result, dateRef.format( formatter.withLocale( LocalizationUtil.parseLocale( "es-ES" ) ) ) );
	}

	@DisplayName( "It tests the BIF LSDateTimeFormat using a traditional chinese long-form format" )
	@Test
	public void testLSDateTimeFormatChina() {
		DateTime dateRef = new DateTime( ZoneId.of( "UTC" ) );
		variables.put( Key.date, dateRef );
		instance.executeSource(
		    """
		    result = LSDateTimeFormat( date, "long", "zh-CN", "UTC" );
		    """,
		    context );
		String				result		= variables.getAsString( Key.of( "result" ) );
		DateTimeFormatter	formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "longDateTime" );
		assertEquals( result, dateRef.format( formatter.withLocale( LocalizationUtil.parseLocale( "zh-CN" ) ) ) );
	}

	@DisplayName( "It tests the BIF LSDateFormat using a localized, Spanish long-form format" )
	@Test
	public void testLSDateFormatSpain() {
		DateTime dateRef = new DateTime( ZoneId.of( "UTC" ) );
		variables.put( Key.date, dateRef );
		instance.executeSource(
		    """
		    result = LSDateFormat( date, "long", "es-ES", "UTC" );
		    """,
		    context );
		String				result		= variables.getAsString( Key.of( "result" ) );
		DateTimeFormatter	formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "longDate" );
		assertEquals( result, dateRef.format( formatter.withLocale( LocalizationUtil.parseLocale( "es-ES" ) ) ) );
	}

	@DisplayName( "It tests the Member function DateTime.LSDateFormat" )
	@Test
	public void testLSDateFormatMember() {
		DateTime dateRef = new DateTime( ZoneId.of( "UTC" ) );
		variables.put( Key.date, dateRef );
		instance.executeSource(
		    """
		    result = date.LSDateFormat( "long", "es-ES", "UTC" );
		    """,
		    context );
		String				result		= variables.getAsString( Key.of( "result" ) );
		DateTimeFormatter	formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "longDate" );
		assertEquals( result, dateRef.format( formatter.withLocale( LocalizationUtil.parseLocale( "es-ES" ) ) ) );
	}

	@DisplayName( "It tests the BIF LSDateFormat using a traditional chinese long-form format" )
	@Test
	public void testLSDateFormatChina() {
		DateTime dateRef = new DateTime( ZoneId.of( "UTC" ) );
		variables.put( Key.date, dateRef );
		instance.executeSource(
		    """
		    result = LSDateFormat( date, "long", "zh-CN", "UTC" );
		    """,
		    context );
		String				result		= variables.getAsString( Key.of( "result" ) );
		DateTimeFormatter	formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "longDate" );
		assertEquals( result, dateRef.format( formatter.withLocale( LocalizationUtil.parseLocale( "zh-CN" ) ) ) );
	}

	@DisplayName( "It tests the BIF LSTimeFormat using a localized, Spanish long-form format" )
	@Test
	public void testLSTimeFormatSpain() {
		DateTime timeRef = new DateTime( ZoneId.of( "UTC" ) );
		variables.put( Key.date, timeRef );
		instance.executeSource(
		    """
		    result = LSTimeFormat( date, "long", "es-ES", "UTC" );
		    """,
		    context );
		String				result		= variables.getAsString( Key.of( "result" ) );

		DateTimeFormatter	formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "longTime" );
		assertEquals( result, timeRef.format( formatter.withLocale( LocalizationUtil.parseLocale( "es-ES" ) ) ) );
	}

	@DisplayName( "It tests the Member function DateTime.LSTimeFormat" )
	@Test
	public void testLSTimeFormatMember() {
		DateTime timeRef = new DateTime( ZoneId.of( "UTC" ) );
		variables.put( Key.date, timeRef );
		instance.executeSource(
		    """
		    result = date.LSTimeFormat( "long", "es-ES", "UTC" );
		    """,
		    context );
		String				result		= variables.getAsString( Key.of( "result" ) );

		DateTimeFormatter	formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "longTime" );
		assertEquals( result, timeRef.format( formatter.withLocale( LocalizationUtil.parseLocale( "es-ES" ) ) ) );
	}

	@DisplayName( "It tests the BIF LSTimeFormat using a traditional chinese long-form format" )
	@Test
	public void testLSTimeFormatChina() {
		DateTime timeRef = new DateTime( ZoneId.of( "UTC" ) );
		variables.put( Key.date, timeRef );
		instance.executeSource(
		    """
		    result = LSTimeFormat( date, "long", "zh-CN", "UTC" );
		    """,
		    context );
		String				result		= variables.getAsString( Key.of( "result" ) );
		DateTimeFormatter	formatter	= ( DateTimeFormatter ) DateTime.COMMON_FORMATTERS.get( "longTime" );
		System.out.println( result );
		assertEquals( result, timeRef.format( formatter.withLocale( LocalizationUtil.parseLocale( "zh-CN" ) ) ) );
	}

}
