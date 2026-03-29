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
package ortus.boxlang.runtime.async;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Parses and evaluates cron expressions for use with the BoxLang scheduler.
 *
 * Supports both:
 * <ul>
 * <li>5-field Unix cron: {@code minute hour day-of-month month day-of-week}</li>
 * <li>6-field Quartz cron: {@code second minute hour day-of-month month day-of-week}</li>
 * </ul>
 *
 * An optional 7th field (year) is silently ignored.
 *
 * Special characters:
 * <ul>
 * <li>{@code *} — any value</li>
 * <li>{@code ?} — no specific value (DOM or DOW only)</li>
 * <li>{@code -} — range: {@code 1-5}</li>
 * <li>{@code ,} — list: {@code 1,3,5}</li>
 * <li>{@code /} — step: {@code 0/15}, {@code *}{@code /5}</li>
 * <li>{@code L} — last day of month (DOM field only)</li>
 * </ul>
 *
 * Month names: JAN FEB MAR APR MAY JUN JUL AUG SEP OCT NOV DEC
 * Day names: SUN MON TUE WED THU FRI SAT (0=SUN internally)
 */
public class CronExpression {

	// --------------------------------------------------------------------------
	// Name mappings
	// --------------------------------------------------------------------------

	private static final Map<String, Integer> MONTH_NAMES = Map.ofEntries(
	    Map.entry( "JAN", 1 ), Map.entry( "FEB", 2 ), Map.entry( "MAR", 3 ),
	    Map.entry( "APR", 4 ), Map.entry( "MAY", 5 ), Map.entry( "JUN", 6 ),
	    Map.entry( "JUL", 7 ), Map.entry( "AUG", 8 ), Map.entry( "SEP", 9 ),
	    Map.entry( "OCT", 10 ), Map.entry( "NOV", 11 ), Map.entry( "DEC", 12 )
	);

	// 0=SUN, 1=MON, ..., 6=SAT (Java DayOfWeek: MON=1 ... SUN=7, so we normalize)
	private static final Map<String, Integer> DOW_NAMES = Map.of(
	    "SUN", 0, "MON", 1, "TUE", 2, "WED", 3, "THU", 4, "FRI", 5, "SAT", 6
	);

	// --------------------------------------------------------------------------
	// Fields
	// --------------------------------------------------------------------------

	private final String		expression;
	private final boolean		hasSeconds;
	private final CronField		secondsField;
	private final CronField		minutesField;
	private final CronField		hoursField;
	private final CronField		domField;
	private final CronField		monthsField;
	private final CronField		dowField;
	private final boolean		domRestricted;
	private final boolean		dowRestricted;

	// --------------------------------------------------------------------------
	// Constructor (private — use parse())
	// --------------------------------------------------------------------------

	private CronExpression( String expression, boolean hasSeconds,
	    CronField secondsField, CronField minutesField, CronField hoursField,
	    CronField domField, CronField monthsField, CronField dowField ) {
		this.expression		= expression;
		this.hasSeconds		= hasSeconds;
		this.secondsField	= secondsField;
		this.minutesField	= minutesField;
		this.hoursField		= hoursField;
		this.domField		= domField;
		this.monthsField	= monthsField;
		this.dowField		= dowField;
		this.domRestricted	= domField != null && domField.values != null && !domField.lastDay;
		this.dowRestricted	= dowField != null && dowField.values != null;
	}

	// --------------------------------------------------------------------------
	// Factory
	// --------------------------------------------------------------------------

