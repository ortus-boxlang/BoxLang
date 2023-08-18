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

import java.text.DecimalFormat;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class is a utility for timing operations.
 */
public class Timer {

	/**
	 * The default timing format
	 */
	public static final DecimalFormat	TIMING_FORMAT	= new DecimalFormat( "#.##" );

	/**
	 * The timers map
	 */
	private Map<String, Long>			timers			= new ConcurrentHashMap<>( 32 );

	/**
	 * The time units
	 */
	public enum TimeUnit {
		SECONDS,
		MILLISECONDS,
		NANOSECONDS
	}

	/**
	 * Start a timer with the given label
	 *
	 * @param label The label
	 *
	 * @return The timer instance
	 */
	public Timer start( String label ) {
		timers.put( label, System.nanoTime() );
		return this;
	}

	/**
	 * Time the given runnable lambda and return the elapsed time in the given time unit
	 *
	 * @param runnable The runnable lambda
	 * @param timeUnit The time unit to return. Allowed values are SECONDS, MILLISECONDS, and NANOSECONDS
	 *
	 * @return The elapsed time in the given time unit as a string
	 */
	public String timeIt( Runnable runnable, TimeUnit timeUnit ) {
		String label = "timer-" + UUID.randomUUID();

		start( label );

		runnable.run();

		return stop( label, timeUnit );
	}

	/**
	 * Time the given runnable lambda and return the elapsed time in milliseconds
	 *
	 * @param runnable The runnable lambda
	 *
	 * @return The elapsed time in the given time unit as a string
	 */
	public String timeIt( Runnable runnable ) {
		String label = "timer-" + UUID.randomUUID();

		start( label );

		runnable.run();

		return stop( label );
	}

	/**
	 * Time the given runnable lambda and return the elapsed time in milliseconds
	 *
	 * @param runnable The runnable lambda
	 * @param label    The label to use for the timer
	 *
	 * @return The elapsed time in the given time unit as a string
	 */
	public String timeIt( Runnable runnable, String label ) {
		start( label );
		runnable.run();
		return stop( label );
	}

	/**
	 * Stop the timer with the given label and return the elapsed time in seconds
	 *
	 * @param label The label
	 *
	 * @return The elapsed time in seconds
	 */
	public long stopAndGetSeconds( String label ) {
		return stopAndGetMillis( label ) / 1000;
	}

	/**
	 * Stop the timer with the given label and return the elapsed time in milliseconds
	 *
	 * @param label The label
	 *
	 * @return The elapsed time in milliseconds
	 */
	public long stopAndGetMillis( String label ) {
		return stopAndGetNanos( label ) / 1_000_000;
	}

	/**
	 * Stop the timer with the given label and return the elapsed time in nanoseconds
	 *
	 * @param label The label
	 *
	 * @return The elapsed time in nanoseconds
	 */
	public long stopAndGetNanos( String label ) {
		if ( !timers.containsKey( label ) ) {
			throw new IllegalArgumentException( "Timer '" + label + "' not started." );
		}

		long	endTime		= System.nanoTime();
		long	startTime	= timers.remove( label );

		return endTime - startTime;
	}

	/**
	 * Stop the timer with the given label and return the elapsed time in the given time unit
	 *
	 * @param label    The label
	 * @param timeUnit The time unit to return. Allowed values are SECONDS, MILLISECONDS, and NANOSECONDS
	 *
	 * @return The elapsed time in the given time unit as a string
	 */
	public String stop( String label, TimeUnit timeUnit ) {
		switch ( timeUnit ) {
			case SECONDS :
				return TIMING_FORMAT.format( stopAndGetSeconds( label ) ) + " seconds";

			case MILLISECONDS :
				return TIMING_FORMAT.format( stopAndGetMillis( label ) ) + " milliseconds";

			case NANOSECONDS :
				return TIMING_FORMAT.format( stopAndGetNanos( label ) ) + " nanoseconds";

			default :
				throw new IllegalArgumentException( "Unsupported time unit: " + timeUnit );
		}
	}

	/**
	 * Stop the timer with the given label and return the elapsed time in the default of milliseconds
	 *
	 * @param label The label
	 *
	 * @return The elapsed time in milliseconds as a string
	 */
	public String stop( String label ) {
		return stop( label, TimeUnit.MILLISECONDS );
	}
}
