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

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.ast.Position;
import ortus.boxlang.ast.expression.BoxIdentifier;

import java.util.Collections;
import java.util.List;

/**
 * AST Node representing a if statement
 */
public class BoxTryCatch extends BoxStatement {

	private final BoxIdentifier			exception;
	private final List<BoxStatement>	catchBody;
	private final BoxTryCatchType		type;
	private final String				name;

	/**
	 *
	 * @param exception
	 * @param catchBody
	 * @param position
	 * @param sourceText
	 */
	public BoxTryCatch( BoxTryCatchType type, String name, BoxExpr exception, List<BoxStatement> catchBody, Position position, String sourceText ) {
		super( position, sourceText );
		this.type	= type;
		this.name	= name;
		if ( exception instanceof BoxIdentifier exp ) {
			this.exception = exp;
		} else {
			throw new IllegalStateException( "Exception must be a BoxIdentifier" );
		}
		this.exception.setParent( this );
		this.catchBody = Collections.unmodifiableList( catchBody );
		this.catchBody.forEach( arg -> arg.setParent( this ) );

	}

	public List<BoxStatement> getCatchBody() {
		return catchBody;
	}

	public BoxIdentifier getException() {
		return exception;
	}

	public BoxTryCatchType getType() {
		return type;
	}

	public String getName() {
		return name;
	}
}
