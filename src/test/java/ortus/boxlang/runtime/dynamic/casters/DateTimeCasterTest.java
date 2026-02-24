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
import java.util.Locale;

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
	@DisplayName( "Test casting full ISO DateTime with millis string to DateTime" )
	public void testCastISODateTimeMillisString() {
		String		dateString	= "2024-05-13T18:40:59.898284";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.setFormat( "yyyy-MM-dd" ).toString() ).isEqualTo( "2024-05-13" );
		assertThat( result.setFormat( "SSSSSS" ).toString() ).isEqualTo( "898284" );

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
	@DisplayName( "Test time only value to AM/PM format" )
	public void testTimeOnlyString() {
		String		dateString	= "11:00";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.format( "hh:mm a" ) ).isEqualTo( "11:00 AM" );
	}

	@Test
	@DisplayName( "Test casting ODBC Date string to DateTime" )
	public void testCastODBCDateString() {
		String		dateString	= "20240402";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
	}

	@Test
	@DisplayName( "Test casting java.util.Date default toString format to DateTime" )
	public void testDateObjToString() {
		String		dateString	= "Tue Nov 22 11:01:51 CET 2022";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.convertToZone( ZoneId.of( "CET" ) ).format( "EEE MMM dd HH:mm:ss zzz yyyy" ) ).isEqualTo( "Tue Nov 22 11:01:51 CET 2022" );
	}

	@Test
	@DisplayName( "Test casting various slash-delimited formats with time" )
	public void testVariousSlashFormats() {
		String		dateString	= "03/28/2025 04:32 PM";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.format( "MM/dd/yyyy hh:mm a" ) ).isEqualTo( "03/28/2025 04:32 PM" );

		dateString	= "03/28/2025 04:32:26 PM";
		result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.format( "MM/dd/yyyy hh:mm:ss a" ) ).isEqualTo( "03/28/2025 04:32:26 PM" );

		dateString	= "03/28/2025 16:32:26";
		result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.format( "MM/dd/yyyy hh:mm:ss a" ) ).isEqualTo( "03/28/2025 04:32:26 PM" );

		dateString	= "11/21/2025 1:05";
		result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.format( "MM/dd/yyyy hh:mm:ss a" ) ).isEqualTo( "11/21/2025 01:05:00 AM" );
	}

	@Test
	@DisplayName( "Test casting invalid string representation of date to DateTime" )
	public void testCastInvalidString() {
		String invalidDateString = "invalid_date_string";
		assertThrows( BoxCastException.class, () -> DateTimeCaster.cast( invalidDateString ) );
	}

	@DisplayName( "Test ts strings" )
	@Test
	public void testTSStrings() {
		String		dateString	= "{ts '2024-05-21 15:02:16'}";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.setFormat( "yyyy-MM-dd" ).toString() ).isEqualTo( "2024-05-21" );
		assertThat( result.setFormat( "HH:mm:ss" ).toString() ).isEqualTo( "15:02:16" );
	}

	@DisplayName( "Test two-year date" )
	@Test
	public void testTwoYearDate() {
		String		dateString	= "14-Sep-20";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.setFormat( "yyyy-MM-dd" ).toString() ).isEqualTo( "2020-09-14" );
	}

	@DisplayName( "Test timestamp without millis and timezone in brackets" )
	@Test
	public void testTimeStampNoMillis() {
		String		dateString	= "2025-09-08T00:00:00Z[Etc/UTC]";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.setFormat( "yyyy-MM-dd" ).toString() ).isEqualTo( "2025-09-08" );
	}

	@DisplayName( "Test African date pattern" )
	@Test
	public void testAfricanMask() {
		String		dateString	= "2018/9/6";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.setFormat( "yyyy-MM-dd" ).toString() ).isEqualTo( "2018-09-06" );
	}

	@DisplayName( "Test short year pattern" )
	@Test
	public void testShortYearPattern() {
		String		dateString	= "9-6-18";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.setFormat( "yyyy-MM-dd" ).toString() ).isEqualTo( "2018-09-06" );
	}

	@DisplayName( "Test short month pattern" )
	@Test
	public void testShortMonthPattern() {
		String		dateString	= "9-30-2010";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.setFormat( "yyyy-MM-dd" ).toString() ).isEqualTo( "2010-09-30" );
	}

	@Test
	@DisplayName( "Test medium format date and time with tz" )
	public void testMedFormatTimezone() {
		// Med string example Aug 26, 2024 22:05:00 UTC
		String		dateString	= "Nov 22, 2022 11:01:51 CET";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.convertToZone( ZoneId.of( "CET" ) ).format( "EEE MMM dd HH:mm:ss zzz yyyy" ) ).isEqualTo( "Tue Nov 22 11:01:51 CET 2022" );
	}

	@Test
	@DisplayName( "Test medium format datetime with no comma separator between year and time and narrow no-break space unicode char" )
	public void testMedFormatWithMeridianAndNoSeparator() {
		// Do not change the no-break space character here; it's intentional
		String		dateString	= "Nov 20, 2025 10:40:09 AM";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.format( "yyyy-MM-dd HH:mm:ss" ) ).isEqualTo( "2025-11-20 10:40:09" );
	}

	@Test
	@DisplayName( "Test medium and long formats" )
	public void testMediumLongFormats() {
		// Weird string example
		String		dateString	= "Nov/21/2025 00:01:00";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.format( "yyyy-MM-dd HH:mm:ss" ) ).isEqualTo( "2025-11-21 00:01:00" );

		// Weird string example 2
		dateString	= "Dec/13/2025 08:00";
		result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.format( "yyyy-MM-dd HH:mm:ss" ) ).isEqualTo( "2025-12-13 08:00:00" );

		dateString	= "Jun-30-2010 04:33";
		result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.format( "yyyy-MM-dd HH:mm" ) ).isEqualTo( "2010-06-30 04:33" );

		dateString	= "Jun-3-2010 04:33";
		result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.format( "yyyy-MM-dd HH:mm" ) ).isEqualTo( "2010-06-03 04:33" );

		dateString	= "Jun-03-2010 04:33";
		result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.format( "yyyy-MM-dd HH:mm" ) ).isEqualTo( "2010-06-03 04:33" );

		dateString	= "January, 05 2026 17:39:13 -0600";
		result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.format( "yyyy-MM-dd HH:mm:ss" ) ).isEqualTo( "2026-01-05 17:39:13" );

		dateString	= "May, 03 2024 00:51:07 -0500";
		result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.format( "yyyy-MM-dd HH:mm:ss" ) ).isEqualTo( "2024-05-03 00:51:07" );

		dateString	= "Dec 26, 2023 7:29:06 PM";
		result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.format( "yyyy-MM-dd hh:mm:ss a" ) ).isEqualTo( "2023-12-26 07:29:06 PM" );
	}

	@Test
	@DisplayName( "Test ms format with meridian" )
	public void testMSEpochWithMeridian() {
		// Med string example Aug 26, 2024 22:05:00 UTC
		String		dateString	= "1899-12-31 06:10 PM";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.format( "yyyy-MM-dd hh:mm a" ) ).isEqualTo( "1899-12-31 06:10 PM" );
	}

	@Test
	@DisplayName( "Test Med with meridian and seconds" )
	public void testMedWithMeridianSeconds() {
		// Med string example Aug 26, 2024 22:05:00 UTC
		String		dateString	= "Jul 17, 2017 9:29:40 PM";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.format( "yyyy-MM-dd hh:mm a" ) ).isEqualTo( "2017-07-17 09:29 PM" );
	}

	@Test
	@DisplayName( "Test casting ODBC Date format {d yyyy-mm-dd} to DateTime" )
	public void testCastODBCDateFormat() {
		String		dateString	= "{d 2024-04-02}";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.format( "yyyy-MM-dd" ) ).isEqualTo( "2024-04-02" );
	}

	@Test
	@DisplayName( "Test casting ODBC Time format {t HH:mm:ss} to DateTime" )
	public void testCastODBCTimeFormat() {
		String		timeString	= "{t 14:30:45}";
		DateTime	result		= DateTimeCaster.cast( timeString );
		assertThat( result ).isNotNull();
		assertThat( result.format( "HH:mm:ss" ) ).isEqualTo( "14:30:45" );
	}

	@Test
	@DisplayName( "Test casting ODBC Timestamp format {ts yyyy-mm-dd HH:mm:ss} to DateTime" )
	public void testCastODBCTimestampFormat() {
		String		timestampString	= "{ts 2024-04-02 14:30:45}";
		DateTime	result			= DateTimeCaster.cast( timestampString );
		assertThat( result ).isNotNull();
		assertThat( result.format( "yyyy-MM-dd HH:mm:ss" ) ).isEqualTo( "2024-04-02 14:30:45" );
	}

	@Test
	@DisplayName( "Test casting MM-DD-YYYY HH:mm:ss format to DateTime" )
	public void testCastMMDDYYYYWithTime() {
		String		dateString	= "01-31-2026 23:59:59";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.format( "MM-dd-yyyy HH:mm:ss" ) ).isEqualTo( "01-31-2026 23:59:59" );
	}

	@Test
	@DisplayName( "Test date parsing with German (de_DE) JVM locale to replicate locale-specific parsing issues" )
	public void testDateParsingWithGermanLocale() {
		// Store the original default locale to restore later
		Locale originalLocale = Locale.getDefault();

		try {
			// Set JVM locale to German (Germany) which can cause date parsing issues
			Locale.setDefault( Locale.GERMANY ); // This is de_DE

			// Test various date formats that should still parse correctly with the fix
			// These formats rely on the common pattern parsers being forced to use Locale.US

			// Test medium format with meridian and seconds that was failing
			String		dateString1	= "Jul 17, 2017 9:29:40 PM";
			DateTime	result1		= DateTimeCaster.cast( dateString1 );
			assertThat( result1 ).isNotNull();
			assertThat( result1.format( "yyyy-MM-dd hh:mm a" ) ).isEqualTo( "2017-07-17 09:29 PM" );

			// Test additional common English date formats
			String		dateString2	= "Apr 02, 2024 12:00:00 AM";
			DateTime	result2		= DateTimeCaster.cast( dateString2 );
			assertThat( result2 ).isNotNull();

			// Test another format that could be affected by locale
			String		dateString3	= "Dec 25, 2023 11:59:59 PM";
			DateTime	result3		= DateTimeCaster.cast( dateString3 );
			assertThat( result3 ).isNotNull();
			assertThat( result3.format( "yyyy-MM-dd" ) ).isEqualTo( "2023-12-25" );

		} finally {
			// Always restore the original locale
			Locale.setDefault( originalLocale );
		}
	}

	@Test
	@DisplayName( "Test MMM-d-yyyy h:mm a pattern with dash delimiter" )
	public void testMMMdyyyyHmmaDash() {
		String		dateString	= "Nov-05-2025 8:43 AM";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.format( "MMM-dd-yyyy h:mm a" ) ).isEqualTo( "Nov-05-2025 8:43 AM" );
	}

	@Test
	@DisplayName( "Test MMM-d-yyyy h:mm a pattern with slash delimiter" )
	public void testMMMdyyyyHmmaSlash() {
		String		dateString	= "Nov/05/2025 8:43 AM";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.format( "MMM-dd-yyyy h:mm a" ) ).isEqualTo( "Nov-05-2025 8:43 AM" );
	}

	@Test
	@DisplayName( "Test MMM-d-yyyy HH:mm:ss pattern with dash delimiter" )
	public void testMMMdyyyyHHmmssWithDash() {
		String		dateString	= "Nov-05-2025 14:43:00";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.format( "MMM-dd-yyyy HH:mm:ss" ) ).isEqualTo( "Nov-05-2025 14:43:00" );
	}

	@Test
	@DisplayName( "Test MMM-d-yyyy HH:mm:ss pattern with slash delimiter" )
	public void testMMMdyyyyHHmmssWithSlash() {
		String		dateString	= "Nov/05/2025 14:43:00";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.format( "MMM-dd-yyyy HH:mm:ss" ) ).isEqualTo( "Nov-05-2025 14:43:00" );
	}

	@Test
	@DisplayName( "Test MMM-d-yyyy HH:mm pattern (no seconds) with dash delimiter" )
	public void testMMMdyyyyHHmmWithDash() {
		String		dateString	= "Nov-05-2025 14:43";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.format( "MMM-dd-yyyy HH:mm" ) ).isEqualTo( "Nov-05-2025 14:43" );
	}

	@Test
	@DisplayName( "Test MMM-d-yyyy HH:mm pattern (no seconds) with slash delimiter" )
	public void testMMMdyyyyHHmmWithSlash() {
		String		dateString	= "Nov/05/2025 14:43";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.format( "MMM-dd-yyyy HH:mm" ) ).isEqualTo( "Nov-05-2025 14:43" );
	}

	@Test
	@DisplayName( "Test d-MMM-yyyy HH:mm:ss pattern with dash delimiter" )
	public void testdMMMyyyyHHmmssWithDash() {
		String		dateString	= "05-Nov-2025 14:43:00";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.format( "dd-MMM-yyyy HH:mm:ss" ) ).isEqualTo( "05-Nov-2025 14:43:00" );
	}

	@Test
	@DisplayName( "Test d-MMM-yyyy HH:mm:ss pattern with slash delimiter" )
	public void testdMMMyyyyHHmmssWithSlash() {
		String		dateString	= "05/Nov/2025 14:43:00";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.format( "dd-MMM-yyyy HH:mm:ss" ) ).isEqualTo( "05-Nov-2025 14:43:00" );
	}

	@Test
	@DisplayName( "Test d-MMM-yyyy HH:mm pattern (no seconds) with dash delimiter" )
	public void testdMMMyyyyHHmmWithDash() {
		String		dateString	= "05-Nov-2025 14:43";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.format( "dd-MMM-yyyy HH:mm" ) ).isEqualTo( "05-Nov-2025 14:43" );
	}

	@Test
	@DisplayName( "Test d-MMM-yyyy HH:mm pattern (no seconds) with slash delimiter" )
	public void testdMMMyyyyHHmmWithSlash() {
		String		dateString	= "05/Nov/2025 14:43";
		DateTime	result		= DateTimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
		assertThat( result.format( "dd-MMM-yyyy HH:mm" ) ).isEqualTo( "05-Nov-2025 14:43" );
	}
}
