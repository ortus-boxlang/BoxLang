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
package ortus.boxlang.transpiler.transformer.expression;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxNewOperation;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

public class BoxNewOperationTransformer extends AbstractTransformer {

	public BoxNewOperationTransformer( JavaTranspiler transpiler ) {
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
		BoxNewOperation	boxNew	= ( BoxNewOperation ) node;
		Expression		expr	= ( Expression ) transpiler.transform( boxNew.getExpression(), TransformerContext.RIGHT );

		String			args	= boxNew.getArguments().stream()
		    .map( it -> transpiler.transform( it ).toString() )
		    .collect( Collectors.joining( ", " ) );

		String			fqn		= expr.toString();
		if ( expr instanceof NameExpr ) {
			fqn = fqn.startsWith( "\"" ) ? fqn : "\"" + fqn + "\"";
		}
		String				finalFqn	= fqn.replace( "java:", "" );
		Map<String, String>	values		= new HashMap<>() {

											{
												put( "expr", finalFqn );
												put( "args", args );
												put( "contextName", transpiler.peekContextName() );

											}
										};
		String				template	= """
		                                  classLocator.load(	context,
		                                                            		StringCaster.cast(${expr}),
		                                                            	  imports
		                                                            	).invokeConstructor( ${contextName}, new Object[] { ${args} } ).unWrapBoxLangClass()
		                                                            """;
		Node				javaStmt	= parseExpression( template, values );
		logger.info( node.getSourceText() + " -> " + javaStmt );
		addIndex( javaStmt, node );
		return javaStmt;

	}
}
