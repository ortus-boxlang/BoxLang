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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import ortus.boxlang.compiler.ast.sql.select.SQLJoin;
import ortus.boxlang.compiler.ast.sql.select.SQLJoinType;
import ortus.boxlang.compiler.ast.sql.select.SQLTable;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLExpression;
import ortus.boxlang.runtime.types.Query;

/**
 * I handle the generation of the intersection of tables in a QoQ statement
 */
public class QoQIntersectionGenerator {

	/**
	 * Create all possible intersections of the tables as a stream of int arrays
	 */
	public static Stream<int[]> createIntersectionStream( QoQSelectExecution QoQExec ) {
		Map<SQLTable, Query>	tableLookup			= QoQExec.tableLookup;
		Query					firstTable			= tableLookup.get( QoQExec.getSelect().getTable() );
		// This is just an estimation of the total number of combinations
		// Streams make it hard to get an exact count without collecting the stream, so we'll just guess
		int						totalCombinations	= firstTable.size();
		// stream of int arrays containing the index of the row in each table
		// use rangeClosed to include 1 through query size
		Stream<int[]>			theStream			= IntStream.rangeClosed( 1, firstTable.size() )
		    .mapToObj( i -> new int[] { i } );

		// If we have one or more joins, we need to create a stream of all possible intersections
		if ( QoQExec.getSelect().getJoins() != null ) {
			for ( SQLJoin thisJoin : QoQExec.getSelect().getJoins() ) {
				var	joinType	= thisJoin.getType();
				var	joinTable	= tableLookup.get( thisJoin.getTable() );
				var	joinOn		= thisJoin.getOn();

				if ( joinType.equals( SQLJoinType.CROSS ) || joinType.equals( SQLJoinType.INNER ) ) {
					theStream			= handleCrossOrInnerJoin( theStream, joinTable, joinOn, QoQExec );
					totalCombinations	*= joinTable.size();
				} else if ( joinType.equals( SQLJoinType.LEFT ) ) {
					theStream			= handleLeftJoin( theStream, joinTable, joinOn, QoQExec );
					totalCombinations	*= joinTable.size();
				} else if ( joinType.equals( SQLJoinType.RIGHT ) ) {
					theStream			= handleRightJoin( theStream, joinTable, joinOn, QoQExec );
					totalCombinations	*= joinTable.size();
				} else if ( joinType.equals( SQLJoinType.FULL ) ) {
					theStream			= handleFullOuterJoin( theStream, joinTable, joinOn, QoQExec );
					totalCombinations	*= joinTable.size();
				}
			}
		}

		// Tweak this based on size of intersections to process
		if ( totalCombinations > 50 ) {
			theStream = theStream.parallel();
		}
		return theStream;
	}

	/**
	 * Handle CROSS and INNER JOINs
	 * 
	 * @param theStream The current stream of intersections
	 * @param joinTable The table to join
	 * @param joinOn    The ON clause
	 * @param QoQExec   The current QoQ execution
	 * 
	 * @return The new stream of intersections
	 */
	private static Stream<int[]> handleCrossOrInnerJoin( Stream<int[]> theStream, Query joinTable, SQLExpression joinOn, QoQSelectExecution QoQExec ) {
		theStream = theStream.flatMap( i -> IntStream.rangeClosed( 1, joinTable.size() ).mapToObj( j -> {
			int[] newIntersection = Arrays.copyOf( i, i.length + 1 );
			newIntersection[ i.length ] = j;
			return newIntersection;
		} ) );
		if ( joinOn != null ) {
			theStream = theStream.filter( i -> ( Boolean ) joinOn.evaluate( QoQExec, i ) );
		}
		return theStream;
	}

	/**
	 * Handle LEFT JOINs
	 * 
	 * @param theStream The current stream of intersections
	 * @param joinTable The table to join
	 * @param joinOn    The ON clause
	 * @param QoQExec   The current QoQ execution
	 * 
	 * @return The new stream of intersections
	 */
	private static Stream<int[]> handleLeftJoin( Stream<int[]> theStream, Query joinTable, SQLExpression joinOn, QoQSelectExecution QoQExec ) {
		return theStream.flatMap( i -> {
			Stream<int[]>	newStream		= IntStream.rangeClosed( 1, joinTable.size() ).mapToObj( j -> {
												int[] newIntersection = Arrays.copyOf( i, i.length + 1 );
												newIntersection[ i.length ] = j;
												return newIntersection;
											} ).filter( j -> ( Boolean ) joinOn.evaluate( QoQExec, j ) );
			List<int[]>		newStreamList	= newStream.collect( Collectors.toList() );
			if ( newStreamList.isEmpty() ) {
				int[] leftOnlyIntersection = Arrays.copyOf( i, i.length + 1 );
				leftOnlyIntersection[ i.length ] = 0; // 0 indicates no match in the right table
				return Stream.of( leftOnlyIntersection );
			}
			return newStreamList.stream();
		} );
	}

