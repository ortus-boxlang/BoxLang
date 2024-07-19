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
package ortus.boxlang.compiler.javaboxpiler.transformer.expression;

import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.Node;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxFunctionalBIFAccess;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;

public class BoxFunctionalBIFAccessTransformer extends AbstractTransformer {

	public BoxFunctionalBIFAccessTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	/**
	 * Transform a BoxFunctionalBIFAccess expression
	 *
	 * @param node    a BoxFunctionalBIFAccess instance
	 * @param context transformation context
	 *
	 * @return Generates a throw
	 *
	 * @throws IllegalStateException
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxFunctionalBIFAccess	boxFunctionalBIFAccess	= ( BoxFunctionalBIFAccess ) node;
		String					name					= boxFunctionalBIFAccess.getName();

		Map<String, String>		values					= new HashMap<>() {

															{
																put( "name", createKey( name ).toString() );
																put( "contextName", transpiler.peekContextName() );

															}
														};

		String					template				= "FunctionalBIFAccess.of( ${name} )";
		Node					javaStmt				= parseExpression( template, values );
		// logger.trace( node.getSourceText() + " -> " + javaStmt );
		addIndex( javaStmt, node );
		return javaStmt;

	}
}
