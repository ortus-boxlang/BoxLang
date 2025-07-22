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

/**
 * Utility class for processing various types of loops in BoxLang components.
 * This class provides static methods for handling query loops, both simple and grouped,
 * with support for nested grouping, row ranges, and loop control statements.
 *
 * <p>
 * The class supports:
 * <ul>
 * <li>Simple query loops with start/end row constraints</li>
 * <li>Grouped query loops with support for nested grouping</li>
 * <li>Loop control statements (break, continue, return)</li>
 * <li>Case-sensitive and case-insensitive group comparisons</li>
 * </ul>
 *
 * <p>
 * Query loops can be configured with:
 * <ul>
 * <li>Start row (1-based index)</li>
 * <li>End row (1-based index)</li>
 * <li>Maximum number of rows to process</li>
 * <li>Labels for loop control</li>
 * </ul>
 *
 * @author BoxLang Development Team
 */
public class LoopUtil {

	/**
	 * Safety counter to prevent infinite loops during grouped query processing.
	 * Throws an exception if more than 1000 iterations are attempted.
	 */
	static int maxloop = 0;

	/**
	 * Processes a simple query loop without grouping functionality.
	 * This method iterates through query rows sequentially, executing the provided
	 * component body for each row within the specified range.
	 *
	 * <p>
	 * The loop supports:
	 * <ul>
	 * <li>Row range constraints (startRow, endRow, maxRows)</li>
	 * <li>Loop control statements (break, continue, return) with optional labels</li>
	 * <li>Proper query row registration for column reference resolution</li>
	 * </ul>
	 *
	 * <p>
	 * Row numbering is 1-based in the parameters but converted to 0-based internally
	 * for query processing.
	 *
	 * @param component      The component containing the loop body to execute
	 * @param context        The current execution context
	 * @param body           The component body to execute for each iteration
	 * @param executionState The execution state struct for storing loop metadata
	 * @param queryOrName    Either a Query object or a string name referencing a query variable
	 * @param startRow       The starting row number (1-based, inclusive). If null, starts at row 1
	 * @param endRow         The ending row number (1-based, inclusive). If null, uses maxRows or query size
	 * @param maxRows        Maximum number of rows to process. If null and endRow is null, processes all rows
	 * @param label          Optional label for break/continue statements. Can be null
	 *
	 * @return A BodyResult indicating how the loop terminated (normal completion, break, continue, or return)
	 *
	 * @throws IllegalArgumentException if the query name is empty or the query cannot be resolved
	 * @throws ClassCastException       if queryOrName cannot be cast to a Query object
	 */
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

