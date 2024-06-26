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
package ortus.boxlang.compiler.ast.expression;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * AST Node representing an invoked expression
 */
public class BoxExpressionInvocation extends BoxExpression {

	private BoxExpression		expr;
	private List<BoxArgument>	arguments;

	/**
	 * Function invocation i.e. create(x)
	 *
	 * @param expr       expression to invoke
	 * @param arguments  list of arguments
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxExpressionInvocation( BoxExpression expr, List<BoxArgument> arguments, Position position, String sourceText ) {
		super( position, sourceText );
		setExpr( expr );
		setArguments( arguments );
	}

	public BoxExpression getExpr() {
		return expr;
	}

	public List<BoxArgument> getArguments() {
		return arguments;
	}

	public void setExpr( BoxExpression expr ) {
		replaceChildren( this.expr, expr );
		if ( this.expr != null ) {
			this.expr.setParent( this );
		}
		this.expr = expr;
	}

	public void setArguments( List<BoxArgument> arguments ) {
		replaceChildren( this.arguments, arguments );
		this.arguments = arguments;
		this.arguments.forEach( arg -> arg.setParent( this ) );
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "expr", expr.toMap() );
		map.put( "arguments", arguments.stream().map( BoxExpression::toMap ).collect( Collectors.toList() ) );
		return map;
	}

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}
}
