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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * A constructor-style match pattern such as Some( value ).
 */
public class BoxMatchConstructorPattern extends BoxMatchPattern {

	private BoxIdentifier			label;
	private List<BoxMatchPattern>	patterns;

	public BoxMatchConstructorPattern( BoxIdentifier label, List<BoxMatchPattern> patterns, Position position, String sourceText ) {
		super( position, sourceText );
		setLabel( label );
		setPatterns( patterns );
	}

	public BoxIdentifier getLabel() {
		return this.label;
	}

	public void setLabel( BoxIdentifier label ) {
		replaceChildren( this.label, label );
		this.label = label;
		if ( this.label != null ) {
			this.label.setParent( this );
		}
	}

	public List<BoxMatchPattern> getPatterns() {
		return this.patterns;
	}

	public void setPatterns( List<BoxMatchPattern> patterns ) {
		replaceChildren( this.patterns, patterns );
		this.patterns = patterns;
		this.patterns.forEach( pattern -> pattern.setParent( this ) );
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();
		map.put( "label", this.label.toMap() );
		map.put( "patterns", this.patterns.stream().map( BoxMatchPattern::toMap ).collect( Collectors.toList() ) );
		return map;
	}

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}
}