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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import ortus.boxlang.runtime.dynamic.casters.BigDecimalCaster;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.operators.StringCompare;

/**
 * A wrapper that normalizes a value for use in hash-based collections (like {@link BoxSet})
 * so that equality and hashCode follow BoxLang's type semantics rather than Java's
 * {@code Object.equals()}.
 *
 * <p>
 * Normalization categories:
 * <ul>
 * <li><b>STRING</b> — java.lang.String, Character, char[] → compared case-insensitively</li>
 * <li><b>NUMERIC</b> — any java.lang.Number subclass → compared by numeric value via BigDecimal</li>
 * <li><b>DATETIME</b> — DateTime, ZonedDateTime, Calendar, LocalDateTime, LocalDate, etc. → compared by epoch millis</li>
 * <li><b>ARRAY</b> — BoxLang Array or any java.util.List → element-wise BoxLang equality</li>
 * <li><b>STRUCT</b> — BoxLang Struct or any java.util.Map → entry-wise BoxLang equality</li>
 * <li><b>ANY</b> — delegates directly to the value's native equals/hashCode</li>
 * </ul>
 *
 * <p>
 * HashCodes are cached for immutable categories (STRING, NUMERIC, DATETIME).
 */
public class NormalizedValue {

	/**
	 * --------------------------------------------------------------------------
	 * Instance fields
	 * --------------------------------------------------------------------------
	 */

	/** The BoxLangType we've normalized to */
	private final BoxLangType	boxLangType;

	/** The original value as passed in */
	private final Object		originalValue;

	/** The normalized representation used for equals/hashCode */
	private final Object		normalizedValue;

	/** Cached hashCode — non-null for immutable types (STRING, NUMERIC, DATETIME), null otherwise */
	private final Integer		hashCode;

	/** Whether string comparisons are case-sensitive (default: false = case-insensitive) */
	private final boolean		caseSensitive;

	/**
	 * --------------------------------------------------------------------------
	 * Constructor
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Create a new NormalizedValue wrapping the given object with case-insensitive string comparison.
	 *
	 * @param value The value to normalize
	 */
	public NormalizedValue( Object value ) {
		this( value, false );
	}