	/**
	 * Parse a cron expression string.
	 *
	 * @param expression The cron expression (5, 6, or 7 fields)
	 *
	 * @return A new CronExpression instance
	 *
	 * @throws BoxRuntimeException if the expression is malformed
	 */
	public static CronExpression parse( String expression ) {
		if ( expression == null || expression.isBlank() ) {
			throw new BoxRuntimeException( "Cron expression cannot be null or empty" );
		}

		String[] parts = expression.trim().split( "\\s+" );

		boolean hasSeconds;
		int		offset;

		if ( parts.length == 5 ) {
			// Unix 5-field: prepend a "0" for seconds
			hasSeconds	= false;
			offset		= -1; // signals "no seconds field"
		} else if ( parts.length == 6 ) {
			hasSeconds	= true;
			offset		= 0;
		} else if ( parts.length == 7 ) {
			// Quartz with optional year — ignore year
			hasSeconds	= true;
			offset		= 0;
		} else {
			throw new BoxRuntimeException(
			    "Invalid cron expression [" + expression + "]: expected 5, 6, or 7 fields, got " + parts.length
			);
		}

		try {
			CronField secondsField;
			CronField minutesField;
			CronField hoursField;
			CronField domField;
			CronField monthsField;
			CronField dowField;

			if ( !hasSeconds ) {
				// 5-field: min hour dom month dow
				secondsField	= new CronField( "0", 0, 59, null );   // always second 0
				minutesField	= new CronField( parts[ 0 ], 0, 59, null );
				hoursField		= new CronField( parts[ 1 ], 0, 23, null );
				domField		= new CronField( parts[ 2 ], 1, 31, null );
				monthsField		= new CronField( parts[ 3 ], 1, 12, MONTH_NAMES );
				dowField		= new CronField( parts[ 4 ], 0, 6, DOW_NAMES );
			} else {
				// 6-field: sec min hour dom month dow
				secondsField	= new CronField( parts[ 0 ], 0, 59, null );
				minutesField	= new CronField( parts[ 1 ], 0, 59, null );
				hoursField		= new CronField( parts[ 2 ], 0, 23, null );
				domField		= new CronField( parts[ 3 ], 1, 31, null );
				monthsField		= new CronField( parts[ 4 ], 1, 12, MONTH_NAMES );
				dowField		= new CronField( parts[ 5 ], 0, 6, DOW_NAMES );
			}

			return new CronExpression( expression, hasSeconds,
			    secondsField, minutesField, hoursField, domField, monthsField, dowField );
		} catch ( Exception e ) {
			throw new BoxRuntimeException( "Invalid cron expression [" + expression + "]: " + e.getMessage(), e );
		}
	}

	// --------------------------------------------------------------------------
	// Core API
	// --------------------------------------------------------------------------

	/**
	 * Returns true if the given LocalDateTime matches this cron expression.
	 *
	 * @param dt The date/time to test (seconds are considered for 6-field expressions)
	 *
	 * @return true if the datetime matches
	 */
	public boolean matches( LocalDateTime dt ) {
		// Seconds
		if ( hasSeconds ) {
			if ( !secondsField.matches( dt.getSecond() ) )
				return false;
		} else {
			// 5-field: only fire at second 0
			if ( dt.getSecond() != 0 )
				return false;
		}

		// Minutes / Hours / Month
		if ( !minutesField.matches( dt.getMinute() ) )
			return false;
		if ( !hoursField.matches( dt.getHour() ) )
			return false;
		if ( !monthsField.matches( dt.getMonthValue() ) )
			return false;

		// DOM / DOW
		boolean domMatch = domField.lastDay
		    ? dt.getDayOfMonth() == dt.toLocalDate().lengthOfMonth()
		    : domField.values == null || domField.values.contains( dt.getDayOfMonth() );

		// Java DayOfWeek: MON=1 ... SUN=7; normalize to 0=SUN, 1=MON ... 6=SAT
		int javaDow = dt.getDayOfWeek().getValue(); // 1=MON ... 7=SUN
		int cronDow = javaDow % 7;                  // MON=1, ..., SAT=6, SUN=0
		boolean dowMatch = dowField.values == null || dowField.values.contains( cronDow );

		boolean domLastDay = domField.lastDay;

		if ( domRestricted && dowRestricted ) {
			// Quartz OR semantics: either DOM or DOW must match
			return domMatch || dowMatch;
		} else if ( domRestricted || domLastDay ) {
			return domMatch;
		} else if ( dowRestricted ) {
			return dowMatch;
		} else {
			// Both are wildcards — day always matches
			return true;
		}
	}

	/**
	 * Computes the next fire time after the given {@code from} datetime.
	 * Uses brute-force iteration (1-second or 1-minute increments) up to 4 years ahead.
	 *
	 * @param from The reference datetime (exclusive — the next fire time will be strictly after this)
	 *
	 * @return An Optional containing the next fire time, or empty if none found within 4 years
	 */
	public Optional<LocalDateTime> nextFireTime( LocalDateTime from ) {
		LocalDateTime	candidate	= hasSeconds
		    ? from.plusSeconds( 1 ).truncatedTo( ChronoUnit.SECONDS )
		    : from.plusMinutes( 1 ).truncatedTo( ChronoUnit.MINUTES );

		LocalDateTime	limit		= from.plusYears( 4 );

		while ( candidate.isBefore( limit ) ) {
			if ( matches( candidate ) ) {
				return Optional.of( candidate );
			}
			candidate = hasSeconds ? candidate.plusSeconds( 1 ) : candidate.plusMinutes( 1 );
		}

		return Optional.empty();
	}

