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

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * AST node representing a match expression.
 */
public class BoxMatchExpression extends BoxExpression {

	private BoxExpression		subject;
	private List<BoxMatchCase>	cases;

	public BoxMatchExpression( BoxExpression subject, List<BoxMatchCase> cases, Position position, String sourceText ) {
		super( position, sourceText );
		setSubject( subject );
		setCases( cases );
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

	public List<BoxMatchCase> getCases() {
		return this.cases;
	}

	public void setCases( List<BoxMatchCase> cases ) {
		replaceChildren( this.cases, cases );
		this.cases = cases;
		this.cases.forEach( matchCase -> matchCase.setParent( this ) );
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();
		map.put( "subject", this.subject.toMap() );
		map.put( "cases", this.cases.stream().map( BoxMatchCase::toMap ).collect( Collectors.toList() ) );
		return map;
	}

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}
}