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
 * A struct literal comes in two forms, ordered and unordered (default).
 * The unordered struct uses curly braces `{}` like a JSON object.
 * The ordered struct, uses square brackets `[]` like an array literal.
 * The difference is structs use a comma-delimited list of key/value pairs.
 * Note, key/value pairs ANYWHERE in Boxlang can either be specified as `
 * foo=bar` OR `foo : bar`. This goes for strut literals, function parameters,
 * or class/UDF metadata.
 */
public class BoxStructLiteral extends BoxExpression {

	private BoxStructType		type;
	private List<BoxExpression>	values;

	/**
	 * Creates the AST node for Struct Literals
	 *
	 * @param values     initialization values
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxStructLiteral( BoxStructType type, List<BoxExpression> values, Position position, String sourceText ) {
		super( position, sourceText );
		setType( type );
		setValues( values );
	}

	public List<BoxExpression> getValues() {
		return values;
	}

	public BoxStructType getType() {
		return type;
	}

	@Override
	public boolean isLiteral() {
		return true;
	}

	public void setValues( List<BoxExpression> values ) {
		replaceChildren( this.values, values );
		this.values = values;
		this.values.forEach( arg -> arg.setParent( this ) );
	}

	public void setType( BoxStructType type ) {
		this.type = type;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "type", enumToMap( type ) );
		map.put( "values", values.stream().map( BoxExpression::toMap ).collect( Collectors.toList() ) );
		return map;
	}

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}
}