	/**
	 * Returns the number of milliseconds from now (in the given timezone) to the next fire time.
	 * Returns 0 if no next fire time is found within 4 years.
	 *
	 * @param timezone The timezone to use for "now"
	 *
	 * @return milliseconds until next fire, or 0
	 */
	public long nextFireDelayMillis( ZoneId timezone ) {
		LocalDateTime now = LocalDateTime.now( timezone );
		return nextFireTime( now )
		    .map( next -> ChronoUnit.MILLIS.between( now, next ) )
		    .orElse( 0L );
	}

	/**
	 * Returns true if this is a 6-field (seconds-level) cron expression.
	 *
	 * @return true for 6-field, false for 5-field
	 */
	public boolean isSecondsField() {
		return hasSeconds;
	}

	/**
	 * Returns the original cron expression string.
	 *
	 * @return the expression string
	 */
	public String getExpression() {
		return expression;
	}

	@Override
	public String toString() {
		return "CronExpression[" + expression + "]";
	}

	// --------------------------------------------------------------------------
	// CronField inner class
	// --------------------------------------------------------------------------

	/**
	 * Represents one field of a cron expression (e.g., minutes, hours, DOM).
	 * Holds the expanded set of matching integer values, or null for wildcard.
	 */
	static final class CronField {

		/** Expanded set of matching values, or null for "any" (wildcard). */
		final Set<Integer>	values;

		/** True if this field is the L (last day of month) specifier. */
		final boolean		lastDay;

		/**
		 * Parse a single cron field token.
		 *
		 * @param field     The raw token string
		 * @param min       Minimum allowed value for this field
		 * @param max       Maximum allowed value for this field
		 * @param nameMap   Optional name-to-int mapping (months, days of week); may be null
		 */
		CronField( String field, int min, int max, Map<String, Integer> nameMap ) {
			if ( field == null ) {
				this.values		= null;
				this.lastDay	= false;
				return;
			}

			String upper = field.toUpperCase();

			// Wildcard
			if ( upper.equals( "*" ) || upper.equals( "?" ) ) {
				this.values		= null;
				this.lastDay	= false;
				return;
			}

			// Last-day-of-month
			if ( upper.equals( "L" ) ) {
				this.values		= null;
				this.lastDay	= true;
				return;
			}

			this.lastDay = false;

			Set<Integer> result = new HashSet<>();

			// Split on comma for lists
			for ( String part : upper.split( "," ) ) {
				if ( part.contains( "/" ) ) {
					// Step expression: base/step
					String[]	stepParts	= part.split( "/" );
					if ( stepParts.length != 2 ) {
						throw new BoxRuntimeException( "Invalid step expression: " + part );
					}
					int	stepVal	= Integer.parseInt( stepParts[ 1 ] );
					int	start;
					if ( stepParts[ 0 ].equals( "*" ) ) {
						start = min;
					} else if ( stepParts[ 0 ].contains( "-" ) ) {
						String[] rangeParts = stepParts[ 0 ].split( "-" );
						start = resolveValue( rangeParts[ 0 ], nameMap );
						int end = resolveValue( rangeParts[ 1 ], nameMap );
						for ( int i = start; i <= end; i += stepVal ) {
							result.add( i );
						}
						continue;
					} else {
						start = resolveValue( stepParts[ 0 ], nameMap );
					}
					for ( int i = start; i <= max; i += stepVal ) {
						result.add( i );
					}
				} else if ( part.contains( "-" ) ) {
					// Range expression: start-end
					String[] rangeParts = part.split( "-" );
					if ( rangeParts.length != 2 ) {
						throw new BoxRuntimeException( "Invalid range expression: " + part );
					}
					int	rangeStart	= resolveValue( rangeParts[ 0 ], nameMap );
					int	rangeEnd	= resolveValue( rangeParts[ 1 ], nameMap );
					for ( int i = rangeStart; i <= rangeEnd; i++ ) {
						result.add( i );
					}
				} else {
					// Single value — also handle 7=SUN alias for DOW
					int val = resolveValue( part, nameMap );
					if ( val == 7 && max == 6 ) {
						val = 0; // 7 is an alias for SUN (0)
					}
					result.add( val );
				}
			}

			this.values = result;
		}

		private static int resolveValue( String token, Map<String, Integer> nameMap ) {
			if ( nameMap != null ) {
				Integer mapped = nameMap.get( token.toUpperCase() );
				if ( mapped != null )
					return mapped;
			}
			return Integer.parseInt( token );
		}

		boolean matches( int value ) {
			return values == null || values.contains( value );
		}
	}
}
