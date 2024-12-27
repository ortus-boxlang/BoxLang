
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import ortus.boxlang.compiler.ast.sql.SQLNode;
import ortus.boxlang.compiler.ast.sql.select.SQLJoin;
import ortus.boxlang.compiler.ast.sql.select.SQLResultColumn;
import ortus.boxlang.compiler.ast.sql.select.SQLSelect;
import ortus.boxlang.compiler.ast.sql.select.SQLSelectStatement;
import ortus.boxlang.compiler.ast.sql.select.SQLTable;
import ortus.boxlang.compiler.ast.sql.select.SQLTableSubQuery;
import ortus.boxlang.compiler.ast.sql.select.SQLTableVariable;
import ortus.boxlang.compiler.ast.sql.select.SQLUnion;
import ortus.boxlang.compiler.ast.sql.select.SQLUnionType;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLExpression;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.compiler.parser.SQLParser;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;
import ortus.boxlang.runtime.types.exceptions.ParseException;
import ortus.boxlang.runtime.util.FRTransService;

/**
 * I handle executing query of queries
 */
public class QoQExecutionService {

	/**
	 * The transaction service used to track subtransactions
	 */
	private static final FRTransService frTransService = FRTransService.getInstance( true );

	/**
	 * Parse a SQL string into an AST
	 * 
	 * @param sql the SQL string
	 * 
	 * @return the AST
	 */
	public static SQLNode parseSQL( String sql ) {
		DynamicObject	trans	= frTransService.startTransaction( "BL QoQ Parse", "" );
		SQLParser		parser	= new SQLParser();
		ParsingResult	result;

		try {
			result = parser.parse( sql );
		} finally {
			frTransService.endTransaction( trans );
		}

		if ( !result.isCorrect() ) {
			throw new ParseException( result.getIssues(), sql );
		}
		IStruct data = Struct.of(
		    "file", null,
		    "result", result
		);
		BoxRuntime.getInstance().announce( "onParse", data );

		return ( SQLNode ) result.getRoot();
	}

	/**
	 * Execute a QoQ statement
	 * 
	 * @param context         the context
	 * @param selectStatement the select statement
	 * @param statement       the QoQ statement
	 * 
	 * @return the query
	 */
	public static Query executeSelectStatement( IBoxContext context, SQLSelectStatement selectStatement, QoQStatement statement ) {

		QoQSelectStatementExecution	QoQStmtExec	= QoQSelectStatementExecution.of(
		    selectStatement,
		    statement instanceof QoQPreparedStatement qp ? qp.getParameters() : null,
		    statement );

		Query						target		= executeSelect( context, selectStatement.getSelect(), statement, QoQStmtExec, true );

		if ( selectStatement.getUnions() != null ) {

			// We actually only need to de-dupe the last union, so we need to find it
			// This is a performance optimization to avoid de-duping every union uneccessarily
			int	lastUnion	= Stream.iterate( 0, i -> i + 1 )
			    .limit( selectStatement.getUnions().size() )
			    .filter( i -> selectStatement.getUnions().get( i ).getType() == SQLUnionType.DISTINCT )
			    .reduce( ( first, second ) -> second )
			    .orElse( -1 );

			int	i			= 0;
			for ( SQLUnion union : selectStatement.getUnions() ) {
				Query unionQuery = executeSelect( context, union.getSelect(), statement, QoQStmtExec, false );
				unionAll( target, unionQuery );
				if ( i == lastUnion ) {
					deDupeQuery( target );
				}
				i++;
			}
		}

		// Apply our sort
		if ( QoQStmtExec.getOrderByColumns() != null ) {
			sort( target, QoQStmtExec.getOrderByColumns() );

			// These were just here for sorting. Nuke them now.
			if ( QoQStmtExec.getAdditionalColumns() != null ) {
				for ( Key key : QoQStmtExec.getAdditionalColumns() ) {
					target.deleteColumn( key );
				}
			}
		}

		Long overallSelectLimit = QoQStmtExec.getOverallSelectLimit();
		// If we have a limit for the final select, apply it here.
		if ( overallSelectLimit > -1 ) {
			target.truncate( overallSelectLimit );
		}
		return target;
	}

