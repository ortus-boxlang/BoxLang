package ortus.boxlang.runtime.cache.policies;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.mockito.Mockito;

import ortus.boxlang.runtime.cache.BoxCacheEntry;
import ortus.boxlang.runtime.cache.ICacheEntry;
import ortus.boxlang.runtime.scopes.Key;

public class BasePolicyTest {

	ConcurrentHashMap<Key, ICacheEntry> cache = new ConcurrentHashMap<>();

	public ICacheEntry createMockEntry() {
		// Create a mock instance of BoxCacheEntry
		return Mockito.mock( BoxCacheEntry.class );
	}

	/**
	 * Assert the ordering of the cache entries according to the policy
	 *
	 * @param policy  The policy to test
	 * @param entries The expected order of the entries to resolve
	 */
	public void assertEntries( ICachePolicy policy, ICacheEntry... entries ) {
		// Sort the map
		List<ICacheEntry> sortedList = cache.entrySet()
		    .stream()
		    .sorted( Map.Entry.comparingByValue( policy.getComparator() ) )
		    .map( Map.Entry::getValue )
		    .toList();

		for ( int i = 0; i < entries.length; i++ ) {
			assertThat( sortedList.get( i ).hashCode() ).isEqualTo( entries[ i ].hashCode() );
		}
	}

}
