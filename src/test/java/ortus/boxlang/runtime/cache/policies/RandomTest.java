package ortus.boxlang.runtime.cache.policies;

import java.util.Comparator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.cache.ICacheEntry;

public class RandomTest {

	@Test
	@DisplayName( "Random Cache Policy: Comparator Test" )
	void testComparator() {
		Random					randomPolicy	= new Random();

		// Get the comparator
		Comparator<ICacheEntry>	comparator		= randomPolicy.getComparator();
		// Test that it works return value must be between -1 and 1
		for ( int i = 0; i < 10; i++ ) {
			int result = comparator.compare( null, null );
			assert result >= -1 && result <= 1;
		}

	}

}
