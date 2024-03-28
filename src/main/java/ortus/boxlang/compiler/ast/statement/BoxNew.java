/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.compiler.ast.statement;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.expression.BoxArgument;
import ortus.boxlang.compiler.ast.expression.BoxFQN;

/**
 * AST Node representing a new statement
 */
public class BoxNew extends BoxStatement {

	private final BoxFQN			fqn;
	private final List<BoxArgument>	arguments;

	/**
	 * Creates the AST node
	 *
	 * @param arguments  list of arguments
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxNew( BoxFQN fqn, List<BoxArgument> arguments, Position position, String sourceText ) {
		super( position, sourceText );
		this.fqn = fqn;
		this.fqn.setParent( this );
		this.arguments = Collections.unmodifiableList( arguments );
		this.arguments.forEach( arg -> arg.setParent( this ) );
	}

	public BoxFQN getFqn() {
		return fqn;
	}

	public List<BoxArgument> getArguments() {
		return arguments;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "fqn", fqn.toMap() );
		map.put( "arguments", arguments.stream().map( s -> s.toMap() ).collect( Collectors.toList() ) );
		return map;
	}
}
