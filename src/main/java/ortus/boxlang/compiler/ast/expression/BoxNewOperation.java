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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ortus.boxlang.compiler.ast.BoxExpr;
import ortus.boxlang.compiler.ast.Position;

/**
 * AST Node representing new statement
 */
public class BoxNewOperation extends BoxExpr {

	private final BoxExpr			expression;
	private final BoxIdentifier		prefix;
	private final List<BoxArgument>	arguments;

	/**
	 * Creates the AST node
	 *
	 * @param expression expression representing the object to instantiate
	 * @param arguments  constructor arguments list
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxNewOperation( BoxIdentifier prefix, BoxExpr expression, List<BoxArgument> arguments, Position position, String sourceText ) {
		super( position, sourceText );
		this.expression = expression;
		if ( expression != null ) {
			this.expression.setParent( this );
		}
		this.arguments = Collections.unmodifiableList( arguments );
		this.arguments.forEach( arg -> arg.setParent( this ) );
		this.prefix = prefix;
		if ( prefix != null ) {
			this.prefix.setParent( this );
		}
	}

	public BoxExpr getExpression() {
		return expression;
	}

	public List<BoxArgument> getArguments() {
		return arguments;
	}

	public BoxIdentifier getPrefix() {
		return prefix;
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
		map.put( "arguments", arguments.stream().map( BoxExpr::toMap ).collect( Collectors.toList() ) );
		return map;
	}
}
