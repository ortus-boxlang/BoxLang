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
package ortus.boxlang.compiler.ast.sql.select;

import java.util.List;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.sql.SQLNode;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLExpression;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Abstract Node class representing SQL SELECT statement
 */
public class SQLSelect extends SQLNode {

	private boolean					distinct;

	private List<SQLResultColumn>	resultColumns;

	private SQLTable				table;

	private List<SQLJoin>			joins;

	private SQLExpression			where;

	private List<SQLExpression>		groupBys;

	private SQLExpression			having;

	/**
	 * Constructor
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code of the statement
	 */
	protected SQLSelect( boolean distinct, List<SQLResultColumn> resultColumns, SQLTable table, List<SQLJoin> joins, SQLExpression where,
	    List<SQLExpression> groupBys,
	    SQLExpression having, Position position, String sourceText ) {
		super( position, sourceText );
		setDistinct( distinct );
		setResultColumns( resultColumns );
		setTable( table );
		setJoins( joins );
		setWhere( where );
		setGroupBys( groupBys );
		setHaving( having );
	}

	/**
	 * Set the DISTINCT flag
	 */
	public void setDistinct( boolean distinct ) {
		this.distinct = distinct;
	}

	/**
	 * Set the result columns
	 */
	public void setResultColumns( List<SQLResultColumn> resultColumns ) {
		replaceChildren( this.resultColumns, resultColumns );
		this.resultColumns = resultColumns;
		resultColumns.forEach( c -> c.setParent( this ) );
	}

	/**
	 * Set the table
	 */
	public void setTable( SQLTable table ) {
		replaceChildren( this.table, table );
		this.table = table;
		table.setParent( this );
	}

	/**
	 * Set the JOINs
	 */
	public void setJoins( List<SQLJoin> joins ) {
		replaceChildren( this.joins, joins );
		this.joins = joins;
		joins.forEach( j -> j.setParent( this ) );
	}

	/**
	 * Set the WHERE node
	 */
	public void setWhere( SQLExpression where ) {
		if ( !where.isBoolean() ) {
			throw new BoxRuntimeException( "WHERE clause must be a boolean expression" );
		}
		replaceChildren( this.where, where );
		this.where = where;
		where.setParent( this );
	}

	/**
	 * Set the GROUP BY nodes
	 */
	public void setGroupBys( List<SQLExpression> groupBys ) {
		replaceChildren( this.groupBys, groupBys );
		this.groupBys = groupBys;
		groupBys.forEach( g -> g.setParent( this ) );
	}

	/**
	 * Set the HAVING node
	 */
	public void setHaving( SQLExpression having ) {
		if ( !having.isBoolean() ) {
			throw new BoxRuntimeException( "HAVING clause must be a boolean expression" );
		}
		replaceChildren( this.having, having );
		this.having = having;
		having.setParent( this );
	}

	/**
	 * Get the DISTINCT flag
	 */
	public boolean isDistinct() {
		return distinct;
	}

	/**
	 * Get the result columns
	 */
	public List<SQLResultColumn> getResultColumns() {
		return resultColumns;
	}

	/**
	 * Get the table
	 */
	public SQLTable getTable() {
		return table;
	}

	/**
	 * Get the JOINs
	 */
	public List<SQLJoin> getJoins() {
		return joins;
	}

	/**
	 * Get the WHERE node
	 */
	public SQLExpression getWhere() {
		return where;
	}

	/**
	 * Get the GROUP BY nodes
	 */
	public List<SQLExpression> getGroupBys() {
		return groupBys;
	}

	/**
	 * Get the HAVING node
	 */
	public SQLExpression getHaving() {
		return having;
	}

	@Override
	public void accept( VoidBoxVisitor v ) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "Unimplemented method 'accept'" );
	}

	@Override
	public BoxNode accept( ReplacingBoxVisitor v ) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "Unimplemented method 'accept'" );
	}
}
