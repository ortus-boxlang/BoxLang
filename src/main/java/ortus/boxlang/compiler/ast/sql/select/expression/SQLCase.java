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

import java.util.List;
import java.util.Map;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;
import ortus.boxlang.runtime.jdbc.qoq.QoQSelectExecution;
import ortus.boxlang.runtime.operators.Compare;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Abstract Node class representing SQL case expression
 */
public class SQLCase extends SQLExpression {

	private SQLExpression			inputExpression;
	private List<SQLCaseWhenThen>	whenThens;
	private SQLExpression			elseExpression;

	/**
	 * Constructor
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code of the statement
	 */
	public SQLCase( SQLExpression inputExpression, List<SQLCaseWhenThen> whenThens, SQLExpression elseExpression, Position position, String sourceText ) {
		super( position, sourceText );
		setInputExpression( inputExpression );
		setWhenThens( whenThens );
		setElseExpression( elseExpression );
	}

	/**
	 * Get the input expression
	 */
	public SQLExpression getInputExpression() {
		return inputExpression;
	}

	/**
	 * Set the input expression
	 */
	public void setInputExpression( SQLExpression inputExpression ) {
		replaceChildren( this.inputExpression, inputExpression );
		this.inputExpression = inputExpression;
		if ( this.inputExpression != null ) {
			this.inputExpression.setParent( this );
		}
	}

	/**
	 * Get the when clauses
	 */
	public List<SQLCaseWhenThen> getWhenThens() {
		return whenThens;
	}

	/**
	 * Set the when clauses
	 */
	public void setWhenThens( List<SQLCaseWhenThen> whenThens ) {
		replaceChildren( this.whenThens, whenThens );
		this.whenThens = whenThens;
		this.whenThens.forEach( e -> e.setParent( this ) );
	}

	/**
	 * Get the else expression
	 */
	public SQLExpression getElseExpression() {
		return elseExpression;
	}

	/**
	 * Set the else expression
	 */
	public void setElseExpression( SQLExpression elseExpression ) {
		replaceChildren( this.elseExpression, elseExpression );
		this.elseExpression = elseExpression;
		if ( this.elseExpression != null ) {
			this.elseExpression.setParent( this );
		}
	}

	/**
	 * Runtime check if the expression evaluates to a boolean value and works for columns as well
	 * 
	 * @param QoQExec Query execution state
	 * 
	 * @return true if the expression evaluates to a boolean value
	 */
	public boolean isBoolean( QoQSelectExecution QoQExec ) {
		// Check first then and guess the rest are the same
		return whenThens.get( 0 ).getThenExpression().isBoolean( QoQExec );
	}

	/**
	 * Runtime check if the expression evaluates to a numeric value and works for columns as well
	 * 
	 * @param QoQExec Query execution state
	 * 
	 * @return true if the expression evaluates to a numeric value
	 */
	public boolean isNumeric( QoQSelectExecution QoQExec ) {
		// Check first then and guess the rest are the same
		return whenThens.get( 0 ).getThenExpression().isNumeric( QoQExec );
	}

	/**
	 * What type does this expression evaluate to
	 */
	public QueryColumnType getType( QoQSelectExecution QoQExec ) {
		// Check first then and guess the rest are the same
		return whenThens.get( 0 ).getThenExpression().getType( QoQExec );
	}

	/**
	 * Evaluate the expression
	 */
	public Object evaluate( QoQSelectExecution QoQExec, int[] intersection ) {
		if ( inputExpression == null ) {
			return processStandardCase( QoQExec, intersection, null );
		} else {
			return processInputCase( QoQExec, intersection, null );
		}
	}

	/**
	 * Evaluate the expression aginst a partition of data
	 */
	public Object evaluateAggregate( QoQSelectExecution QoQExec, List<int[]> intersections ) {
		if ( inputExpression == null ) {
			return processStandardCase( QoQExec, null, intersections );
		} else {
			return processInputCase( QoQExec, null, intersections );
		}
	}

	/**
	 * Process case/when/then with no input expression. Each when expression must be a boolean
	 * 
	 * @param QoQExec       Query execution state
	 * @param intersection  The intersection of the data to evaluate
	 * @param intersections The list of intersections to evaluate
	 * 
	 * @return The result of the case expression
	 */
	private Object processStandardCase( QoQSelectExecution QoQExec, int[] intersection, List<int[]> intersections ) {

		// If any when case when expressions are not boolean throw an error
		for ( SQLCaseWhenThen whenThen : whenThens ) {
			if ( !whenThen.getWhenExpression().isBoolean( QoQExec ) ) {
				throw new BoxRuntimeException( "Case/When/Then expressions must be boolean.  The case [" + whenThen.getWhenExpression().getSourceText()
				    + "] is a [" + whenThen.getWhenExpression().getType( QoQExec ).toString() + "]" );
			}
		}
		for ( SQLCaseWhenThen whenThen : whenThens ) {
			Boolean result;
			if ( intersection != null ) {
				result = ( Boolean ) whenThen.getWhenExpression().evaluate( QoQExec, intersection );
			} else {
				result = ( Boolean ) whenThen.getWhenExpression().evaluateAggregate( QoQExec, intersections );
			}
			if ( result ) {
				return whenThen.getThenExpression().evaluate( QoQExec, intersection );
			}
		}
		if ( elseExpression != null ) {
			return elseExpression.evaluate( QoQExec, intersection );
		}
		return null;
	}

	private Object processInputCase( QoQSelectExecution QoQExec, int[] intersection, List<int[]> intersections ) {
		Object inputValue;
		if ( intersection != null ) {
			inputValue = inputExpression.evaluate( QoQExec, intersection );
		} else {
			inputValue = inputExpression.evaluateAggregate( QoQExec, intersections );
		}

		for ( SQLCaseWhenThen whenThen : whenThens ) {
			Boolean	result;
			Object	caseValue;
			if ( intersection != null ) {
				caseValue = whenThen.getWhenExpression().evaluate( QoQExec, intersection );
			} else {
				caseValue = whenThen.getWhenExpression().evaluateAggregate( QoQExec, intersections );
			}
			if ( Compare.invoke( inputValue, caseValue ) == 0 ) {
				return whenThen.getThenExpression().evaluate( QoQExec, intersection );
			}
		}
		if ( elseExpression != null ) {
			return elseExpression.evaluate( QoQExec, intersection );
		}
		return null;
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

		if ( inputExpression != null ) {
			map.put( "inputExpression", inputExpression.toMap() );
		} else {
			map.put( "inputExpression", null );
		}

		map.put( "whenThens", whenThens.stream().map( SQLCaseWhenThen::toMap ).toArray() );

		if ( elseExpression != null ) {
			map.put( "elseExpression", elseExpression.toMap() );
		} else {
			map.put( "elseExpression", null );
		}
		return map;
	}

}
