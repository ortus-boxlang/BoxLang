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
package ortus.boxlang.runtime.dynamic;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.GenericCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.ListUtil;
import ortus.boxlang.runtime.types.Query;

/**
 * I help iterating over queries in tag output
 */
public class QueryOutputUtil {

	@FunctionalInterface
	public interface ContextConsumer {

		void accept( IBoxContext context );
	}

	public static void doLoop( IBoxContext context, Object queryOrName, Object group, Object groupCaseSensitive, Object startRow, Object maxRows,
	    ContextConsumer consumer ) {
		CastAttempt<String>	queryName	= StringCaster.attempt( queryOrName );
		Query				theQuery;
		// If the input is a string, get a variable in the context of that name
		if ( queryName.wasSuccessful() ) {
			queryOrName = ExpressionInterpreter.getVariable( context, queryName.get(), false );
		}
		// See if what we have is a query!
		theQuery = ( Query ) GenericCaster.cast( queryOrName, "query" );
		int	iStartRow	= ( startRow == null ) ? 0 : ( ( int ) GenericCaster.cast( startRow, "int" ) - 1 );
		int	iEndRow		= ( maxRows == null ) ? ( theQuery.size() - 1 ) : ( ( int ) GenericCaster.cast( maxRows, "int" ) + iStartRow - 1 );

		// TODO: Throw exceptions if incoming data is out of bounds?
		iStartRow	= Math.max( iStartRow, 0 );
		iEndRow		= Math.min( iEndRow, theQuery.size() - 1 );

		boolean		isGrouped		= group != null;
		Key[]		groupKeys		= null;
		Object[]	lastGroupValues	= null;
		if ( isGrouped ) {
			groupKeys		= ListUtil.asList( StringCaster.cast( group ), "," )
			    .stream()
			    .map( ( c ) -> Key.of( ( ( String ) c ).trim() ) )
			    .toArray( Key[]::new );
			lastGroupValues	= new Object[ groupKeys.length ];
		}
		// This allows query references to know what row we're on and for unscoped column references to work
		context.registerQueryLoop( theQuery, iStartRow );
		try {
			for ( int i = iStartRow; i <= iEndRow; i++ ) {
				if ( isGrouped ) {
					Object[]	thisGroupValues	= new Object[ groupKeys.length ];
					boolean		isSameGroup		= true;
					for ( int j = 0; j < groupKeys.length; j++ ) {
						thisGroupValues[ j ] = theQuery.getCell( groupKeys[ j ], i );
						if ( !thisGroupValues[ j ].equals( lastGroupValues[ j ] ) ) {
							isSameGroup = false;
						}
					}
					if ( isSameGroup ) {
						// Next row, please!
						context.incrementQueryLoop( theQuery );
						continue;
					} else {
						lastGroupValues = thisGroupValues;
					}
				}
				// Run the code inside of the output loop
				consumer.accept( context );
				// Next row, please!
				context.incrementQueryLoop( theQuery );
			}
		} finally {
			// This query is DONE!
			context.unregisterQueryLoop( theQuery );
		}
	}
}