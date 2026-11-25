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

import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxSet;

/**
 * Backs the Groovy parser's {@code <<} operator, which Groovy overloads for collection append
 * ({@code list << item}) in addition to Java's own numeric left-shift. This is deliberately kept
 * separate from {@link BitwiseSignedLeftShift} - the shared, dialect-neutral operator every
 * BoxLang dialect's own {@code <<}/{@code b<<} compiles to - rather than changing that operator's
 * behavior, which would affect native BoxLang and CFML source as well. Only the Groovy compiler
 * front-end ({@code GroovyExpressionVisitor#visitShiftExpr}) ever emits a call into this class.
 * <p>
 * When the left operand is a collection ({@link Array} or {@link BoxSet}), the right operand is
 * appended in place and the (mutated) collection itself is returned, matching real Groovy's
 * {@code <<} - and enabling the same chaining idiom, {@code list << a << b}. For anything else,
 * this simply delegates to the existing {@link BitwiseSignedLeftShift} implementation, so numeric
 * {@code <<} behaves identically to every other BoxLang dialect. Groovy's own {@code String <<}
 * (which returns a mutable buffer, not a plain String) is deliberately NOT modeled - a narrower,
 * documented gap rather than an attempt at Groovy's full, more surprising semantics there.
 */
public class GroovyLeftShiftOrAppend implements IOperator {

	/**
	 * @param left  The left operand
	 * @param right The right operand
	 *
	 * @return The appended collection, or the numeric left-shift result
	 */
	public static Object invoke( Object left, Object right ) {
		if ( left instanceof Array array ) {
			array.add( right );
			return array;
		}
		if ( left instanceof BoxSet set ) {
			set.add( right );
			return set;
		}
		return BitwiseSignedLeftShift.invoke( left, right );
	}

}
