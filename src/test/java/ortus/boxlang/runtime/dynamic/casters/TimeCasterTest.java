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

import java.time.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class TimeCasterTest {

	@DisplayName( "It can cast null to not be a Time" )
	@Test
	public void testNull() {
		assertThat( TimeCaster.attempt( null ).wasSuccessful() ).isFalse();
	}

	@Test
	@DisplayName( "Test casting valid string representation of time to Time" )
	public void testCastValidString() {
		String		dateString	= "2:41 PM";
		LocalTime	result		= TimeCaster.cast( dateString );
		assertThat( result ).isNotNull();
	}

	@Test
	@DisplayName( "Test casting full Time string to Time" )
	public void testCastFullDateTimeString() {
		String		timeString	= "9:38:15 PM UTC";
		LocalTime	result		= TimeCaster.cast( timeString );
		assertThat( result ).isNotNull();
	}

	@Test
	@DisplayName( "Test casting long Time string to DateTime" )
	public void testCastLongDateTimeString() {
		String		dateTimeString	= "9:40:55 PM UTC";
		LocalTime	result			= TimeCaster.cast( dateTimeString );
		assertThat( result ).isNotNull();
	}

	@Test
	@DisplayName( "Test casting medium Time string to DateTime" )
	public void testCastMediumDateTimeString() {
		String		dateTimeString	= "9:40:46 PM";
		LocalTime	result			= TimeCaster.cast( dateTimeString );
		assertThat( result ).isNotNull();
	}

	@Test
	@DisplayName( "Test casting short Time string to DateTime" )
	public void testCastShortDateTimeString() {
		String		dateTimeString	= "9:40 PM";
		LocalTime	result			= TimeCaster.cast( dateTimeString );
		assertThat( result ).isNotNull();
	}
}
