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
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxNew;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;

public class BoxNewTransformer extends AbstractTransformer {

	public BoxNewTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	/**
	 * Transform a new expression
	 *
	 * @param node    a BoxNewOperation instance
	 * @param context transformation context
	 *
	 * @return Generates a throw
	 *
	 * @throws IllegalStateException
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxNew		boxNew	= ( BoxNew ) node;
		Expression	expr	= ( Expression ) transpiler.transform( boxNew.getExpression(), TransformerContext.RIGHT );

		String		fqn		= expr.toString();
		if ( expr instanceof NameExpr ) {
			fqn = fqn.startsWith( "\"" ) ? fqn : "\"" + fqn + "\"";
		}
		String				finalFqn	= fqn.replace( "java:", "" );
		Map<String, String>	values		= new HashMap<>() {

											{
												put( "expr", finalFqn );
												put( "contextName", transpiler.peekContextName() );
												put( "prefix", boxNew.getPrefix() == null ? "" : boxNew.getPrefix().getName() + ":" );

											}
										};
		for ( int i = 0; i < boxNew.getArguments().size(); i++ ) {
			Expression expr2 = ( Expression ) transpiler.transform( ( BoxNode ) boxNew.getArguments().get( i ), context );
			values.put( "arg" + i, expr2.toString() );
		}
		String	template	= "classLocator.load(context,\"${prefix}\".concat( StringCaster.cast(${expr})),imports).invokeConstructor( ${contextName}, "
		    + generateArguments( boxNew.getArguments() ) + " ).unWrapBoxLangClass()";
		Node	javaStmt	= parseExpression( template, values );
		// logger.trace( node.getSourceText() + " -> " + javaStmt );
		addIndex( javaStmt, node );
		return javaStmt;

	}
}