	/**
	 * Execute a select statement
	 * 
	 * @param context     the context
	 * @param select      the select
	 * @param statement   the QoQ statement
	 * @param QoQStmtExec the QoQ statement execution
	 * @param firstSelect if this is the first select
	 * 
	 * @return the query
	 */
	public static Query executeSelect( IBoxContext context, SQLSelect select, QoQStatement statement, QoQSelectStatementExecution QoQStmtExec,
	    boolean firstSelect ) {
		// If there is a group by or aggregate function, we will need to partiton the query
		boolean					isAggregate		= select.hasAggregateResult() || select.getGroupBys() != null;
		// If there is no order by, and no aggregate, we can limit the results early by stopping as soon as the limit is reached
		boolean					canEarlyLimit	= !isAggregate && QoQStmtExec.getSelectStatement().getOrderBys() == null;
		Map<SQLTable, Query>	tableLookup		= new LinkedHashMap<SQLTable, Query>();
		// This boolean expression will be used to filter the records we keep
		SQLExpression			where			= select.getWhere();
		boolean					hasTable		= select.getTable() != null;
		Long					thisSelectLimit	= select.getLimitValue();
		// If we can early limit, and there are no unions, apply the select statement limit here, if smaller
		if ( canEarlyLimit && !select.isDistinct() && QoQStmtExec.getSelectStatement().getUnions() == null ) {
			Long overallSelectLimit = QoQStmtExec.getOverallSelectLimit();
			if ( overallSelectLimit > -1 && ( thisSelectLimit == -1 || overallSelectLimit < thisSelectLimit ) ) {
				thisSelectLimit = overallSelectLimit;
			}
		}

		if ( hasTable ) {
			// Tables are added in the order of their index, which represents the encounter-order in the SQL
			tableLookup.put( select.getTable(), getSourceQuery( context, QoQStmtExec, select.getTable() ) );

			// Register joins
			if ( select.getJoins() != null ) {
				for ( SQLJoin thisJoin : select.getJoins() ) {
					SQLTable table = thisJoin.getTable();
					tableLookup.put( table, getSourceQuery( context, QoQStmtExec, table ) );
				}
			}
		}

		// This holds the AST and the runtime values for the query
		QoQSelectExecution			QoQExec			= QoQStmtExec.newQoQSelectExecution(
		    select,
		    tableLookup
		);
		// Calculate the result columns for this select
		Map<Key, TypedResultColumn>	resultColumns	= QoQExec.calculateResultColumns( firstSelect );

		// If this is the first select, (not a union) calculate order bys, which may modify the result columns
		if ( firstSelect ) {
			QoQExec.calculateOrderBys();
		}

		// Create empty query object to hold result
		Query target;

		// If there are no tables, and we are just selecting out literal values, we can just add the row and return
		// This code path ignores the where clause and top/limit. While technically vaid, it is not a common use case.
		if ( !hasTable ) {
			target = buildTargetQuery( QoQExec, 1 );
			Object[] values = new Object[ resultColumns.size() ];
			for ( Key key : resultColumns.keySet() ) {
				SQLResultColumn	resultColumn	= resultColumns.get( key ).resultColumn;
				Object			value			= resultColumn.getExpression().evaluate( QoQExec, null );
				values[ resultColumn.getOrdinalPosition() - 1 ] = value;
			}
			target.addRow( values );
			return target;
		}
		// initial size is sum of all rows in all tables
		target = buildTargetQuery( QoQExec, tableLookup.values().stream().mapToInt( Query::size ).sum() );

		// We have one or more tables, so build our stream of intersections, processing our joins as needed
		Stream<int[]> intersections = QoQIntersectionGenerator.createIntersectionStream( QoQExec );

		// If we have a where clause, add it as a filter to the stream
		if ( where != null ) {
			intersections = intersections.filter( intersection -> ( Boolean ) where.evaluate( QoQExec, intersection ) );
		}

		// Enforce top/limit for this select. This would be a "top N" clause in the select or a "limit N" clause BEFORE the order by, which
		// could exist or all selects in a union.
		if ( canEarlyLimit && !select.isDistinct() && thisSelectLimit > -1 ) {
			intersections = intersections.limit( thisSelectLimit );
		}
		if ( select.hasAggregateResult() || select.getGroupBys() != null ) {
			target = executeAggregateSelect( QoQExec, target, intersections );
		} else {
			final Query finalTarget = target;
			// No partitioning, just create the final result set
			intersections.forEach( intersection -> {
				// System.out.println( Arrays.toString( intersection ) );
				Object[]	values	= new Object[ resultColumns.size() ];
				int			colPos	= 0;
				// Build up row data as native array
				for ( Key key : resultColumns.keySet() ) {
					values[ colPos++ ] = resultColumns.get( key ).resultColumn.getExpression().evaluate( QoQExec, intersection );
				}
				finalTarget.addRow( values );
			} );
			( ( ArrayList<?> ) target.getData() ).trimToSize();
		}

		// Apply distinct to the final result set
		if ( select.isDistinct() ) {
			deDupeQuery( target );
		}

		// If we have a limit for this select, apply it here.
		if ( thisSelectLimit > -1 ) {
			target.truncate( thisSelectLimit );
		}

		return target;
	}

