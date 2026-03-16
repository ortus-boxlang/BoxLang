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

import java.util.List;
import java.util.Map;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * Root AST node for a named local class defined inside a script or template.
 * <p>
 * Unlike a top-level {@code BoxClass} (which represents a {@code .bx} class file),
 * a {@code BoxLocalClass} is a named class defined inline in a {@code .bxs} script,
 * {@code .bxm} template, or {@code <bx:script>} block. Its name is scoped to the
 * enclosing script and may only be instantiated within it via {@code new Name()}.
 * <p>
 * Example:
 * 
 * <pre>{@code
 * class Person {
 *     function init( required String name ) {
 *         variables.name = arguments.name;
 *     }
 *     function getName() {
 *         return variables.name;
 *     }
 * }
 * p = new Person( "Luis" );
 * }</pre>
 */
public class BoxLocalClass extends BoxStatement {

	/** The unqualified name of the class as written in source (e.g. {@code Person}). */
	private BoxIdentifier						name;

	/** Imports declared inside the local class body. */
	private List<BoxImport>						imports;

	/** Executable statements and function declarations in the class body. */
	private List<BoxStatement>					body;

	/** Class-level annotations (pre- and post- annotations, plus keyword-converted ones). */
	private List<BoxAnnotation>					annotations;

	/** Javadoc / documentation annotations. */
	private List<BoxDocumentationAnnotation>	documentation;

	/** Declared {@code property} members. */
	private List<BoxProperty>					properties;

	/**
	 * Creates an AST node for a named local class.
	 *
	 * @param name          the simple class name identifier
	 * @param imports       import statements declared inside the class
	 * @param body          class body statements
	 * @param annotations   class-level annotations
	 * @param documentation documentation annotations
	 * @param properties    property declarations
	 * @param position      source position
	 * @param sourceText    original source text
	 */
	public BoxLocalClass(
	    BoxIdentifier name,
	    List<BoxImport> imports,
	    List<BoxStatement> body,
	    List<BoxAnnotation> annotations,
	    List<BoxDocumentationAnnotation> documentation,
	    List<BoxProperty> properties,
	    Position position,
	    String sourceText ) {
		super( position, sourceText );
		setName( name );
		setImports( imports );
		setBody( body );
		setAnnotations( annotations );
		setDocumentation( documentation );
		setProperties( properties );
	}

	// -------------------------------------------------------------------------
	// Getters
	// -------------------------------------------------------------------------

	public BoxIdentifier getName() {
		return this.name;
	}

	public List<BoxImport> getImports() {
		return this.imports;
	}

	public List<BoxStatement> getBody() {
		return this.body;
	}

	public List<BoxAnnotation> getAnnotations() {
		return this.annotations;
	}

	public List<BoxDocumentationAnnotation> getDocumentation() {
		return this.documentation;
	}

	public List<BoxProperty> getProperties() {
		return this.properties;
	}

	// -------------------------------------------------------------------------
	// Setters (maintain parent links for visitor traversal)
	// -------------------------------------------------------------------------

	public void setName( BoxIdentifier name ) {
		replaceChildren( this.name, name );
		this.name = name;
		if ( this.name != null ) {
			this.name.setParent( this );
		}
	}

	public void setImports( List<BoxImport> imports ) {
		replaceChildren( this.imports, imports );
		this.imports = imports;
		this.imports.forEach( arg -> arg.setParent( this ) );
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

	public void setProperties( List<BoxProperty> properties ) {
		replaceChildren( this.properties, properties );
		this.properties = properties;
		this.properties.forEach( arg -> arg.setParent( this ) );
	}

	// -------------------------------------------------------------------------
	// Visitor dispatch
	// -------------------------------------------------------------------------

	@Override
	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	@Override
	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}

	// -------------------------------------------------------------------------
	// Serialisation helpers
	// -------------------------------------------------------------------------

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();
		map.put( "name", this.name != null ? this.name.toMap() : null );
		map.put( "imports", this.imports.stream().map( BoxImport::toMap ).collect( java.util.stream.Collectors.toList() ) );
		map.put( "body", this.body.stream().map( BoxStatement::toMap ).collect( java.util.stream.Collectors.toList() ) );
		map.put( "annotations", this.annotations.stream().map( BoxAnnotation::toMap ).collect( java.util.stream.Collectors.toList() ) );
		map.put( "documentation", this.documentation.stream().map( BoxDocumentationAnnotation::toMap ).collect( java.util.stream.Collectors.toList() ) );
		map.put( "properties", this.properties.stream().map( BoxProperty::toMap ).collect( java.util.stream.Collectors.toList() ) );
		return map;
	}
}
