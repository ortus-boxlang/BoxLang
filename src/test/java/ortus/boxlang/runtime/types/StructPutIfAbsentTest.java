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
package ortus.boxlang.runtime.types;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;

/**
 * Verifies Struct.putIfAbsent follows the java.util.Map / ConcurrentMap contract
 */
class StructPutIfAbsentTest {

	@DisplayName( "Absent key returns null and inserts" )
	@Test
	void testAbsent() {
		IStruct s = new Struct();
		assertThat( s.putIfAbsent( Key.of( "k1" ), "a" ) ).isNull();
		assertThat( s.get( Key.of( "k1" ) ) ).isEqualTo( "a" );
	}

	@DisplayName( "Present key returns the existing value and does not overwrite" )
	@Test
	void testPresent() {
		IStruct s = new Struct();
		s.putIfAbsent( Key.of( "k1" ), "a" );
		assertThat( s.putIfAbsent( Key.of( "k1" ), "b" ) ).isEqualTo( "a" );
		assertThat( s.putIfAbsent( "k1", "c" ) ).isEqualTo( "a" );
		assertThat( s.get( Key.of( "k1" ) ) ).isEqualTo( "a" );
	}

	@DisplayName( "Works for every struct type" )
	@Test
	void testAllTypes() {
		for ( Struct.TYPES type : Struct.TYPES.values() ) {
			for ( boolean concurrent : new boolean[] { true, false } ) {
				IStruct s = new Struct( type, concurrent );
				assertThat( s.putIfAbsent( Key.of( "k" ), "a" ) ).isNull();
				assertThat( s.putIfAbsent( Key.of( "k" ), "b" ) ).isEqualTo( "a" );
				assertThat( s.size() ).isEqualTo( 1 );
			}
		}
	}

	@DisplayName( "Case sensitive structs treat differently cased keys as distinct" )
	@Test
	void testCaseSensitive() {
		IStruct s = new Struct( Struct.TYPES.CASE_SENSITIVE );
		assertThat( s.putIfAbsent( "foo", "a" ) ).isNull();
		assertThat( s.putIfAbsent( "FOO", "b" ) ).isNull();
		assertThat( s.putIfAbsent( "foo", "c" ) ).isEqualTo( "a" );
		assertThat( s.size() ).isEqualTo( 2 );
	}

	@DisplayName( "Case insensitive structs treat differently cased keys as the same" )
	@Test
	void testCaseInsensitive() {
		IStruct s = new Struct();
		s.putIfAbsent( "foo", "a" );
		assertThat( s.putIfAbsent( "FOO", "b" ) ).isEqualTo( "a" );
		assertThat( s.size() ).isEqualTo( 1 );
	}

	@DisplayName( "A key holding null counts as present for tryPutIfAbsent" )
	@Test
	void testNullValues() {
		IStruct s = new Struct();
		assertThat( s.tryPutIfAbsent( Key.of( "n" ), null ) ).isTrue();
		assertThat( s.containsKey( Key.of( "n" ) ) ).isTrue();
		assertThat( s.tryPutIfAbsent( Key.of( "n" ), "x" ) ).isFalse();
		assertThat( s.get( Key.of( "n" ) ) ).isNull();
		// Same ambiguity as java.util.Map: a null value reads as null
		assertThat( s.putIfAbsent( Key.of( "n" ), "x" ) ).isNull();
		assertThat( s.get( Key.of( "n" ) ) ).isNull();
	}

	@DisplayName( "Listeners fire once on insert and not on a no-op" )
	@Test
	void testListeners() {
		Struct			s		= new Struct();
		AtomicInteger	fired	= new AtomicInteger();
		s.registerChangeListener( ( key, newValue, oldValue, obj ) -> {
			fired.incrementAndGet();
			return newValue;
		} );
		s.putIfAbsent( Key.of( "k" ), "a" );
		assertThat( fired.get() ).isEqualTo( 1 );
		s.putIfAbsent( Key.of( "k" ), "b" );
		assertThat( fired.get() ).isEqualTo( 1 );
		assertThat( s.get( Key.of( "k" ) ) ).isEqualTo( "a" );
	}

