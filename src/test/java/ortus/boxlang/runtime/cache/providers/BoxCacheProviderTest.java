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
package ortus.boxlang.runtime.cache.providers;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ortus.boxlang.runtime.async.executors.ExecutorRecord;
import ortus.boxlang.runtime.cache.filters.WildcardFilter;
import ortus.boxlang.runtime.config.segments.CacheConfig;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.AsyncService;
import ortus.boxlang.runtime.services.AsyncService.ExecutorType;
import ortus.boxlang.runtime.services.CacheService;
import ortus.boxlang.runtime.types.IStruct;

public class BoxCacheProviderTest {

	static BoxCacheProvider	boxCache;
	static CacheConfig		config			= new CacheConfig();
	static CacheService		cacheService	= Mockito.mock( CacheService.class );
	static ExecutorRecord	executorRecord	= AsyncService.buildExecutor( "tests", ExecutorType.SCHEDULED, 20 );

	@BeforeAll
	static void setup() {
		Mockito.when( cacheService.getTaskScheduler() ).thenReturn( executorRecord );

		boxCache = new BoxCacheProvider();
		boxCache.configure( cacheService, config );
	}

	@AfterAll
	static void tearDown() {
		boxCache.shutdown();
	}

	@BeforeEach
	void reset() {
		boxCache.clearAll();
		boxCache.getStats().reset();
	}

	@Test
	@DisplayName( "When creating a BoxCacheProvider, it should have initial properties set" )
	void testBoxCacheProviderInitialization() {
		assertThat( boxCache ).isNotNull();
		assertThat( boxCache.isEnabled() ).isTrue();
		assertThat( boxCache.isReportingEnabled() ).isTrue();
		assertThat( boxCache.getObjectStore() ).isNotNull();
		assertThat( boxCache.getConfig() ).isNotNull();
		assertThat( boxCache.getStats() ).isNotNull();
		assertThat( boxCache.getReapingFuture() ).isNotNull();
		assertThat( boxCache.getReapingFuture().isDone() ).isFalse();
		assertThat( boxCache.getName().getName() ).isEqualTo( "default" );
		assertThat( boxCache.getObjectStore().getName() ).isEqualTo( "ConcurrentStore" );
	}

	@Test
	@DisplayName( "Get the cache store metdata key map" )
	void testGetCacheStoreMetadataKeyMap() {
		assertThat( boxCache.getStoreMetadataKeyMap() ).isNotNull();
		assertThat( boxCache.getStoreMetadataKeyMap().size() ).isEqualTo( 9 );
	}

	@Test
	@DisplayName( "Get the cache store metadata report" )
	void testGetCacheStoreMetadataReport() {
		// Store 2 cache items
		boxCache.set( "key1", "value1" );
		boxCache.set( "key2", "value2" );

		// Get the metadata report
		var metadataReport = boxCache.getStoreMetadataReport();

		// Verify that it contains an entry for each entry in the cache
		assertThat( metadataReport.get( "key1" ) ).isNotNull();
		assertThat( metadataReport.get( "key2" ) ).isNotNull();
	}

