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
package ortus.boxlang.runtime.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.google.common.truth.Truth;

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class TimerTest {

	private Timer timer;

	@BeforeEach
	void setUp() {
		timer = new Timer();
	}

	@Test
	void testTimeIt() {
		String elapsedTime = timer.timeIt( () -> {
			// Simulate some work
			try {
				Thread.sleep( 100 );
			} catch ( InterruptedException e ) {
				throw new BoxRuntimeException( e.getMessage(), e );
			}
		} );

		Truth.assertThat( elapsedTime ).isNotNull();
		System.out.println( "Elapsed Time: " + elapsedTime );
	}

	@Test
	void testStopAndGetSeconds() {
		timer.start( "testTimer" );

		// Simulate some work
		try {
			Thread.sleep( 100 );
		} catch ( InterruptedException e ) {
			throw new BoxRuntimeException( e.getMessage(), e );
		}

		long elapsedSeconds = timer.stopAndGetSeconds( "testTimer" );

		Truth.assertThat( elapsedSeconds ).isAtLeast( 0L );
		System.out.println( "Elapsed Seconds: " + elapsedSeconds );
	}

	@Test
	void testStopAndGetMillis() {
		timer.start( "testTimer" );

		// Simulate some work
		try {
			Thread.sleep( 100 );
		} catch ( InterruptedException e ) {
			throw new BoxRuntimeException( e.getMessage(), e );
		}

		long elapsedMillis = timer.stopAndGetMillis( "testTimer" );

		Truth.assertThat( elapsedMillis ).isAtLeast( 0L );
		System.out.println( "Elapsed Milliseconds: " + elapsedMillis );
	}

	@Test
	void testStopAndGetNanos() {
		timer.start( "testTimer" );

		// Simulate some work
		try {
			Thread.sleep( 100 );
		} catch ( InterruptedException e ) {
			throw new BoxRuntimeException( e.getMessage(), e );
		}

		long elapsedNanos = timer.stopAndGetNanos( "testTimer" );

		Truth.assertThat( elapsedNanos ).isAtLeast( 0L );
		System.out.println( "Elapsed Nanoseconds: " + elapsedNanos );
	}

	@DisplayName( "Can time a runnable lambda" )
	@Test
	void testCanTimeRunnableLambda() {
		// Given
		Runnable	runnable	= () -> {
									// Simulate some work
									try {
										Thread.sleep( 100 );
									} catch ( InterruptedException e ) {
										throw new BoxRuntimeException( e.getMessage(), e );
									}
								};

		// When
		String		elapsedTime	= timer.timeIt( runnable );

		// Then
		Truth.assertThat( elapsedTime ).isNotNull();
		System.out.println( "Lambda Elapsed Time: " + elapsedTime );
	}

	@DisplayName( "Time and print a lambda using the default of milliseconds" )
	@Test
	void testTimeAndPrintLambda() {
		// Given
		Runnable runnable = () -> {
			// Simulate some work
			try {
				Thread.sleep( 100 );
			} catch ( InterruptedException e ) {
				throw new BoxRuntimeException( e.getMessage(), e );
			}
		};

		// When
		long time = Timer.timeAndPrint( runnable );

		// Then
		Truth.assertThat( time ).isAtLeast( 0L );
	}
}
