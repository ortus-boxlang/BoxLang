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
import ortus.boxlang.compiler.ast.expression.BoxMatchPattern;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;

public class BoxRefutableDestructuringDeclaration extends BoxStatement {

	private BoxMatchPattern	pattern;
	private BoxExpression	subject;
	private BoxStatement	elseBody;
	private boolean			finalDeclaration;

	public BoxRefutableDestructuringDeclaration( BoxMatchPattern pattern, BoxExpression subject, BoxStatement elseBody, boolean finalDeclaration,
	    Position position, String sourceText ) {
		super( position, sourceText );
		setPattern( pattern );
		setSubject( subject );
		setElseBody( elseBody );
		setFinalDeclaration( finalDeclaration );
	}

	public BoxMatchPattern getPattern() {
		return this.pattern;
	}

	public void setPattern( BoxMatchPattern pattern ) {
		replaceChildren( this.pattern, pattern );
		this.pattern = pattern;
		if ( this.pattern != null ) {
			this.pattern.setParent( this );
		}
	}

	public BoxExpression getSubject() {
		return this.subject;
	}

	public void setSubject( BoxExpression subject ) {
		replaceChildren( this.subject, subject );
		this.subject = subject;
		if ( this.subject != null ) {
			this.subject.setParent( this );
		}
	}

	public BoxStatement getElseBody() {
		return this.elseBody;
	}

	public void setElseBody( BoxStatement elseBody ) {
		replaceChildren( this.elseBody, elseBody );
		this.elseBody = elseBody;
		if ( this.elseBody != null ) {
			this.elseBody.setParent( this );
		}
	}

	public boolean isFinalDeclaration() {
		return this.finalDeclaration;
	}

	public boolean isLocalDeclaration() {
		return !this.finalDeclaration;
	}

	public void setFinalDeclaration( boolean finalDeclaration ) {
		this.finalDeclaration = finalDeclaration;
	}

	public void validatePatternHasBindingTarget() {
		if ( this.pattern.hasBindingTarget() ) {
			return;
		}

		throw new ExpressionException(
		    "Refutable destructuring declarations must contain at least one binding target.",
		    this.pattern );
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();
		map.put( "pattern", this.pattern.toMap() );
		map.put( "subject", this.subject.toMap() );
		map.put( "elseBody", this.elseBody.toMap() );
		map.put( "finalDeclaration", this.finalDeclaration );
		return map;
	}

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}
}