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

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * Represent A closure declaration
 */
public class BoxClosure extends BoxExpression {

	private List<BoxArgumentDeclaration>	args;
	private List<BoxAnnotation>				annotations;
	private BoxStatement					body;

	/**
	 * Creates a Closure AST node
	 *
	 * @param args        arguments
	 * @param annotations annotations
	 * @param body        body of the closure
	 * @param position    position of the statement in the source code
	 * @param sourceText  source code that originated the Node
	 */
	public BoxClosure( List<BoxArgumentDeclaration> args, List<BoxAnnotation> annotations, BoxStatement body, Position position,
	    String sourceText ) {
		super( position, sourceText );
		setArgs( args );
		setAnnotations( annotations );
		setBody( body );
	}

	public List<BoxArgumentDeclaration> getArgs() {
		return args;
	}

	public List<BoxAnnotation> getAnnotations() {
		return annotations;
	}

	public BoxStatement getBody() {
		return body;
	}

	public void setArgs( List<BoxArgumentDeclaration> args ) {
		replaceChildren( this.args, args );
		this.args = args;
		this.args.forEach( arg -> arg.setParent( this ) );
	}

	public void setAnnotations( List<BoxAnnotation> annotations ) {
		replaceChildren( this.annotations, annotations );
		this.annotations = annotations;
		this.annotations.forEach( arg -> arg.setParent( this ) );
	}

	public void setBody( BoxStatement body ) {
		replaceChildren( this.body, body );
		this.body = body;
		this.body.setParent( this );
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "annotations", annotations.stream().map( BoxAnnotation::toMap ).collect( java.util.stream.Collectors.toList() ) );
		map.put( "args", args.stream().map( BoxArgumentDeclaration::toMap ).collect( java.util.stream.Collectors.toList() ) );
		map.put( "body", body.toMap() );
		return map;
	}

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}

}