	@DisplayName( "A listener returning null vetoes the insert" )
	@Test
	void testListenerVeto() {
		Struct s = new Struct();
		s.registerChangeListener( ( key, newValue, oldValue, obj ) -> null );
		s.putIfAbsent( Key.of( "k" ), "a" );
		assertThat( s.containsKey( Key.of( "k" ) ) ).isFalse();
	}

	@DisplayName( "Concurrent callers: exactly one winner per key" )
	@Test
	void testConcurrentWinners() throws Exception {
		final int		threads	= 48;
		final int		keys	= 300;
		IStruct			s		= new Struct();
		AtomicInteger[]	winners	= new AtomicInteger[ keys ];
		for ( int i = 0; i < keys; i++ ) {
			winners[ i ] = new AtomicInteger();
		}
		CountDownLatch	start	= new CountDownLatch( 1 );
		ExecutorService	pool	= Executors.newFixedThreadPool( threads );
		List<Future<?>>	futures	= new ArrayList<>();
		for ( int t = 0; t < threads; t++ ) {
			final int id = t;
			futures.add( pool.submit( () -> {
				start.await();
				for ( int i = 0; i < keys; i++ ) {
					if ( s.putIfAbsent( Key.of( "key-" + i ), "thread-" + id ) == null ) {
						winners[ i ].incrementAndGet();
					}
				}
				return null;
			} ) );
		}
		start.countDown();
		for ( Future<?> f : futures ) {
			f.get();
		}
		pool.shutdown();
		for ( int i = 0; i < keys; i++ ) {
			assertThat( winners[ i ].get() ).isEqualTo( 1 );
		}
		assertThat( s.size() ).isEqualTo( keys );
	}

	@DisplayName( "Concurrent tryPutIfAbsent: exactly one winner per key" )
	@Test
	void testConcurrentTry() throws Exception {
		IStruct			s		= new Struct();
		AtomicInteger	wins	= new AtomicInteger();
		CountDownLatch	start	= new CountDownLatch( 1 );
		ExecutorService	pool	= Executors.newFixedThreadPool( 32 );
		List<Future<?>>	futures	= new ArrayList<>();
		for ( int t = 0; t < 32; t++ ) {
			futures.add( pool.submit( () -> {
				start.await();
				if ( s.tryPutIfAbsent( Key.of( "claim" ), "x" ) ) {
					wins.incrementAndGet();
				}
				return null;
			} ) );
		}
		start.countDown();
		for ( Future<?> f : futures ) {
			f.get();
		}
		pool.shutdown();
		assertThat( wins.get() ).isEqualTo( 1 );
	}

	@DisplayName( "Under contention the listener fires exactly once per key, only for the winner" )
	@Test
	void testListenerFiresOncePerKeyConcurrently() throws Exception {
		Struct			s		= new Struct();
		AtomicInteger	fired	= new AtomicInteger();
		s.registerChangeListener( ( key, newValue, oldValue, obj ) -> {
			fired.incrementAndGet();
			return newValue;
		} );
		CountDownLatch	start	= new CountDownLatch( 1 );
		ExecutorService	pool	= Executors.newFixedThreadPool( 32 );
		List<Future<?>>	futures	= new ArrayList<>();
		for ( int t = 0; t < 32; t++ ) {
			futures.add( pool.submit( () -> {
				start.await();
				for ( int i = 0; i < 200; i++ ) {
					s.putIfAbsent( Key.of( "key-" + i ), "x" );
				}
				return null;
			} ) );
		}
		start.countDown();
		for ( Future<?> f : futures ) {
			f.get();
		}
		pool.shutdown();
		assertThat( fired.get() ).isEqualTo( 200 );
	}

}
