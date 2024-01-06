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
package ortus.boxlang.ast.statement;

import java.util.List;
import java.util.Map;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.ast.Position;

/**
 * Root node for a property
 */
public class BoxProperty extends BoxNode {

	private final List<BoxAnnotation>				annotations;
	private final List<BoxDocumentationAnnotation>	documentation;

	/**
	 * Creates an AST for a program which is represented by a list of statements
	 *
	 * @param statements list of the statements nodes
	 * @param position   position within the source code
	 * @param sourceText source code
	 *
	 * @see Position
	 * @see BoxStatement
	 */
	public BoxProperty( List<BoxAnnotation> annotations, List<BoxDocumentationAnnotation> documentation, Position position, String sourceText ) {
		super( position, sourceText );
		this.annotations = annotations;
		this.annotations.forEach( arg -> arg.setParent( this ) );
		this.documentation = documentation;
		this.documentation.forEach( arg -> arg.setParent( this ) );
	}

	public List<BoxAnnotation> getAnnotations() {
		return annotations;
	}

	public List<BoxDocumentationAnnotation> getDocumentation() {
		return documentation;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "annotations", annotations.stream().map( BoxAnnotation::toMap ).collect( java.util.stream.Collectors.toList() ) );
		map.put( "documentation", documentation.stream().map( BoxDocumentationAnnotation::toMap ).collect( java.util.stream.Collectors.toList() ) );
		return map;
	}
}
