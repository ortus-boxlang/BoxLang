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
package ortus.boxlang.runtime.util;

import static com.google.common.truth.Truth.assertThat;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.junit.jupiter.api.Test;

/**
 * Test the LocalizationUtil formatter caching functionality
 */
public class LocalizationUtilCacheTest {

	@Test
	public void testCommonFormatterCaching() {
		// Test that CommonFormatters are properly cached/initialized
		// This tests the getCommonFormatters() method which replaced getCommonPatternDateTimeParsers()

		// Call the method that uses CommonFormatters internally
		// We'll test with the same date string twice to ensure consistent behavior
		String									testDateString	= "2023-12-25T14:30:00";

		// This should use our optimized CommonFormatter approach
		// The test ensures the optimization doesn't break basic functionality
		ortus.boxlang.runtime.types.DateTime	result1			= LocalizationUtil.parseFromCommonPatterns( testDateString, null );
		ortus.boxlang.runtime.types.DateTime	result2			= LocalizationUtil.parseFromCommonPatterns( testDateString, null );

		// Both should parse successfully and return equivalent results
		assertThat( result1 ).isNotNull();
		assertThat( result2 ).isNotNull();
		assertThat( result1.toString() ).isEqualTo( result2.toString() );
	}

	@Test
	public void testLocaleSpecificCaching() {
		Locale				germanLocale		= Locale.GERMAN;
		Locale				frenchLocale		= Locale.FRENCH;

		// Test that locale-specific formatters are cached separately
		DateTimeFormatter	germanFormatter1	= LocalizationUtil.getLocaleDateTimeParsers( germanLocale );
		DateTimeFormatter	germanFormatter2	= LocalizationUtil.getLocaleDateTimeParsers( germanLocale );
		DateTimeFormatter	frenchFormatter		= LocalizationUtil.getLocaleDateTimeParsers( frenchLocale );

		// German formatters should be the same instance
		assertThat( germanFormatter1 ).isSameInstanceAs( germanFormatter2 );

		// German and French formatters should be different instances
		assertThat( germanFormatter1 ).isNotSameInstanceAs( frenchFormatter );
	}

	@Test
	public void testAllFormatterTypesAreCached() {
		Locale				testLocale		= Locale.US;

		// Test each formatter type returns the same instance on repeated calls
		DateTimeFormatter	zonedFormatter1	= LocalizationUtil.getLocaleZonedDateTimeParsers( testLocale );
		DateTimeFormatter	zonedFormatter2	= LocalizationUtil.getLocaleZonedDateTimeParsers( testLocale );
		assertThat( zonedFormatter1 ).isSameInstanceAs( zonedFormatter2 );

		DateTimeFormatter	altZonedFormatter1	= LocalizationUtil.getAltLocaleZonedDateTimeParsers( testLocale );
		DateTimeFormatter	altZonedFormatter2	= LocalizationUtil.getAltLocaleZonedDateTimeParsers( testLocale );
		assertThat( altZonedFormatter1 ).isSameInstanceAs( altZonedFormatter2 );

		DateTimeFormatter	dateFormatter1	= LocalizationUtil.getLocaleDateParsers( testLocale );
		DateTimeFormatter	dateFormatter2	= LocalizationUtil.getLocaleDateParsers( testLocale );
		assertThat( dateFormatter1 ).isSameInstanceAs( dateFormatter2 );

		DateTimeFormatter	timeFormatter1	= LocalizationUtil.getLocaleTimeParsers( testLocale );
		DateTimeFormatter	timeFormatter2	= LocalizationUtil.getLocaleTimeParsers( testLocale );
		assertThat( timeFormatter1 ).isSameInstanceAs( timeFormatter2 );
	}

	@Test
	public void testPatternFormatterCaching() {
		String				pattern		= "yyyy-MM-dd HH:mm:ss";

		// Test that pattern formatters are cached
		DateTimeFormatter	formatter1	= LocalizationUtil.getPatternFormatter( pattern );
		DateTimeFormatter	formatter2	= LocalizationUtil.getPatternFormatter( pattern );

		// Should be the same instance due to caching
		assertThat( formatter1 ).isSameInstanceAs( formatter2 );
	}

	@Test
	public void testPatternFormatterWithLocaleCaching() {
		String				pattern				= "yyyy-MM-dd HH:mm:ss";
		Locale				germanLocale		= Locale.GERMAN;
		Locale				frenchLocale		= Locale.FRENCH;

		// Test that pattern formatters with locale are cached separately
		DateTimeFormatter	germanFormatter1	= LocalizationUtil.getPatternFormatter( pattern, germanLocale );
		DateTimeFormatter	germanFormatter2	= LocalizationUtil.getPatternFormatter( pattern, germanLocale );
		DateTimeFormatter	frenchFormatter		= LocalizationUtil.getPatternFormatter( pattern, frenchLocale );
		DateTimeFormatter	noLocaleFormatter	= LocalizationUtil.getPatternFormatter( pattern );

		// German formatters should be the same instance
		assertThat( germanFormatter1 ).isSameInstanceAs( germanFormatter2 );

		// Different locales should be different instances
		assertThat( germanFormatter1 ).isNotSameInstanceAs( frenchFormatter );
		assertThat( germanFormatter1 ).isNotSameInstanceAs( noLocaleFormatter );
		assertThat( frenchFormatter ).isNotSameInstanceAs( noLocaleFormatter );
	}

	@Test
	public void testDifferentPatternsCachedSeparately() {
		String				pattern1	= "yyyy-MM-dd";
		String				pattern2	= "yyyy/MM/dd";

		// Test that different patterns create different cache entries
		DateTimeFormatter	formatter1	= LocalizationUtil.getPatternFormatter( pattern1 );
		DateTimeFormatter	formatter2	= LocalizationUtil.getPatternFormatter( pattern2 );

		// Should be different instances for different patterns
		assertThat( formatter1 ).isNotSameInstanceAs( formatter2 );

		// But repeated calls with same pattern should return same instance
		DateTimeFormatter formatter1Again = LocalizationUtil.getPatternFormatter( pattern1 );
		assertThat( formatter1 ).isSameInstanceAs( formatter1Again );
	}
}