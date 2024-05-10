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

import ortus.boxlang.compiler.ast.comment.BoxDocComment;
import ortus.boxlang.compiler.ast.statement.BoxDocumentationAnnotation;

/**
 * Root node for a Class
 */
public interface IBoxDocumentableNode {

	/**
	 * Get documentation from node
	 * 
	 * @return list of documentation annotations
	 */
	public List<BoxDocumentationAnnotation> getDocumentation();

	/**
	 * Set documentation for node
	 * 
	 * @param documentation list of documentation annotations
	 */
	public void setDocumentation( List<BoxDocumentationAnnotation> documentation );

	/**
	 * Get the last documentation comment
	 * 
	 * @return the last documentation comment
	 */
	public BoxDocComment getDocComment();

	/**
	 * Called when all comments are associated with the node, so it can process any doc comment contents
	 */
	public default void finalizeDocumentation() {
		BoxDocComment doc = getDocComment();
		if ( doc != null ) {
			for ( BoxDocumentationAnnotation n : doc.getAnnotations() ) {
				getDocumentation().add( n );
				n.setParent( ( BoxNode ) this );
			}
		}
	}

}
