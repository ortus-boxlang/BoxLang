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
package ortus.boxlang.runtime.cache.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class BoxCacheStatsTest {

	BoxCacheStats cacheStats;

	@BeforeEach
	void setUp() {
		// Create a test fixture
		cacheStats = new BoxCacheStats();
	}

	@Test
	@DisplayName( "When creating a BoxCacheStats, it should have initial properties set" )
	void testBoxCacheStatsInitialization() {
		assertThat( cacheStats.hitRate() ).isEqualTo( 0L );
		assertThat( cacheStats.garbageCollections() ).isEqualTo( 0L );
		assertThat( cacheStats.evictionCount() ).isEqualTo( 0L );
		assertThat( cacheStats.hits() ).isEqualTo( 0L );
		assertThat( cacheStats.misses() ).isEqualTo( 0L );
		assertThat( cacheStats.lastReapDatetime() ).isNotNull();
		assertThat( cacheStats.started() ).isNotNull();
	}

	@Test
	@DisplayName( "When recording an eviction, the eviction count should increase" )
	void testRecordEviction() {
		// When
		cacheStats.recordEviction();
		// Then
		assertThat( cacheStats.evictionCount() ).isEqualTo( 1L );
	}

	@Test
	@DisplayName( "When recording a cache hit, the hit count should increase" )
	void testRecordHit() {
		// When
		cacheStats.recordHit();
		// Then
		assertThat( cacheStats.hits() ).isEqualTo( 1L );
	}

	@Test
	@DisplayName( "When recording a cache miss, the miss count should increase" )
	void testRecordMiss() {
		// When
		cacheStats.recordMiss();
		// Then
		assertThat( cacheStats.misses() ).isEqualTo( 1L );
	}

	@Test
	@DisplayName( "When resetting the cache stats, all counts should be reset to 0" )
	void testReset() {
		// Given
		cacheStats.recordEviction();
		cacheStats.recordHit();
		cacheStats.recordMiss();
		// When
		cacheStats.reset();
		// Then
		assertThat( cacheStats.evictionCount() ).isEqualTo( 0L );
		assertThat( cacheStats.hits() ).isEqualTo( 0L );
		assertThat( cacheStats.misses() ).isEqualTo( 0L );
	}

	@Test
	@DisplayName( "When recording a garbage collection hit, the garbage collection count should increase" )
	void testRecordGCHit() {
		// When
		cacheStats.recordGCHit();
		// Then
		assertThat( cacheStats.garbageCollections() ).isEqualTo( 1L );
	}
}
