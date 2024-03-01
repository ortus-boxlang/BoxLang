package ortus.boxlang.runtime.cache.policies;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ortus.boxlang.runtime.cache.ICacheEntry;
import ortus.boxlang.runtime.scopes.Key;

public class MFUTest extends BasePolicyTest {

	@Test
	@DisplayName( "MFU Cache Policy: Comparator Test" )
	void testComparator() {
		// Create 3 cache entries with different timestamps in ascending order
		ICacheEntry entry1 = createMockEntry();
		Mockito.when( entry1.hits() ).thenReturn( 20L );
		ICacheEntry entry2 = createMockEntry();
		Mockito.when( entry2.hits() ).thenReturn( 1L );
		ICacheEntry entry3 = createMockEntry();
		Mockito.when( entry3.hits() ).thenReturn( 15L );

		// Put them in a ConcurrentHashMap
		cache.put( Key.of( "entry1" ), entry1 );
		cache.put( Key.of( "entry2" ), entry2 );
		cache.put( Key.of( "entry3" ), entry3 );

		MFU policy = new MFU();

		// Test the ordering according to the lastAccessed
		assertEntries( policy, entry1, entry3, entry2 );

	}

}
