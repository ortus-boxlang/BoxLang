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
package ortus.boxlang.transpiler.transformer.statement;

import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.Node;

import ortus.boxlang.ast.BoxBufferOutput;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.transpiler.Transpiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

/**
 * Transform a String Interpolation the equivalent Java Parser AST nodes
 */
public class BoxBufferOutputTransformer extends AbstractTransformer {

	public BoxBufferOutputTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	/**
	 * Transform a String interpolation expression
	 *
	 * @param node    a BoxStringInterpolation
	 * @param context transformation context
	 *
	 * @return a Java Parser Expression
	 *
	 * @throws IllegalStateException
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxBufferOutput		bufferOuput	= ( BoxBufferOutput ) node;
		Node				javaExpr;

		String				expr		= "${contextName}.writeToBuffer( ${expression} );";

		Map<String, String>	values		= new HashMap<>() {

											{
												put( "contextName", transpiler.peekContextName() );
												put( "expression", transpiler.transform( bufferOuput.getExpression(), TransformerContext.NONE ).toString() );
											}
										};
		javaExpr = parseStatement( expr, values );

		logger.info( "{} -> {}", node.getSourceText(), javaExpr );
		addIndex( javaExpr, node );
		return javaExpr;
	}
}
