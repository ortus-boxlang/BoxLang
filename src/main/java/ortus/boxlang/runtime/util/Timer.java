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

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * This class is a utility for timing operations. You can use it as a static helper or as an instance.
 *
 * It contains a map of timers that are started and stopped by label. The label is used to identify the timer.
 */
public class Timer {

	/**
	 * The default timing format
	 */
	public static final DecimalFormat	TIMING_FORMAT		= new DecimalFormat( "#.##" );

	/**
	 * The timers map if used as an instance
	 */
	private Map<String, Long>			timers				= new ConcurrentHashMap<>( 32 );

	/**
	 * The number of nanoseconds in a millisecond
	 */
	private static final long			NANO_2_MILLIS		= 1000000L;

	/**
	 * The number of milliseconds in a second
	 */
	private static final long			MILLIS_2_SECONDS	= 1000;

	/**
	 * Whether or not to auto remove timers when they are stopped
	 */
	private boolean						autoRemoveTimers;

	/**
	 * The time units it supports
	 */
	public enum TimeUnit {
		SECONDS,
		MILLISECONDS,
		NANOSECONDS
	}

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Create a new timer instance with auto remove timers set to true
	 */
	public Timer() {
		this.autoRemoveTimers = true;
	}

	/**
	 * Create a new timer instance
	 *
	 * @param autoRemoveTimers Whether or not to auto remove timers when they are stopped
	 */
	public Timer( boolean autoRemoveTimers ) {
		this.autoRemoveTimers = autoRemoveTimers;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Static Helpers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Create a new timer instance with an auto remove timers option
	 *
	 * @param autoRemoveTimers Whether or not to auto remove timers when they are stopped
	 *
	 * @return The timer instance
	 */
	public static Timer create( boolean autoRemoveTimers ) {
		return new Timer( autoRemoveTimers );
	}

	/**
	 * Create a new timer instance with auto remove timers set to true
	 *
	 * @return The timer instance
	 */
	public static Timer create() {
		return new Timer();
	}

	/**
	 * Time the given runnable lambda, print the elapsed time in the default of milliseconds
	 * to the console, and return the elapsed time in the given time unit as long
	 *
	 * @param runnable The runnable lambda
	 */
	public static long timeAndPrint( Runnable runnable ) {
		return timeAndPrint( runnable, null, TimeUnit.MILLISECONDS );
	}

	/**
	 * Time the given runnable lambda, print the elapsed time in the default of milliseconds
	 * to the console, and return the elapsed time in the given time unit as long
	 *
	 * @param runnable The runnable lambda
	 * @param label    The label to use for the timer output
	 */
	public static long timeAndPrint( Runnable runnable, String label ) {
		return timeAndPrint( runnable, label, TimeUnit.MILLISECONDS );
	}

	/**
	 * Time the given runnable lambda, print the elapsed time in the given time unit
	 * to the console, and return the elapsed time in the given time unit as long
	 *
	 * @param runnable The runnable lambda
	 * @param label    The label to use for the timer output
	 * @param timeUnit The time unit to return. Allowed values are SECONDS, MILLISECONDS, and NANOSECONDS
	 */
	public static long timeAndPrint( Runnable runnable, String label, TimeUnit timeUnit ) {
		// if label is null, just use "Lambda"
		if ( label == null ) {
			label = "Lambda";
		}

		var start = System.nanoTime();
		runnable.run();
		long elapsed = System.nanoTime() - start;

		System.out.println( label + " took: " + TIMING_FORMAT.format( convert( elapsed, timeUnit ) ) + " " + timeUnit );

		return elapsed;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Timer Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get all timers
	 *
	 * @return The timers map
	 */
	public Map<String, Long> getTimers() {
		return timers;
	}

	/**
	 * Clear all timers
	 */
	public void clearTimers() {
		timers.clear();
	}

	/**
	 * This convenience methods prints out to the console all the timers
	 * using the passed timeunit
	 */
	public void printTimers( TimeUnit timeUnit ) {
		System.out.println( "|----------------------|----------------------|" );
		System.out.println( "| Timer                | Duration             |" );
		System.out.println( "|----------------------|----------------------|" );

		for ( Map.Entry<String, Long> entry : timers.entrySet() ) {
			String	key			= entry.getKey();
			long	duration	= entry.getValue();

			// Adjust the column widths based on your data
			System.out.printf(
			    "| %-20s | %-20s |%n",
			    key,
			    TIMING_FORMAT.format( convert( duration, timeUnit ) )
			);
		}

		System.out.println( "|----------------------|----------------------|" );
		System.out.println( "" );
		System.out.println( "Duration = " + timeUnit );
	}

	/**
	 * This convenience methods prints out to the console all the timers
	 * using the passed milliseconds
	 */
	public void printTimers() {
		printTimers( TimeUnit.MILLISECONDS );
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
		return stopAndGetMillis( label ) / MILLIS_2_SECONDS;
	}

	/**
	 * Stop the timer with the given label and return the elapsed time in milliseconds
	 *
	 * @param label The label
	 *
	 * @return The elapsed time in milliseconds
	 */
	public long stopAndGetMillis( String label ) {
		return stopAndGetNanos( label ) / NANO_2_MILLIS;
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
			throw new BoxRuntimeException( "Timer '" + label + "' not started." );
		}

		long	endTime		= System.nanoTime();
		long	startTime	= timers.get( label );
		long	duration	= endTime - startTime;

		if ( this.autoRemoveTimers ) {
			timers.remove( label );
		} else {
			// Store the final timer value if not in auto-remove mode
			timers.put( label, duration );
		}

		return duration;
	}

	/**
	 * Stop the timer with the given label and return the elapsed time in the given time unit
	 *
	 * @param label    The label
	 * @param timeUnit The time unit to return. Allowed values are SECONDS, MILLISECONDS, and NANOSECONDS
	 *
	 * @return The elapsed time in the given time unit as a string: 123.45 seconds
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
				throw new BoxRuntimeException( "Unsupported time unit: " + timeUnit );
		}
	}

	/**
	 * Stop the timer with the given label and return the elapsed time in the default of milliseconds
	 *
	 * @param label The label
	 *
	 * @return The elapsed time in milliseconds as a string: 123.45 milliseconds
	 */
	public String stop( String label ) {
		return stop( label, TimeUnit.MILLISECONDS );
	}

	/**
	 * Stop the timer with the given label, print the elapsed time in the default of milliseconds
	 *
	 * @param label The label
	 *
	 * @return The timer instance
	 */
	public Timer stopAndPrint( String label ) {
		return stopAndPrint( label, TimeUnit.MILLISECONDS );
	}

	/**
	 * Stop the timer with the given label, print the elapsed time in the default of milliseconds
	 *
	 * @param label The label
	 *
	 * @return The timer instance
	 */
	public Timer stopAndPrint( String label, TimeUnit timeUnit ) {
		System.out.println(
		    label + " took: " + stop( label, timeUnit )
		);
		return this;
	}

	/**
	 * This method receives a long time in nanoseconds
	 * and returns it in the appropriate time unit
	 *
	 * @param time     The time in nanoseconds
	 * @param timeUnit The time unit to return. Allowed values are SECONDS, MILLISECONDS, and NANOSECONDS
	 *
	 * @return The time in the given time unit
	 */
	public static long convert( long time, TimeUnit timeUnit ) {
		switch ( timeUnit ) {
			case SECONDS :
				return time / NANO_2_MILLIS / MILLIS_2_SECONDS;

			case MILLISECONDS :
				return time / NANO_2_MILLIS;

			case NANOSECONDS :
				return time;

			default :
				throw new BoxRuntimeException( "Unsupported time unit: " + timeUnit );
		}
	}
}
