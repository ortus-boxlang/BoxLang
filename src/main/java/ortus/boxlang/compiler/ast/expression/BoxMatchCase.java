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
package ortus.boxlang.compiler.ast.expression;

import java.util.Map;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * A single match branch.
 */
public class BoxMatchCase extends BoxNode {

	private BoxMatchPattern	pattern;
	private BoxExpression	guard;
	private BoxExpression	body;

	public BoxMatchCase( BoxMatchPattern pattern, BoxExpression guard, BoxExpression body, Position position, String sourceText ) {
		super( position, sourceText );
		setPattern( pattern );
		setGuard( guard );
		setBody( body );
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

	public BoxExpression getGuard() {
		return this.guard;
	}

	public void setGuard( BoxExpression guard ) {
		replaceChildren( this.guard, guard );
		this.guard = guard;
		if ( this.guard != null ) {
			this.guard.setParent( this );
		}
	}

	public BoxExpression getBody() {
		return this.body;
	}

	public void setBody( BoxExpression body ) {
		replaceChildren( this.body, body );
		this.body = body;
		if ( this.body != null ) {
			this.body.setParent( this );
		}
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();
		map.put( "pattern", this.pattern.toMap() );
		map.put( "guard", this.guard != null ? this.guard.toMap() : null );
		map.put( "body", this.body.toMap() );
		return map;
	}

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}
}