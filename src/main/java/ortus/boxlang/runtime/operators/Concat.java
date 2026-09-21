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
package ortus.boxlang.runtime.operators;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.BoxStringBuilder;

/**
 * Performs String Concat
 * {@code a = "Hello" & "World" }
 */
public class Concat implements IOperator {

	/**
	 * @param left  The left operand
	 * @param right The right operand
	 *
	 * @return The two strings conctenated
	 */
	public static String invoke( Object left, Object right ) {
		left	= ( left == null ) ? "" : left;
		right	= ( right == null ) ? "" : right;
		return ( StringCaster.cast( left ) ).concat( StringCaster.cast( right ) );
	}

	/**
	 * @param segments array of segments to concat
	 *
	 * @return The sgements conctenated
	 */
	public static String invoke( Object... segments ) {
		if ( segments.length == 0 ) {
			return "";
		} else if ( segments.length == 1 ) {
			return StringCaster.cast( segments[ 0 ] );
		} else if ( segments.length < 4 ) {
			// Performance testing shows raw string concat is faster or the same as string builder for up to 3 segments
			String result = "";
			for ( Object segment : segments ) {
				if ( segment != null ) {
					if ( segment instanceof String stringSegment )
						result = result.concat( stringSegment );
					else
						result = result.concat( StringCaster.cast( segment ) );
				}
			}
			return result;
		} else {
			// Once we get to 4 or more segments, string builder becomes more efficient for concatenation
			// Note, it's very important that we set the initial capactity of our StringBuilder for it to be faster!
			String[]	castedSegments	= new String[ segments.length ];
			int			totalLength		= 0;
			for ( int i = 0; i < segments.length; i++ ) {
				Object segment = segments[ i ];
				if ( segment == null ) {
					continue;
				}
				if ( segment instanceof String stringSegment ) {
					castedSegments[ i ] = stringSegment;
				} else {
					castedSegments[ i ] = StringCaster.cast( segment );
				}
				totalLength += castedSegments[ i ].length();
			}

			StringBuilder sb = new StringBuilder( totalLength );
			for ( int i = 0; i < castedSegments.length; i++ ) {
				if ( castedSegments[ i ] != null ) {
					sb.append( castedSegments[ i ] );
				}
			}
			return sb.toString();
		}
	}

	/**
	 * Apply this operator to an object/key and set the new value back in the same object/key.
	 * When the current value is a {@link BoxStringBuilder}, mutates it in-place and returns the
	 * same instance without re-assigning, preserving the reference identity.
	 *
	 * @return The result — either a {@code String} or the mutated {@link BoxStringBuilder}
	 */
	public static Object invoke( IBoxContext context, Object target, Key name, Object right ) {
		Object current = context.unwrapQueryColumn( Referencer.get( context, target, name, false ) );
		if ( current instanceof BoxStringBuilder sb ) {
			sb.append( right );
			return sb;
		}
		if ( current instanceof java.lang.StringBuilder javaSB ) {
			javaSB.append( StringCaster.cast( right ) );
			return javaSB;
		}
		String result = invoke( current, right );
		Referencer.set( context, target, name, result );
		return result;
	}

	/**
	 * Apply this operator to an object/key and set the new value back in the same object/key
	 *
	 * @return The result
	 */
	public static String invoke( IBoxContext context, Object target, Key name, Object... segments ) {
		Object current = Referencer.get( context, target, name, false );

		// Optimize for StringBuilder types - mutate in-place
		if ( current instanceof BoxStringBuilder sb ) {
			for ( Object segment : segments ) {
				sb.append( segment );
			}
			return sb.toString();
		}
		if ( current instanceof java.lang.StringBuilder javaSB ) {
			for ( Object segment : segments ) {
				javaSB.append( StringCaster.cast( segment ) );
			}
			return javaSB.toString();
		}

		// For strings, create array with current value + all segments and delegate to optimized varargs method
		Object[] allSegments = new Object[ segments.length + 1 ];
		allSegments[ 0 ] = current;
		System.arraycopy( segments, 0, allSegments, 1, segments.length );
		String result = invoke( allSegments );
		Referencer.set( context, target, name, result );
		return result;
	}

}
