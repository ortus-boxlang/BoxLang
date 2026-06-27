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
		StringBuilder sb = new StringBuilder();
		for ( Object segment : segments ) {
			if ( segment != null ) {
				sb.append( StringCaster.cast( segment ) );
			}
		}
		return sb.toString();
	}

	/**
	 * Apply this operator to an object/key and set the new value back in the same object/key.
	 * When the current value is a {@link BoxStringBuilder}, mutates it in-place and returns the
	 * same instance without re-assigning, preserving the reference identity.
	 *
	 * @return The result — either a {@code String} or the mutated {@link BoxStringBuilder}
	 */
	public static Object invoke( IBoxContext context, Object target, Key name, Object right ) {
		Object current = Referencer.get( context, target, name, false );
		if ( current instanceof BoxStringBuilder sb ) {
			sb.append( right );
			return sb;
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
		StringBuilder sb = new StringBuilder();
		sb.append( Referencer.get( context, target, name, false ) );
		for ( Object segment : segments ) {
			sb.append( StringCaster.cast( segment ) );
		}
		String result = sb.toString();
		Referencer.set( context, target, name, result );
		return result;
	}

}
