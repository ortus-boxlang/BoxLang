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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.IBoxDocumentableNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * Root node for a property
 */
public class BoxProperty extends BoxNode implements IBoxDocumentableNode {

	private List<BoxAnnotation>					annotations;
	private List<BoxAnnotation>					postAnnotations;
	private List<BoxDocumentationAnnotation>	documentation;

	/**
	 * Creates an AST for a program which is represented by a list of statements
	 *
	 * @param annotations   list of the annotations
	 * @param documentation list of the documentation annotations
	 * @param position      position within the source code
	 * @param sourceText    source code
	 *
	 * @see Position
	 * @see BoxStatement
	 */
	public BoxProperty( List<BoxAnnotation> annotations, List<BoxAnnotation> postAnnotations, List<BoxDocumentationAnnotation> documentation, Position position,
	    String sourceText ) {
		super( position, sourceText );
		setAnnotations( annotations );
		setPostAnnotations( postAnnotations );
		setDocumentation( documentation );
	}

	public List<BoxAnnotation> getAnnotations() {
		return annotations;
	}

	public List<BoxAnnotation> getPostAnnotations() {
		return postAnnotations;
	}

	public List<BoxAnnotation> getAllAnnotations() {
		List<BoxAnnotation> allAnnotations = new ArrayList<>();
		allAnnotations.addAll( annotations );
		allAnnotations.addAll( postAnnotations );
		return allAnnotations;
	}

	public void setPostAnnotations( List<BoxAnnotation> postAnnotations ) {
		replaceChildren( this.postAnnotations, postAnnotations );
		this.postAnnotations = postAnnotations;
		this.postAnnotations.forEach( arg -> arg.setParent( this ) );
	}

	public List<BoxDocumentationAnnotation> getDocumentation() {
		return documentation;
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

		map.put( "annotations", getAllAnnotations().stream().map( BoxAnnotation::toMap ).collect( java.util.stream.Collectors.toList() ) );
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
