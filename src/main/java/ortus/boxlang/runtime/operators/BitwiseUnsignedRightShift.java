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
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Performs BitwiseUnsignedRightShift
 * {@code z = x b>>> y}
 */
public class BitwiseUnsignedRightShift implements IOperator {

	/**
	 * @param left  The left operand
	 * @param right The right operand
	 *
	 * @return The result
	 */
	public static Number invoke( Object left, Object right ) {
		if ( left instanceof Short l ) {
			if ( right instanceof Short r ) {
				return l >>> r;
			} else if ( right instanceof Integer r ) {
				return l >>> r;
			} else if ( right instanceof Long r ) {
				return l >>> r;
			} else if ( right instanceof Byte r ) {
				return l >>> r;
			} else if ( right instanceof Character r ) {
				return l >>> r;
			}
		}

		if ( left instanceof Integer l ) {
			if ( right instanceof Short r ) {
				return l >>> r;
			} else if ( right instanceof Integer r ) {
				return l >>> r;
			} else if ( right instanceof Long r ) {
				return l >>> r;
			} else if ( right instanceof Byte r ) {
				return l >>> r;
			} else if ( right instanceof Character r ) {
				return l >>> r;
			}
		}

		if ( left instanceof Long l ) {
			if ( right instanceof Short r ) {
				return l >>> r;
			} else if ( right instanceof Integer r ) {
				return l >>> r;
			} else if ( right instanceof Long r ) {
				return l >>> r;
			} else if ( right instanceof Byte r ) {
				return l >>> r;
			} else if ( right instanceof Character r ) {
				return l >>> r;
			}
		}

		if ( left instanceof Byte l ) {
			if ( right instanceof Short r ) {
				return l >>> r;
			} else if ( right instanceof Integer r ) {
				return l >>> r;
			} else if ( right instanceof Long r ) {
				return l >>> r;
			} else if ( right instanceof Byte r ) {
				return l >>> r;
			} else if ( right instanceof Character r ) {
				return l >>> r;
			}
		}

		if ( left instanceof Character l ) {
			if ( right instanceof Short r ) {
				return l >>> r;
			} else if ( right instanceof Integer r ) {
				return l >>> r;
			} else if ( right instanceof Long r ) {
				return l >>> r;
			} else if ( right instanceof Byte r ) {
				return l >>> r;
			} else if ( right instanceof Character r ) {
				return l >>> r;
			}
		}

		String	leftClass	= left == null ? "null" : left.getClass().getName();
		String	rightClass	= right == null ? "null" : right.getClass().getName();
		throw new BoxRuntimeException( "Invalid types for bitwise operation. Left: " + leftClass + "; Right: " + rightClass );
	}

	/**
	 * Apply this operator to an object/key and set the new value back in the same object/key
	 *
	 * @return The result
	 */
	public static Number invoke( IBoxContext context, Object target, Key name, Object right ) {
		Number result = invoke( Referencer.get( context, target, name, false ), right );
		Referencer.set( context, target, name, result );
		return result;
	}

}
