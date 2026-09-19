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
package ortus.boxlang.compiler.javaboxpiler.transformer.statement;

import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxImport;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.Transpiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;
import ortus.boxlang.runtime.loader.ClassLocator;

public class BoxImportTransformer extends AbstractTransformer {

	public BoxImportTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	/**
	 * Transform an import statement
	 *
	 * @param node    a BoxImport instance
	 * @param context transformation context
	 *
	 * @return Generates an entry in the list of import
	 *
	 * @throws IllegalStateException
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxImport boxImport = ( BoxImport ) node;
		// Work around for now so tag lib imports don't blow up
		if ( boxImport.getExpression() == null ) {
			return new EmptyStmt();
		}
		// An import's target is always a plain dotted name (a BoxFQN) in practice - read its raw
		// text directly rather than going through generic transform() dispatch, which (as of
		// BoxFQNTransformer) now returns a proper, already-quoted Java string-literal expression
		// for a BoxFQN - correct for consumers that embed it as a real sub-expression (e.g.
		// "instanceof"/"as"), but wrong here, where the result is substituted, unquoted, into this
		// method's OWN "\"${namespace}\"" template (double-quoting it otherwise). Any other
		// expression shape (not expected in practice for an import) still falls back to the
		// generic dispatch path.
		String				namespaceText	= boxImport.getExpression() instanceof ortus.boxlang.compiler.ast.expression.BoxFQN fqn
		    ? fqn.getValue()
		    : ( ( Expression ) transpiler.transform( boxImport.getExpression(), TransformerContext.RIGHT ) ).toString();
		String				alias			= boxImport.getAlias() != null
		    ? " as " + boxImport.getAlias().getName()
		    : "";

		Map<String, String>	values			= new HashMap<>() {

												{
													put( "namespace", namespaceText + alias );

												}
											};
		String				template		= "ImportDefinition.parse( \"${namespace}\" )";

		Expression			javaStmt		= parseExpression( template, values );
		// logger.trace( node.getSourceText() + " -> " + javaStmt );
		addIndex( javaStmt, node );
		transpiler.addImport( namespaceText + alias );
		transpiler.addJImport( javaStmt );
		// We have to return something based on how these transformers are setup, so we just return an empty statement.
		return new EmptyStmt();
	}

	/**
	 * Helper method to transform an inner class import.
	 * 
	 * @param name       the name of the import as it appears in the source code
	 * @param className  the java class name
	 * @param transpiler the transpiler instance to add the import to
	 */
	@SuppressWarnings( "null" )
	public static void transformInnerClassImport( String name, String className, Transpiler transpiler ) {
		// This registers it with the transpiler so we can map identifiers directly referencing our inner class such as MyInnerClass.staticMember
		transpiler.addImport( name );

		Expression javaStmt = new MethodCallExpr(
		    new NameExpr( "ImportDefinition" ),
		    "fromClassRef",
		    new NodeList<>(
		        new StringLiteralExpr( ClassLocator.BX_PREFIX ),
		        new StringLiteralExpr( name ),
		        new ClassExpr( new ClassOrInterfaceType( null, className ) )
		    )
		);
		// This is the bytecode to create the import definition with the class reference.
		transpiler.addJImport( javaStmt );
	}
}
