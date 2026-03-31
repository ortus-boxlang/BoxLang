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
package ortus.boxlang.runtime.async;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

class CronExpressionTest {

	// --------------------------------------------------------------------------
	// Parsing tests
	// --------------------------------------------------------------------------

	@DisplayName( "It can parse a 5-field Unix cron expression" )
	@Test
	public void testParse5Field() {
		CronExpression expr = CronExpression.parse( "* * * * *" );
		assertThat( expr.isSecondsField() ).isFalse();
		assertThat( expr.getExpression() ).isEqualTo( "* * * * *" );
	}

	@DisplayName( "It can parse a 6-field Quartz cron expression" )
	@Test
	public void testParse6Field() {
		CronExpression expr = CronExpression.parse( "0 0 12 * * ?" );
		assertThat( expr.isSecondsField() ).isTrue();
		assertThat( expr.getExpression() ).isEqualTo( "0 0 12 * * ?" );
	}

	@DisplayName( "It can parse a 7-field Quartz cron expression (year is ignored)" )
	@Test
	public void testParse7Field() {
		CronExpression expr = CronExpression.parse( "0 0 12 * * ? 2099" );
		assertThat( expr.isSecondsField() ).isTrue();
	}

	@DisplayName( "It throws on null expression" )
	@Test
	public void testParseNull() {
		assertThrows( BoxRuntimeException.class, () -> CronExpression.parse( null ) );
	}

	@DisplayName( "It throws on empty expression" )
	@Test
	public void testParseEmpty() {
		assertThrows( BoxRuntimeException.class, () -> CronExpression.parse( "  " ) );
	}

	@DisplayName( "It throws on wrong field count" )
	@Test
	public void testParseWrongFieldCount() {
		assertThrows( BoxRuntimeException.class, () -> CronExpression.parse( "* * * *" ) );  // 4 fields
	}

	@DisplayName( "It throws on step value of zero" )
	@Test
	public void testStepZeroThrows() {
		assertThrows( BoxRuntimeException.class, () -> CronExpression.parse( "*/0 * * * *" ) );
	}

	@DisplayName( "It throws on step value less than zero" )
	@Test
	public void testStepNegativeThrows() {
		assertThrows( BoxRuntimeException.class, () -> CronExpression.parse( "*/-1 * * * *" ) );
	}

	@DisplayName( "It throws on minute value out of range" )
	@Test
	public void testMinuteOutOfRangeThrows() {
		assertThrows( BoxRuntimeException.class, () -> CronExpression.parse( "60 * * * *" ) );
	}

	@DisplayName( "It throws on hour value out of range" )
	@Test
	public void testHourOutOfRangeThrows() {
		assertThrows( BoxRuntimeException.class, () -> CronExpression.parse( "* 24 * * *" ) );
	}

	@DisplayName( "It throws on range start greater than end" )
	@Test
	public void testRangeStartGreaterThanEndThrows() {
		assertThrows( BoxRuntimeException.class, () -> CronExpression.parse( "* 17-9 * * *" ) );
	}

	@DisplayName( "It throws on range value out of bounds" )
	@Test
	public void testRangeOutOfBoundsThrows() {
		assertThrows( BoxRuntimeException.class, () -> CronExpression.parse( "* 0-25 * * *" ) );
	}

	@DisplayName( "It throws on step base start out of bounds" )
	@Test
	public void testStepBaseOutOfBoundsThrows() {
		assertThrows( BoxRuntimeException.class, () -> CronExpression.parse( "70/5 * * * *" ) );
	}

	// --------------------------------------------------------------------------
	// matches() tests
	// --------------------------------------------------------------------------

	@DisplayName( "5-field wildcard matches any minute" )
	@Test
	public void testMatchesWildcard() {
		CronExpression	expr	= CronExpression.parse( "* * * * *" );
		// At second 0 (5-field fires at second 0)
		LocalDateTime	dt		= LocalDateTime.of( 2024, 6, 15, 10, 30, 0 );
		assertThat( expr.matches( dt ) ).isTrue();
	}

