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

import ortus.boxlang.compiler.ast.BoxClass;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * AST node for a named local class defined inside a script or template.
 * <p>
 * Unlike a top-level {@link BoxClass} (which represents a {@code .bx} class file),
 * a {@code BoxLocalClass} is a named class defined inline in a {@code .bxs} script,
 * {@code .bxm} template, or {@code <bx:script>} block. Its name is scoped to the
 * enclosing script and may only be instantiated within it via {@code new Name()}.
 * <p>
 * This node extends {@link BoxClass} and adds only a {@code name} field; all other
 * class structure (body, annotations, documentation, properties) is inherited.
 * The {@code imports} list inherited from {@link BoxClass} is always empty on this
 * AST node — hoisted imports for peer local-class resolution are added by the
 * transpiler at compile time.
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
public class BoxLocalClass extends BoxClass {

	/** The unqualified name of the class as written in source (e.g. {@code Person}). */
	private BoxIdentifier name;

	/**
	 * Creates an AST node for a named local class.
	 *
	 * @param name          the simple class name identifier
	 * @param body          class body statements
	 * @param annotations   class-level annotations
	 * @param documentation documentation annotations
	 * @param properties    property declarations
	 * @param position      source position
	 * @param sourceText    original source text
	 */
	public BoxLocalClass(
	    BoxIdentifier name,
	    List<BoxStatement> body,
	    List<BoxAnnotation> annotations,
	    List<BoxDocumentationAnnotation> documentation,
	    List<BoxProperty> properties,
	    Position position,
	    String sourceText ) {
		super( List.of(), body, annotations, documentation, properties, position, sourceText );
		setName( name );
	}

	public BoxIdentifier getName() {
		return this.name;
	}

	public void setName( BoxIdentifier name ) {
		replaceChildren( this.name, name );
		this.name = name;
		if ( this.name != null ) {
			this.name.setParent( this );
		}
	}

	@Override
	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	@Override
	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();
		map.put( "name", this.name != null ? this.name.toMap() : null );
		return map;
	}
}
