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

import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
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
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.operators.Compare;
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

	public static Query executeSelectStatement( IBoxContext context, SQLSelectStatement selectStatement, QoQStatement statement ) {

		QoQSelectStatementExecution	QoQStmtExec	= QoQSelectStatementExecution.of(
		    selectStatement,
		    statement instanceof QoQPreparedStatement qp ? qp.getParameters() : null,
		    statement );

		Query						target		= executeSelect( context, selectStatement.getSelect(), statement, QoQStmtExec, true );

		if ( selectStatement.getUnions() != null ) {
			for ( SQLUnion union : selectStatement.getUnions() ) {
				Query unionQuery = executeSelect( context, union.getSelect(), statement, QoQStmtExec, false );
				if ( union.getType() == SQLUnionType.ALL ) {
					unionAll( target, unionQuery );
				} else {
					// distinct
					unionDistinct( target, unionQuery );
				}
			}
		}

		// TODO: Implement a sort that doesn't turn the query into a list of structs and back again
		if ( QoQStmtExec.getOrderByColumns() != null ) {
			target.sort( ( row1, row2 ) -> {
				var orderBys = QoQStmtExec.getOrderByColumns();
				for ( var orderBy : orderBys ) {
					var	name	= orderBy.name;
					int	result	= Compare.invoke( row1.get( name ), row2.get( name ) );
					if ( result != 0 ) {
						return orderBy.ascending ? result : -result;
					}
				}
				return 0;
			} );
			// These were just here for sorting. Nuke them now.
			if ( QoQStmtExec.getAdditionalColumns() != null ) {
				for ( Key key : QoQStmtExec.getAdditionalColumns() ) {
					target.deleteColumn( key );
				}
			}
		}

		// This is the maxRows in the query options. It takes priority.
		Long overallSelectLimit;
		try {
			overallSelectLimit = statement.getLargeMaxRows();
		} catch ( SQLException e ) {
			throw new DatabaseException( "Error getting max rows from statement", e );
		}
		// If that wasn't set, use the limit clause AFTER the order by (which could apply at the end of a union)
		if ( overallSelectLimit == -1 ) {
			overallSelectLimit = selectStatement.getLimitValue();
		}

		// If we have a limit for the final select, apply it here.
		if ( overallSelectLimit > -1 ) {
			target.truncate( overallSelectLimit );
		}
		return target;
	}

	public static Query executeSelect( IBoxContext context, SQLSelect select, QoQStatement statement, QoQSelectStatementExecution QoQStmtExec,
	    boolean firstSelect ) {
		boolean					canEarlyLimit	= QoQStmtExec.getSelectStatement().getOrderBys() == null;
		Map<SQLTable, Query>	tableLookup		= new LinkedHashMap<SQLTable, Query>();
		// This boolean expression will be used to filter the records we keep
		SQLExpression			where			= select.getWhere();
		boolean					hasTable		= select.getTable() != null;
		Long					thisSelectLimit	= select.getLimitValue();

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
		Query target = buildTargetQuery( QoQExec );

		// If there are no tables, and we are just selecting out literal values, we can just add the row and return
		// This code path ignores the where clause and top/limit. While technically vaid, it is not a common use case.
		if ( !hasTable ) {
			Object[] values = new Object[ resultColumns.size() ];
			for ( Key key : resultColumns.keySet() ) {
				SQLResultColumn	resultColumn	= resultColumns.get( key ).resultColumn;
				Object			value			= resultColumn.getExpression().evaluate( QoQExec, null );
				values[ resultColumn.getOrdinalPosition() - 1 ] = value;
			}
			target.addRow( values );
			return target;
		}

		// We have one or more tables, so build our stream of intersections, processing our joins as needed
		Stream<int[]> intersections = QoQIntersectionGenerator.createIntersectionStream( QoQExec );

		if ( select.hasAggregateResult() ) {

		}

		// Enforce top/limit for this select. This would be a "top N" clause in the select or a "limit N" clause BEFORE the order by, which
		// could exist or all selects in a union.
		if ( canEarlyLimit && thisSelectLimit > -1 ) {
			intersections = intersections.limit( thisSelectLimit );
		}

		// If we have a where clause, add it as a filter to the stream
		if ( where != null ) {
			intersections = intersections.filter( intersection -> ( Boolean ) where.evaluate( QoQExec, intersection ) );
		}

		// Process/create the rows for the final query.
		intersections.forEach( intersection -> {
			// System.out.println( Arrays.toString( intersection ) );
			Object[]	values	= new Object[ resultColumns.size() ];
			int			colPos	= 0;
			// Build up row data as native array
			for ( Key key : resultColumns.keySet() ) {
				SQLResultColumn	resultColumn	= resultColumns.get( key ).resultColumn;
				Object			value			= resultColumn.getExpression().evaluate( QoQExec, intersection );
				values[ colPos++ ] = value;
			}
			target.addRow( values );
		} );

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
	private static void unionDistinct( Query target, Query unionQuery ) {
		// TODO: IMPLEMENT!
		unionAll( target, unionQuery );
	}

	/**
	 * Build the target query based on the result columns
	 * 
	 * @param resultColumns the result columns
	 * 
	 * @return the target query
	 */
	private static Query buildTargetQuery( QoQSelectExecution QoQExec ) {
		Map<Key, TypedResultColumn>	resultColumns	= QoQExec.getResultColumns();
		Query						target			= new Query();
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

	public record TypedResultColumn( QueryColumnType type, SQLResultColumn resultColumn ) {

		public static TypedResultColumn of( QueryColumnType type, SQLResultColumn resultColumn ) {
			return new TypedResultColumn( type, resultColumn );
		}
	}

	public record NameAndDirection( Key name, boolean ascending ) {

		public static NameAndDirection of( Key name, boolean ascending ) {
			return new NameAndDirection( name, ascending );
		}
	}

}
