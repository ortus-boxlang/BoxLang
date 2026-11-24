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

import java.util.Map;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * AST Node representing an import statement
 */
public class BoxImport extends BoxStatement {

	private BoxExpression	expression;
	private BoxIdentifier	alias;

	/**
	 * For a tag library import, &lt;cfimport taglib="/tags" prefix="app"&gt;: the directory and the prefix as written.
	 * Such an import has no expression; the tags written with its prefix carry the directory themselves.
	 */
	private String			taglib;
	private String			taglibPrefix;

	/**
	 * Creates the AST node
	 *
	 * @param expression argument expression to assert
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxImport( BoxExpression expression, BoxIdentifier alias, Position position, String sourceText ) {
		super( position, sourceText );
		setExpression( expression );
		setAlias( alias );
	}

	public BoxExpression getExpression() {
		return expression;
	}

	public BoxIdentifier getAlias() {
		return alias;
	}

	public void setExpression( BoxExpression expression ) {
		replaceChildren( this.expression, expression );
		this.expression = expression;
		if ( expression != null ) {
			this.expression.setParent( this );
		}
	}

	public void setAlias( BoxIdentifier alias ) {
		replaceChildren( this.alias, alias );
		this.alias = alias;
		if ( alias != null ) {
			this.alias.setParent( this );
		}
	}

	/**
	 * Mark this import as a tag library import
	 *
	 * @param taglib the tag library directory as written
	 * @param prefix the prefix as written
	 */
	public void setTaglib( String taglib, String prefix ) {
		this.taglib			= taglib;
		this.taglibPrefix	= prefix;
	}

	public String getTaglib() {
		return this.taglib;
	}

	public String getTaglibPrefix() {
		return this.taglibPrefix;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();
		if ( this.taglib != null ) {
			map.put( "taglib", this.taglib );
			map.put( "taglibPrefix", this.taglibPrefix );
		}
		if ( expression != null ) {
			map.put( "expression", expression.toMap() );
		} else {
			map.put( "expression", null );
		}
		if ( alias != null ) {
			map.put( "alias", alias.toMap() );
		} else {
			map.put( "alias", null );
		}
		return map;
	}

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}
}
