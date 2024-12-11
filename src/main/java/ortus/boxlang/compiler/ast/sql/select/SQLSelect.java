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
import java.util.Map;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.sql.SQLNode;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLExpression;
import ortus.boxlang.compiler.ast.sql.select.expression.literal.SQLNumberLiteral;
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

	private SQLNumberLiteral		limit;

	/**
	 * Constructor
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code of the statement
	 */
	public SQLSelect( boolean distinct, List<SQLResultColumn> resultColumns, SQLTable table, List<SQLJoin> joins, SQLExpression where,
	    List<SQLExpression> groupBys, SQLExpression having, SQLNumberLiteral limit, Position position, String sourceText ) {
		super( position, sourceText );
		setDistinct( distinct );
		setResultColumns( resultColumns );
		setTable( table );
		setJoins( joins );
		setWhere( where );
		setGroupBys( groupBys );
		setHaving( having );
		setLimit( limit );
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
		if ( resultColumns != null ) {
			for ( int i = 0; i < resultColumns.size(); i++ ) {
				SQLResultColumn column = resultColumns.get( i );
				column.setParent( this );
				column.setOrdinalPosition( i + 1 );
			}
		}
	}

	/**
	 * Set the table
	 */
	public void setTable( SQLTable table ) {
		replaceChildren( this.table, table );
		this.table = table;
		if ( table != null ) {
			table.setParent( this );
		}
	}

	/**
	 * Set the JOINs
	 */
	public void setJoins( List<SQLJoin> joins ) {
		replaceChildren( this.joins, joins );
		this.joins = joins;
		if ( joins != null ) {
			joins.forEach( j -> j.setParent( this ) );
		}
	}

	/**
	 * Set the WHERE node
	 */
	public void setWhere( SQLExpression where ) {
		if ( where != null && !where.isBoolean( null ) ) {
			throw new BoxRuntimeException( "WHERE clause must be a boolean expression" );
		}
		replaceChildren( this.where, where );
		this.where = where;
		if ( where != null ) {
			where.setParent( this );
		}
	}

	/**
	 * Set the GROUP BY nodes
	 */
	public void setGroupBys( List<SQLExpression> groupBys ) {
		replaceChildren( this.groupBys, groupBys );
		this.groupBys = groupBys;
		if ( groupBys != null ) {
			groupBys.forEach( g -> g.setParent( this ) );
		}
	}

	/**
	 * Set the HAVING node
	 */
	public void setHaving( SQLExpression having ) {
		if ( having != null && !having.isBoolean( null ) ) {
			throw new BoxRuntimeException( "HAVING clause must be a boolean expression" );
		}
		replaceChildren( this.having, having );
		this.having = having;
		if ( having != null ) {
			having.setParent( this );
		}
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

	/**
	 * Set the LIMIT node
	 */
	public void setLimit( SQLNumberLiteral limit ) {
		replaceChildren( this.limit, limit );
		this.limit = limit;
		if ( limit != null ) {
			limit.setParent( this );
		}
	}

	/**
	 * Get the LIMIT node
	 */
	public SQLNumberLiteral getLimit() {
		return limit;
	}

	/**
	 * Get the value of the limit node, defaulting to -1 if not set. -1 means no limit.
	 */
	public Long getLimitValue() {
		if ( getLimit() == null ) {
			return -1L;
		}
		return getLimit().getValue().longValue();
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

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		if ( distinct ) {
			map.put( "distinct", distinct );
		} else {
			map.put( "distinct", null );
		}

		map.put( "resultColumns", resultColumns.stream().map( SQLResultColumn::toMap ).toList() );
		if ( table != null ) {
			map.put( "table", table.toMap() );
		} else {
			map.put( "table", null );
		}
		if ( joins != null ) {
			map.put( "joins", joins.stream().map( SQLJoin::toMap ).toList() );
		} else {
			map.put( "joins", null );
		}
		if ( where != null ) {
			map.put( "where", where.toMap() );
		} else {
			map.put( "where", null );
		}
		if ( groupBys != null ) {
			map.put( "groupBys", groupBys.stream().map( SQLExpression::toMap ).toList() );
		} else {
			map.put( "groupBys", null );
		}
		if ( having != null ) {
			map.put( "having", having.toMap() );
		} else {
			map.put( "having", null );
		}
		if ( limit != null ) {
			map.put( "limit", limit.toMap() );
		} else {
			map.put( "limit", null );
		}

		return map;
	}

}
