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
package ortus.boxlang.runtime.dynamic.casters;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;

public class DateTimeCasterTest {

	@DisplayName( "It can cast null to not be a date" )
	@Test
	public void testNull() {
		assertThat( DateTimeCaster.attempt( null ).wasSuccessful() ).isFalse();
	}

	@Test
	@DisplayName( "Test casting ZonedDateTime to DateTime" )
	public void testCastZonedDateTime() {
		ZonedDateTime	zonedDateTime	= ZonedDateTime.now();
		DateTime		result			= DateTimeCaster.cast( zonedDateTime );
		assertThat( result ).isNotNull();
		assertThat( result.getWrapped() ).isEqualTo( zonedDateTime );
	}

	@Test
	@DisplayName( "Test casting Calendar to DateTime" )
	public void testCastCalendar() {
		Calendar	calendar	= Calendar.getInstance();
		DateTime	result		= DateTimeCaster.cast( calendar );
		assertThat( result ).isNotNull();
		assertThat( result.getWrapped() ).isEqualTo( calendar.toInstant().atZone( ZoneId.systemDefault() ) );
	}

	@Test
	@DisplayName( "Test casting Date to DateTime" )
	public void testCastDate() {
		Date		date	= new Date();
		DateTime	result	= DateTimeCaster.cast( date );
		assertThat( result ).isNotNull();
		assertThat( result.getWrapped() ).isEqualTo( date.toInstant().atZone( ZoneId.systemDefault() ) );
	}

	@Test
	@DisplayName( "Test casting LocalDateTime to DateTime" )
	public void testCastLocalDateTime() {
		LocalDateTime	localDateTime	= LocalDateTime.now();
		DateTime		result			= DateTimeCaster.cast( localDateTime );
		assertThat( result ).isNotNull();
		assertThat( result.getWrapped() ).isEqualTo( localDateTime.atZone( ZoneId.systemDefault() ) );
	}

	@Test
	@DisplayName( "Test casting LocalDate to DateTime" )
	public void testCastLocalDate() {
		LocalDate	localDate	= LocalDate.now();
		DateTime	result		= DateTimeCaster.cast( localDate );
		assertThat( result ).isNotNull();
		assertThat( result.getWrapped() ).isEqualTo( localDate.atStartOfDay( ZoneId.systemDefault() ) );
	}

	@Test
	@DisplayName( "Test casting valid string representation of date to DateTime" )
	public void testCastValidString() {
		String		dateString	= "2024-04-02T12:00:00Z";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
	}

	@Test
	@DisplayName( "Test casting full DateTime string to DateTime" )
	public void testCastFullDateTimeString() {
		String		dateTimeString	= "Tue, 02 Apr 2024 21:01:00 CEST";
		DateTime	result			= DateTimeCaster.cast( dateTimeString );
		assertThat( result ).isNotNull();
	}

	@Test
	@DisplayName( "Test casting long DateTime string to DateTime" )
	public void testCastLongDateTimeString() {
		String		dateTimeString	= "02 Apr 2024 21:01:00";
		DateTime	result			= DateTimeCaster.cast( dateTimeString );
		assertThat( result ).isNotNull();
	}

	@Test
	@DisplayName( "Test casting medium DateTime string to DateTime" )
	public void testCastMediumDateTimeString() {
		String		dateTimeString	= "02-Apr-2024 21:01:00";
		DateTime	result			= DateTimeCaster.cast( dateTimeString );
		assertThat( result ).isNotNull();
	}

	@Test
	@DisplayName( "Test casting short DateTime string to DateTime" )
	public void testCastShortDateTimeString() {
		String		dateTimeString	= "02/04/2024 21:01:00";
		DateTime	result			= DateTimeCaster.cast( dateTimeString );
		assertThat( result ).isNotNull();
	}

	@Test
	@DisplayName( "Test casting ISO DateTime string to DateTime" )
	public void testCastISODateTimeString() {
		String		dateTimeString	= "2024-04-02T21:01:00Z";
		DateTime	result			= DateTimeCaster.cast( dateTimeString );
		assertThat( result ).isNotNull();
	}

	@Test
	@DisplayName( "Test casting ODBC DateTime string to DateTime" )
	public void testCastODBCDateTimeString() {
		String		dateTimeString	= "20240402210100";
		DateTime	result			= DateTimeCaster.cast( dateTimeString );
		assertThat( result ).isNotNull();
	}

	@Test
	@DisplayName( "Test casting full Date string to DateTime" )
	public void testCastFullDateString() {
		String		dateString	= "Tue, 02 Apr 2024";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
	}

	@Test
	@DisplayName( "Test casting long Date string to DateTime" )
	public void testCastLongDateString() {
		String		dateString	= "02 Apr 2024";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
	}

	@Test
	@DisplayName( "Test casting medium Date string to DateTime" )
	public void testCastMediumDateString() {
		String		dateString	= "02-Apr-2024";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
	}

	@Test
	@DisplayName( "Test casting short Date string to DateTime" )
	public void testCastShortDateString() {
		String		dateString	= "02/04/2024";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
	}

	@Test
	@DisplayName( "Test casting Month First long Date string to DateTime" )
	public void testCastMonthFirstLongDateString() {
		String		dateString	= "Apr 02 2024";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
	}

	@Test
	@DisplayName( "Test casting Month First medium Date string to DateTime" )
	public void testCastMonthFirstMediumDateString() {
		String		dateString	= "Apr-02-2024";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
	}

	@Test
	@DisplayName( "Test casting Month First short Date string to DateTime" )
	public void testCastMonthFirstShortDateString() {
		String		dateString	= "04 02 2024";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
	}

	@Test
	@DisplayName( "Test casting ISO Date string to DateTime" )
	public void testCastISODateString() {
		String		dateString	= "2024-04-02";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
	}

	@Test
	@DisplayName( "Test casting ODBC Date string to DateTime" )
	public void testCastODBCDateString() {
		String		dateString	= "20240402";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
	}

	@Test
	@DisplayName( "Test casting invalid string representation of date to DateTime" )
	public void testCastInvalidString() {
		String invalidDateString = "invalid_date_string";
		assertThrows( BoxCastException.class, () -> DateTimeCaster.cast( invalidDateString ) );
	}
}