	@Test
	@DisplayName( "Get the cache store metadata report for a specific key" )
	void testGetCachedObjectMetadata() {
		boxCache.set( "key1", "testing" );
		var results = boxCache.getCachedObjectMetadata( "key1" );
		assertThat( results ).isNotNull();
		assertThat( results.get( "hits" ) ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "It can clear all cache items with a filter" )
	void testClearAllWithFilter() {
		boxCache.set( "testKey", "test" );
		boxCache.set( "testKey2", "test" );
		boxCache.set( "key3", "test" );

		boxCache.clearAll( new WildcardFilter( "testKe*" ) );

		assertThat( boxCache.lookup( "testKey" ) ).isFalse();
		assertThat( boxCache.lookup( "testKey2" ) ).isFalse();
	}

	@Test
	@DisplayName( "It can clear specific cache items" )
	void testClear() {
		boxCache.set( "testKey", "test" );
		assertThat( boxCache.lookup( "testKey" ) ).isTrue();
		boxCache.clear( "testKey" );
		assertThat( boxCache.lookup( "testKey" ) ).isFalse();
	}

	@Test
	@DisplayName( "It can clear multiple cache items" )
	void testClearMultiple() {
		boxCache.set( "testKey", "test" );
		boxCache.set( "testKey2", "test" );
		assertThat( boxCache.lookup( "testKey" ) ).isTrue();
		assertThat( boxCache.lookup( "testKey2" ) ).isTrue();
		boxCache.clear( "testKey", "testKey2" );
		assertThat( boxCache.lookup( "testKey" ) ).isFalse();
		assertThat( boxCache.lookup( "testKey2" ) ).isFalse();
	}

	@Test
	@DisplayName( "It can get all keys in the cache" )
	void testGetKeys() {
		boxCache.set( "testKey", "test" );
		boxCache.set( "testKey2", "test" );

		assertThat( boxCache.getKeys() ).containsAtLeast( "testKey", "testKey2" );
	}

	@Test
	@DisplayName( "It can get all keys in the cache using a key filter" )
	void testGetKeysWithFilter() {
		boxCache.set( "testKey", "test" );
		boxCache.set( "testKey2", "test" );
		boxCache.set( "key3", "test" );

		assertThat( boxCache.getKeys( new WildcardFilter( "testKe*" ) ).size() ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "It can get a key stream from the cache" )
	void testGetKeyStream() {
		boxCache.set( "testKey", "test" );
		boxCache.set( "testKey2", "test" );

		assertThat( boxCache.getKeysStream() ).isNotNull();
	}

	@Test
	@DisplayName( "It can get a key stream with a cache filter from the cache" )
	void testGetKeyStreamWithFilter() {
		boxCache.set( "testKey", "test" );
		boxCache.set( "testKey2", "test" );
		boxCache.set( "asdfsd1", "test" );
		boxCache.set( "asdfsd2", "test" );

		var test = boxCache
		    .getKeysStream( new WildcardFilter( "testKe*" ) )
		    .count();
		assertThat( test ).isEqualTo( 2 );
	}

	@Test
	@DisplayName( "It can lookup for cache keys" )
	void testLookup() {
		boxCache.set( "testKey", "test" );
		assertThat( boxCache.lookup( "testKey" ) ).isTrue();
		assertThat( boxCache.lookup( "bogusmasterkey" ) ).isFalse();
	}

	@Test
	@DisplayName( "It can lookup for multiple cache keys" )
	void testLookupMultiple() {
		boxCache.set( "testKey", "test" );
		boxCache.set( "testKey2", "test" );

		var results = boxCache.lookup( "testKey", "testKey2" );

		assertThat( results.size() ).isEqualTo( 2 );
		assertThat( results.getAsBoolean( Key.of( "testKey" ) ) ).isTrue();
		assertThat( results.getAsBoolean( Key.of( "testKey2" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "It can lookup for multiple cache keys with a filter" )
	void testLookupMultipleWithFilter() {
		boxCache.set( "testKey", "test" );
		boxCache.set( "testKey2", "test" );
		boxCache.set( "asdfsd1", "test" );
		boxCache.set( "asdfsd2", "test" );

		var results = boxCache.lookup( new WildcardFilter( "testKe*" ) );

		assertThat( results.size() ).isEqualTo( 2 );
		assertThat( results.getAsBoolean( Key.of( "testKey" ) ) ).isTrue();
		assertThat( results.getAsBoolean( Key.of( "testKey2" ) ) ).isTrue();
	}

	@Test
	@DisplayName( "It can get a cache item with stats" )
	void testGet() {
		boxCache.set( "testKey", "test" );
		assertThat( boxCache.get( "testKey" ).get() ).isEqualTo( "test" );
		assertThat( boxCache.getStats().misses() ).isEqualTo( 0 );
		// Invalid one
		assertThat( boxCache.get( "bogusKey" ).isPresent() ).isFalse();
		assertThat( boxCache.getStats().hits() ).isEqualTo( 1 );
		assertThat( boxCache.getStats().misses() ).isEqualTo( 1 );
	}

	@Test
	@DisplayName( "It can get multiple cache items" )
	void testGetMultiple() {
		boxCache.set( "testKey", "test" );
		boxCache.set( "testKey2", "test" );

		IStruct results = boxCache.get( "testKey", "testKey2" );

		assertThat( results.size() ).isEqualTo( 2 );
		assertThat( results.getAsOptional( Key.of( "testKey" ) ).get() ).isEqualTo( "test" );
		assertThat( results.getAsOptional( Key.of( "testKey2" ) ).get() ).isEqualTo( "test" );
	}

	@Test
	@DisplayName( "It can get multiple cache items with a filter" )
	void testGetMultipleWithFilter() {
		boxCache.set( "testKey", "test" );
		boxCache.set( "testKey2", "test" );
		boxCache.set( "asdfsd1", "test" );
		boxCache.set( "asdfsd2", "test" );

		IStruct results = boxCache.get( new WildcardFilter( "testKe*" ) );

		assertThat( results.getAsOptional( Key.of( "testKey" ) ).get() ).isEqualTo( "test" );
		assertThat( results.getAsOptional( Key.of( "testKey2" ) ).get() ).isEqualTo( "test" );
	}

	@Test
	@DisplayName( "It can do getOrSet() operations" )
	void testGetOrSet() {
		// Clear just in case
		boxCache.clear( "testKey" );
		assertThat( boxCache.getOrSet( "testKey", () -> "test" ) ).isEqualTo( "test" );
		// Lookkup test
		assertThat( boxCache.lookup( "testKey" ) ).isTrue();
		assertThat( boxCache.getOrSet( "testKey", () -> "test" ) ).isEqualTo( "test" );
	}

}
