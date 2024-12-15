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
	QoQStatement					JDBCStatement;

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

	public static QoQSelectStatementExecution of( SQLSelectStatement selectStatement, List<ParamItem> params, QoQStatement JDBCStatement ) {
		return new QoQSelectStatementExecution( selectStatement, params, JDBCStatement );
	}

	public SQLSelectStatement getSelectStatement() {
		return selectStatement;
	}

	public List<ParamItem> getParams() {
		return params;
	}

	public Set<Key> getResultColumnName() {
		return resultColumnNames;
	}

	public void setResultColumnNames( Set<Key> resultColumnNames ) {
		this.resultColumnNames = resultColumnNames;
	}

	public List<NameAndDirection> getOrderByColumns() {
		return orderByColumns;
	}

	public void setOrderByColumns( List<NameAndDirection> orderByColumns ) {
		this.orderByColumns = orderByColumns;
	}

	public List<QoQSelectExecution> getSelects() {
		return selects;
	}

	public Set<Key> getAdditionalColumns() {
		return additionalColumns;
	}

	public void setAdditionalColumns( Set<Key> additionalColumns ) {
		this.additionalColumns = additionalColumns;
	}

	public QoQSelectStatementExecution addSelect( QoQSelectExecution select ) {
		select.setQoQSelectStatementExecution( this );
		selects.add( select );
		return this;
	}

	public QoQSelectExecution newQoQSelectExecution( SQLSelect select, Map<SQLTable, Query> tableLookup ) {
		var QoQExec = QoQSelectExecution.of( select, tableLookup );
		addSelect( QoQExec );
		return QoQExec;
	}

	public QoQStatement getJDBCStatement() {
		return JDBCStatement;
	}

}
