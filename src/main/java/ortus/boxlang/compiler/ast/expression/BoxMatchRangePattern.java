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

public class BoxMatchRangePattern extends BoxMatchPattern {

	private BoxExpression	from;
	private BoxExpression	to;

	public BoxMatchRangePattern( BoxExpression from, BoxExpression to, Position position, String sourceText ) {
		super( position, sourceText );
		setFrom( from );
		setTo( to );
	}

	public BoxExpression getFrom() {
		return this.from;
	}

	public void setFrom( BoxExpression from ) {
		replaceChildren( this.from, from );
		this.from = from;
		if ( this.from != null ) {
			this.from.setParent( this );
		}
	}

	public BoxExpression getTo() {
		return this.to;
	}

	public void setTo( BoxExpression to ) {
		replaceChildren( this.to, to );
		this.to = to;
		if ( this.to != null ) {
			this.to.setParent( this );
		}
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();
		map.put( "from", this.from.toMap() );
		map.put( "to", this.to.toMap() );
		return map;
	}

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}
}