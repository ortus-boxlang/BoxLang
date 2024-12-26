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
package ortus.boxlang.compiler.ast.sql.select.expression;

import java.util.Map;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.sql.SQLNode;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * Abstract Node class representing SQL case when/then expression
 */
public class SQLCaseWhenThen extends SQLNode {

	private SQLExpression	whenExpression;
	private SQLExpression	thenExpression;

	/**
	 * Constructor
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code of the statement
	 */
	public SQLCaseWhenThen( SQLExpression whenExpression, SQLExpression thenExpression, Position position, String sourceText ) {
		super( position, sourceText );
		setWhenExpression( whenExpression );
		setThenExpression( thenExpression );
	}

	/**
	 * Get the when expression
	 */
	public SQLExpression getWhenExpression() {
		return whenExpression;
	}

	/**
	 * Set the when expression
	 */
	public void setWhenExpression( SQLExpression whenExpression ) {
		replaceChildren( this.whenExpression, whenExpression );
		this.whenExpression = whenExpression;
		this.whenExpression.setParent( this );
	}

	/**
	 * Get the then expression
	 */
	public SQLExpression getThenExpression() {
		return thenExpression;
	}

	/**
	 * Set the then expression
	 */
	public void setThenExpression( SQLExpression thenExpression ) {
		replaceChildren( this.thenExpression, thenExpression );
		this.thenExpression = thenExpression;
		this.thenExpression.setParent( this );
	}

	@Override
	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	@Override
	public BoxNode accept( ReplacingBoxVisitor v ) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "Unimplemented method 'accept'" );
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "whenExpression", whenExpression.toMap() );
		map.put( "thenExpression", thenExpression.toMap() );
		return map;
	}

}
