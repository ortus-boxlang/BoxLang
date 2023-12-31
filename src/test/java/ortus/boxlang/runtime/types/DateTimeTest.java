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
package ortus.boxlang.runtime.types;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.dynamic.casters.LongCaster;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.ZoneId;

public class DateTimeTest {

	@DisplayName( "Test Constructors" )
	@Test
	void testConstructors() {
		DateTime			defaultDateTime		= new DateTime();
		ZonedDateTime		referenceNow		= ZonedDateTime.of( LocalDateTime.now(), ZoneId.systemDefault() );
		DateTimeFormatter	referenceFormatter	= DateTimeFormatter.ISO_LOCAL_DATE;
		assertThat( defaultDateTime.withFormat( "yyyy-MM-dd" ).toString() ).isEqualTo( referenceFormatter.format( referenceNow ) );
		DateTime referenceZoned = new DateTime( referenceNow );
		assertThat( referenceZoned.withFormat( "yyyy-MM-dd" ).toString() ).isEqualTo( referenceFormatter.format( referenceNow ) );
		DateTime parsedDateTime = new DateTime( "2023-12-31 23:59:59", "yyyy-MM-dd HH:mm:ss" );
		assertThat( parsedDateTime.withFormat( "yyyy-MM-dd HH:mm:ss" ).toString() ).isEqualTo( "2023-12-31 23:59:59" );
	}

	@DisplayName( "Tests the default format method" )
	@Test
	void testDefaultFormat() {
		DateTime	defaultDateTime	= new DateTime();
		String		dateValue		= defaultDateTime.toString();
		assertThat( dateValue ).isEqualTo( defaultDateTime.withFormat( "'{ts '''yyyy-MM-dd HH:mm:ss'''}'" ).toString() );
	}

	@DisplayName( "Tests the setTimezone method" )
	@Test
	void testTimezoneMods() {
		DateTime	defaultDateTime	= new DateTime();
		String		dateValue		= defaultDateTime.withFormat( "yyyy-MM-dd HH:mm:ss" ).toString();
		String		zonedValue		= defaultDateTime.toISOString();
		defaultDateTime.setTimezone( "UTC" );
		assertThat( dateValue ).isEqualTo( defaultDateTime.withFormat( "yyyy-MM-dd HH:mm:ss" ).toString() );
		assertThat( zonedValue ).isNotEqualTo( defaultDateTime.toISOString() );
	}

	@DisplayName( "Tests DateTime modifications" )
	@Test
	void testModifications() {
		DateTime	initialDateTime	= new DateTime( "2023-12-31 00:00:00", "yyyy-MM-dd HH:mm:ss" );
		Long		initialDay		= LongCaster.cast( initialDateTime.format( "d" ) );
		assertThat( initialDay ).isEqualTo( 31l );
		// Days
		assertThat( LongCaster.cast( initialDateTime.modify( "d", 1l ).format( "d" ) ) ).isEqualTo( 1l );
		assertThat( Long.signum( -1l ) ).isEqualTo( -1 );
		initialDateTime.modify( "d", -1l );
		assertThat( initialDay ).isEqualTo( LongCaster.cast( initialDateTime.format( "d" ) ) );
		// Years
		Long initialYear = LongCaster.cast( initialDateTime.format( "yyyy" ) );
		assertThat( LongCaster.cast( initialDateTime.modify( "yyyy", 1l ).format( "yyyy" ) ) ).isGreaterThan( initialYear );
		initialDateTime.modify( "yyyy", -1l );
		assertThat( initialYear ).isEqualTo( LongCaster.cast( initialDateTime.format( "yyyy" ) ) );
		// Months
		Long initialMonth = LongCaster.cast( initialDateTime.format( "M" ) );
		assertThat( LongCaster.cast( initialDateTime.modify( "m", 1l ).format( "M" ) ) ).isEqualTo( 1l );
		initialDateTime.modify( "m", -1l );
		assertThat( LongCaster.cast( initialDateTime.format( "M" ) ) ).isEqualTo( initialMonth );
		// Weeks
		Long initialWeek = LongCaster.cast( initialDateTime.format( "w" ) );
		// Our date is the first week in the new month
		assertThat( initialWeek ).isEqualTo( 1l );
		assertThat( LongCaster.cast( initialDateTime.modify( "ww", 1l ).format( "w" ) ) ).isEqualTo( 2l );
		initialDateTime.modify( "ww", -1l );
		assertThat( LongCaster.cast( initialDateTime.format( "w" ) ) ).isEqualTo( initialWeek );
		// Hours
		Long initialHour = LongCaster.cast( initialDateTime.format( "H" ) );
		assertThat( initialHour ).isEqualTo( 0l );
		assertThat( LongCaster.cast( initialDateTime.modify( "h", 1l ).format( "H" ) ) ).isEqualTo( 1l );
		initialDateTime.modify( "h", -1l );
		assertThat( LongCaster.cast( initialDateTime.format( "H" ) ) ).isEqualTo( initialHour );
		// Minutes
		Long initialMinute = LongCaster.cast( initialDateTime.format( "m" ) );
		assertThat( initialMinute ).isEqualTo( 0l );
		assertThat( LongCaster.cast( initialDateTime.modify( "n", 1l ).format( "m" ) ) ).isEqualTo( 1l );
		initialDateTime.modify( "n", -1l );
		assertThat( LongCaster.cast( initialDateTime.format( "m" ) ) ).isEqualTo( initialMinute );
		// Seconds
		Long initialSeconds = LongCaster.cast( initialDateTime.format( "s" ) );
		assertThat( initialSeconds ).isEqualTo( 0l );
		assertThat( LongCaster.cast( initialDateTime.modify( "s", 1l ).format( "s" ) ) ).isEqualTo( 1l );
		initialDateTime.modify( "s", -1l );
		assertThat( LongCaster.cast( initialDateTime.format( "s" ) ) ).isEqualTo( initialSeconds );
		// Milliseconds
		Long initialMillis = LongCaster.cast( initialDateTime.format( "A" ) );
		assertThat( initialMillis ).isEqualTo( 0l );
		assertThat( LongCaster.cast( initialDateTime.modify( "l", 1l ).format( "A" ) ) ).isEqualTo( 1l );
		initialDateTime.modify( "l", -1l );
		assertThat( LongCaster.cast( initialDateTime.format( "A" ) ) ).isEqualTo( initialMillis );
		// Week Day ( working )
		Long initialWeekDay = LongCaster.cast( initialDateTime.format( "e" ) );
		assertThat( LongCaster.cast( initialDateTime.modify( "w", 1l ).format( "e" ) ) ).isEqualTo( 2l );
		assertThat( Long.signum( -1l ) ).isEqualTo( -1 );
		initialDateTime.modify( "w", -1l );
		assertThat( initialWeekDay ).isEqualTo( LongCaster.cast( initialDateTime.format( "e" ) ) );
		// Quarter
		Long initialQuarter = LongCaster.cast( initialDateTime.format( "Q" ) );
		assertThat( LongCaster.cast( initialDateTime.modify( "q", 1l ).format( "Q" ) ) ).isEqualTo( 1l );
		assertThat( Long.signum( -1l ) ).isEqualTo( -1 );
		initialDateTime.modify( "q", -1l );
		assertThat( initialQuarter ).isEqualTo( LongCaster.cast( initialDateTime.format( "Q" ) ) );
	}

}
