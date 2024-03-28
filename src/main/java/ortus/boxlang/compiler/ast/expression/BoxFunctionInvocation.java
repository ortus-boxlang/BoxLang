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
import ortus.boxlang.compiler.ast.ReferenceByName;

/**
 * AST Node representing a fully qualified name
 */
public class BoxFunctionInvocation extends BoxExpr {

	private final ReferenceByName name;

	public ReferenceByName getName() {
		return name;
	}

	private final List<BoxArgument> arguments;

	public List<BoxArgument> getArguments() {
		return arguments;
	}

	/**
	 * Function invocation i.e. create(x)
	 *
	 * @param name       name of the function to invoke
	 * @param arguments  list of arguments
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxFunctionInvocation( String name, List<BoxArgument> arguments, Position position, String sourceText ) {
		super( position, sourceText );
		this.name		= new ReferenceByName( name );
		this.arguments	= Collections.unmodifiableList( arguments );
		this.arguments.forEach( arg -> arg.setParent( this ) );
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "name", name );
		map.put( "arguments", arguments.stream().map( BoxExpr::toMap ).collect( Collectors.toList() ) );
		return map;
	}
}
