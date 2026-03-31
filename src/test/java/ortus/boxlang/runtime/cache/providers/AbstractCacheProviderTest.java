package ortus.boxlang.runtime.cache.providers;

import static com.google.common.truth.Truth.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;

public class AbstractCacheProviderTest {

	static BoxRuntime instance;

	@BeforeAll
	static void setup() {
		instance = BoxRuntime.getInstance( true );
	}

	@Test
	@DisplayName( "Tests the static method toDuration" )
	void testToDurationMethod() {
		assertThat( AbstractCacheProvider.toDuration( "", Duration.ofSeconds( 2000 ) ).toSeconds() ).isEqualTo( 2000 );
		Duration testDuration = Duration.ofSeconds( 1500 );
		assertThat( AbstractCacheProvider.toDuration( testDuration ) ).isEqualTo( testDuration );
		assertThat( AbstractCacheProvider.toDuration( "3000", Duration.ofSeconds( 2000 ) ).toSeconds() ).isEqualTo( 3000 );
		assertThat( AbstractCacheProvider.toDuration( null, Duration.ofSeconds( 2000 ) ).toSeconds() ).isEqualTo( 2000 );
		assertThat( AbstractCacheProvider.toDuration( "" ).toSeconds() > 0 ).isFalse();
	}
}
