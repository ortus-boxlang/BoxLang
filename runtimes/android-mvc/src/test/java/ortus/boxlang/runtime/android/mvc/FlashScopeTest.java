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
package ortus.boxlang.runtime.android.mvc;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link FlashScope}: data that survives exactly one request hop.
 */
class FlashScopeTest {

	@DisplayName( "Staged data is not readable until the next request is persisted" )
	@Test
	void testStagedNotReadableImmediately() {
		FlashScope flash = new FlashScope();
		flash.put( "message", "Saved!" );

		// Not yet rotated into the readable bucket.
		assertThat( flash.exists( "message" ) ).isFalse();
		assertThat( flash.get( "message" ) ).isNull();
	}

	@DisplayName( "Staged data becomes readable after one persist (request hop)" )
	@Test
	void testSurvivesOneHop() {
		FlashScope flash = new FlashScope();
		flash.put( "message", "Saved!" );

		// Simulate the start of the next request.
		flash.persist();

		assertThat( flash.exists( "message" ) ).isTrue();
		assertThat( flash.get( "message" ) ).isEqualTo( "Saved!" );
	}

	@DisplayName( "Flash data is discarded after the hop it was read in" )
	@Test
	void testDiscardedAfterSecondHop() {
		FlashScope flash = new FlashScope();
		flash.put( "message", "Saved!" );

		flash.persist();					// request N: readable
		assertThat( flash.get( "message" ) ).isEqualTo( "Saved!" );

		flash.persist();					// request N+1: gone
		assertThat( flash.exists( "message" ) ).isFalse();
	}

	@DisplayName( "keep() re-stages readable data so it survives another hop" )
	@Test
	void testKeep() {
		FlashScope flash = new FlashScope();
		flash.put( "message", "Saved!" );

		flash.persist();					// readable now
		flash.keep();						// re-stage for next hop
		flash.persist();					// still readable

		assertThat( flash.get( "message" ) ).isEqualTo( "Saved!" );
	}

	@DisplayName( "get() returns the default when the key is absent" )
	@Test
	void testGetWithDefault() {
		FlashScope flash = new FlashScope();
		assertThat( flash.get( "missing", "fallback" ) ).isEqualTo( "fallback" );
	}

	@DisplayName( "clear() empties both buckets" )
	@Test
	void testClear() {
		FlashScope flash = new FlashScope();
		flash.put( "a", 1 );
		flash.persist();
		flash.put( "b", 2 );

		flash.clear();
		flash.persist();
		assertThat( flash.exists( "a" ) ).isFalse();
		assertThat( flash.exists( "b" ) ).isFalse();
	}
}