	/**
	 * Create query partitioned by group by and aggregate functions
	 * 
	 * @param qoQExec       the query execution state
	 * @param resultColumns the result columns
	 * 
	 * @return
	 */
	private static Query executeAggregateSelect( QoQSelectExecution QoQExec, Query target, Stream<int[]> intersections ) {
		Map<Key, TypedResultColumn>	resultColumns	= QoQExec.getResultColumns();
		List<SQLExpression>			groupBys		= QoQExec.getSelect().getGroupBys();
		SQLExpression				having			= QoQExec.getSelect().getHaving();

		// Build up our partitions
		intersections.forEach( intersection -> {
			String partitionKey;
			if ( groupBys != null ) {
				StringBuilder sb = new StringBuilder();
				for ( SQLExpression expression : groupBys ) {
					// TODO: hash large values
					Object cellValue = expression.evaluate( QoQExec, intersection );
					sb.append( StringCaster.cast( cellValue == null ? "<<NULL>>" : cellValue ) );
				}
				partitionKey = sb.toString();
			} else {
				partitionKey = "ALL";
			}
			QoQExec.addPartition( partitionKey, intersection );
		} );

		// If there are aggregates in the select, but no group by, and no records were returned, we return a single empty rows
		if ( groupBys == null && QoQExec.getPartitions().isEmpty() ) {
			Object[] values = new Object[ resultColumns.size() ];
			for ( Key key : resultColumns.keySet() ) {
				SQLResultColumn	resultColumn	= resultColumns.get( key ).resultColumn;
				Object			value			= resultColumn.getExpression().evaluateAggregate( QoQExec, List.of() );
				values[ resultColumn.getOrdinalPosition() - 1 ] = value;
			}
			target.addRow( values );
			( ( ArrayList<?> ) target.getData() ).trimToSize();
			return target;
		}

		// Make stream parallel if we have a lot of partitions
		var partitionStream = QoQExec.getPartitions().values().stream();
		if ( QoQExec.getPartitions().size() > 50 ) {
			partitionStream = partitionStream.parallel();
		}

		// Filter out partitions that don't match the having clause
		if ( having != null ) {
			partitionStream = partitionStream.filter( partition -> ( Boolean ) having.evaluateAggregate( QoQExec, partition ) );
		}

		// Build up the final result set
		partitionStream.forEach( partition -> {
			Object[]	values	= new Object[ resultColumns.size() ];
			int			colPos	= 0;
			for ( Key key : resultColumns.keySet() ) {
				values[ colPos++ ] = resultColumns.get( key ).resultColumn.getExpression().evaluateAggregate( QoQExec, partition );
			}
			target.addRow( values );
		} );
		( ( ArrayList<?> ) target.getData() ).trimToSize();

		return target;
	}

	/**
	 * Union two queries together, keeping all rows
	 * 
	 * @param target     the target query
	 * @param unionQuery the query to union
	 */
	private static void unionAll( Query target, Query unionQuery ) {
		for ( int i = 0; i < unionQuery.size(); i++ ) {
			target.addRow( unionQuery.getRow( i ) );
		}
	}

