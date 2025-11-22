package ortus.boxlang.runtime.util;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Test the LocalizationUtil formatter caching functionality
 */
public class LocalizationUtilCacheTest {

	@Test
	public void testFormatterCaching() {
		// Test that the same formatter instance is returned for repeated calls
		DateTimeFormatter	formatter1	= LocalizationUtil.getCommonPatternDateTimeParsers();
		DateTimeFormatter	formatter2	= LocalizationUtil.getCommonPatternDateTimeParsers();

		// Should be the same instance due to caching
		assertThat( formatter1 ).isSameInstanceAs( formatter2 );
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