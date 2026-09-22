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

import java.io.Serializable;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;
import ortus.boxlang.runtime.dynamic.casters.GenericCaster;
import ortus.boxlang.runtime.dynamic.casters.NumberCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.interop.DynamicInteropService;
import ortus.boxlang.runtime.operators.Compare;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.RangeMeta;
import ortus.boxlang.runtime.types.util.TypeUtil;

/**
 * Immutable inclusive range value supporting arbitrary types.
 *
 * A range can be:
 * <ul>
 * <li>Fully iterable — when a stepper function is available and {@code from} is present</li>
 * <li>Contains-only — when only a comparator is available (no stepper)</li>
 * </ul>
 *
 * Copy-on-write semantics: {@link #asc()}, {@link #desc()}, and {@link #step(Number)}
 * return new Range instances.
 *
 * @param <T> the type of values in this range
 */
@SuppressWarnings( "unchecked" )
public class Range<T> implements IType, IReferenceable, Iterable<T>, Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Pre-computed element category for fast coercion dispatch.
	 */
	private enum ElementCategory {
		NUMBER, STRING, CHARACTER, IRANGEABLE, OTHER
	}

	private final T							from;
	private final T							to;
	private final T							low;
	private final T							high;
	private final Number					step;
	private final String					unit;
	private final boolean					ascending;
	private final boolean					fromExclusive;
	private final boolean					toExclusive;
	private final Class<?>					elementType;
	private final ElementCategory			elementCategory;
	private final String					elementTypeName;
	private final boolean					strictTypeCheck;
	private final Comparator<T>				comparator;
	private final BiFunction<T, Number, T>	stepper;
	private final boolean					hasDirectionStepConflict;
	private final int						hashCode;

	private transient BoxMeta<?>			$bx;

	/**
	 * Full constructor.
	 *
	 * @param from       the start of the range (nullable — no start means not iterable)
	 * @param to         the end of the range (nullable — no end means open-ended/infinite)
	 * @param step       the step amount (defaults to 1 if null)
	 * @param comparator the comparator for ordering/contains checks (required)
	 * @param stepper    the function to advance values (nullable — no stepper means contains-only)
	 */
	public Range( T from, T to, Number step, Comparator<T> comparator, BiFunction<T, Number, T> stepper ) {
		this( from, to, step, null, false, false, comparator, stepper );
	}

	/**
	 * Full constructor with unit.
	 *
	 * @param from       the start of the range (nullable — no start means not iterable)
	 * @param to         the end of the range (nullable — no end means open-ended/infinite)
	 * @param step       the step amount (defaults to 1 if null)
	 * @param unit       the unit name for display purposes (nullable)
	 * @param comparator the comparator for ordering/contains checks (required)
	 * @param stepper    the function to advance values (nullable — no stepper means contains-only)
	 */
	public Range( T from, T to, Number step, String unit, Comparator<T> comparator, BiFunction<T, Number, T> stepper ) {
		this( from, to, step, unit, false, false, comparator, stepper );
	}

	/**
	 * Full constructor with unit and exclusivity.
	 *
	 * @param from          the start of the range (nullable — no start means not iterable)
	 * @param to            the end of the range (nullable — no end means open-ended/infinite)
	 * @param step          the step amount (defaults to 1 if null)
	 * @param unit          the unit name for display purposes (nullable)
	 * @param fromExclusive whether the start bound is exclusive
	 * @param toExclusive   whether the end bound is exclusive
	 * @param comparator    the comparator for ordering/contains checks (required)
	 * @param stepper       the function to advance values (nullable — no stepper means contains-only)
	 */
	public Range( T from, T to, Number step, String unit, boolean fromExclusive, boolean toExclusive, Comparator<T> comparator,
	    BiFunction<T, Number, T> stepper ) {
		this( from, to, step, unit, fromExclusive, toExclusive, comparator, stepper, null, null, null, false );
	}

	/**
	 * Private full constructor with type override fields.
	 * Used internally by copy-on-write methods like {@link #type(String)} and {@link #type(Class)}.
	 *
	 * @param from            the start of the range (nullable)
	 * @param to              the end of the range (nullable)
	 * @param step            the step amount (defaults to 1 if null)
	 * @param unit            the unit name (nullable)
	 * @param fromExclusive   whether the start bound is exclusive
	 * @param toExclusive     whether the end bound is exclusive
	 * @param comparator      the comparator for ordering/contains checks (required)
	 * @param stepper         the function to advance values (nullable)
	 * @param elementType     explicit element type override (nullable — inferred from bounds if null)
	 * @param elementCategory explicit category override (nullable — computed from elementType if null)
	 * @param elementTypeName string type name for GenericCaster checks (nullable)
	 * @param strictTypeCheck true when type(Class) is used — only allows exact instanceof matches
	 */
	@SuppressWarnings( "null" )
	private Range( T from, T to, Number step, String unit, boolean fromExclusive, boolean toExclusive, Comparator<T> comparator,
	    BiFunction<T, Number, T> stepper, Class<?> elementType, ElementCategory elementCategory, String elementTypeName, boolean strictTypeCheck ) {
		Objects.requireNonNull( comparator, "Range comparator cannot be null" );

		this.from			= from;
		this.to				= to;
		this.fromExclusive	= fromExclusive;
		this.toExclusive	= toExclusive;
		this.comparator		= comparator;
		this.step			= step != null ? step : ( from != null && to != null && comparator.compare( from, to ) > 0 ? -1 : 1 );
		if ( this.step.doubleValue() == 0d ) {
			throw new BoxRuntimeException( "Range step cannot be 0." );
		}
		this.unit						= unit;
		this.ascending					= this.step.doubleValue() > 0;
		this.hasDirectionStepConflict	= hasDirectionStepConflict( from, to, this.ascending, comparator, stepper );
		this.elementTypeName			= elementTypeName;
		this.strictTypeCheck			= strictTypeCheck;

		// Use explicit type if provided, otherwise infer from bounds
		if ( elementType != null ) {
			this.elementType		= elementType;
			this.elementCategory	= elementCategory != null ? elementCategory : categorize( elementType );
		} else {
			this.elementType		= from != null ? from.getClass() : ( to != null ? to.getClass() : Object.class );
			this.elementCategory	= categorize( this.elementType );
		}

		this.stepper	= stepper;
		this.hashCode	= computeHashCode();

		// Pre-compute sorted bounds for fast contains checks
		if ( from != null && to != null ) {
			if ( comparator.compare( from, to ) <= 0 ) {
				this.low	= from;
				this.high	= to;
			} else {
				this.low	= to;
				this.high	= from;
			}
		} else if ( from != null ) {
			// Only from — it's a lower bound if ascending, upper if descending
			this.low	= this.ascending ? from : null;
			this.high	= this.ascending ? null : from;
		} else if ( to != null ) {
			// Only to — it's an upper bound if ascending, lower if descending
			this.low	= this.ascending ? null : to;
			this.high	= this.ascending ? to : null;
		} else {
			this.low	= null;
			this.high	= null;
		}
	}

	/**
	 * Convenience constructor defaulting step to 1.
	 *
	 * @param from       the start of the range
	 * @param to         the end of the range
	 * @param comparator the comparator for ordering/contains checks
	 * @param stepper    the function to advance values
	 */
	public Range( T from, T to, Comparator<T> comparator, BiFunction<T, Number, T> stepper ) {
		this( from, to, null, null, false, false, comparator, stepper );
	}

	/**
	 * Convenience constructor for contains-only ranges (no stepper).
	 *
	 * @param from       the start of the range
	 * @param to         the end of the range
	 * @param comparator the comparator for ordering/contains checks
	 */
	public Range( T from, T to, Comparator<T> comparator ) {
		this( from, to, null, null, false, false, comparator, null );
	}

	// ======================== Accessors ========================

	/**
	 * Get the start (lower) bound of this range.
	 *
	 * @return the from value, or null if open-start
	 */
	public T getFrom() {
		return this.from;
	}

	/**
	 * Get the end (upper) bound of this range.
	 *
	 * @return the to value, or null if open-end
	 */
	public T getTo() {
		return this.to;
	}

	/**
	 * Get the step value for iteration.
	 *
	 * @return the step amount
	 */
	public Number getStep() {
		return this.step;
	}

	/**
	 * Returns true if this range can be iterated (has a start value and a stepper).
	 *
	 * @return true if iterable
	 */
	public boolean isIterable() {
		return this.from != null && this.stepper != null;
	}

	/**
	 * Returns true if both bounds are present (closed/bounded range).
	 *
	 * @return true if both from and to are non-null
	 */
	public boolean isBounded() {
		return this.from != null && this.to != null;
	}

	/**
	 * Returns true if neither bound is present (fully open/unbounded range).
	 *
	 * @return true if both from and to are null
	 */
	public boolean isUnbounded() {
		return this.from == null && this.to == null;
	}

	/**
	 * Returns true if exactly one bound is present (half-open range).
	 *
	 * @return true if exactly one of from/to is null
	 */
	public boolean isHalfBounded() {
		return ( this.from == null ) != ( this.to == null );
	}

	/**
	 * Returns true if this range has a lower bound (from).
	 *
	 * @return true if from is non-null
	 */
	public boolean hasFrom() {
		return this.from != null;
	}

	/**
	 * Returns true if this range has an upper bound (to).
	 *
	 * @return true if to is non-null
	 */
	public boolean hasTo() {
		return this.to != null;
	}

	/**
	 * Returns true if the start (from) bound is exclusive.
	 *
	 * @return true if from is exclusive
	 */
	public boolean isFromExclusive() {
		return this.fromExclusive;
	}

	/**
	 * Returns true if the end (to) bound is exclusive.
	 *
	 * @return true if to is exclusive
	 */
	public boolean isToExclusive() {
		return this.toExclusive;
	}

	/**
	 * Returns true if this range contains no elements.
	 * A range is empty if it is not iterable, or if the starting value is already
	 * past the ending value in the direction of iteration.
	 *
	 * @return true if this range has zero elements
	 */
	public boolean isEmpty() {
		if ( !isIterable() ) {
			return true;
		}
		if ( this.to == null ) {
			return false;
		}
		return isIterationEmpty( this.from, this.to, this.fromExclusive, this.toExclusive, this.ascending, this.comparator, this.stepper );
	}

	/**
	 * Returns true if this range progresses in ascending order.
	 *
	 * @return true if ascending
	 */
	public boolean isAscending() {
		if ( this.from == null || this.to == null ) {
			return this.ascending;
		}
		return this.comparator.compare( this.from, this.to ) <= 0;
	}

	// ======================== Copy-on-write modifiers ========================

	/**
	 * Return a new range with ascending step (positive).
	 * Bounds are preserved as-is; if from &gt; to the resulting range will be empty.
	 *
	 * @return a new ascending Range
	 */
	public Range<T> asc() {
		Number newStep = this.step.doubleValue() >= 0 ? this.step : negateStep( this.step );
		return new Range<>( this.from, this.to, newStep, this.unit, this.fromExclusive, this.toExclusive, this.comparator, this.stepper );
	}

	/**
	 * Return a new range with descending step (negative).
	 * Bounds are preserved as-is; if from &lt; to the resulting range will be empty.
	 *
	 * @return a new descending Range
	 */
	public Range<T> desc() {
		Number newStep = this.step.doubleValue() <= 0 ? this.step : negateStep( this.step );
		return new Range<>( this.from, this.to, newStep, this.unit, this.fromExclusive, this.toExclusive, this.comparator, this.stepper );
	}

	/**
	 * Return a new range with the given step value.
	 * Bounds are preserved as-is; if the step direction conflicts with from → to ordering,
	 * the resulting range will be empty.
	 *
	 * @param newStep the step amount to use
	 *
	 * @return a new Range with the specified step
	 */
	public Range<T> step( Number newStep ) {
		Objects.requireNonNull( newStep, "Step cannot be null" );
		return new Range<>( this.from, this.to, newStep, this.unit, this.fromExclusive, this.toExclusive, this.comparator, this.stepper );
	}

	/**
	 * Return a new range with a step defined by an amount and unit.
	 * <p>
	 * First checks if the IRangeable provides a custom stepper for the unit
	 * (for calendar units like months/years that can't be a fixed numeric step).
	 * Otherwise delegates to {@link IRangeable#rangeStepFromUnit(Number, String)}
	 * to compute a numeric step value. Only valid for IRangeable-based ranges.
	 *
	 * @param amount the number of units per step
	 * @param unit   the unit name (interpretation is type-specific, e.g., "hours", "months")
	 *
	 * @return a new Range with the computed step
	 */
	@SuppressWarnings( "rawtypes" )
	public Range<T> step( Number amount, String unit ) {
		if ( this.elementCategory != ElementCategory.IRANGEABLE ) {
			throw new BoxRuntimeException(
			    String.format( "step(amount, unit) is only supported on IRangeable ranges, not [%s].", this.elementType.getSimpleName() )
			);
		}
		Objects.requireNonNull( unit, "Unit cannot be null" );
		IRangeable<?>	ref			= ( IRangeable<?> ) ( this.from != null ? this.from : this.to );
		BiFunction		unitStepper	= ref.rangeUnitStepper( unit );
		if ( unitStepper != null ) {
			// Calendar unit — install the custom stepper directly with the amount as step
			return new Range<>( this.from, this.to, amount, unit, this.fromExclusive, this.toExclusive, this.comparator, unitStepper );
		}
		// Fixed-duration unit — use numeric step path
		Number computedStep = ref.rangeStepFromUnit( amount, unit );
		return step( computedStep );
	}

	/**
	 * Return a new range with a type constraint for contains() checks.
	 * Only valid on fully unbounded ranges ({@code ..}) since bounded ranges
	 * already have a type inferred from their bounds.
	 * <p>
	 * The type name is used with BoxLang's {@link GenericCaster} which handles all
	 * known BL types (string, numeric, integer, boolean, date, etc.) with their
	 * standard coercion rules, and falls back to the {@code instanceof} operator
	 * for custom Java or BoxLang class names.
	 *
	 * @param typeName the type name to constrain this range to
	 *
	 * @return a new Range with the type constraint applied
	 */
	public Range<T> type( String typeName ) {
		Objects.requireNonNull( typeName, "Type name cannot be null" );
		if ( !isUnbounded() ) {
			throw new BoxRuntimeException( "Cannot set type on a bounded range. The type is already inferred from the range bounds." );
		}
		return new Range<>( this.from, this.to, this.step, this.unit, this.fromExclusive, this.toExclusive,
		    this.comparator, this.stepper, null, null, typeName, false );
	}

	/**
	 * Return a new range with a type constraint using a Java Class reference directly.
	 * Only valid on fully unbounded ranges ({@code ..}) since bounded ranges
	 * already have a type inferred from their bounds.
	 * <p>
	 * When a specific Java class is provided, only exact {@code isInstance()} checks are
	 * performed — no loose coercion is applied. This is the strictest form of type checking.
	 *
	 * @param typeClass the Java Class to constrain this range to
	 *
	 * @return a new Range with the type constraint applied
	 */
	public Range<T> type( Class<?> typeClass ) {
		Objects.requireNonNull( typeClass, "Type class cannot be null" );
		if ( !isUnbounded() ) {
			throw new BoxRuntimeException( "Cannot set type on a bounded range. The type is already inferred from the range bounds." );
		}
		return new Range<>( this.from, this.to, this.step, this.unit, this.fromExclusive, this.toExclusive,
		    this.comparator, this.stepper, typeClass, categorize( typeClass ), null, true );
	}

	// ======================== Contains ========================

	/**
	 * Check if a value falls within this range's bounds.
	 * Accepts any object — returns false if null or incompatible type.
	 *
	 * @param value the value to check
	 *
	 * @return true if the value is within the range
	 */
	public boolean contains( Object value ) {
		if ( this.hasDirectionStepConflict ) {
			return false;
		}

		// null is never in any range
		if ( value == null ) {
			return false;
		}

		// Range-in-range: check if the inner range's bounds are within this range
		if ( value instanceof Range<?> inner ) {
			return containsRange( inner );
		}

		// Unbounded range with no type constraint contains everything non-null
		if ( isUnbounded() && this.elementType == Object.class && this.elementTypeName == null ) {
			return true;
		}

		// String type name — use GenericCaster which handles all BL type coercion
		// and falls back to instanceof for custom class names
		if ( this.elementTypeName != null ) {
			return GenericCaster.attempt( null, value, this.elementTypeName, false ).isPresent();
		}

		// Strict type check (type(Class<?>)) — only exact instanceof, no coercion
		if ( this.strictTypeCheck ) {
			return this.elementType.isInstance( value );
		}

		// Attempt to coerce the value to the range's type
		T coerced = coerceValue( value );
		if ( coerced == null ) {
			return false;
		}

		// Unbounded typed range — coercion succeeded so the value matches the type
		if ( isUnbounded() ) {
			return true;
		}

		// If this is a stepped range (step > 1 or has a unit) and is iterable,
		// check step-reachability by iterating until we match or pass the target.
		if ( isSteppedRange() && isIterable() ) {
			return containsStepped( coerced );
		}

		return containsBounds( coerced );
	}

	/**
	 * Check if a value is within this range's bounds (no step consideration).
	 *
	 * @param coerced the already-coerced value to check
	 *
	 * @return true if within bounds
	 */
	private boolean containsBounds( T coerced ) {
		// Determine which exclusivity applies to the low and high bounds.
		// low/high are sorted, so if from <= to then low=from, high=to;
		// if from > to then low=to, high=from (flipped).
		boolean	lowExclusive;
		boolean	highExclusive;
		if ( this.from != null && this.to != null ) {
			boolean fromIsLow = ( this.low == this.from );
			lowExclusive	= fromIsLow ? this.fromExclusive : this.toExclusive;
			highExclusive	= fromIsLow ? this.toExclusive : this.fromExclusive;
		} else if ( this.from != null ) {
			// Only from present — it's either low or high
			lowExclusive	= ( this.low != null ) ? this.fromExclusive : false;
			highExclusive	= ( this.high != null ) ? this.fromExclusive : false;
		} else {
			// Only to present
			lowExclusive	= ( this.low != null ) ? this.toExclusive : false;
			highExclusive	= ( this.high != null ) ? this.toExclusive : false;
		}

		// Above the highest boundary?
		if ( this.high != null ) {
			int cmp = this.comparator.compare( coerced, this.high );
			if ( highExclusive ? cmp >= 0 : cmp > 0 ) {
				return false;
			}
		}

		// Below the lowest boundary?
		if ( this.low != null ) {
			int cmp = this.comparator.compare( coerced, this.low );
			if ( lowExclusive ? cmp <= 0 : cmp < 0 ) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Check if a value is reachable by stepping through this range.
	 * Iterates from the start, advancing by the step, until we either
	 * match the target or pass it (according to ascending/descending direction).
	 * Safe for half-bounded ranges because iteration stops once the target is exceeded.
	 *
	 * @param target the coerced value to check for step-reachability
	 *
	 * @return true if the target is exactly reached by the stepper
	 */
	private boolean containsStepped( T target ) {
		T current = this.fromExclusive ? this.stepper.apply( this.from, this.ascending ? 1 : -1 ) : this.from;

		while ( current != null ) {
			// If we have an upper bound and we've exceeded it, stop
			if ( this.to != null ) {
				int toCmp = this.comparator.compare( current, this.to );
				if ( this.ascending ? ( this.toExclusive ? toCmp >= 0 : toCmp > 0 ) : ( this.toExclusive ? toCmp <= 0 : toCmp < 0 ) ) {
					return false;
				}
			}

			int cmp = this.comparator.compare( current, target );

			// Exact match — value is reachable
			if ( cmp == 0 ) {
				return true;
			}

			// Passed the target without hitting it — not reachable
			if ( this.ascending && cmp > 0 ) {
				return false;
			}
			if ( !this.ascending && cmp < 0 ) {
				return false;
			}

			// Advance
			current = this.stepper.apply( current, this.step );
		}

		return false;
	}

	/**
	 * Determine if this range has a non-trivial step that requires iteration for contains checks.
	 * A range is "stepped" if it has a unit or if the absolute step value is greater than 1.
	 *
	 * @return true if this range uses custom stepping
	 */
	private boolean isSteppedRange() {
		return this.unit != null || Math.abs( this.step.doubleValue() ) > 1 || this.elementCategory == ElementCategory.IRANGEABLE;
	}

	/**
	 * Check if an inner range is entirely contained within this range's bounds.
	 * Both bounds of the inner range must fall within this range, accounting for exclusivity.
	 * An empty inner range is always contained. An unbounded inner range is only contained
	 * if this range is also unbounded on the corresponding side.
	 *
	 * @param inner the range to check
	 *
	 * @return true if the inner range is fully within this range
	 */
	private boolean containsRange( Range<?> inner ) {
		// Unbounded outer contains any range
		if ( isUnbounded() ) {
			return true;
		}

		// Different base types — not comparable
		if ( this.elementType != inner.elementType ) {
			return false;
		}

		// An empty inner range is trivially contained
		if ( inner.from != null && inner.stepper != null && inner.isEmpty() ) {
			return true;
		}

		boolean	ourLowExclusive		= getLowExclusive();
		boolean	ourHighExclusive	= getHighExclusive();
		boolean	innerLowExclusive	= inner.getLowExclusive();
		boolean	innerHighExclusive	= inner.getHighExclusive();

		// Check inner's low bound is within our low bound
		if ( inner.low != null ) {
			if ( this.low != null ) {
				@SuppressWarnings( "unchecked" )
				int cmp = this.comparator.compare( ( T ) inner.low, this.low );
				if ( cmp < 0 ) {
					return false;
				}
				// Same low bound: inner must be at least as exclusive as us
				if ( cmp == 0 && ourLowExclusive && !innerLowExclusive ) {
					return false;
				}
			}
			// else: we have no low bound, so inner.low is fine
		} else {
			// Inner has no low bound — we must also have no low bound
			if ( this.low != null ) {
				return false;
			}
		}

		// Check inner's high bound is within our high bound
		if ( inner.high != null ) {
			if ( this.high != null ) {
				@SuppressWarnings( "unchecked" )
				int cmp = this.comparator.compare( ( T ) inner.high, this.high );
				if ( cmp > 0 ) {
					return false;
				}
				// Same high bound: inner must be at least as exclusive as us
				if ( cmp == 0 && ourHighExclusive && !innerHighExclusive ) {
					return false;
				}
			}
			// else: we have no high bound, so inner.high is fine
		} else {
			// Inner has no high bound — we must also have no high bound
			if ( this.high != null ) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Get the exclusivity flag for the low (sorted) bound.
	 */
	private boolean getLowExclusive() {
		if ( this.from != null && this.to != null ) {
			return ( this.low == this.from ) ? this.fromExclusive : this.toExclusive;
		} else if ( this.from != null ) {
			return ( this.low != null ) ? this.fromExclusive : false;
		} else {
			return ( this.low != null ) ? this.toExclusive : false;
		}
	}

	/**
	 * Get the exclusivity flag for the high (sorted) bound.
	 */
	private boolean getHighExclusive() {
		if ( this.from != null && this.to != null ) {
			return ( this.high == this.to ) ? this.toExclusive : this.fromExclusive;
		} else if ( this.from != null ) {
			return ( this.high != null ) ? this.fromExclusive : false;
		} else {
			return ( this.high != null ) ? this.toExclusive : false;
		}
	}

	// ======================== Clamp ========================

	/**
	 * Clamp a value to this range's bounds.
	 * If the value is below the low bound, return the low bound.
	 * If the value is above the high bound, return the high bound.
	 * If the value is within bounds, return it unchanged.
	 * <p>
	 * For fully unbounded ranges, the value is type-checked/coerced and returned as-is
	 * since there are no bounds to snap to.
	 *
	 * @param value the value to clamp
	 *
	 * @return the clamped value snapped to the nearest boundary if out of bounds
	 */
	@SuppressWarnings( "unchecked" )
	public T clamp( Object value ) {
		if ( value == null ) {
			throw new BoxRuntimeException( "Cannot clamp a null value." );
		}

		// String type name — validate via GenericCaster (same as contains())
		if ( this.elementTypeName != null ) {
			CastAttempt<Object> attempt = GenericCaster.attempt( null, value, this.elementTypeName, false );
			if ( !attempt.isPresent() ) {
				throw new BoxRuntimeException(
				    String.format( "Cannot clamp value [%s] — incompatible with range type [%s].",
				        value, this.elementTypeName )
				);
			}
			// Unbounded typed range — return the cast value
			return ( T ) attempt.get();
		}

		// Strict type check (type(Class<?>)) — only exact instanceof
		if ( this.strictTypeCheck && !this.elementType.isInstance( value ) ) {
			throw new BoxRuntimeException(
			    String.format( "Cannot clamp value [%s] — incompatible with range type [%s].",
			        value, this.elementType.getSimpleName() )
			);
		}

		T coerced = coerceValue( value );
		if ( coerced == null ) {
			throw new BoxRuntimeException(
			    String.format( "Cannot clamp value [%s] — incompatible with range type [%s].",
			        value, this.elementType.getSimpleName() )
			);
		}

		// Fully unbounded — no bounds to snap to, just return the coerced value
		if ( isUnbounded() ) {
			return coerced;
		}

		// Snap to low bound if below
		if ( this.low != null ) {
			int cmp = this.comparator.compare( coerced, this.low );
			if ( cmp < 0 ) {
				return this.low;
			}
		}

		// Snap to high bound if above
		if ( this.high != null ) {
			int cmp = this.comparator.compare( coerced, this.high );
			if ( cmp > 0 ) {
				return this.high;
			}
		}

		return coerced;
	}

	// ======================== Position Checks ========================

	/**
	 * Check if a value falls before (below) this range's low bound.
	 * Returns false for unbounded ranges with no low bound.
	 *
	 * @param value the value to check
	 *
	 * @return true if the value is before the range
	 */
	@SuppressWarnings( "unchecked" )
	public boolean isValueBefore( Object value ) {
		if ( this.hasDirectionStepConflict ) {
			return false;
		}
		if ( value == null || this.low == null ) {
			return false;
		}
		T coerced = coerceValue( value );
		if ( coerced == null ) {
			return false;
		}
		int cmp = this.comparator.compare( coerced, this.low );
		return getLowExclusive() ? cmp <= 0 : cmp < 0;
	}

	/**
	 * Check if a value falls after (above) this range's high bound.
	 * Returns false for unbounded ranges with no high bound.
	 *
	 * @param value the value to check
	 *
	 * @return true if the value is after the range
	 */
	@SuppressWarnings( "unchecked" )
	public boolean isValueAfter( Object value ) {
		if ( this.hasDirectionStepConflict ) {
			return false;
		}
		if ( value == null || this.high == null ) {
			return false;
		}
		T coerced = coerceValue( value );
		if ( coerced == null ) {
			return false;
		}
		int cmp = this.comparator.compare( coerced, this.high );
		return getHighExclusive() ? cmp >= 0 : cmp > 0;
	}

	/**
	 * Attempt to coerce a value to this range's type.
	 * Returns null if the value is incompatible.
	 *
	 * @param value the value to coerce
	 *
	 * @return the coerced value, or null if incompatible
	 */
	@SuppressWarnings( "null" )
	private T coerceValue( Object value ) {
		// Direct type match (also handles unbounded Object.class ranges since everything is an instance of Object)
		if ( this.elementType.isInstance( value ) ) {
			return ( T ) value;
		}

		switch ( this.elementCategory ) {
			case NUMBER :
				Number num = NumberCaster.attempt( value ).orElse( null );
				return num != null ? ( T ) num : null;
			case STRING :
				String str = StringCaster.attempt( value ).orElse( null );
				return str != null ? ( T ) str : null;
			case CHARACTER :
				if ( value instanceof String s && s.length() == 1 ) {
					return ( T ) ( Character ) s.charAt( 0 );
				}
				return null;
			case IRANGEABLE :
				IRangeable<?> ref = ( IRangeable<?> ) ( this.from != null ? this.from : this.to );
				return ( T ) ref.rangeCoerce( value );
			default :
				return null;
		}
	}

	/**
	 * Determine whether this range defines an unsatisfiable progression.
	 * This includes direction-step conflicts and exclusive single-point ranges.
	 */
	private static <T> boolean isIterationEmpty( T from, T to, boolean fromExclusive, boolean toExclusive, boolean ascending,
	    Comparator<T> comparator, BiFunction<T, Number, T> stepper ) {
		if ( from == null || to == null || stepper == null ) {
			return false;
		}

		int cmp = comparator.compare( from, to );
		if ( ascending ) {
			return cmp > 0 || ( cmp == 0 && ( fromExclusive || toExclusive ) );
		}
		return cmp < 0 || ( cmp == 0 && ( fromExclusive || toExclusive ) );
	}

	/**
	 * Determine whether this range has a paradoxical direction-step conflict.
	 * This only applies to iterable bounded ranges where step direction and bound ordering disagree.
	 */
	private static <T> boolean hasDirectionStepConflict( T from, T to, boolean ascending,
	    Comparator<T> comparator, BiFunction<T, Number, T> stepper ) {
		if ( from == null || to == null || stepper == null ) {
			return false;
		}

		int cmp = comparator.compare( from, to );
		return ascending ? cmp > 0 : cmp < 0;
	}

	/**
	 * Categorize a type once at construction time for fast coercion dispatch.
	 *
	 * @param type the class to categorize
	 *
	 * @return the ElementCategory for the given type
	 */
	private static ElementCategory categorize( Class<?> type ) {
		if ( IRangeable.class.isAssignableFrom( type ) ) {
			return ElementCategory.IRANGEABLE;
		}
		if ( Number.class.isAssignableFrom( type ) ) {
			return ElementCategory.NUMBER;
		}
		if ( String.class == type ) {
			return ElementCategory.STRING;
		}
		if ( Character.class == type ) {
			return ElementCategory.CHARACTER;
		}
		return ElementCategory.OTHER;
	}

	// ======================== Iteration ========================

	@Override
	public Iterator<T> iterator() {
		if ( !isIterable() ) {
			throw new BoxRuntimeException(
			    String.format( "Range [%s] of type [%s] is not iterable.%s%s",
			        asString(),
			        this.elementType.getSimpleName(),
			        this.from == null ? " Missing start value." : "",
			        this.stepper == null ? " No stepper function available (contains-only range)." : "" )
			);
		}

		return new Iterator<>() {

			private T		current		= initCurrent();
			private boolean	exhausted	= false;

			private T initCurrent() {
				if ( Range.this.fromExclusive ) {
					// Skip the from value by advancing one unit in the iteration direction
					Number unitStep = Range.this.ascending ? 1 : -1;
					return Range.this.stepper.apply( Range.this.from, unitStep );
				}
				return Range.this.from;
			}

			@Override
			public boolean hasNext() {
				if ( this.exhausted || this.current == null ) {
					return false;
				}
				// If no upper bound, infinite iteration
				if ( Range.this.to == null ) {
					return true;
				}
				// Check if current is still within bounds
				int cmp = Range.this.comparator.compare( this.current, Range.this.to );
				if ( Range.this.ascending ) {
					return Range.this.toExclusive ? cmp < 0 : cmp <= 0;
				} else {
					return Range.this.toExclusive ? cmp > 0 : cmp >= 0;
				}
			}

			@Override
			public T next() {
				if ( !hasNext() ) {
					throw new NoSuchElementException();
				}
				T result = this.current;
				this.current = Range.this.stepper.apply( this.current, Range.this.step );
				if ( this.current == null ) {
					this.exhausted = true;
				}
				return result;
			}
		};
	}

	/**
	 * Materialize this range into a BoxLang Array.
	 * Requires an iterable range with a finite upper bound.
	 *
	 * @return a new Array containing all values in this range
	 */
	public Array toArray() {
		if ( !isIterable() ) {
			throw new BoxRuntimeException( "Cannot materialize a non-iterable range into an array." );
		}
		if ( this.to == null ) {
			throw new BoxRuntimeException( "Cannot materialize an unbounded range into an array." );
		}
		Array result = new Array();
		for ( T value : this ) {
			result.add( value );
		}
		return result;
	}

	/**
	 * Return a sequential Stream over the elements of this range.
	 * The range must be iterable (has a start value and a stepper).
	 *
	 * @return a Stream over the range elements
	 */
	public Stream<T> stream() {
		if ( !isIterable() ) {
			throw new BoxRuntimeException(
			    String.format( "Range [%s] of type [%s] is not iterable and cannot be streamed.",
			        asString(), this.elementType.getSimpleName() )
			);
		}
		int characteristics = Spliterator.ORDERED | Spliterator.IMMUTABLE;
		if ( isBounded() ) {
			characteristics |= Spliterator.SIZED;
		}
		return StreamSupport.stream(
		    Spliterators.spliteratorUnknownSize( iterator(), characteristics ),
		    false
		);
	}

	// ======================== IType ========================

	@Override
	public String asString() {
		String	fromStr	= this.from != null ? this.from.toString() : "";
		String	toStr	= this.to != null ? this.to.toString() : "";
		// Choose the range operator based on exclusivity: >.., ..<, or >..<
		String	rangeOp;
		if ( this.fromExclusive && this.toExclusive ) {
			rangeOp = ">..<";
		} else if ( this.fromExclusive ) {
			rangeOp = ">..";
		} else if ( this.toExclusive ) {
			rangeOp = "..<";
		} else {
			rangeOp = "..";
		}
		StringBuilder	sb		= new StringBuilder( fromStr ).append( rangeOp ).append( toStr );
		double			stepVal	= this.step.doubleValue();
		if ( this.unit != null ) {
			sb.append( ".step(" ).append( this.step ).append( ", \"" ).append( this.unit ).append( "\")" );
		} else if ( stepVal != 1.0 && stepVal != -1.0 ) {
			sb.append( ".step(" ).append( this.step ).append( ")" );
		} else if ( !this.ascending ) {
			sb.append( ".desc()" );
		}
		return sb.toString();
	}

	@Override
	public String getBoxTypeName() {
		return "Range";
	}

	@Override
	public BoxMeta<?> getBoxMeta() {
		if ( this.$bx == null ) {
			this.$bx = new RangeMeta( this );
		}
		return this.$bx;
	}

	// ======================== IReferenceable ========================

	@Override
	public Object dereference( IBoxContext context, Key name, Boolean safe ) {
		if ( name.equals( BoxMeta.key ) ) {
			return getBoxMeta();
		}
		return DynamicInteropService.dereference( context, this.getClass(), this, name, safe );
	}

	@Override
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Object[] positionalArguments, Boolean safe ) {
		return DynamicInteropService.invoke( context, this, name.getName(), safe, positionalArguments );
	}

	@Override
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Map<Key, Object> namedArguments, Boolean safe ) {
		return DynamicInteropService.invoke( context, this, name.getName(), safe, namedArguments );
	}

	@Override
	public Object assign( IBoxContext context, Key name, Object value ) {
		throw new BoxRuntimeException( "Range values are immutable." );
	}

	// ======================== Object overrides ========================

	@Override
	public boolean equals( Object obj ) {
		if ( this == obj ) {
			return true;
		}
		if ( ! ( obj instanceof Range<?> other ) ) {
			return false;
		}
		return Objects.equals( this.from, other.from )
		    && Objects.equals( this.to, other.to )
		    && Objects.equals( this.step, other.step )
		    && Objects.equals( this.elementType, other.elementType )
		    && this.fromExclusive == other.fromExclusive
		    && this.toExclusive == other.toExclusive;
	}

	@Override
	public int hashCode() {
		return this.hashCode;
	}

	@Override
	public String toString() {
		return asString();
	}

	private int computeHashCode() {
		return Objects.hash( this.from, this.to, this.step, this.elementType, this.fromExclusive, this.toExclusive );
	}

	/**
	 * Negate a step value while preserving its numeric type (Integer stays Integer, etc.).
	 *
	 * @param step the step to negate
	 *
	 * @return the negated step in the same numeric type
	 */
	private static Number negateStep( Number step ) {
		if ( step instanceof Integer i ) {
			return -i;
		}
		if ( step instanceof Long l ) {
			return -l;
		}
		return -step.doubleValue();
	}

	// ======================== Factory Methods ========================

	/**
	 * Create a range by inspecting the types of the operands.
	 * This is the primary entry point for creating ranges in Java code.
	 *
	 * @param left  The start of the range (nullable for open-start)
	 * @param right The end of the range (nullable for open-end)
	 *
	 * @return A typed Range instance
	 */
	public static Range<?> of( Object left, Object right ) {
		return of( left, right, false, false );
	}

	/**
	 * Create a range by inspecting the types of the operands with exclusivity flags.
	 *
	 * @param left          The start of the range (nullable for open-start)
	 * @param right         The end of the range (nullable for open-end)
	 * @param fromExclusive Whether the start bound is exclusive
	 * @param toExclusive   Whether the end bound is exclusive
	 *
	 * @return A typed Range instance
	 */
	public static Range<?> of( Object left, Object right, boolean fromExclusive, boolean toExclusive ) {
		// Both null — unbounded range that contains everything
		if ( left == null && right == null ) {
			Comparator<Object> comparator = ( a, b ) -> 0;
			return new Range<>( null, null, null, null, fromExclusive, toExclusive, comparator, null );
		}

		// --- Direct Type Checks (fastest path) ---

		// Both IRangeable of the same type
		if ( left instanceof IRangeable && right instanceof IRangeable ) {
			if ( left.getClass() != right.getClass() ) {
				// Attempt to coerce right to left's type, then vice versa
				Object coerced = ( ( IRangeable<?> ) left ).rangeCoerce( right );
				if ( coerced != null ) {
					return buildRangeableRange( ( IRangeable<?> ) left, ( IRangeable<?> ) coerced, fromExclusive, toExclusive );
				}
				coerced = ( ( IRangeable<?> ) right ).rangeCoerce( left );
				if ( coerced != null ) {
					return buildRangeableRange( ( IRangeable<?> ) coerced, ( IRangeable<?> ) right, fromExclusive, toExclusive );
				}
				throw new BoxRuntimeException(
				    String.format( "Range operator requires both operands to be the same type. Got [%s] and [%s].",
				        TypeUtil.getObjectName( left ), TypeUtil.getObjectName( right ) )
				);
			}
			return buildRangeableRange( ( IRangeable<?> ) left, ( IRangeable<?> ) right, fromExclusive, toExclusive );
		}

		// One side is IRangeable — try to coerce the other
		if ( left instanceof IRangeable && right != null ) {
			Object coerced = ( ( IRangeable<?> ) left ).rangeCoerce( right );
			if ( coerced != null ) {
				return buildRangeableRange( ( IRangeable<?> ) left, ( IRangeable<?> ) coerced, fromExclusive, toExclusive );
			}
		}
		if ( right instanceof IRangeable && left != null ) {
			Object coerced = ( ( IRangeable<?> ) right ).rangeCoerce( left );
			if ( coerced != null ) {
				return buildRangeableRange( ( IRangeable<?> ) coerced, ( IRangeable<?> ) right, fromExclusive, toExclusive );
			}
		}

		// Both Number
		if ( left instanceof Number && right instanceof Number ) {
			return buildNumberRange( ( Number ) left, ( Number ) right, fromExclusive, toExclusive );
		}

		// Both Character
		if ( left instanceof Character && right instanceof Character ) {
			return buildCharRange( ( Character ) left, ( Character ) right, fromExclusive, toExclusive );
		}

		// --- Date Coercion — if one side is a known date type, try to cast the other ---
		if ( left != null && right != null ) {
			boolean	leftIsDate	= DateTimeCaster.isKnownDateClass( left );
			boolean	rightIsDate	= DateTimeCaster.isKnownDateClass( right );
			if ( leftIsDate || rightIsDate ) {
				CastAttempt<DateTime>	leftAttempt		= DateTimeCaster.attempt( left );
				CastAttempt<DateTime>	rightAttempt	= DateTimeCaster.attempt( right );
				if ( leftAttempt.isPresent() && rightAttempt.isPresent() ) {
					return buildRangeableRange( leftAttempt.get(), rightAttempt.get(), fromExclusive, toExclusive );
				}
			}
		}

		// --- String Coercion ---
		if ( left instanceof String ls && right instanceof String rs ) {
			// Try numeric first (so "1".."5" becomes a number range, not char)
			Number	leftNum		= NumberCaster.attempt( ls ).orElse( null );
			Number	rightNum	= NumberCaster.attempt( rs ).orElse( null );
			if ( leftNum != null && rightNum != null ) {
				return buildNumberRange( leftNum, rightNum, fromExclusive, toExclusive );
			}
			// Single-char strings → Character range
			if ( ls.length() == 1 && rs.length() == 1 ) {
				return buildCharRange( ls.charAt( 0 ), rs.charAt( 0 ), fromExclusive, toExclusive );
			}
			// Both strings are Comparable — contains-only range
			return buildComparableRange( ( Comparable<?> ) left, ( Comparable<?> ) right, fromExclusive, toExclusive );
		}

		// --- Mixed type: one is Number, attempt to cast the other ---
		if ( left instanceof Number && right != null ) {
			Number rightNum = NumberCaster.attempt( right ).orElse( null );
			if ( rightNum != null ) {
				return buildNumberRange( ( Number ) left, rightNum, fromExclusive, toExclusive );
			}
		}
		if ( right instanceof Number && left != null ) {
			Number leftNum = NumberCaster.attempt( left ).orElse( null );
			if ( leftNum != null ) {
				return buildNumberRange( leftNum, ( Number ) right, fromExclusive, toExclusive );
			}
		}

		// --- Both Comparable of the same type — contains-only ---
		if ( left instanceof Comparable && right instanceof Comparable && left.getClass() == right.getClass() ) {
			return buildComparableRange( ( Comparable<?> ) left, ( Comparable<?> ) right, fromExclusive, toExclusive );
		}

		// --- Half-open ranges (one side null) ---
		if ( left == null || right == null ) {
			Object nonNull = left != null ? left : right;
			if ( nonNull instanceof IRangeable ) {
				return buildRangeableRange( ( IRangeable<?> ) nonNull, null, fromExclusive, toExclusive );
			}
			if ( nonNull instanceof Number ) {
				return buildNumberRange( ( Number ) left, ( Number ) right, fromExclusive, toExclusive );
			}
			if ( nonNull instanceof Character ) {
				return buildCharRange( ( Character ) left, ( Character ) right, fromExclusive, toExclusive );
			}
			if ( nonNull instanceof String s && s.length() == 1 ) {
				Character c = s.charAt( 0 );
				return buildCharRange( left != null ? c : null, right != null ? c : null, fromExclusive, toExclusive );
			}
			if ( nonNull instanceof Comparable ) {
				return buildComparableRange( ( Comparable<?> ) left, ( Comparable<?> ) right, fromExclusive, toExclusive );
			}
		}

		throw new BoxRuntimeException(
		    String.format( "Cannot create a range from types [%s] and [%s]. Values must be Comparable.",
		        TypeUtil.getObjectName( left ), TypeUtil.getObjectName( right ) )
		);
	}

	// ======================== Private Builder Methods ========================

	/**
	 * Build a numeric range with the BoxLang Compare operator for ordering.
	 *
	 * @param from          the start number (nullable)
	 * @param to            the end number (nullable)
	 * @param fromExclusive whether the start bound is exclusive
	 * @param toExclusive   whether the end bound is exclusive
	 *
	 * @return a numeric Range
	 */
	private static Range<Number> buildNumberRange( Number from, Number to, boolean fromExclusive, boolean toExclusive ) {
		Comparator<Number>					comparator	= ( a, b ) -> Compare.invoke( a, b );
		BiFunction<Number, Number, Number>	stepper		= RangeSteppers.NUMBER_STEPPER;
		Number								step		= ( from != null && to != null ) ? ( Compare.invoke( from, to ) <= 0 ? 1 : -1 ) : 1;
		return new Range<>( from, to, step, null, fromExclusive, toExclusive, comparator, stepper );
	}

	/**
	 * Build a character range using natural Character ordering.
	 *
	 * @param from          the start character (nullable)
	 * @param to            the end character (nullable)
	 * @param fromExclusive whether the start bound is exclusive
	 * @param toExclusive   whether the end bound is exclusive
	 *
	 * @return a character Range
	 */
	private static Range<Character> buildCharRange( Character from, Character to, boolean fromExclusive, boolean toExclusive ) {
		Comparator<Character>						comparator	= Character::compareTo;
		BiFunction<Character, Number, Character>	stepper		= RangeSteppers.CHARACTER_STEPPER;
		Number										step		= ( from != null && to != null ) ? ( from.compareTo( to ) <= 0 ? 1 : -1 ) : 1;
		return new Range<>( from, to, step, null, fromExclusive, toExclusive, comparator, stepper );
	}

	/**
	 * Build a range from IRangeable objects using their rangeCompare/rangeAdvance methods.
	 *
	 * @param from          the start IRangeable (nullable)
	 * @param to            the end IRangeable (nullable)
	 * @param fromExclusive whether the start bound is exclusive
	 * @param toExclusive   whether the end bound is exclusive
	 *
	 * @return an iterable Range over the IRangeable type
	 */
	@SuppressWarnings( "rawtypes" )
	private static <T extends IRangeable<T>> Range<T> buildRangeableRange( IRangeable from, IRangeable to, boolean fromExclusive, boolean toExclusive ) {
		Comparator<T>				comparator	= ( a, b ) -> a.rangeCompare( ( T ) b );
		BiFunction<T, Number, T>	stepper		= ( current, step ) -> current.rangeAdvance( step );
		Number						step		= from != null && to != null ? ( from.rangeCompare( to ) <= 0 ? 1 : -1 ) : 1;
		return new Range<>( ( T ) from, ( T ) to, step, null, fromExclusive, toExclusive, comparator, stepper );
	}

	/**
	 * Build a contains-only range from two Comparable values (no iteration support).
	 *
	 * @param from          the start Comparable (nullable)
	 * @param to            the end Comparable (nullable)
	 * @param fromExclusive whether the start bound is exclusive
	 * @param toExclusive   whether the end bound is exclusive
	 *
	 * @return a contains-only Range
	 */
	@SuppressWarnings( "rawtypes" )
	private static <T extends Comparable<T>> Range<T> buildComparableRange( Comparable from, Comparable to, boolean fromExclusive, boolean toExclusive ) {
		Comparator<T> comparator = ( a, b ) -> ( ( Comparable ) a ).compareTo( b );
		return new Range<>( ( T ) from, ( T ) to, null, null, fromExclusive, toExclusive, comparator, null );
	}
}