		Query	theQuery			= getQuery( context, queryOrName );
		int		iStartRow			= calculateStartRow( startRow );
		int		iEndRow				= calculateEndRow( startRow, endRow, maxRows, theQuery.size() );
		// -1 means the query wasn't originally registered
		int		originalQueryLoop	= context.getQueryRow( theQuery, -1 );

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
			if ( originalQueryLoop > -1 ) {
				// If we were originally registered, then unregister us
				context.registerQueryLoop( theQuery, originalQueryLoop );
			} else {
				// Otherwise, we were never registered, so just unregister us
				context.unregisterQueryLoop( theQuery );
			}
		}
		return Component.DEFAULT_RETURN;
	}

	/**
	 * Processes a grouped query loop with support for nested grouping hierarchies.
	 * This method groups query rows by specified column values and executes the component
	 * body once per group change, allowing for complex nested loop structures.
	 *
	 * <p>
	 * Grouping behavior:
	 * <ul>
	 * <li>Groups are determined by comparing values in specified columns</li>
	 * <li>The loop executes the body when entering a new group</li>
	 * <li>Nested loops can create hierarchical grouping structures</li>
	 * <li>Group comparisons can be case-sensitive or case-insensitive</li>
	 * </ul>
	 *
	 * <p>
	 * The method handles complex scenarios including:
	 * <ul>
	 * <li>Parent-child group relationships</li>
	 * <li>Group change detection across multiple levels</li>
	 * <li>Proper row advancement when groups change</li>
	 * <li>Break/continue statement propagation between nested loops</li>
	 * </ul>
	 *
	 * <p>
	 * Example usage for nested grouping:
	 * 
	 * <pre>
	 * // Outer loop: group by department
	 * bx:loop query="employees" group="department"
	 *   // Inner loop: group by manager within department
	 *   bx:loop group="manager"
	 *     // Process each employee in this manager/department combination
	 *     bx:loop
	 * </pre>
	 *
	 * @param component            The component containing the loop body to execute
	 * @param context              The current execution context
	 * @param body                 The component body to execute for each group iteration
	 * @param executionState       The execution state struct for storing loop metadata
	 * @param queryOrName          Either a Query object or a string name referencing a query variable
	 * @param group                Comma-separated list of column names to group by. Can be null for ungrouped behavior
	 * @param groupCaseSensitive   Whether group value comparisons should be case-sensitive
	 * @param startRow             The starting row number (1-based, inclusive). If null, starts at row 1
	 * @param endRow               The ending row number (1-based, inclusive). If null, uses maxRows or query size
	 * @param maxRows              Maximum number of rows to process. If null and endRow is null, processes all rows
	 * @param label                Optional label for break/continue statements. Can be null
	 * @param parentExecutionState Execution state from parent grouped loop, used for nested grouping. Can be null
	 *
	 * @return A BodyResult indicating how the loop terminated (normal completion, break, continue, or return)
	 *
	 * @throws IllegalArgumentException if the query name is empty or the query cannot be resolved
	 * @throws IllegalStateException    if more than 1000 loop iterations are attempted (safety measure)
	 * @throws ClassCastException       if queryOrName cannot be cast to a Query object
	 */
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
		// -1 means the query wasn't originally registered
		int originalQueryLoop = context.getQueryRow( theQuery, -1 );

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
				groupData.setCurrentRow( i ).calcGroupValuesForRow();

				// Run the code inside of the output loop
				Component.BodyResult bodyResult = component.processBody( context, body );

				if ( groupData.hasChildGroup() && i != groupData.getCurrentRow() ) {
					// System.out.println( "taking child current row of " + groupData.getCurrentRow() + " and overwriting my row of " + i + " for group [" + group + "]" );
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
							// System.out.println( "one of our groups has changed, exiting this inner loop [" + group + "]" );
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
							// If the parent hasn't changed and we didn't break in this loop, then just continue to the next row
							if ( groupData.getParentGroup().isSameGroupAggregate() && !bodyResult.isBreak( label ) ) {
								// System.out.println( "parent group is same, so continuing to next row [" + i + "] of group [" + group + "]" );
								continue;
							} else {
								// Otherwise, we need to break out of this loop and return to our parent group
								// System.out.println( "done running off rows, breaking to parent -- group [" + group + "]" );
								break;
							}
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
			if ( originalQueryLoop > -1 ) {
				// If we were originally registered, then unregister us
				context.registerQueryLoop( theQuery, originalQueryLoop );
			} else {
				// Otherwise, we were never registered, so just unregister us
				context.unregisterQueryLoop( theQuery );
			}
		}
		return Component.DEFAULT_RETURN;
	}

	/**
	 * Resolves a query object from either a direct Query reference or a string variable name.
	 * This method handles the common pattern where loop components can accept either
	 * a query object directly or a string name that references a query variable in the context.
	 *
	 * @param context     The current execution context used for variable resolution
	 * @param queryOrName Either a Query object directly, or a String containing the name
	 *                    of a variable that holds a Query object
	 *
	 * @return The resolved Query object
	 *
	 * @throws IllegalArgumentException if queryOrName is a string but is empty
	 * @throws ClassCastException       if the resolved object cannot be cast to a Query
	 * @throws RuntimeException         if the variable name cannot be resolved in the context
	 */
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
	 * Converts from 1-based user input to 0-based internal indexing.
	 *
	 * <p>
	 * Conversion rules:
	 * <ul>
	 * <li>null input → 0 (start at first row)</li>
	 * <li>1-based input → 0-based output (subtract 1)</li>
	 * </ul>
	 *
	 * @param startRow The starting row (1-based, can be null)
	 *
	 * @return The effective zero-based start row index
	 */
	private static int calculateStartRow( Integer startRow ) {
		return ( startRow == null ) ? 0 : ( startRow - 1 );
	}

	/**
	 * Calculates the effective end row for a query loop, considering all constraints.
	 * Converts from 1-based user input to 0-based internal indexing and applies
	 * the most restrictive constraint among endRow, maxRows, and query size.
	 *
	 * <p>
	 * Constraint priority (most restrictive wins):
	 * <ol>
	 * <li>endRow (if specified) - direct end boundary</li>
	 * <li>maxRows (if specified) - relative to start row</li>
	 * <li>querySize - never exceed available data</li>
	 * </ol>
	 *
	 * <p>
	 * Calculation logic:
	 * <ul>
	 * <li>If endRow specified: use endRow - 1 (convert to 0-based)</li>
	 * <li>If maxRows specified: startRow + maxRows - 1</li>
	 * <li>Otherwise: querySize - 1 (process all remaining rows)</li>
	 * <li>Final result: min(calculated_end, querySize - 1)</li>
	 * </ul>
	 *
	 * @param startRow  The starting row (1-based, can be null)
	 * @param endRow    The ending row (1-based, can be null)
	 * @param maxRows   The maximum number of rows to process (can be null)
	 * @param querySize The total number of rows in the query
	 *
	 * @return The effective zero-based end row index, guaranteed to be within query bounds
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
	 * Data structure for managing grouped query loop state and hierarchical grouping relationships.
	 * 
	 * <p>This class encapsulates all the state needed to track group changes during query loop execution,
	 * including support for nested grouping where multiple GroupData instances form a parent-child hierarchy.
	 * 
	 * <p>Key responsibilities:
	 * <ul>
	 *   <li>Track current row position across the group hierarchy</li>
	 *   <li>Detect when group values change by comparing current vs. previous row values</li>
	 *   <li>Manage parent-child relationships for nested grouped loops</li>
	 *   <li>Provide aggregate group change detection across the entire hierarchy</li>
	 * </ul>
	 * 
	 * <p>Group change detection works by:
	 * <ol>
	 *   <li>Storing the previous row's group column values</li>
	 *   <li>Comparing current row's values against previous values</li>
	 *   <li>Setting isSameGroup flag based on comparison results</li>
	 *   <li>Recursively updating parent group data for hierarchy consistency</li>
	 * </ol>
	 * 
	 * <p>Example hierarchy for nested grouping:
	 * <pre>
	 * Department GroupData (parent)
	 *   ↳ Manager GroupData (child)
	 *       ↳ Employee loop (no GroupData, just iterates)
	 * </pre>
	 * 
	 * @see #calcGroupValuesForRow() for group change detection logic
	 * @see #isSameGroupAggregate() for hierarchy-wide group status
	 */
	public static class GroupData {

		/** 
		 * Current row index (0-based). 
		 * In hierarchical grouping, only the root parent maintains the authoritative position.
		 */
		private int			currentRow		= 0;
		
		/** 
		 * Array of column keys to group by. 
		 * Null or empty array indicates no grouping at this level.
		 */
		private Key[]		groupKeys;
		
		/** 
		 * Whether group value comparisons should be case-sensitive.
		 */
		private boolean		groupCaseSensitive;
		
		/** 
		 * Values from the previous row for group change detection.
		 * Null on first iteration, populated thereafter.
		 */
		private Object[]	lastGroupValues	= null;
		
		/** 
		 * Whether the current row belongs to the same group as the previous row.
		 * Updated by calcGroupValuesForRow().
		 */
		private boolean		isSameGroup		= true;
		
		/** 
		 * Reference to parent GroupData in nested grouping hierarchy.
		 * Null for top-level groups.
		 */
		private GroupData	parentGroup		= null;
		
		/** 
		 * Whether this GroupData has child grouped loops nested within it.
		 * Used to optimize loop advancement logic.
		 */
		private boolean		hasChild		= false;
		
		/** 
		 * The query being processed.
		 * Used for retrieving cell values during group comparison.
		 */
		private Query		query;

		/**
		 * Constructs a new GroupData instance for managing grouped query loop state.
		 * 
		 * <p>Automatically establishes parent-child relationships when a parentGroup is provided,
		 * ensuring the hierarchy is properly maintained for nested grouped loops.
		 * 
		 * @param groupKeys Array of column keys to group by. Can be null for ungrouped behavior
		 * @param groupCaseSensitive Whether group value comparisons should be case-sensitive
		 * @param query The query object being processed
		 * @param parentGroup Parent GroupData for nested grouping hierarchy. Can be null for top-level groups
		 */
		public GroupData( Key[] groupKeys, boolean groupCaseSensitive, Query query, GroupData parentGroup ) {
			this.groupKeys			= groupKeys;
			this.groupCaseSensitive	= groupCaseSensitive;
			this.query				= query;
			this.parentGroup		= parentGroup;
			if ( parentGroup != null ) {
				parentGroup.setHasChildGroup();
			}
		}

		/**
		 * Gets the current row index for the group hierarchy.
		 * 
		 * <p>In nested grouping scenarios, only the root parent maintains the authoritative
		 * row position. Child groups delegate to their parent to ensure consistency
		 * across the entire hierarchy.
		 * 
		 * @return The current 0-based row index
		 */
		public int getCurrentRow() {
			return parentGroup == null ? currentRow : parentGroup.getCurrentRow();
		}

		/**
		 * Sets the current row index for the group hierarchy.
		 * 
		 * <p>In nested grouping scenarios, the row position is always set at the root
		 * parent level to maintain consistency. Child groups delegate the operation
		 * up the hierarchy chain.
		 * 
		 * @param currentRow The new 0-based row index to set
		 * @return This GroupData instance for method chaining
		 */
		public GroupData setCurrentRow( int currentRow ) {
			if ( parentGroup == null ) {
				this.currentRow = currentRow;
			} else {
				parentGroup.setCurrentRow( currentRow );
			}
			return this;
		}

		/**
		 * Returns whether the current row belongs to the same group as the previous row.
		 * 
		 * <p>This flag is updated by {@link #calcGroupValuesForRow()} and reflects
		 * whether any of the group column values changed between the previous and current row.
		 * 
		 * @return true if current row is in the same group as previous row, false if group changed
		 */
		public boolean isSameGroup() {
			return isSameGroup;
		}

		/**
		 * Checks if this group and all parent groups in the hierarchy are unchanged.
		 * 
		 * <p>This method provides aggregate group status across the entire nesting hierarchy.
		 * It returns true only if:
		 * <ul>
		 *   <li>This group's values haven't changed (isSameGroup == true), AND</li>
		 *   <li>All parent groups' values haven't changed (recursive check)</li>
		 * </ul>
		 * 
		 * <p>This is crucial for nested grouping logic where a change in any parent group
		 * should trigger group boundary processing even if the current level hasn't changed.
		 * 
		 * @return true if this group and all parent groups are unchanged, false otherwise
		 */
		public boolean isSameGroupAggregate() {
			return isSameGroup && ( parentGroup == null || parentGroup.isSameGroup() );
		}

		/**
		 * Sets the same group flag for this GroupData instance.
		 * 
		 * <p>This method is typically called by {@link #calcGroupValuesForRow()}
		 * after comparing current and previous row values.
		 * 
		 * @param isSameGroup true if current row is in same group as previous, false if group changed
		 * @return This GroupData instance for method chaining
		 */
		public GroupData setSameGroup( boolean isSameGroup ) {
			this.isSameGroup = isSameGroup;
			return this;
		}

		/**
		 * Calculates and compares group values for the current row to detect group changes.
		 * 
		 * <p>This is the core group change detection algorithm. It:
		 * <ol>
		 *   <li>Retrieves current row values for all group columns</li>
		 *   <li>Compares them against stored previous row values</li>
		 *   <li>Sets isSameGroup flag based on comparison results</li>
		 *   <li>Stores current values as "previous" for next iteration</li>
		 *   <li>Recursively updates parent group data for hierarchy consistency</li>
		 * </ol>
		 * 
		 * <p>Group change detection rules:
		 * <ul>
		 *   <li>First row (lastGroupValues == null): Always considered same group (true)</li>
		 *   <li>Subsequent rows: Compare each group column value using Configure.invoke()</li>
		 *   <li>Any changed value → isSameGroup = false</li>
		 *   <li>All values identical → isSameGroup = true</li>
		 * </ul>
		 * 
		 * <p>The comparison respects the groupCaseSensitive setting for string comparisons.
		 * 
		 * @return This GroupData instance for method chaining
		 */
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

		/**
		 * Gets the query object being processed by this group.
		 * 
		 * @return The Query object
		 */
		public Query getQuery() {
			return query;
		}

		/**
		 * Checks if this GroupData has a parent in the grouping hierarchy.
		 * 
		 * @return true if this group has a parent (is nested), false if this is a top-level group
		 */
		public boolean hasParentGroup() {
			return parentGroup != null;
		}

		/**
		 * Gets the parent GroupData in the grouping hierarchy.
		 * 
		 * @return The parent GroupData, or null if this is a top-level group
		 */
		public GroupData getParentGroup() {
			return parentGroup;
		}

		/**
		 * Checks if this GroupData has child grouped loops nested within it.
		 * 
		 * <p>This flag is used to optimize loop advancement logic. When a group has children,
		 * the parent loop needs to coordinate with child loops to determine proper row advancement.
		 * 
		 * @return true if child grouped loops exist within this group, false otherwise
		 */
		public boolean hasChildGroup() {
			return hasChild;
		}

		/**
		 * Marks this GroupData as having child grouped loops.
		 * 
		 * <p>This method is automatically called during GroupData construction when
		 * a child GroupData is created with this instance as its parent.
		 * 
		 * @return This GroupData instance for method chaining
		 */
		public GroupData setHasChildGroup() {
			this.hasChild = true;
			return this;
		}

		/**
		 * Checks if this GroupData instance has active grouping configured.
		 * 
		 * <p>A group "has groups" if it was configured with one or more column names
		 * to group by. Groups without grouping configuration behave like simple iteration.
		 * 
		 * @return true if group keys are configured and non-empty, false otherwise
		 */
		public boolean hasGroups() {
			return groupKeys != null && groupKeys.length > 0;
		}
	}
}
