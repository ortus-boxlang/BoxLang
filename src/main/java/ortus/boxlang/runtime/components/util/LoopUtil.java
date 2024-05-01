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
package ortus.boxlang.runtime.components.util;

import java.util.HashMap;
import java.util.Map;

import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.GenericCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.operators.EqualsEquals;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.util.ListUtil;

public class LoopUtil {

	public static Component.BodyResult processQueryLoop(
	    Component component,
	    IBoxContext context,
	    Component.ComponentBody body,
	    IStruct executionState,
	    Object queryOrName,
	    String group,
	    Boolean groupCaseSensitive,
	    Integer startRow,
	    Integer endRow,
	    Integer maxRows,
	    String label ) {

		CastAttempt<String>	queryName	= StringCaster.attempt( queryOrName );
		Query				theQuery;
		// If the input is a string, get a variable in the context of that name
		if ( queryName.wasSuccessful() ) {
			// check not empty
			if ( queryName.get().isEmpty() ) {
				throw new IllegalArgumentException( "The query name cannot be empty" );
			}
			queryOrName = ExpressionInterpreter.getVariable( context, queryName.get(), false );
		}
		// See if what we have is a query!
		theQuery = ( Query ) GenericCaster.cast( context, queryOrName, "query" );
		int	iStartRow	= ( startRow == null ) ? 0 : ( startRow - 1 );
		int	iEndRow;
		if ( endRow != null ) {
			iEndRow = endRow - 1;
		} else {
			iEndRow = ( maxRows == null ) ? ( theQuery.size() - 1 ) : ( maxRows + iStartRow - 1 );
		}

		// TODO: Throw exceptions if incoming data is out of bounds?
		iEndRow = Math.min( iEndRow, theQuery.size() - 1 );

		// If there's nothing to loop over, exit stage left
		if ( iEndRow <= iStartRow ) {
			return Component.DEFAULT_RETURN;
		}

		boolean					isGrouped			= group != null;
		Key[]					groupKeys			= null;
		Object[]				lastGroupValues		= null;
		Map<String, Boolean>	isSameGroup			= new HashMap<>();
		int						currentGroupEndRow	= 0;
		isSameGroup.put( "value", true );

		if ( isGrouped ) {
			groupKeys		= ListUtil.asList( group, "," )
			    .stream()
			    .map( ( c ) -> Key.of( ( ( String ) c ).trim() ) )
			    .toArray( Key[]::new );

			lastGroupValues	= new Object[ groupKeys.length ];
			// Calculate the group values for the first row
			lastGroupValues	= getGroupValuesForRow( theQuery, groupKeys, lastGroupValues, iStartRow, isSameGroup );
		}
		// This allows query references to know what row we're on and for unscoped column references to work
		context.registerQueryLoop( theQuery, iStartRow );
		try {
			for ( int i = iStartRow; i <= iEndRow; i++ ) {
				// System.out.println( "i: " + i );
				if ( isGrouped ) {
					if ( i < iEndRow ) {
						lastGroupValues = getGroupValuesForRow( theQuery, groupKeys, lastGroupValues, i + 1, isSameGroup );
					} else {
						isSameGroup.put( "value", false );
					}
					if ( isSameGroup.get( "value" ) ) {
						// System.out.println( "Same group" );
						continue;
					} else {
						// TODO: handle nested output with groups
						currentGroupEndRow = i;
					}
				}
				// Run the code inside of the output loop
				Component.BodyResult bodyResult = component.processBody( context, body );
				// IF there was a return statement inside our body, we early exit now
				if ( bodyResult.isEarlyExit() ) {
					if ( bodyResult.isContinue( label ) ) {
						continue;
					} else if ( bodyResult.isBreak( label ) ) {
						break;
					} else {
						return bodyResult;
					}
				}
				// Next row, please!
				context.registerQueryLoop( theQuery, i + 1 );
			}
		} finally {
			// This query is DONE!
			context.unregisterQueryLoop( theQuery );
		}
		return Component.DEFAULT_RETURN;
	}

	private static Object[] getGroupValuesForRow( Query query, Key[] groupKeys, Object[] lastGroupValues, int row, Map<String, Boolean> isSameGroup ) {
		Object[] thisGroupValues = new Object[ groupKeys.length ];
		isSameGroup.put( "value", true );
		// System.out.println( "calc group data for Row: " + row );
		for ( int j = 0; j < groupKeys.length; j++ ) {
			thisGroupValues[ j ] = query.getCell( groupKeys[ j ], row );
			// TODO: Use case sensitive flag. Perhaps this needs to use compare?
			if ( !EqualsEquals.invoke( thisGroupValues[ j ], lastGroupValues[ j ] ) ) {
				// System.out.println( "Row: " + row + " is not the same as last row key " + groupKeys[ j ].getName() + " " + thisGroupValues[ j ] + " != "
				// + lastGroupValues[ j ] );
				isSameGroup.put( "value", false );
			}
		}
		return thisGroupValues;
	}
}
