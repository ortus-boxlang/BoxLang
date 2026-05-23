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
 * AST Node representing a Set literal.
 *
 * <p>
 * Syntax forms:
 * 
 * <pre>
 * set{ 1, 2, 3 }              // default (hash) variant
 * set&lt;linked&gt;{ "a", "b" }     // LinkedHashSet — preserves insertion order
 * set&lt;sorted&gt;{ 9, 1, 5 }      // TreeSet — natural ordering
 * </pre>
 */
public class BoxSetLiteral extends BoxExpression implements IBoxLiteral {

	private List<BoxExpression>	values;

	/**
	 * Optional backing-storage variant: null for default, otherwise one of
	 * {@code "linked"} / {@code "ordered"} / {@code "sorted"} / {@code "tree"} / {@code "hash"}.
	 */
	private final String		variant;

	public BoxSetLiteral( String variant, List<BoxExpression> values, Position position, String sourceText ) {
		super( position, sourceText );
		this.variant = variant;
		setValues( values );
	}

	public List<BoxExpression> getValues() {
		return values;
	}

	public String getVariant() {
		return variant;
	}

	public void setValues( List<BoxExpression> values ) {
		replaceChildren( this.values, values );
		this.values = values;
		this.values.forEach( arg -> arg.setParent( this ) );
	}

	@Override
	public boolean isLiteral() {
		return values.stream().allMatch( BoxExpression::isLiteral );
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();
		map.put( "variant", variant );
		map.put( "values", values.stream().map( BoxExpression::toMap ).collect( Collectors.toList() ) );
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
		return "a set literal";
	}
}