	/**
	 * Create a new NormalizedValue wrapping the given object.
	 *
	 * @param value         The value to normalize
	 * @param caseSensitive Whether string comparisons should be case-sensitive (default: false)
	 */
	public NormalizedValue( Object value, boolean caseSensitive ) {
		this.originalValue	= value;
		this.caseSensitive	= caseSensitive;

		if ( value == null ) {
			this.boxLangType		= BoxLangType.ANY;
			this.normalizedValue	= null;
			this.hashCode			= 0;
			return;
		}

		// Attempt categorization in order of likelihood/cost

		// 1. String/Simple: String, Character, char[]
		if ( value instanceof String str ) {
			this.boxLangType		= BoxLangType.STRING;
			this.normalizedValue	= caseSensitive ? str : str.toLowerCase();
			this.hashCode			= this.normalizedValue.hashCode();
			return;
		}
		if ( value instanceof Character ch ) {
			this.boxLangType		= BoxLangType.STRING;
			this.normalizedValue	= caseSensitive ? ch.toString() : Character.toLowerCase( ch.charValue() ) + "";
			this.hashCode			= this.normalizedValue.hashCode();
			return;
		}
		if ( value instanceof char[] chars ) {
			this.boxLangType = BoxLangType.STRING;
			String strVal = new String( chars );
			this.normalizedValue	= caseSensitive ? strVal : strVal.toLowerCase();
			this.hashCode			= this.normalizedValue.hashCode();
			return;
		}

		// 2. Numeric: any Number subclass
		if ( value instanceof Number num ) {
			this.boxLangType		= BoxLangType.NUMERIC;
			this.normalizedValue	= toBigDecimal( num );
			this.hashCode			= computeNumericHashCode( ( BigDecimal ) this.normalizedValue );
			return;
		}

		// 3. DateTime: known date classes
		if ( DateTimeCaster.isKnownDateClass( value ) ) {
			this.boxLangType = BoxLangType.DATETIME;
			DateTime dt = DateTimeCaster.cast( value );
			this.normalizedValue	= dt.toEpochMillis();
			this.hashCode			= Long.hashCode( ( long ) this.normalizedValue );
			return;
		}

		// 4. Array: BoxLang Array or any Java List
		if ( value instanceof List<?> ) {
			this.boxLangType		= BoxLangType.ARRAY;
			this.normalizedValue	= value;
			this.hashCode			= null;
			return;
		}

		// 5. Struct: BoxLang Struct/IStruct or any Java Map
		if ( value instanceof Map<?, ?> ) {
			this.boxLangType		= BoxLangType.STRUCT;
			this.normalizedValue	= value;
			this.hashCode			= null;
			return;
		}

		// 6. ANY: delegate directly
		this.boxLangType		= BoxLangType.ANY;
		this.normalizedValue	= value;
		this.hashCode			= null;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Public API
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Factory method to create a new NormalizedValue with case-insensitive string comparison.
	 *
	 * @param value The value to normalize
	 *
	 * @return A new NormalizedValue instance
	 */
	public static NormalizedValue of( Object value ) {
		return new NormalizedValue( value );
	}

	/**
	 * Factory method to create a new NormalizedValue.
	 *
	 * @param value         The value to normalize
	 * @param caseSensitive Whether string comparisons should be case-sensitive
	 *
	 * @return A new NormalizedValue instance
	 */
	public static NormalizedValue of( Object value, boolean caseSensitive ) {
		return new NormalizedValue( value, caseSensitive );
	}

	/**
	 * @return The BoxLangType we've normalized to
	 */
	public BoxLangType getBoxLangType() {
		return this.boxLangType;
	}

	/**
	 * @return The original unwrapped value
	 */
	public Object getOriginalValue() {
		return this.originalValue;
	}

	/**
	 * @return The normalized representation used internally for equality
	 */
	public Object getNormalizedValue() {
		return this.normalizedValue;
	}

	/**
	 * --------------------------------------------------------------------------
	 * equals / hashCode
	 * --------------------------------------------------------------------------
	 */

	@Override
	public int hashCode() {
		if ( this.hashCode != null ) {
			return this.hashCode;
		}
		// Non-cached types: delegate to original object
		return this.originalValue == null ? 0 : this.originalValue.hashCode();
	}

	@Override
	public boolean equals( Object obj ) {
		if ( this == obj ) {
			return true;
		}
		if ( obj == null || ! ( obj instanceof NormalizedValue other ) ) {
			return false;
		}

		// null handling
		if ( this.originalValue == null && other.originalValue == null ) {
			return true;
		}
		if ( this.originalValue == null || other.originalValue == null ) {
			return false;
		}

		// If types match, compare within type
		if ( this.boxLangType == other.boxLangType ) {
			return switch ( this.boxLangType ) {
				case STRING -> StringCompare.invoke( ( String ) this.normalizedValue, ( String ) other.normalizedValue, this.caseSensitive ) == 0;
				case NUMERIC -> ( ( BigDecimal ) this.normalizedValue ).compareTo( ( BigDecimal ) other.normalizedValue ) == 0;
				case DATETIME -> ( ( long ) this.normalizedValue ) == ( ( long ) other.normalizedValue );
				default -> {
					// If both are Comparable and type-compatible, use compareTo
					if ( this.normalizedValue instanceof Comparable lc && other.normalizedValue instanceof Comparable rc
					    && ( this.normalizedValue.getClass().isAssignableFrom( other.normalizedValue.getClass() )
					        || other.normalizedValue.getClass().isAssignableFrom( this.normalizedValue.getClass() ) ) ) {
						yield ( ( Comparable ) this.normalizedValue ).compareTo( other.normalizedValue ) == 0;
					}
					yield this.normalizedValue.equals( other.normalizedValue );
				}
			};
		}

		// Different normalized types are never equal
		return false;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Object overrides
	 * --------------------------------------------------------------------------
	 */

	@Override
	public String toString() {
		return this.originalValue == null ? "null" : this.originalValue.toString();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Private helpers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Convert any Number to BigDecimal for consistent numeric comparison.
	 */
	private static BigDecimal toBigDecimal( Number num ) {
		if ( num instanceof BigDecimal bd ) {
			return bd.stripTrailingZeros();
		}
		return BigDecimalCaster.cast( num ).stripTrailingZeros();
	}

	/**
	 * Compute a stable hashCode for a numeric BigDecimal value.
	 * We strip trailing zeros so that 1.0 and 1.00 have the same hash,
	 * then use the stripped representation's hashCode.
	 */
	private static int computeNumericHashCode( BigDecimal bd ) {
		// BigDecimal.hashCode() is NOT consistent with compareTo (scale-sensitive).
		// Use Double.hashCode for values in double range, else use unscaled + scale.
		if ( bd.precision() <= 15 ) {
			return Double.hashCode( bd.doubleValue() );
		}
		// For very high-precision values, use string representation of stripped form
		return bd.toPlainString().hashCode();
	}

}
