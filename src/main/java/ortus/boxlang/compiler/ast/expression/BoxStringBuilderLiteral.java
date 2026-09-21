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
import java.util.Objects;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * AST Node representing a {@code sb{"..."}} / {@code sb{'...'}} literal.
 *
 * <p>
 * Syntax form:
 *
 * <pre>
 * sb{""}                      // empty StringBuilder
 * sb{"Hello World"}           // seeded StringBuilder
 * sb{"Hello #name#"}          // interpolation supported
 * </pre>
 *
 * <p>
 * The {@code initialValue} child expression is either a {@link BoxStringLiteral} (no interpolation)
 * or a {@link BoxStringInterpolation} (with {@code #...#} expressions).
 * Code generators wrap the evaluated string in a {@link ortus.boxlang.runtime.types.BoxStringBuilder}.
 */
public class BoxStringBuilderLiteral extends BoxExpression implements IBoxLiteral {

	/** The initial string value expression (literal or interpolation). */
	private BoxExpression initialValue;

	public BoxStringBuilderLiteral( BoxExpression initialValue, Position position, String sourceText ) {
		super( position, sourceText );
		setInitialValue( initialValue );
	}

	public BoxExpression getInitialValue() {
		return this.initialValue;
	}

	public void setInitialValue( BoxExpression initialValue ) {
		BoxExpression nonNullInitialValue = Objects.requireNonNull( initialValue, "initialValue cannot be null" );
		replaceChildren( this.initialValue, nonNullInitialValue );
		this.initialValue = nonNullInitialValue;
		this.initialValue.setParent( this );
	}

	@Override
	public boolean isLiteral() {
		return this.initialValue.isLiteral();
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();
		map.put( "initialValue", this.initialValue.toMap() );
		return map;
	}

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}

	@Override
	public String getDescription() {
		return "StringBuilder literal";
	}

}
