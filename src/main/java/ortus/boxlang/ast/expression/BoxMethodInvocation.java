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
package ortus.boxlang.ast.expression;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.Position;

/**
 * AST Node representing a method invocation like:
 * <code>object.method(1,"a")</code>
 */
public class BoxMethodInvocation extends BoxExpr {

	private final BoxExpr			name;

	private final List<BoxArgument>	arguments;
	private final BoxExpr			obj;
	private final Boolean			safe;
	// The reason for this flag is so we know how to treat the name expression.
	// For foo.bar() it's ALWAYS just a raw string, but for foo[ bar ]() it's a variable lookup
	// We could use different nodes for each, but that would be a lot of duplication
	private final Boolean			usedDotAccess;

	public final List<BoxArgument> getArguments() {
		return arguments;
	}

	/**
	 * Creates the AST node
	 *
	 * @param name          name of the method
	 * @param obj           object
	 * @param arguments     list of BoxArgument representing the arguments
	 * @param safe          true if the method is safe
	 * @param usedDotAccess true if the method was accessed using the dot operator
	 * @param position      position of the statement in the source code
	 * @param sourceText    source code that originated the Node
	 *
	 * @see BoxArgument
	 */
	public BoxMethodInvocation( BoxExpr name, BoxExpr obj, List<BoxArgument> arguments, Boolean safe, Boolean usedDotAccess, Position position,
	    String sourceText ) {
		super( position, sourceText );
		this.name = name;
		this.name.setParent( this );
		this.obj = obj;
		this.obj.setParent( this );
		this.safe		= safe;
		this.arguments	= Collections.unmodifiableList( arguments );
		this.arguments.forEach( arg -> arg.setParent( this ) );
		this.usedDotAccess = usedDotAccess;
	}

	public BoxExpr getName() {
		return name;
	}

	public BoxExpr getObj() {
		return obj;
	}

	public Boolean isSafe() {
		return safe;
	}

	public Boolean getUsedDotAccess() {
		return usedDotAccess;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "obj", obj.toMap() );
		map.put( "name", name.toMap() );
		map.put( "arguments", arguments.stream().map( BoxExpr::toMap ).collect( Collectors.toList() ) );
		map.put( "safe", safe );
		return map;
	}
}
