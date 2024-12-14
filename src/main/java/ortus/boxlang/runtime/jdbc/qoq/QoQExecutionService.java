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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ortus.boxlang.compiler.ast.sql.SQLNode;
import ortus.boxlang.compiler.ast.sql.select.SQLJoin;
import ortus.boxlang.compiler.ast.sql.select.SQLResultColumn;
import ortus.boxlang.compiler.ast.sql.select.SQLSelect;
import ortus.boxlang.compiler.ast.sql.select.SQLSelectStatement;
import ortus.boxlang.compiler.ast.sql.select.SQLTable;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLColumn;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLExpression;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLStarExpression;
import ortus.boxlang.compiler.ast.sql.select.expression.literal.SQLNumberLiteral;
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

	public static Query executeSelect( IBoxContext context, SQLSelectStatement selectStatement, QoQStatement statement ) throws SQLException {
		Map<SQLTable, Query>	tableLookup	= new LinkedHashMap<SQLTable, Query>();
		boolean					hasTable	= selectStatement.getSelect().getTable() != null;
		Query					source		= null;

		// TODO: Process all joins
		if ( hasTable ) {
			String tableVarName = selectStatement.getSelect().getTable().getVariableName();
			source = getSourceQuery( context, tableVarName );
			// TODO: ensure tables are added in the order of their index, which represents the encounter-order in the SQL
			tableLookup.put( selectStatement.getSelect().getTable(), source );
		}

		// Register joins
		if ( selectStatement.getSelect().getJoins() != null ) {
			for ( SQLJoin thisJoin : selectStatement.getSelect().getJoins() ) {
				SQLTable	table			= thisJoin.getTable();
				String		tableVarName	= table.getVariableName();
				tableLookup.put( table, getSourceQuery( context, tableVarName ) );
			}
		}

		// This holds the AST and the runtime values for the query
		QoQExecution				QoQExec			= QoQExecution.of(
		    selectStatement,
		    tableLookup,
		    statement instanceof QoQPreparedStatement qp ? qp.getParameters() : null
		);

		var							intersections	= QoQIntersectionGenerator.createIntersectionStream( QoQExec );

		Map<Key, TypedResultColumn>	resultColumns	= calculateResultColumns( QoQExec );
		calculateOrderBys( QoQExec );

		Query			target			= buildTargetQuery( QoQExec );

		// print out arrays
		/*
		 * intersections.forEach( i -> System.out.println( Arrays.toString( i ) ) );
		 * if ( true )
		 * return target;
		 */
		// Process one select
		// TODO: refactor this out
		SQLSelect		select			= selectStatement.getSelect();

		Long			thisSelectLimit	= select.getLimitValue();
		// This boolean expression will be used to filter the records we keep
		SQLExpression	where			= select.getWhere();
		boolean			canEarlyLimit	= selectStatement.getOrderBys() == null;

		// Just selecting out literal values
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

		if ( select.hasAggregateResult() ) {

		}

		// enforce top/limit for this select. This would be a "top N" clause in the select or a "limit N" clause BEFORE the order by, which
		// could exist or all selects in a union.
		if ( canEarlyLimit && thisSelectLimit > -1 ) {
			intersections = intersections.limit( thisSelectLimit );

		}

		// 1-based index!
		intersections.forEach( intersection -> {
			// System.out.println( Arrays.toString( intersection ) );
			// Evaluate the where expression
			if ( where == null || ( Boolean ) where.evaluate( QoQExec, intersection ) ) {
				Object[]	values	= new Object[ resultColumns.size() ];
				int			colPos	= 0;
				for ( Key key : resultColumns.keySet() ) {
					SQLResultColumn	resultColumn	= resultColumns.get( key ).resultColumn;
					Object			value			= resultColumn.getExpression().evaluate( QoQExec, intersection );
					values[ colPos++ ] = value;
				}
				target.addRow( values );
			}
		} );

		// TODO: Implement a sort that doesn't turn the query into a list of structs and back again
		if ( QoQExec.getOrderByColumns() != null ) {
			target.sort( ( row1, row2 ) -> {
				var orderBys = QoQExec.getOrderByColumns();
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
			if ( QoQExec.getAdditionalColumns() != null ) {
				for ( Key key : QoQExec.getAdditionalColumns() ) {
					target.deleteColumn( key );
				}
			}
		}

		// This is the maxRows in the query options. It takes priority.
		Long overallSelectLimit = statement.getLargeMaxRows();
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

	private static void calculateOrderBys( QoQExecution qoQExec ) {
		SQLSelectStatement selectStatement = qoQExec.select;
		if ( selectStatement.getOrderBys() == null ) {
			return;
		}
		Set<Key>					additionalColumns	= new LinkedHashSet<Key>();
		Map<Key, TypedResultColumn>	resultColumns		= qoQExec.getResultColumns();
		List<NameAndDirection>		orderByColumns		= new ArrayList<NameAndDirection>();
		int							additionalCounter	= 1;
		for ( var orderBy : selectStatement.getOrderBys() ) {
			SQLExpression expr = orderBy.getExpression();
			if ( expr instanceof SQLColumn column ) {
				var match = resultColumns.entrySet().stream().filter( rc -> column.getName().equals( rc.getKey() ) ).findFirst();
				if ( match.isPresent() ) {
					orderByColumns.add( NameAndDirection.of( match.get().getKey(), orderBy.isAscending() ) );
					continue;
				}
			} else if ( expr instanceof SQLNumberLiteral num ) {
				// This is a number literal, which is a 1-based index into the result set
				int index = num.getValue().intValue();
				if ( index < 1 || index > resultColumns.size() ) {
					throw new DatabaseException( "The column index [" + index + "] in the order by clause is out of range." );
				}
				orderByColumns.add( NameAndDirection.of( resultColumns.keySet().toArray( new Key[ 0 ] )[ index - 1 ], orderBy.isAscending() ) );
				continue;
			}
			// TODO: Figure out if this exact expression is already in the result set and use that
			// To do this, we need something like toString() implemented to compare two expressions for equivalence
			Key newName = Key.of( "__order_by_column_" + additionalCounter++ );
			resultColumns.put( newName,
			    TypedResultColumn.of( QueryColumnType.OBJECT, new SQLResultColumn( expr, newName.getName(), resultColumns.size() + 1, null, null ) ) );
			orderByColumns.add( NameAndDirection.of( newName, orderBy.isAscending() ) );
			additionalColumns.add( newName );

		}
		qoQExec.setOrderByColumns( orderByColumns );
		qoQExec.setAdditionalColumns( additionalColumns );
	}

	/**
	 * Build the target query based on the result columns
	 * 
	 * @param resultColumns the result columns
	 * 
	 * @return the target query
	 */
	private static Query buildTargetQuery( QoQExecution QoQExec ) {
		Map<Key, TypedResultColumn>	resultColumns	= QoQExec.getResultColumns();
		Query						target			= new Query();
		for ( Key key : resultColumns.keySet() ) {
			target.addColumn( key, resultColumns.get( key ).type );
		}
		return target;
	}

	private static Map<Key, TypedResultColumn> calculateResultColumns( QoQExecution QoQExec ) {
		Map<Key, TypedResultColumn> resultColumns = new LinkedHashMap<Key, TypedResultColumn>();
		for ( SQLResultColumn resultColumn : QoQExec.select.getSelect().getResultColumns() ) {
			// For *, expand all columns in the query
			if ( resultColumn.isStarExpression() ) {
				// The same table joined more than once will still have separate SQLTable instances in the AST.
				// If we have a specific alias such as t.* this will still match since the correct SQLTable reference will be associated with the result column
				SQLTable	starTable		= ( ( SQLStarExpression ) resultColumn.getExpression() ).getTable();
				Key			tableName		= starTable == null ? null : starTable.getName();

				var			matchingTables	= QoQExec.tableLookup.keySet().stream().filter( t -> starTable == null || starTable == t )
				    .toList();

				if ( matchingTables.isEmpty() ) {
					throw new DatabaseException(
					    "The table alias [" + tableName + "] in the result column [" + resultColumn.getSourceText() + "] is does not match a table." );
				}
				matchingTables.stream().forEach( t -> {
					var thisTable = QoQExec.tableLookup.get( t );
					for ( Key key : thisTable.getColumns().keySet() ) {
						resultColumns.put( key,
						    TypedResultColumn.of(
						        thisTable.getColumns().get( key ).getType(),
						        new SQLResultColumn(
						            new SQLColumn( t, key.getName(), null, null ),
						            null,
						            resultColumns.size() + 1,
						            null,
						            null
						        )
						    )
						);
					}
				} );
				// Non-star columns are named after the column, or given a column_0, column_1, etc name
			} else {
				resultColumns.put( resultColumn.getResultColumnName(),
				    TypedResultColumn.of( resultColumn.getExpression().getType( QoQExec ), resultColumn ) );
			}
		}
		QoQExec.setResultColumns( resultColumns );
		return resultColumns;
	}

	private static Query getSourceQuery( IBoxContext context, String tableVarName ) {
		Object oSource = ExpressionInterpreter.getVariable( context, tableVarName, false );
		if ( oSource instanceof Query qSource ) {
			return qSource;
		} else {
			throw new DatabaseException( "The QoQ table name [" + tableVarName + "] cannot be found as a variable." );
		}
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
