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
 * AST Node representing new statement
 */
public class BoxNew extends BoxExpression {

	private BoxExpression		expression;
	private BoxIdentifier		prefix;
	private List<BoxArgument>	arguments;

	/**
	 * Creates the AST node
	 *
	 * @param expression expression representing the object to instantiate
	 * @param arguments  constructor arguments list
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxNew( BoxIdentifier prefix, BoxExpression expression, List<BoxArgument> arguments, Position position, String sourceText ) {
		super( position, sourceText );
		setExpression( expression );
		setArguments( arguments );
		setPrefix( prefix );
	}

	public BoxExpression getExpression() {
		return expression;
	}

	public List<BoxArgument> getArguments() {
		return arguments;
	}

	public BoxIdentifier getPrefix() {
		return prefix;
	}

	public void setExpression( BoxExpression expression ) {
		replaceChildren( this.expression, expression );
		this.expression = expression;
		if ( expression != null ) {
			this.expression.setParent( this );
		}
	}

	public void setArguments( List<BoxArgument> arguments ) {
		replaceChildren( this.arguments, arguments );
		this.arguments = arguments;
		this.arguments.forEach( arg -> arg.setParent( this ) );
	}

	public void setPrefix( BoxIdentifier prefix ) {
		replaceChildren( this.prefix, prefix );
		this.prefix = prefix;
		if ( prefix != null ) {
			this.prefix.setParent( this );
		}
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		if ( prefix == null ) {
			map.put( "prefix", null );
		} else {
			map.put( "prefix", prefix.toMap() );
		}
		if ( expression == null ) {
			map.put( "expression", null );
		} else {
			map.put( "expression", expression.toMap() );
		}
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
