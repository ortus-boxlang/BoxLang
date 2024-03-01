package ortus.boxlang.runtime.cache.filters;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;

public class WildcardFilterTest {

	@Test
	@DisplayName( "Test apply method with wildcard pattern" )
	public void testApply() {
		WildcardFilter	filter	= new WildcardFilter( "hello*world", false );
		Key				key1	= new Key( "hello123world" );
		Key				key2	= new Key( "helloABCworld" );
		Key				key3	= new Key( "hello" );

		assertThat( filter.apply( key1 ) ).isTrue();
		assertThat( filter.apply( key2 ) ).isTrue();
		assertThat( filter.apply( key3 ) ).isFalse();
	}

	@Test
	@DisplayName( "Test apply method with case sensitivity" )
	public void testApplyCaseSensitivity() {
		WildcardFilter	filter	= new WildcardFilter( "hello*world" );
		Key				key1	= new Key( "hello123world" );
		Key				key2	= new Key( "helloABCworld" );
		Key				key3	= new Key( "Hello123world" );

		assertThat( filter.apply( key1 ) ).isTrue();
		assertThat( filter.apply( key2 ) ).isTrue();
		assertThat( filter.apply( key3 ) ).isFalse();
	}

	@Test
	@DisplayName( "Test apply method with ? pattern" )
	public void testApplyQuestionMark() {
		WildcardFilter	filter	= new WildcardFilter( "lui?" );
		Key				key1	= new Key( "lui1" );
		Key				key2	= new Key( "lui2" );
		Key				key3	= new Key( "lui" );

		assertThat( filter.apply( key1 ) ).isTrue();
		assertThat( filter.apply( key2 ) ).isTrue();
		assertThat( filter.apply( key3 ) ).isFalse();
	}

}
