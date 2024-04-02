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
package ortus.boxlang.compiler.ast;

import java.util.List;
import java.util.Map;

import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

public class BoxDocumentation extends BoxNode {

	private List<BoxNode> annotations;

	/**
	 * Create a instance of a BoxDocumentation
	 *
	 * @param annotations
	 * @param position    position within the source code
	 * @param sourceText  source code
	 */
	public BoxDocumentation( List<BoxNode> annotations, Position position, String sourceText ) {
		super( position, sourceText );
		setAnnotations( annotations );
	}

	public List<BoxNode> getAnnotations() {
		return annotations;
	}

	public void setAnnotations( List<BoxNode> annotations ) {
		replaceChildren( this.annotations, annotations );
		this.annotations = annotations;
		this.annotations.forEach( arg -> arg.setParent( this ) );
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "annotations", annotations.stream().map( BoxNode::toMap ).collect( java.util.stream.Collectors.toList() ) );
		return map;
	}

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}

}