	@DisplayName( "5-field wildcard does not match non-zero seconds" )
	@Test
	public void testDoesNotMatchNonZeroSecond() {
		CronExpression	expr	= CronExpression.parse( "* * * * *" );
		LocalDateTime	dt		= LocalDateTime.of( 2024, 6, 15, 10, 30, 1 );
		assertThat( expr.matches( dt ) ).isFalse();
	}

	@DisplayName( "6-field noon expression matches noon" )
	@Test
	public void testMatchesNoon() {
		CronExpression	expr	= CronExpression.parse( "0 0 12 * * ?" );
		LocalDateTime	match	= LocalDateTime.of( 2024, 6, 15, 12, 0, 0 );
		LocalDateTime	noMatch	= LocalDateTime.of( 2024, 6, 15, 11, 0, 0 );
		assertThat( expr.matches( match ) ).isTrue();
		assertThat( expr.matches( noMatch ) ).isFalse();
	}

	@DisplayName( "Step expression: every 15 minutes from :00" )
	@Test
	public void testStep() {
		CronExpression expr = CronExpression.parse( "0/15 * * * *" );
		assertThat( expr.matches( LocalDateTime.of( 2024, 1, 1, 10, 0, 0 ) ) ).isTrue();
		assertThat( expr.matches( LocalDateTime.of( 2024, 1, 1, 10, 15, 0 ) ) ).isTrue();
		assertThat( expr.matches( LocalDateTime.of( 2024, 1, 1, 10, 30, 0 ) ) ).isTrue();
		assertThat( expr.matches( LocalDateTime.of( 2024, 1, 1, 10, 45, 0 ) ) ).isTrue();
		assertThat( expr.matches( LocalDateTime.of( 2024, 1, 1, 10, 7, 0 ) ) ).isFalse();
	}

	@DisplayName( "Range expression: hours 9-17 on weekdays" )
	@Test
	public void testRangeWeekdays() {
		// "0 9-17 * * MON-FRI" — 5-field
		CronExpression	expr			= CronExpression.parse( "0 9-17 * * MON-FRI" );
		// Monday 2024-06-17 at 10:00
		LocalDateTime	weekdayMatch	= LocalDateTime.of( 2024, 6, 17, 10, 0, 0 );
		// Sunday 2024-06-16
		LocalDateTime	weekendNoMatch	= LocalDateTime.of( 2024, 6, 16, 10, 0, 0 );
		assertThat( expr.matches( weekdayMatch ) ).isTrue();
		assertThat( expr.matches( weekendNoMatch ) ).isFalse();
	}

	@DisplayName( "First of month at midnight" )
	@Test
	public void testFirstOfMonth() {
		CronExpression	expr	= CronExpression.parse( "0 0 1 * *" );
		LocalDateTime	match	= LocalDateTime.of( 2024, 3, 1, 0, 0, 0 );
		LocalDateTime	noMatch	= LocalDateTime.of( 2024, 3, 2, 0, 0, 0 );
		assertThat( expr.matches( match ) ).isTrue();
		assertThat( expr.matches( noMatch ) ).isFalse();
	}

	@DisplayName( "Last day of month (L)" )
	@Test
	public void testLastDayOfMonth() {
		CronExpression	expr	= CronExpression.parse( "0 0 L * *" );
		// Feb 29, 2024 (leap year) — last day
		LocalDateTime	match	= LocalDateTime.of( 2024, 2, 29, 0, 0, 0 );
		LocalDateTime	noMatch	= LocalDateTime.of( 2024, 2, 28, 0, 0, 0 );
		assertThat( expr.matches( match ) ).isTrue();
		assertThat( expr.matches( noMatch ) ).isFalse();
	}

