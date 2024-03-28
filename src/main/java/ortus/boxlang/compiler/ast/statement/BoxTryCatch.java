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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;

/**
 * AST Node representing a if statement
 */
public class BoxTryCatch extends BoxStatement {

	private BoxIdentifier		exception;
	private List<BoxStatement>	catchBody;
	private List<BoxExpression>	catchTypes;

	public BoxTryCatch( List<BoxExpression> catchTypes, BoxExpression exception, List<BoxStatement> catchBody, Position position, String sourceText ) {
		super( position, sourceText );
		setCatchTypes( catchTypes );
		setException( exception );
		setCatchBody( catchBody );
	}

	public List<BoxStatement> getCatchBody() {
		return catchBody;
	}

	public BoxIdentifier getException() {
		return exception;
	}

	public List<BoxExpression> getCatchTypes() {
		return this.catchTypes;
	}

	void setException( BoxExpression exception ) {
		if ( exception instanceof BoxIdentifier exp ) {
			replaceChildren( this.exception, exp );
			this.exception = exp;
			this.exception.setParent( this );
		} else {
			throw new IllegalStateException( "Exception must be a BoxIdentifier" );
		}
	}

	void setCatchBody( List<BoxStatement> catchBody ) {
		replaceChildren( this.catchBody, catchBody );
		this.catchBody = catchBody;
		this.catchBody.forEach( arg -> arg.setParent( this ) );
	}

	void setCatchTypes( List<BoxExpression> catchTypes ) {
		replaceChildren( this.catchTypes, catchTypes );
		this.catchTypes = catchTypes;
		this.catchTypes.forEach( arg -> arg.setParent( this ) );
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "exception", exception.toMap() );
		map.put( "catchBody", catchBody.stream().map( BoxStatement::toMap ).collect( Collectors.toList() ) );
		map.put( "catchTypes", catchTypes.stream().map( BoxExpression::toMap ).collect( Collectors.toList() ) );

		return map;
	}
}
