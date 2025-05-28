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

import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.GenericCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.operators.Compare;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.util.ListUtil;

public class LoopUtil {

	static int maxloop = 0;

	public static Component.BodyResult processQueryLoop(
	    Component component,
	    IBoxContext context,
	    Component.ComponentBody body,
	    IStruct executionState,
	    Object queryOrName,
	    Integer startRow,
	    Integer endRow,
	    Integer maxRows,
	    String label ) {

		Query	theQuery	= getQuery( context, queryOrName );
		int		iStartRow	= calculateStartRow( startRow );
		int		iEndRow		= calculateEndRow( startRow, endRow, maxRows, theQuery.size() );

		// If there's nothing to loop over, exit stage left
		if ( iEndRow < iStartRow ) {
			return Component.DEFAULT_RETURN;
		}

		// This allows query references to know what row we're on and for unscoped
		// column references to work
		context.registerQueryLoop( theQuery, iStartRow );
		try {
			for ( int i = iStartRow; i <= iEndRow; i++ ) {
				// Run the code inside of the output loop
				Component.BodyResult bodyResult = component.processBody( context, body );
				// IF there was a return statement inside our body, we early exit now
				if ( bodyResult.isEarlyExit() ) {
					if ( bodyResult.isContinue( label ) ) {
						context.registerQueryLoop( theQuery, i + 1 );
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

	public static Component.BodyResult processQueryLoopGrouped(
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
	    String label,
	    IStruct parentExecutionState ) {

		Query	theQuery	= getQuery( context, queryOrName );
		int		iStartRow	= calculateStartRow( startRow );
		int		iEndRow		= calculateEndRow( startRow, endRow, maxRows, theQuery.size() );
		executionState.put( Key.endRow, iEndRow + 1 );

		// If there's nothing to loop over, exit stage left
		if ( iEndRow < iStartRow ) {
			return Component.DEFAULT_RETURN;
		}

		GroupData groupData = new GroupData(
		    group != null && !group.isBlank() ? ListUtil.asList( group, "," )
		        .stream()
		        .map( ( c ) -> Key.of( ( ( String ) c ).trim() ) )
		        .toArray( Key[]::new ) : null,
		    groupCaseSensitive,
		    theQuery,
		    parentExecutionState == null ? null : ( GroupData ) parentExecutionState.get( Key.groupData ) );

		executionState.put( Key.groupData, groupData );

		try {
			for ( int i = iStartRow; i <= iEndRow; i++ ) {
				maxloop++;
				if ( maxloop > 1000 ) {
					throw new IllegalStateException(
					    "Looping over more than 1000 rows is not allowed, please check your query or loop logic." );
				}
				// This allows query references to know what row we're on and for unscoped
				// column references to work
				context.registerQueryLoop( theQuery, i );
				// System.out.println( "i: " + i + " for group [" + group + "]" );
				groupData.setCurrentRow( i );

				// Run the code inside of the output loop
				Component.BodyResult bodyResult = component.processBody( context, body );

				if ( groupData.hasChildGroup() ) {
					/*
					 * System.out.println(
					 * "taking child currenrt row of " + groupData.getCurrentRow() + " and overwriting my row of " + i + " for group [" + group + "]" );
					 */
					i = groupData.getCurrentRow();
				}

				// If there was a return statement inside our body, we early exit now
				if ( bodyResult.isReturn() ) {
					return bodyResult;
				}

				// so long as there are more rows
				if ( i + 1 < theQuery.size() ) {
					// If there are no children loops "inside" of us, or we have no grouping
					// ourselves, then we are a dead end...
					if ( !groupData.hasChildGroup() || !groupData.hasGroups() ) {
						// ... so let's calc the next row
						groupData.setCurrentRow( i + 1 ).calcGroupValuesForRow();
						// If some group data has changed, we need to exit this inner loop
						if ( !groupData.isSameGroupAggregate() && groupData.hasParentGroup() ) {
							/*
							 * System.out
							 * .println( "one of our groups has changed, exiting this inner loop [" + group + "]" );
							 */
							groupData.setCurrentRow( i );
							break;
						}

					}
					// if we have no child loop and we have groupings...
					if ( !groupData.hasChildGroup() && groupData.hasGroups() ) {
						// ... then we need to "run off" the grouped rows
						// System.out.println( "no child group, so running off identical rows of group [" + group + "]" );
						while ( ++i <= iEndRow
						    && groupData.setCurrentRow( i ).calcGroupValuesForRow().isSameGroupAggregate() ) {
							// System.out.println( "skipping row [" + i + "] of group [" + group + "]" );
						}
						i--;
						if ( groupData.hasParentGroup() ) {
							groupData.setCurrentRow( i );
							break;
						}
					}

					// Check this after the logic above, so we can "run off" any rows first
					if ( bodyResult.isBreak( label ) ) {
						// Run off the rest of our rows until our parent group changes
						if ( groupData.hasGroups() && groupData.hasParentGroup() ) {
							while ( ++i <= iEndRow
							    && groupData.getParentGroup().setCurrentRow( i ).calcGroupValuesForRow().isSameGroupAggregate() ) {
								// System.out.println( "we have break, so skipping row [" + i + "] of group [" + group + "]" );
							}
							i--;
							groupData.setCurrentRow( i );
						}

						break;
					}

				}

				// we just exited from our body (and possibly inner loops), and our current
				// level of grouping has changed and we have a parent, then keep unrolling the
				// stack
				if ( groupData.hasParentGroup() && !groupData.getParentGroup().isSameGroupAggregate() ) {
					// System.out.println( "my parents' group has changed, exiting this inner loop [" + group + "]" );
					groupData.setCurrentRow( i );
					break;
				}

			}
		} finally {
			context.unregisterQueryLoop( theQuery );
		}
		return Component.DEFAULT_RETURN;
	}

	private static Query getQuery( IBoxContext context, Object queryOrName ) {
		CastAttempt<String> queryName = StringCaster.attempt( queryOrName );
		// If the input is a string, get a variable in the context of that name
		if ( queryName.wasSuccessful() ) {
			// check not empty
			if ( queryName.get().isEmpty() ) {
				throw new IllegalArgumentException( "The query name cannot be empty" );
			}
			queryOrName = ExpressionInterpreter.getVariable( context, queryName.get(), false );
		}
		// See if what we have is a query!
		return ( Query ) GenericCaster.cast( context, queryOrName, "query" );
	}

	/**
	 * Calculates the effective start row for a query loop, given startRow.
	 * Returns zero-based index.
	 *
	 * @param startRow The starting row (1-based, can be null)
	 * 
	 * @return The effective zero-based start row index
	 */
	private static int calculateStartRow( Integer startRow ) {
		return ( startRow == null ) ? 0 : ( startRow - 1 );
	}

	/**
	 * Calculates the effective end row for a query loop, given startRow, endRow,
	 * maxRows, and the query size.
	 * Returns zero-based index.
	 *
	 * @param startRow  The starting row (1-based, can be null)
	 * @param endRow    The ending row (1-based, can be null)
	 * @param maxRows   The maximum number of rows to process (can be null)
	 * @param querySize The total number of rows in the query
	 * 
	 * @return The effective zero-based end row index
	 */
	private static int calculateEndRow( Integer startRow, Integer endRow, Integer maxRows, int querySize ) {
		int	iStartRow	= calculateStartRow( startRow );
		int	iEndRow;
		if ( endRow != null ) {
			iEndRow = endRow - 1;
		} else {
			iEndRow = ( maxRows == null ) ? ( querySize - 1 ) : ( maxRows + iStartRow - 1 );
		}
		return Math.min( iEndRow, querySize - 1 );
	}

	/**
	 * A class to hold the group data for a query loop. There will be one of these
	 * for each nested grouped query loop.
	 */
	public static class GroupData {

		// 0-based index
		private int			currentRow		= 0;
		private Key[]		groupKeys;
		private boolean		groupCaseSensitive;
		private Object[]	lastGroupValues	= null;
		private boolean		isSameGroup		= true;
		private GroupData	parentGroup		= null;
		private boolean		hasChild		= false;
		private Query		query;

		public GroupData( Key[] groupKeys, boolean groupCaseSensitive, Query query, GroupData parentGroup ) {
			this.groupKeys			= groupKeys;
			this.groupCaseSensitive	= groupCaseSensitive;
			this.query				= query;
			this.parentGroup		= parentGroup;
			if ( parentGroup != null ) {
				parentGroup.setHasChildGroup();
			}
		}

		public int getCurrentRow() {
			return parentGroup == null ? currentRow : parentGroup.getCurrentRow();
		}

		public GroupData setCurrentRow( int currentRow ) {
			if ( parentGroup == null ) {
				this.currentRow = currentRow;
			} else {
				parentGroup.setCurrentRow( currentRow );
			}
			return this;
		}

		public boolean isSameGroup() {
			return isSameGroup;
		}

		/**
		 * Is my level and all my parents the same group?
		 * 
		 * @return true if this group and all parent groups are the same
		 */
		public boolean isSameGroupAggregate() {
			return isSameGroup && ( parentGroup == null || parentGroup.isSameGroup() );
		}

		public GroupData setSameGroup( boolean isSameGroup ) {
			this.isSameGroup = isSameGroup;
			return this;
		}

		public GroupData calcGroupValuesForRow() {
			isSameGroup = true;

			if ( groupKeys != null ) {
				int			currentRow		= getCurrentRow();
				Object[]	thisGroupValues	= new Object[ groupKeys.length ];
				isSameGroup = true;
				// System.out.println( "calc group data for Row: " + currentRow + " with group keys: ["
				// + Arrays.toString( groupKeys ) + "]" );
				for ( int j = 0; j < groupKeys.length; j++ ) {
					thisGroupValues[ j ] = query.getCell( groupKeys[ j ], currentRow );
					if ( lastGroupValues != null
					    && Compare.invoke( thisGroupValues[ j ], lastGroupValues[ j ], groupCaseSensitive ) != 0 ) {
						/*
						 * System.out
						 * .println(
						 * "Row: " + currentRow + " is not the same as last row key "
						 * + groupKeys[ j ].getName() + " " + thisGroupValues[ j ] + " != "
						 * + lastGroupValues[ j ] );
						 */
						isSameGroup = false;
					}
				}
				lastGroupValues = thisGroupValues;
			}

			if ( parentGroup != null ) {
				parentGroup.calcGroupValuesForRow();
			}
			return this;
		}

		public Query getQuery() {
			return query;
		}

		public boolean hasParentGroup() {
			return parentGroup != null;
		}

		public GroupData getParentGroup() {
			return parentGroup;
		}

		public boolean hasChildGroup() {
			return hasChild;
		}

		public GroupData setHasChildGroup() {
			this.hasChild = true;
			return this;
		}

		public boolean hasGroups() {
			return groupKeys != null && groupKeys.length > 0;
		}
	}
}
