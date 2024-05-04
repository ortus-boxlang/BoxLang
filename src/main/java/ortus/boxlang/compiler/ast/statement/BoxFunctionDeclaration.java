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
package ortus.boxlang.compiler.ast.statement;

import java.util.List;
import java.util.Map;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * AST Node representing a function definition
 */
public class BoxFunctionDeclaration extends BoxStatement {

	private BoxAccessModifier					accessModifier;
	private List<BoxMethodDeclarationModifier>	modifiers;
	private String								name;
	private List<BoxArgumentDeclaration>		args;
	private BoxReturnType						type;
	// abstract function interfaces have a null body
	private List<BoxStatement>					body;
	private List<BoxAnnotation>					annotations;
	private List<BoxDocumentationAnnotation>	documentation;

	/**
	 * Creates the AST node
	 *
	 * @param accessModifier public,private etc
	 * @param name           name of the function
	 * @param type           return type
	 * @param args           List of arguments
	 * @param annotations    annotations
	 * @param documentation  documentation
	 * @param body           body of the function
	 * @param position       position of the statement in the source code
	 * @param sourceText     source code that originated the Node
	 *
	 * @see BoxAccessModifier
	 * @see BoxArgumentDeclaration
	 */
	public BoxFunctionDeclaration( BoxAccessModifier accessModifier, List<BoxMethodDeclarationModifier> modifiers, String name, BoxReturnType type,
	    List<BoxArgumentDeclaration> args,
	    List<BoxAnnotation> annotations, List<BoxDocumentationAnnotation> documentation, List<BoxStatement> body, Position position, String sourceText ) {
		super( position, sourceText );
		setAccessModifier( accessModifier );
		setModifiers( modifiers );
		setName( name );
		setType( type );
		setArgs( args );
		setBody( body );
		setAnnotations( annotations );
		setDocumentation( documentation );
	}

	public BoxFunctionDeclaration( BoxAccessModifier accessModifier, List<BoxMethodDeclarationModifier> modifiers, String name, BoxReturnType type,
	    List<BoxArgumentDeclaration> args,
	    List<BoxAnnotation> annotations, List<BoxDocumentationAnnotation> documentation, Position position, String sourceText ) {
		this( accessModifier, modifiers, name, type, args, annotations, documentation, null, position, sourceText );
	}

	public BoxAccessModifier getAccessModifier() {
		return accessModifier;
	}

	public List<BoxMethodDeclarationModifier> getModifiers() {
		return modifiers;
	}

	public String getName() {
		return name;
	}

	public BoxReturnType getType() {
		return type;
	}

	public List<BoxArgumentDeclaration> getArgs() {
		return args;
	}

	public List<BoxStatement> getBody() {
		return body;
	}

	public List<BoxAnnotation> getAnnotations() {
		return annotations;
	}

	public List<BoxDocumentationAnnotation> getDocumentation() {
		return documentation;
	}

	public void setAccessModifier( BoxAccessModifier accessModifier ) {
		this.accessModifier = accessModifier;
	}

	public void setModifiers( List<BoxMethodDeclarationModifier> modifiers ) {
		this.modifiers = modifiers;
	}

	public void setName( String name ) {
		this.name = name;
	}

	public void setType( BoxReturnType type ) {
		replaceChildren( this.type, type );
		this.type = type;
		if ( type != null ) {
			this.type.setParent( this );
		}
	}

	public void setArgs( List<BoxArgumentDeclaration> args ) {
		replaceChildren( this.args, args );
		this.args = args;
		this.args.forEach( arg -> arg.setParent( this ) );
	}

	public void setBody( List<BoxStatement> body ) {
		replaceChildren( this.body, body );
		this.body = body;
		if ( this.body != null ) {
			this.body.forEach( stmt -> stmt.setParent( this ) );
		}
	}

	public void setAnnotations( List<BoxAnnotation> annotations ) {
		replaceChildren( this.annotations, annotations );
		this.annotations = annotations;
		this.annotations.forEach( arg -> arg.setParent( this ) );
	}

	public void setDocumentation( List<BoxDocumentationAnnotation> documentation ) {
		replaceChildren( this.documentation, documentation );
		this.documentation = documentation;
		this.documentation.forEach( arg -> arg.setParent( this ) );
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "accessModifier", accessModifier != null ? enumToMap( accessModifier ) : null );
		map.put( "modifiers", modifiers.stream().map( op -> enumToMap( op ) ).toList() );
		map.put( "type", type != null ? type.toMap() : null );
		map.put( "name", name );
		map.put( "args", args.stream().map( BoxArgumentDeclaration::toMap ).collect( java.util.stream.Collectors.toList() ) );
		if ( this.body != null ) {
			map.put( "body", body.stream().map( BoxStatement::toMap ).collect( java.util.stream.Collectors.toList() ) );
		} else {
			map.put( "body", null );
		}
		map.put( "annotations", annotations.stream().map( BoxAnnotation::toMap ).collect( java.util.stream.Collectors.toList() ) );
		map.put( "documentation", documentation.stream().map( BoxDocumentationAnnotation::toMap ).collect( java.util.stream.Collectors.toList() ) );
		return map;
	}

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}
}
