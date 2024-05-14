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

import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * Static Initializer block for a Class
 */
public class BoxStaticInitializer extends BoxStatement {

	private List<BoxStatement> body;

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
	public BoxStaticInitializer( List<BoxStatement> body, Position position, String sourceText ) {
		super( position, sourceText );
		setBody( body );
	}

	public List<BoxStatement> getBody() {
		return body;
	}

	public void setBody( List<BoxStatement> body ) {
		replaceChildren( this.body, body );
		this.body = body;
		this.body.forEach( arg -> arg.setParent( this ) );
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "body", body.stream().map( BoxStatement::toMap ).collect( java.util.stream.Collectors.toList() ) );
		return map;
	}

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}
}
