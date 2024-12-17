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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import ortus.boxlang.compiler.ast.sql.select.SQLResultColumn;
import ortus.boxlang.compiler.ast.sql.select.SQLSelect;
import ortus.boxlang.compiler.ast.sql.select.SQLSelectStatement;
import ortus.boxlang.compiler.ast.sql.select.SQLTable;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLColumn;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLExpression;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLStarExpression;
import ortus.boxlang.compiler.ast.sql.select.expression.literal.SQLNumberLiteral;
import ortus.boxlang.runtime.jdbc.qoq.QoQExecutionService.NameAndDirection;
import ortus.boxlang.runtime.jdbc.qoq.QoQExecutionService.TypedResultColumn;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.exceptions.DatabaseException;

/**
 * A wrapper class to hold together both the SQL AST being executed as well as the runtime values for a given execution of the query
 */
public class QoQSelectExecution {

	public SQLSelect						select;
	public Map<Key, TypedResultColumn>		resultColumns			= null;
	public Map<SQLTable, Query>				tableLookup;
	public QoQSelectStatementExecution		selectStatementExecution;
	public Map<String, List<int[]>>			partitions				= new ConcurrentHashMap<String, List<int[]>>();

	private Map<SQLSelectStatement, Query>	independentSubQueries	= new ConcurrentHashMap<SQLSelectStatement, Query>();

	/**
	 * Constructor
	 * 
	 * @param select
	 * @param tableLookup
	 * @param params
	 * 
	 * @return
	 */
	private QoQSelectExecution( SQLSelect select, Map<SQLTable, Query> tableLookup ) {
		this.select			= select;
		this.tableLookup	= tableLookup;
	}

	public static QoQSelectExecution of( SQLSelect select, Map<SQLTable, Query> tableLookup ) {
		return new QoQSelectExecution( select, tableLookup );
	}

	public SQLSelect getSelect() {
		return select;
	}

	public Map<SQLTable, Query> getTableLookup() {
		return tableLookup;
	}

	public Map<Key, TypedResultColumn> getResultColumns() {
		return resultColumns;
	}

	public void setResultColumns( Map<Key, TypedResultColumn> resultColumns ) {
		this.resultColumns = resultColumns;
	}

	public QoQSelectStatementExecution getSelectStatementExecution() {
		return selectStatementExecution;
	}

	public void setQoQSelectStatementExecution( QoQSelectStatementExecution selectStatementExecution ) {
		this.selectStatementExecution = selectStatementExecution;
	}

	public Map<Key, TypedResultColumn> calculateResultColumns( boolean firstSelect ) {
		Map<Key, TypedResultColumn> resultColumns = new LinkedHashMap<Key, TypedResultColumn>();
		for ( SQLResultColumn resultColumn : getSelect().getResultColumns() ) {
			// For *, expand all columns in the query
			if ( resultColumn.isStarExpression() ) {
				// The same table joined more than once will still have separate SQLTable instances in the AST.
				// If we have a specific alias such as t.* this will still match since the correct SQLTable reference will be associated with the result column
				SQLTable	starTable		= ( ( SQLStarExpression ) resultColumn.getExpression() ).getTable();

				var			matchingTables	= tableLookup.keySet().stream().filter( t -> starTable == null || starTable == t )
				    .toList();

				if ( matchingTables.isEmpty() ) {
					throw new DatabaseException(
					    "The table reference in the result column [" + resultColumn.getSourceText() + "] does not match a table." );
				}
				matchingTables.stream().forEach( t -> {
					var thisTable = tableLookup.get( t );
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
				    TypedResultColumn.of( resultColumn.getExpression().getType( this ), resultColumn ) );
			}
		}
		setResultColumns( resultColumns );
		// Given a union, the first result set names are used for all unioned selects as well
		if ( firstSelect ) {
			getSelectStatementExecution().setResultColumnNames( resultColumns.keySet() );
		}
		return resultColumns;
	}

	public void calculateOrderBys() {
		var					QoQStmtExec		= selectStatementExecution;
		SQLSelectStatement	selectStatement	= QoQStmtExec.selectStatement;
		if ( selectStatement.getOrderBys() == null ) {
			return;
		}
		boolean						isUnion					= selectStatement.getUnions() != null;
		Set<Key>					additionalColumns		= new LinkedHashSet<Key>();
		Map<Key, TypedResultColumn>	resultColumns			= getResultColumns();
		List<NameAndDirection>		orderByColumns			= new ArrayList<NameAndDirection>();
		int							additionalCounter		= 1;
		int							numOriginalResulColumns	= resultColumns.size();
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
				if ( index < 1 || index > numOriginalResulColumns ) {
					throw new DatabaseException( "The column index [" + index + "] in the order by clause is out of range as there are only "
					    + numOriginalResulColumns + " column(s)." );
				}
				orderByColumns.add( NameAndDirection.of( resultColumns.keySet().toArray( new Key[ 0 ] )[ index - 1 ], orderBy.isAscending() ) );
				continue;
			}
			// TODO: This isn't quite right as a literal expression is technically OK in the order by of a union query, even though it's fairly useless.
			// We need the query sort to be rewritten to eval expressions on the fly for that to work however. Not worth addressing at the moment.
			if ( isUnion ) {
				throw new DatabaseException( "The order by clause in a union query must reference a column by name that is in the select list or index." );
			}
			// TODO: Figure out if this exact expression is already in the result set and use that
			// To do this, we need something like toString() implemented to compare two expressions for equivalence
			Key newName = Key.of( "__order_by_column_" + additionalCounter++ );
			resultColumns.put( newName,
			    TypedResultColumn.of( QueryColumnType.OBJECT, new SQLResultColumn( expr, newName.getName(), resultColumns.size() + 1, null, null ) ) );
			orderByColumns.add( NameAndDirection.of( newName, orderBy.isAscending() ) );
			additionalColumns.add( newName );

		}
		QoQStmtExec.setOrderByColumns( orderByColumns );
		QoQStmtExec.setAdditionalColumns( additionalColumns );
	}

	/**
	 * Indepenant sub queries are not based on the context of the outer query and can be cached here.
	 * 
	 * @param subquery
	 * 
	 * @return
	 */
	public Query getIndepententSubQuery( SQLSelectStatement subquery ) {
		return independentSubQueries.computeIfAbsent(
		    subquery,
		    sq -> QoQExecutionService.executeSelectStatement( selectStatementExecution.getJDBCStatement().getContext(), sq,
		        selectStatementExecution.getJDBCStatement() )
		);
	}

	public void addPartition( String partitionName, int[] partition ) {
		partitions.computeIfAbsent( partitionName, p -> new ArrayList<int[]>() ).add( partition );
	}

	public List<int[]> getPartition( String partitionName ) {
		return partitions.get( partitionName );
	}

	public Map<String, List<int[]>> getPartitions() {
		return partitions;
	}

}
