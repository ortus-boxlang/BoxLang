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

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.ast.Position;
import ortus.boxlang.ast.statement.BoxAnnotation;
import ortus.boxlang.ast.statement.BoxArgumentDeclaration;

/**
 * Represent A lambda declaration
 */
public class BoxLambda extends BoxExpr {

	private final List<BoxArgumentDeclaration>	args;
	private final List<BoxAnnotation>			annotations;
	private final List<BoxStatement>			body;

	/**
	 * Creates a Lambda declaration AST node
	 *
	 * @param args        arguments
	 * @param annotations annotations
	 * @param body        body of the function
	 * @param position    position of the statement in the source code
	 * @param sourceText  source code that originated the Node
	 */
	public BoxLambda( List<BoxArgumentDeclaration> args, List<BoxAnnotation> annotations, List<BoxStatement> body, Position position,
	    String sourceText ) {
		super( position, sourceText );
		this.args = Collections.unmodifiableList( args );
		this.args.forEach( arg -> arg.setParent( this ) );
		this.annotations = annotations;
		this.annotations.forEach( arg -> arg.setParent( this ) );
		this.body = Collections.unmodifiableList( body );
		this.body.forEach( stmt -> stmt.setParent( this ) );
	}

	public List<BoxArgumentDeclaration> getArgs() {
		return args;
	}

	public List<BoxAnnotation> getAnnotations() {
		return annotations;
	}

	public List<BoxStatement> getBody() {
		return body;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "annotations", annotations.stream().map( BoxAnnotation::toMap ).collect( java.util.stream.Collectors.toList() ) );
		map.put( "args", args.stream().map( BoxArgumentDeclaration::toMap ).collect( java.util.stream.Collectors.toList() ) );
		map.put( "body", body.stream().map( BoxStatement::toMap ).collect( java.util.stream.Collectors.toList() ) );
		return map;
	}

}