	/**
	 * Union two queries together, keeping only distinct rows
	 * 
	 * @param target     the target query
	 * @param unionQuery the query to union
	 */
	private static void deDupeQuery( Query target ) {
		Map<String, Object>	seen						= new ConcurrentHashMap<>();
		Object				concurrentHashMapsAreDumb	= new Object();
		Object[][]			newData						= new Object[ target.size() ][];
		Object[][]			oldData						= target.getData().toArray( new Object[ 0 ][] );
		AtomicInteger		newSize						= new AtomicInteger( 0 );
		int					estimatedRowSize			= target.getColumns().size() * 15;

		// Stream over indices and directly remove rows if they're duplicates
		IntStream			stream						= IntStream.range( 0, target.size() );
		if ( target.size() > 100 ) {
			stream = stream.parallel();
		}
		stream.forEach( i -> {
			StringBuilder	sb	= new StringBuilder( estimatedRowSize );
			Object[]		row	= oldData[ i ];
			for ( Object value : row ) {
				sb.append( value );
			}
			String key = sb.toString();
			if ( seen.putIfAbsent( key, concurrentHashMapsAreDumb ) == null ) {
				newData[ newSize.getAndIncrement() ] = row;
			}
		} );

		Object[][] finalData = new Object[ newSize.get() ][];
		System.arraycopy( newData, 0, finalData, 0, newSize.get() );
		target.setData( new ArrayList<>( Arrays.asList( finalData ) ) );
	}

	private static void sort( Query target, List<NameAndDirection> orderBys ) {
		target.sortData( ( row1, row2 ) -> {
			for ( NameAndDirection orderBy : orderBys ) {
				int result = QoQCompare.invoke( orderBy.type, row1[ orderBy.position ], row2[ orderBy.position ] );
				if ( result != 0 ) {
					return orderBy.ascending ? result : -result;
				}
			}
			return 0;
		} );
	}

	/**
	 * Build the target query based on the result columns
	 * 
	 * @param resultColumns the result columns
	 * 
	 * @return the target query
	 */
	private static Query buildTargetQuery( QoQSelectExecution QoQExec, int initialSize ) {
		Map<Key, TypedResultColumn>	resultColumns	= QoQExec.getResultColumns();
		Query						target			= new Query( initialSize );
		for ( Key key : resultColumns.keySet() ) {
			target.addColumn( key, resultColumns.get( key ).type );
		}
		return target;
	}

	private static Query getSourceQuery( IBoxContext context, QoQSelectStatementExecution QoQStmtExec, SQLTable table ) {
		if ( table instanceof SQLTableVariable tableVar ) {
			String	tableVarName	= tableVar.getVariableName();
			Object	oSource			= ExpressionInterpreter.getVariable( context, tableVarName, false );
			if ( oSource instanceof Query qSource ) {
				return qSource;
			} else if ( oSource == null ) {
				throw new DatabaseException( "The QoQ table name [" + tableVarName + "] cannot be found as a variable." );
			} else {
				throw new DatabaseException(
				    "The QoQ table name [" + tableVarName + "] is not of type query, but instead is [" + oSource.getClass().getName() + "]" );
			}
		} else if ( table instanceof SQLTableSubQuery tableSub ) {
			return executeSelectStatement( context, tableSub.getSelectStatement(), QoQStmtExec.getJDBCStatement() );
		}
		throw new DatabaseException( "Unknown table type [" + table.getClass().getName() + "]" );
	}

	/**
	 * Represent a result column with a runtime type
	 * TODO: We may not need this since the expression can tell us the type directly
	 */
	public record TypedResultColumn( QueryColumnType type, int position, SQLResultColumn resultColumn ) {

		public static TypedResultColumn of( QueryColumnType type, int position, SQLResultColumn resultColumn ) {
			return new TypedResultColumn( type, position, resultColumn );
		}
	}

	// Represent the name and order of an order by statement. This is calculated at runtime since the actual column names may be based on a *
	public record NameAndDirection( Key name, QueryColumnType type, int position, boolean ascending ) {

		public static NameAndDirection of( Key name, QueryColumnType type, int position, boolean ascending ) {
			return new NameAndDirection( name, type, position, ascending );
		}
	}

}
