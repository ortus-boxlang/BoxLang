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

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.Position;

/**
 * AST Node representing a function/method argument definition
 */
public class BoxArgumentDeclaration extends BoxStatement {

	private Boolean								required;
	private String								name;
	private String								type;
	private BoxExpression						value;
	private List<BoxAnnotation>					annotations;
	private List<BoxDocumentationAnnotation>	documentation;

	/**
	 * Creates the AST node
	 *
	 * @param expression argument expression to assert
	 */

	/**
	 * Creates the AST node
	 *
	 * @param required      required parameter
	 * @param type          type parameter
	 * @param name          parameter name
	 * @param defaultValue  optional default value
	 * @param annotations   list of annotation
	 * @param documentation list of annotation
	 * @param position      position of the statement in the source code
	 * @param sourceText    source code that originated the Node
	 */

	public BoxArgumentDeclaration( Boolean required, String type, String name, BoxExpression defaultValue, List<BoxAnnotation> annotations,
	    List<BoxDocumentationAnnotation> documentation, Position position, String sourceText ) {
		super( position, sourceText );
		setRequired( required );
		setType( type );
		setName( name );
		setValue( defaultValue );
		setAnnotations( annotations );
		setDocumentation( documentation );
	}

	public String getName() {
		return name;
	}

	public String getType() {
		return type;
	}

	public BoxExpression getValue() {
		return value;
	}

	public Boolean getRequired() {
		return required;
	}

	public List<BoxAnnotation> getAnnotations() {
		return annotations;
	}

	public List<BoxDocumentationAnnotation> getDocumentation() {
		return documentation;
	}

	void setValue( BoxExpression value ) {
		replaceChildren( this.value, value );
		this.value = value;
		if ( this.value != null ) {
			this.value.setParent( this );
		}
	}

	void setName( String name ) {
		this.name = name;
	}

	void setType( String type ) {
		this.type = type;
	}

	void setRequired( Boolean required ) {
		this.required = required;
	}

	void setAnnotations( List<BoxAnnotation> annotations ) {
		replaceChildren( this.annotations, annotations );
		this.annotations = annotations;
		if ( this.annotations != null ) {
			this.annotations.forEach( arg -> arg.setParent( this ) );
		}
	}

	void setDocumentation( List<BoxDocumentationAnnotation> documentation ) {
		replaceChildren( this.documentation, documentation );
		this.documentation = documentation;
		if ( this.documentation != null ) {
			this.documentation.forEach( arg -> arg.setParent( this ) );
		}
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "name", name );
		if ( value != null ) {
			map.put( "value", value.toMap() );
		} else {
			map.put( "value", null );
		}
		map.put( "required", required );
		map.put( "type", type );
		map.put( "annotations", annotations.stream().map( BoxAnnotation::toMap ).collect( java.util.stream.Collectors.toList() ) );
		map.put( "documentation", documentation.stream().map( BoxDocumentationAnnotation::toMap ).collect( java.util.stream.Collectors.toList() ) );

		return map;
	}
}
