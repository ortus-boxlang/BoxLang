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
import ortus.boxlang.compiler.ast.sql.SQLStatement;
import ortus.boxlang.compiler.ast.sql.select.expression.SQLOrderBy;
import ortus.boxlang.compiler.ast.sql.select.expression.literal.SQLNumberLiteral;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * Abstract Node class representing SQL SELECT statement
 * I may represent more than one actual SQLSelect unioned together
 */
public class SQLSelectStatement extends SQLStatement {

	private SQLSelect			select;
	private List<SQLSelect>		unions;
	private List<SQLOrderBy>	orderBys;
	private SQLNumberLiteral	limit;

	/**
	 * Constructor
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code of the statement
	 */
	public SQLSelectStatement( SQLSelect select, List<SQLSelect> unions, List<SQLOrderBy> orderBys, SQLNumberLiteral limit, Position position,
	    String sourceText ) {
		super( position, sourceText );
		setSelect( select );
		setUnions( unions );
		setOrderBys( orderBys );
		setLimit( limit );
	}

	/**
	 * Set the SELECT statement
	 */
	public void setSelect( SQLSelect select ) {
		replaceChildren( this.select, select );
		this.select = select;
		if ( select != null ) {
			this.select.setParent( this );
		}
	}

	/**
	 * Get the SELECT statement
	 */
	public SQLSelect getSelect() {
		return select;
	}

	/**
	 * Set the UNIONed SELECT statements
	 */
	public void setUnions( List<SQLSelect> unions ) {
		replaceChildren( this.unions, unions );
		this.unions = unions;
		if ( unions != null ) {
			unions.forEach( u -> u.setParent( this ) );
		}
	}

	/**
	 * Get the UNIONed SELECT statements
	 */
	public List<SQLSelect> getUnions() {
		return unions;
	}

	/**
	 * Set the ORDER BY nodes
	 */
	public void setOrderBys( List<SQLOrderBy> orderBys ) {
		replaceChildren( this.orderBys, orderBys );
		this.orderBys = orderBys;
		if ( orderBys != null ) {
			orderBys.forEach( o -> o.setParent( this ) );
		}
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

	/**
	 * Get the ORDER BY nodes
	 */
	public List<SQLOrderBy> getOrderBys() {
		return orderBys;
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

		map.put( "select", select.toMap() );
		if ( unions != null ) {
			map.put( "unions", unions.stream().map( SQLSelect::toMap ).toList() );
		} else {
			map.put( "unions", null );
		}
		if ( orderBys != null ) {
			map.put( "orderBys", orderBys.stream().map( SQLOrderBy::toMap ).toList() );
		} else {
			map.put( "orderBys", null );
		}
		return map;
	}

}
