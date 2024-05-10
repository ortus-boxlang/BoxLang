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
package ortus.boxlang.compiler.ast;

import java.util.List;
import java.util.Map;

import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxDocumentationAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxImport;
import ortus.boxlang.compiler.ast.statement.BoxProperty;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * Root node for a Class
 */
public class BoxClass extends BoxNode implements IBoxDocumentableNode {

	private List<BoxStatement>					body;
	private List<BoxImport>						imports;
	private List<BoxAnnotation>					annotations;
	private List<BoxDocumentationAnnotation>	documentation;
	private List<BoxProperty>					properties;

	/**
	 * Creates an AST for a Class
	 *
	 * @param imports       list of imports
	 * @param body          list of statements
	 * @param annotations   list of annotations
	 * @param documentation list of documentation annotations
	 * @param properties    list of properties
	 * @param position      position in the source file
	 * @param sourceText    the source text
	 *
	 * @see Position
	 * @see BoxStatement
	 */
	public BoxClass( List<BoxImport> imports, List<BoxStatement> body, List<BoxAnnotation> annotations,
	    List<BoxDocumentationAnnotation> documentation, List<BoxProperty> properties, Position position,
	    String sourceText ) {
		super( position, sourceText );
		setImports( imports );
		setBody( body );
		setAnnotations( annotations );
		setDocumentation( documentation );
		setProperties( properties );
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

	public List<BoxImport> getImports() {
		return imports;
	}

	public List<BoxProperty> getProperties() {
		return properties;
	}

	public void setBody( List<BoxStatement> body ) {
		replaceChildren( this.body, body );
		this.body = body;
		this.body.forEach( arg -> arg.setParent( this ) );
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

	public void setImports( List<BoxImport> imports ) {
		replaceChildren( this.imports, imports );
		this.imports = imports;
		this.imports.forEach( arg -> arg.setParent( this ) );
	}

	public void setProperties( List<BoxProperty> properties ) {
		replaceChildren( this.properties, properties );
		this.properties = properties;
		this.properties.forEach( arg -> arg.setParent( this ) );
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "imports", imports.stream().map( BoxImport::toMap ).collect( java.util.stream.Collectors.toList() ) );
		map.put( "body", body.stream().map( BoxStatement::toMap ).collect( java.util.stream.Collectors.toList() ) );
		map.put( "annotations", annotations.stream().map( BoxAnnotation::toMap ).collect( java.util.stream.Collectors.toList() ) );
		map.put( "documentation", documentation.stream().map( BoxDocumentationAnnotation::toMap ).collect( java.util.stream.Collectors.toList() ) );
		map.put( "properties", properties.stream().map( BoxProperty::toMap ).collect( java.util.stream.Collectors.toList() ) );
		return map;
	}

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}
}