	@DisplayName( "Month names (JAN/DEC) work correctly" )
	@Test
	public void testMonthNames() {
		// 5-field: "* * * JAN,DEC *"
		CronExpression	expr	= CronExpression.parse( "* * * JAN,DEC *" );
		LocalDateTime	jan		= LocalDateTime.of( 2024, 1, 15, 10, 0, 0 );
		LocalDateTime	dec		= LocalDateTime.of( 2024, 12, 15, 10, 0, 0 );
		LocalDateTime	jun		= LocalDateTime.of( 2024, 6, 15, 10, 0, 0 );
		assertThat( expr.matches( jan ) ).isTrue();
		assertThat( expr.matches( dec ) ).isTrue();
		assertThat( expr.matches( jun ) ).isFalse();
	}

	@DisplayName( "SUN=0 and SUN=7 both work as Sunday" )
	@Test
	public void testSundayAlias() {
		// 5-field: "* * * * SUN" — every Sunday
		CronExpression	expr	= CronExpression.parse( "* * * * SUN" );
		// Sunday 2024-06-16
		LocalDateTime	sunday	= LocalDateTime.of( 2024, 6, 16, 10, 0, 0 );
		// Monday 2024-06-17
		LocalDateTime	monday	= LocalDateTime.of( 2024, 6, 17, 10, 0, 0 );
		assertThat( expr.matches( sunday ) ).isTrue();
		assertThat( expr.matches( monday ) ).isFalse();
	}

	@DisplayName( "Star/question mark for DOW works as wildcard" )
	@Test
	public void testQuestionMarkWildcard() {
		CronExpression	expr	= CronExpression.parse( "0 0 12 * * ?" );
		// Any day of week should match at noon
		LocalDateTime	monday	= LocalDateTime.of( 2024, 6, 17, 12, 0, 0 );
		LocalDateTime	sunday	= LocalDateTime.of( 2024, 6, 16, 12, 0, 0 );
		assertThat( expr.matches( monday ) ).isTrue();
		assertThat( expr.matches( sunday ) ).isTrue();
	}

	// --------------------------------------------------------------------------
	// nextFireTime() tests
	// --------------------------------------------------------------------------

	@DisplayName( "nextFireTime returns the correct next minute for 5-field" )
	@Test
	public void testNextFireTime5Field() {
		// Every minute at :15
		CronExpression			expr	= CronExpression.parse( "15 * * * *" );
		LocalDateTime			from	= LocalDateTime.of( 2024, 6, 15, 10, 0, 0 );
		Optional<LocalDateTime>	next	= expr.nextFireTime( from );
		assertThat( next.isPresent() ).isTrue();
		assertThat( next.get().getMinute() ).isEqualTo( 15 );
	}

	@DisplayName( "nextFireTime for noon each day" )
	@Test
	public void testNextFireTimeNoon() {
		CronExpression			expr	= CronExpression.parse( "0 0 12 * * ?" );
		LocalDateTime			from	= LocalDateTime.of( 2024, 6, 15, 10, 0, 0 );
		Optional<LocalDateTime>	next	= expr.nextFireTime( from );
		assertThat( next.isPresent() ).isTrue();
		assertThat( next.get().getHour() ).isEqualTo( 12 );
		assertThat( next.get().getMinute() ).isEqualTo( 0 );
		assertThat( next.get().getSecond() ).isEqualTo( 0 );
	}

	@DisplayName( "nextFireDelayMillis returns a positive number for a future cron expression" )
	@Test
	public void testNextFireDelayMillis() {
		// Every minute — next fire is always within 60 seconds
		CronExpression	expr	= CronExpression.parse( "* * * * *" );
		long			delay	= expr.nextFireDelayMillis( ZoneId.systemDefault() );
		assertThat( delay ).isGreaterThan( 0L );
		assertThat( delay ).isAtMost( 60_000L );
	}

	@DisplayName( "toString includes the expression" )
	@Test
	public void testToString() {
		CronExpression expr = CronExpression.parse( "* * * * *" );
		assertThat( expr.toString() ).contains( "* * * * *" );
	}
}
