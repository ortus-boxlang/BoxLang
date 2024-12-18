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
import java.util.List;
import java.util.Map;
import java.util.Set;

import ortus.boxlang.compiler.ast.sql.select.SQLSelect;
import ortus.boxlang.compiler.ast.sql.select.SQLSelectStatement;
import ortus.boxlang.compiler.ast.sql.select.SQLTable;
import ortus.boxlang.runtime.jdbc.qoq.QoQExecutionService.NameAndDirection;
import ortus.boxlang.runtime.jdbc.qoq.QoQPreparedStatement.ParamItem;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Query;

/**
 * A wrapper class to hold together both the SQL AST being executed as well as the runtime values for a given execution of the query
 */
public class QoQSelectStatementExecution {

	public SQLSelectStatement		selectStatement;
	public Set<Key>					resultColumnNames	= null;
	public List<ParamItem>			params;
	public List<NameAndDirection>	orderByColumns		= null;
	public Set<Key>					additionalColumns	= null;
	public List<QoQSelectExecution>	selects				= new ArrayList<QoQSelectExecution>();
	public QoQStatement				JDBCStatement;

	/**
	 * Constructor
	 * 
	 * @param select
	 * @param tableLookup
	 * @param params
	 * 
	 * @return
	 */
	private QoQSelectStatementExecution( SQLSelectStatement selectStatement, List<ParamItem> params, QoQStatement JDBCStatement ) {
		this.selectStatement	= selectStatement;
		this.params				= params;
		this.JDBCStatement		= JDBCStatement;
	}

	/**
	 * Factory method
	 * 
	 * @param selectStatement The Select statement node
	 * @param params          The parameters to the query
	 * @param JDBCStatement   The JDBC statement
	 * 
	 * @return A new instance of QoQSelectStatementExecution
	 */
	public static QoQSelectStatementExecution of( SQLSelectStatement selectStatement, List<ParamItem> params, QoQStatement JDBCStatement ) {
		return new QoQSelectStatementExecution( selectStatement, params, JDBCStatement );
	}

	/**
	 * Get the select statement node
	 * 
	 * @return The select statement node
	 */
	public SQLSelectStatement getSelectStatement() {
		return selectStatement;
	}

	/**
	 * Get the parameters to the query
	 * 
	 * @return The parameters to the query
	 */
	public List<ParamItem> getParams() {
		return params;
	}

	/**
	 * Get the result column names
	 * 
	 * @return The result column names
	 */
	public Set<Key> getResultColumnName() {
		return resultColumnNames;
	}

	/**
	 * Set the result column names
	 * 
	 * @param resultColumnNames The result column names
	 */
	public void setResultColumnNames( Set<Key> resultColumnNames ) {
		this.resultColumnNames = resultColumnNames;
	}

	/**
	 * Get the order by columns
	 * 
	 * @return The order by columns
	 */
	public List<NameAndDirection> getOrderByColumns() {
		return orderByColumns;
	}

	/**
	 * Set the order by columns
	 * 
	 * @param orderByColumns The order by columns
	 */
	public void setOrderByColumns( List<NameAndDirection> orderByColumns ) {
		this.orderByColumns = orderByColumns;
	}

	/**
	 * Get the selects
	 * 
	 * @return The selects
	 */
	public List<QoQSelectExecution> getSelects() {
		return selects;
	}

	/**
	 * get the additional columns, which are only being selected for the order by
	 * 
	 * @return The additional columns
	 */
	public Set<Key> getAdditionalColumns() {
		return additionalColumns;
	}

	/**
	 * Set the additional columns
	 * 
	 * @param additionalColumns The additional columns
	 */
	public void setAdditionalColumns( Set<Key> additionalColumns ) {
		this.additionalColumns = additionalColumns;
	}

	/**
	 * Add a select
	 * 
	 * @param select The select
	 * 
	 * @return The QoQSelectStatementExecution
	 */
	public QoQSelectStatementExecution addSelect( QoQSelectExecution select ) {
		select.setQoQSelectStatementExecution( this );
		selects.add( select );
		return this;
	}

	/**
	 * Create a new QoQSelectExecution
	 * 
	 * @param select      The select node
	 * @param tableLookup The table lookup
	 * 
	 * @return The new QoQSelectExecution
	 */
	public QoQSelectExecution newQoQSelectExecution( SQLSelect select, Map<SQLTable, Query> tableLookup ) {
		var QoQExec = QoQSelectExecution.of( select, tableLookup );
		addSelect( QoQExec );
		return QoQExec;
	}

	/**
	 * Get the JDBC statement
	 * 
	 * @return The JDBC statement
	 */
	public QoQStatement getJDBCStatement() {
		return JDBCStatement;
	}

}
