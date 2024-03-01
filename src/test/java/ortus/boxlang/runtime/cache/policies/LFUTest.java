package ortus.boxlang.runtime.cache.policies;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ortus.boxlang.runtime.cache.ICacheEntry;
import ortus.boxlang.runtime.scopes.Key;

public class LFUTest extends BasePolicyTest {

	@Test
	@DisplayName( "LFU Cache Policy: Comparator Test" )
	void testComparator() {
		ICacheEntry entry1 = createMockEntry();
		Mockito.when( entry1.hits() ).thenReturn( 5l );
		ICacheEntry entry2 = createMockEntry();
		Mockito.when( entry2.hits() ).thenReturn( 3l );
		ICacheEntry entry3 = createMockEntry();
		Mockito.when( entry3.hits() ).thenReturn( 10l );

		cache.put( Key.of( "entry1" ), entry1 );
		cache.put( Key.of( "entry2" ), entry2 );
		cache.put( Key.of( "entry3" ), entry3 );

		LFU policy = new LFU();

		// Test the ordering according to the lastAccessed
		assertEntries( policy, entry2, entry1, entry3 );
	}
}