	/**
	 * Handle RIGHT JOINs
	 * 
	 * @param theStream The current stream of intersections
	 * @param joinTable The table to join
	 * @param joinOn    The ON clause
	 * @param QoQExec   The current QoQ execution
	 * 
	 * @return The new stream of intersections
	 */
	private static Stream<int[]> handleRightJoin( Stream<int[]> theStream, Query joinTable, SQLExpression joinOn, QoQSelectExecution QoQExec ) {
		List<int[]>		leftRows	= theStream.collect( Collectors.toList() ); // Collect the left rows to avoid reusing the stream
		Stream<int[]>	rightStream	= IntStream.rangeClosed( 1, joinTable.size() ).mapToObj( j -> new int[] { j } );
		return rightStream.flatMap( j -> {
			Stream<int[]>	newStream		= leftRows.stream().map( i -> {
												int[] newIntersection = Arrays.copyOf( i, i.length + 1 );
												newIntersection[ i.length ] = j[ 0 ];
												return newIntersection;
											} ).filter( joint -> ( Boolean ) joinOn.evaluate( QoQExec, joint ) );
			List<int[]>		newStreamList	= newStream.collect( Collectors.toList() );
			if ( newStreamList.isEmpty() ) {
				int[] rightOnlyIntersection = new int[ leftRows.get( 0 ).length + 1 ];
				rightOnlyIntersection[ rightOnlyIntersection.length - 1 ] = j[ 0 ];
				for ( int k = 0; k < rightOnlyIntersection.length - 1; k++ ) {
					rightOnlyIntersection[ k ] = 0; // 0 indicates no match in the left table
				}
				return Stream.of( rightOnlyIntersection );
			}
			return newStreamList.stream();
		} );
	}

	/**
	 * Handle FULL OUTER JOINs
	 * 
	 * @param theStream The current stream of intersections
	 * @param joinTable The table to join
	 * @param joinOn    The ON clause
	 * @param QoQExec   The current QoQ execution
	 * 
	 * @return The new stream of intersections
	 */
	private static Stream<int[]> handleFullOuterJoin( Stream<int[]> theStream, Query joinTable, SQLExpression joinOn, QoQSelectExecution QoQExec ) {
		List<int[]>			leftRows		= theStream.collect( Collectors.toList() ); // Collect the left rows to avoid reusing the stream
		Stream<int[]>		rightStream		= IntStream.rangeClosed( 1, joinTable.size() ).mapToObj( j -> new int[] { j } );

		// Process LEFT JOIN logic
		Stream<int[]>		leftJoinStream	= leftRows.stream().flatMap( i -> {
												Stream<int[]>	newStream		= IntStream.rangeClosed( 1, joinTable.size() ).mapToObj( j -> {
																																							int[] newIntersection = Arrays
																																							    .copyOf(
																																							        i,
																																							        i.length
																																							            + 1 );
																																							newIntersection[ i.length ] = j;
																																							return newIntersection;
																																						} )
												    .filter( j -> joinOn == null || ( Boolean ) joinOn.evaluate( QoQExec, j ) );
												List<int[]>		newStreamList	= newStream.collect( Collectors.toList() );
												if ( newStreamList.isEmpty() ) {
													int[] leftOnlyIntersection = Arrays.copyOf( i, i.length + 1 );
													leftOnlyIntersection[ i.length ] = 0; // 0 indicates no match in the right table
													return Stream.of( leftOnlyIntersection );
												}
												return newStreamList.stream();
											} );

		// Process RIGHT JOIN logic
		Stream<int[]>		rightJoinStream	= rightStream.flatMap( j -> {
												Stream<int[]>	newStream		= leftRows.stream().map( i -> {
																																							int[] newIntersection = Arrays
																																							    .copyOf(
																																							        i,
																																							        i.length
																																							            + 1 );
																																							newIntersection[ i.length ] = j[ 0 ];
																																							return newIntersection;
																																						} )
												    .filter( joint -> joinOn == null || ( Boolean ) joinOn.evaluate( QoQExec, joint ) );
												List<int[]>		newStreamList	= newStream.collect( Collectors.toList() );
												if ( newStreamList.isEmpty() ) {
													int[] rightOnlyIntersection = new int[ leftRows.get( 0 ).length + 1 ];
													rightOnlyIntersection[ rightOnlyIntersection.length - 1 ] = j[ 0 ];
													for ( int k = 0; k < rightOnlyIntersection.length - 1; k++ ) {
														rightOnlyIntersection[ k ] = 0; // 0 indicates no match in the left table
													}
													return Stream.of( rightOnlyIntersection );
												}
												return newStreamList.stream();
											} );

		// Combine LEFT JOIN and RIGHT JOIN results and remove duplicates using a Set
		Set<List<Integer>>	seen			= new HashSet<>();
		return Stream.concat( leftJoinStream, rightJoinStream )
		    .filter( arr -> seen.add( Arrays.stream( arr ).boxed().collect( Collectors.toList() ) ) ); // Remove duplicates
	}
}
