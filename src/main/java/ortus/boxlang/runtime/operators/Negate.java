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

import java.math.BigDecimal;

import ortus.boxlang.runtime.dynamic.casters.NumberCaster;

/**
 * Performs mathematic Negation
 * {@code a = -num}
 */
public class Negate implements IOperator {

	/**
	 * @param object The object to negate
	 *
	 * @return The result
	 */
	public static Number invoke( Object object ) {
		Number nObject = NumberCaster.cast( object );
		if ( nObject instanceof BigDecimal bd ) {
			BigDecimal d = bd.negate();
			if ( d.compareTo( BigDecimal.ZERO ) == 0 ) {
				return BigDecimal.ZERO;
			} else {
				return d;
			}
		} else if ( nObject instanceof Integer ) {
			int l = nObject.intValue();
			if ( l == 0 ) {
				return 0;
			} else {
				return -l;
			}
		} else if ( nObject instanceof Long ) {
			long l = nObject.longValue();
			if ( l == 0 ) {
				return 0;
			} else {
				return -l;
			}
		} else {
			double d = nObject.doubleValue();
			if ( d == 0 ) {
				return 0;
			} else {
				return -d;
			}
		}
	}

}
