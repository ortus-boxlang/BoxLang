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
package ortus.boxlang.runtime.types.util;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.types.BoxSet;
import ortus.boxlang.runtime.types.Function;

/**
 * Shared iteration helpers for {@link BoxSet} BIFs.
 *
 * <p>
 * Sets have no positional index but functional methods (each, map, filter, …) still
 * pass an ordinal so callbacks can interoperate with Array-style signatures
 * {@code (value, index, set)}. Lambdas / Java functional interfaces that accept a single
 * argument only receive the value.
 */
public final class SetUtil {

	private SetUtil() {
	}

	/**
	 * Invoke {@code callback(value, ordinal, set)} (or {@code callback(value)} for strict-args
	 * functions) for every element of the set.
	 */
	public static void each( BoxSet set, Function callback, IBoxContext context ) {
		boolean	strict	= callback.requiresStrictArguments();
		int		ordinal	= 0;
		for ( Object value : set ) {
			if ( strict ) {
				context.invokeFunction( callback, new Object[] { value } );
			} else {
				context.invokeFunction( callback, new Object[] { value, ordinal, set } );
			}
			ordinal++;
		}
	}

	/**
	 * Apply {@code callback} to every element and collect the results into a new {@link BoxSet}
	 * of the same variant as the source. Duplicates produced by the callback are deduplicated.
	 */
	public static BoxSet map( BoxSet set, Function callback, IBoxContext context ) {
		BoxSet	out		= new BoxSet( set.getType() );
		boolean	strict	= callback.requiresStrictArguments();
		int		ordinal	= 0;
		for ( Object value : set ) {
			Object result = strict
			    ? context.invokeFunction( callback, new Object[] { value } )
			    : context.invokeFunction( callback, new Object[] { value, ordinal, set } );
			out.add( result );
			ordinal++;
		}
		return out;
	}

	/**
	 * Return a new set containing only those elements for which {@code predicate} returns truthy.
	 */
	public static BoxSet filter( BoxSet set, Function predicate, IBoxContext context ) {
		return partition( set, predicate, context, true );
	}

	/**
	 * Return a new set containing only those elements for which {@code predicate} returns falsy
	 * (the complement of {@link #filter}).
	 */
	public static BoxSet reject( BoxSet set, Function predicate, IBoxContext context ) {
		return partition( set, predicate, context, false );
	}

	private static BoxSet partition( BoxSet set, Function predicate, IBoxContext context, boolean keepTruthy ) {
		BoxSet	out		= new BoxSet( set.getType() );
		boolean	strict	= predicate.requiresStrictArguments();
		int		ordinal	= 0;
		for ( Object value : set ) {
			Object	result	= strict
			    ? context.invokeFunction( predicate, new Object[] { value } )
			    : context.invokeFunction( predicate, new Object[] { value, ordinal, set } );
			boolean	truthy	= BooleanCaster.cast( result );
			if ( truthy == keepTruthy ) {
				out.add( value );
			}
			ordinal++;
		}
		return out;
	}

	/**
	 * Left-fold the set with {@code callback(accumulator, value, ordinal, set)} starting from
	 * {@code initial}.
	 */
	public static Object reduce( BoxSet set, Function callback, Object initial, IBoxContext context ) {
		boolean	strict	= callback.requiresStrictArguments();
		Object	acc		= initial;
		int		ordinal	= 0;
		for ( Object value : set ) {
			acc = strict
			    ? context.invokeFunction( callback, new Object[] { acc, value } )
			    : context.invokeFunction( callback, new Object[] { acc, value, ordinal, set } );
			ordinal++;
		}
		return acc;
	}

	/**
	 * Test whether every element satisfies the predicate (short-circuit).
	 */
	public static boolean every( BoxSet set, Function predicate, IBoxContext context ) {
		boolean	strict	= predicate.requiresStrictArguments();
		int		ordinal	= 0;
		for ( Object value : set ) {
			Object result = strict
			    ? context.invokeFunction( predicate, new Object[] { value } )
			    : context.invokeFunction( predicate, new Object[] { value, ordinal, set } );
			if ( !BooleanCaster.cast( result ) ) {
				return false;
			}
			ordinal++;
		}
		return true;
	}

	/**
	 * Test whether at least one element satisfies the predicate (short-circuit).
	 */
	public static boolean some( BoxSet set, Function predicate, IBoxContext context ) {
		boolean	strict	= predicate.requiresStrictArguments();
		int		ordinal	= 0;
		for ( Object value : set ) {
			Object result = strict
			    ? context.invokeFunction( predicate, new Object[] { value } )
			    : context.invokeFunction( predicate, new Object[] { value, ordinal, set } );
			if ( BooleanCaster.cast( result ) ) {
				return true;
			}
			ordinal++;
		}
		return false;
	}

	/**
	 * Find the first element matching the predicate, or {@code null} if none match.
	 */
	public static Object find( BoxSet set, Function predicate, IBoxContext context ) {
		boolean	strict	= predicate.requiresStrictArguments();
		int		ordinal	= 0;
		for ( Object value : set ) {
			Object result = strict
			    ? context.invokeFunction( predicate, new Object[] { value } )
			    : context.invokeFunction( predicate, new Object[] { value, ordinal, set } );
			if ( BooleanCaster.cast( result ) ) {
				return value;
			}
			ordinal++;
		}
		return null;
	}

}
