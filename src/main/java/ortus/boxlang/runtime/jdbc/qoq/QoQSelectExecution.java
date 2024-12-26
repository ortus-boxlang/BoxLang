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

	/**
	 * Factory method
	 * 
	 * @param select      select AST node
	 * @param tableLookup table lookup
	 * 
	 * @return new QoQSelectExecution instance
	 */
	public static QoQSelectExecution of( SQLSelect select, Map<SQLTable, Query> tableLookup ) {
		return new QoQSelectExecution( select, tableLookup );
	}

	/**
	 * Get the select node
	 */
	public SQLSelect getSelect() {
		return select;
	}

	/**
	 * Get the table lookup
	 * 
	 * @return table lookup
	 */
	public Map<SQLTable, Query> getTableLookup() {
		return tableLookup;
	}

	/**
	 * Get the result columns
	 * 
	 * @return result columns
	 */
	public Map<Key, TypedResultColumn> getResultColumns() {
		return resultColumns;
	}

	/**
	 * Set the result columns
	 * 
	 * @param resultColumns result columns
	 */
	public void setResultColumns( Map<Key, TypedResultColumn> resultColumns ) {
		this.resultColumns = resultColumns;
	}

	/**
	 * Get the select statement execution
	 * 
	 * @return select statement execution
	 */
	public QoQSelectStatementExecution getSelectStatementExecution() {
		return selectStatementExecution;
	}

	/**
	 * Set the select statement execution
	 * 
	 * @param selectStatementExecution select statement execution
	 */
	public void setQoQSelectStatementExecution( QoQSelectStatementExecution selectStatementExecution ) {
		this.selectStatementExecution = selectStatementExecution;
	}

	/**
	 * Calculate the result columns
	 * 
	 * @param firstSelect whether this is the first select
	 * 
	 * @return result columns
	 */
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
						        resultColumns.size(),
						        new SQLResultColumn(
						            new SQLColumn( t, key.getName(), null, null ),
						            null,
						            resultColumns.size(),
						            null,
						            null
						        )
						    )
						);
					}
				} );
				// Non-star columns are named after the column, or given a column_0, column_1, etc name
			} else if ( !resultColumns.containsKey( resultColumn.getResultColumnName() ) ) {
				resultColumns.put( resultColumn.getResultColumnName(),
				    TypedResultColumn.of( resultColumn.getExpression().getType( this ), resultColumns.size(), resultColumn ) );
			}
		}
		setResultColumns( resultColumns );
		// Given a union, the first result set names are used for all unioned selects as well
		if ( firstSelect ) {
			getSelectStatementExecution().setResultColumnNames( resultColumns.keySet() );
		}
		return resultColumns;
	}

	/**
	 * Calculate the order by columns
	 */
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
				var match = resultColumns.entrySet().stream().filter( rc -> {
					// Check if the column name matches the name or alias an expression in the result set, regardless of whether it's a column
					if ( column.getName().equals( rc.getKey() ) ) {
						return true;
					}
					// Check if the order by name matches the name of a column in the result set
					if ( rc.getValue().resultColumn().getExpression() instanceof SQLColumn c ) {
						return column.getName().equals( c.getName() );
					}
					return false;
				} ).findFirst();
				if ( match.isPresent() ) {
					orderByColumns.add(
					    NameAndDirection.of( match.get().getKey(), match.get().getValue().type(), match.get().getValue().position(), orderBy.isAscending() ) );
					continue;
				}
			} else if ( expr instanceof SQLNumberLiteral num ) {
				// This is a number literal, which is a 1-based index into the result set
				int index = num.getValue().intValue();
				if ( index < 1 || index > numOriginalResulColumns ) {
					throw new DatabaseException( "The column index [" + index + "] in the order by clause is out of range as there are only "
					    + numOriginalResulColumns + " column(s)." );
				}
				orderByColumns.add(
				    NameAndDirection.of(
				        resultColumns.keySet().toArray( new Key[ 0 ] )[ index - 1 ],
				        resultColumns.values().toArray( new TypedResultColumn[ 0 ] )[ index - 1 ].type(),
				        index - 1,
				        orderBy.isAscending()
				    )
				);
				continue;
			} else {
				// Loop over result columns like above, but compare the tostring() representations to look for a match
				var match = resultColumns.entrySet()
				    .stream()
				    .filter( rc -> expr.toString().equals( rc.getValue().resultColumn().getExpression().toString() ) )
				    .findFirst();

				if ( match.isPresent() ) {
					orderByColumns.add(
					    NameAndDirection.of( match.get().getKey(), match.get().getValue().type(), match.get().getValue().position(), orderBy.isAscending() ) );
					continue;
				}
			}

			// TODO: This isn't quite right as a literal expression is technically OK in the order by of a union query, even though it's fairly useless.
			// We need the query sort to be rewritten to eval expressions on the fly for that to work however. Not worth addressing at the moment.
			if ( isUnion ) {
				throw new DatabaseException( "The order by clause in a union query must reference a column by name that is in the select list or index." );
			}

			if ( select.isDistinct() ) {
				throw new DatabaseException( "The order by clause in a distinct query must reference a column by name that is in the select list." );
			}

			// TODO: Figure out if this exact expression is already in the result set and use that
			// To do this, we need something like toString() implemented to compare two expressions for equivalence
			Key	newName		= Key.of( "__order_by_column_" + additionalCounter++ );
			int	newColPos	= resultColumns.size();
			resultColumns.put( newName,
			    TypedResultColumn.of( QueryColumnType.OBJECT, newColPos,
			        new SQLResultColumn( expr, newName.getName(), newColPos, null, null ) ) );
			orderByColumns.add( NameAndDirection.of( newName, expr.getType( this ), newColPos, orderBy.isAscending() ) );
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

	/**
	 * Add a partition
	 * 
	 * @param partitionName Name of the partition
	 * @param partition     The partition
	 */
	public void addPartition( String partitionName, int[] partition ) {
		List<int[]> thisPartition = partitions.computeIfAbsent( partitionName, p -> new ArrayList<int[]>() );
		synchronized ( thisPartition ) {
			thisPartition.add( partition );
		}
	}

	/**
	 * Get a partition
	 * 
	 * @param partitionName Name of the partition
	 * 
	 * @return The partition
	 */
	public List<int[]> getPartition( String partitionName ) {
		return partitions.get( partitionName );
	}

	/**
	 * Get all partitions
	 * 
	 * @return All partitions
	 */
	public Map<String, List<int[]>> getPartitions() {
		return partitions;
	}

}
