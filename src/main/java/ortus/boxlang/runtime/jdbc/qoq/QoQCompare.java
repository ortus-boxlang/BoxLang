
/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.jdbc.qoq;

import ortus.boxlang.runtime.operators.Compare;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;

/**
 * I handle executing query of queries
 */
public class QoQCompare {

	/**
	 * Perform a compare optimized for query types.
	 * We're assuming both inputs are the same type for now
	 * 
	 * @param type  the type of the expressions being compared
	 * @param left  the left value
	 * @param right the right value
	 * 
	 * @return
	 */
	public static int invoke( QueryColumnType type, Object left, Object right ) {
		Integer result = null;

		// This code may suffer from cast exceptions due to us assuming the type of the column
		// Add defensive checks to this as neccessary. We are purposefully not just letting the casters sort it
		// since we want raw performance based on the types we SHOULD already be able to know from the query
		// System.out.println( "QoQCompare.java: invoke: type: " + type + ", left: " + left + ", right: " + right );
		try {
			if ( left == null && right == null ) {
				result = 0;
			} else if ( left == null ) {
				result = -1;
			} else if ( right == null ) {
				result = 1;
			} else if ( type == QueryColumnType.VARCHAR || type == QueryColumnType.CHAR ) {
				result = left.toString().compareToIgnoreCase( right.toString() );
			} else if ( type == QueryColumnType.BIGINT || type == QueryColumnType.DECIMAL || type == QueryColumnType.DOUBLE
			    || type == QueryColumnType.INTEGER ) {
				if ( left instanceof Double ld && right instanceof Double rd ) {
					result = ld.compareTo( rd );
				} else if ( left instanceof Integer li && right instanceof Integer ri ) {
					result = li.compareTo( ri );
				} else if ( left instanceof Long ll && right instanceof Long rl ) {
					result = ll.compareTo( rl );
				} else if ( left instanceof Number ln && right instanceof Number rn ) {
					result = Double.compare( ln.doubleValue(), rn.doubleValue() );
				}
			} else if ( type == QueryColumnType.BIT || type == QueryColumnType.BOOLEAN ) {
				// Account for an int or a boolean
				Boolean	bLeft	= null;
				Boolean	bRight	= null;
				if ( left instanceof Boolean bl ) {
					bLeft = bl;
				} else if ( left instanceof Number nl ) {
					bLeft = nl.intValue() == 1;
				}
				if ( bLeft != null ) {
					if ( right instanceof Boolean br ) {
						bRight = br;
					} else if ( right instanceof Number nr ) {
						bRight = nr.intValue() == 1;
					}
					if ( bRight != null ) {
						result = bLeft.compareTo( bRight );
					}
				}

			} else if ( type == QueryColumnType.DATE || type == QueryColumnType.TIME || type == QueryColumnType.TIMESTAMP ) {
				// Dates SHOULD get picked up by the casters
				result = Compare.invoke( left, right );
				// System.out.println( "compare.invoke1" );
			} else {
				// All other types, we'll just let the casters sort it out
				result = Compare.invoke( left, right );
				// System.out.println( "compare.invoke2" );
			}

			// If the casting didn't work for our strict comparisons above, we'll let the casters sort it out
			if ( result == null ) {
				result = Compare.invoke( left, right );
				// System.out.println( "compare.invoke3" );
			}
		} catch ( ClassCastException e ) {
			String	leftType	= left == null ? "null" : left.getClass().getName();
			String	rightType	= right == null ? "null" : right.getClass().getName();
			throw new DatabaseException( "SQL Comparison -- unexpected data [" + leftType + "/" + rightType + "] in column of type " + type.toString(), e );
		}
		return result;
	}
}
