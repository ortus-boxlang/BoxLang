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

import java.util.List;
import java.util.Map;
import java.util.Set;

import ortus.boxlang.compiler.ast.sql.select.SQLSelectStatement;
import ortus.boxlang.compiler.ast.sql.select.SQLTable;
import ortus.boxlang.runtime.jdbc.qoq.QoQExecutionService.NameAndDirection;
import ortus.boxlang.runtime.jdbc.qoq.QoQExecutionService.TypedResultColumn;
import ortus.boxlang.runtime.jdbc.qoq.QoQPreparedStatement.ParamItem;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Query;

/**
 * A wrapper class to hold together both the SQL AST being executed as well as the runtime values for a given execution of the query
 */
public class QoQExecution {

	public SQLSelectStatement			select;
	public Map<Key, TypedResultColumn>	resultColumns		= null;
	public Map<SQLTable, Query>			tableLookup;
	public List<ParamItem>				params;
	public List<NameAndDirection>		orderByColumns		= null;
	public Set<Key>						additionalColumns	= null;

	/**
	 * Constructor
	 * 
	 * @param select
	 * @param tableLookup
	 * @param params
	 * 
	 * @return
	 */
	private QoQExecution( SQLSelectStatement select, Map<SQLTable, Query> tableLookup, List<ParamItem> params ) {
		this.select			= select;
		this.tableLookup	= tableLookup;
		this.params			= params;
	}

	public static QoQExecution of( SQLSelectStatement select, Map<SQLTable, Query> tableLookup, List<ParamItem> params ) {
		return new QoQExecution( select, tableLookup, params );
	}

	public SQLSelectStatement getSelect() {
		return select;
	}

	public Map<SQLTable, Query> getTableLookup() {
		return tableLookup;
	}

	public List<ParamItem> getParams() {
		return params;
	}

	public Map<Key, TypedResultColumn> getResultColumns() {
		return resultColumns;
	}

	public void setResultColumns( Map<Key, TypedResultColumn> resultColumns ) {
		this.resultColumns = resultColumns;
	}

	public List<NameAndDirection> getOrderByColumns() {
		return orderByColumns;
	}

	public void setOrderByColumns( List<NameAndDirection> orderByColumns ) {
		this.orderByColumns = orderByColumns;
	}

	public Set<Key> getAdditionalColumns() {
		return additionalColumns;
	}

	public void setAdditionalColumns( Set<Key> additionalColumns ) {
		this.additionalColumns = additionalColumns;
	}

}
