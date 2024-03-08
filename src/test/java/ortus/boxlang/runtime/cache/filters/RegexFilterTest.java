package ortus.boxlang.runtime.cache.filters;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;

public class RegexFilterTest {

	@Test
	@DisplayName( "Regex Filter: Case-Insensitive Test" )
	void testRegexFilterCaseInsensitive() {
		// Create a sample regex filter (you can adapt this to your actual implementation)
		RegexFilter	regexFilter	= new RegexFilter( "^h(ello|at).*" );

		// Test the filter with some keys
		Key			key1		= new Key( "hello" );
		Key			key2		= new Key( "Hello" );
		Key			key3		= new Key( "hat" );
		Key			key4		= new Key( "Hat" );
		Key			key5		= new Key( "hut" );

		assertThat( regexFilter.test( key1 ) ).isTrue();
		assertThat( regexFilter.test( key2 ) ).isTrue();
		assertThat( regexFilter.test( key3 ) ).isTrue();
		assertThat( regexFilter.test( key4 ) ).isTrue();
		assertThat( regexFilter.test( key5 ) ).isFalse();
	}

	@Test
	@DisplayName( "Regex Filter: Case-Sensitive Test" )
	void testRegexFilterCaseSensitive() {
		// Create a sample regex filter (you can adapt this to your actual implementation)
		RegexFilter	regexFilter	= new RegexFilter( "^h(ello|at).*", false );

		// Test the filter with some keys
		Key			key1		= new Key( "hello" );
		Key			key2		= new Key( "Hello" );
		Key			key3		= new Key( "hat" );
		Key			key4		= new Key( "Hat" );
		Key			key5		= new Key( "hut" );

		assertThat( regexFilter.test( key1 ) ).isTrue();
		assertThat( regexFilter.test( key2 ) ).isFalse();
		assertThat( regexFilter.test( key3 ) ).isTrue();
		assertThat( regexFilter.test( key4 ) ).isFalse();
		assertThat( regexFilter.test( key5 ) ).isFalse();
	}
}
