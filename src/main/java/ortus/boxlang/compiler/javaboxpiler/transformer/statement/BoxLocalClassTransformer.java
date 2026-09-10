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

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.EmptyStmt;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxClosure;
import ortus.boxlang.compiler.ast.expression.BoxLambda;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxLocalClass;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Named local classes — whether in a script/template or as an inner class inside a BoxClass —
 * are pre-compiled before body transformation. The source declaration statement itself emits
 * no runtime Java statement.
 * If the class is nested inside a function body, a compile-time error is thrown since class
 * definitions inside functions are not supported.
 */
public class BoxLocalClassTransformer extends AbstractTransformer {

	public BoxLocalClassTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxLocalClass	localClass	= ( BoxLocalClass ) node;
		String			localName	= localClass.getName().getName();
		// This check is a failsafe. Our BoxVisitor also validates this at parse time.
		if ( localClass.getFirstNodeOfTypes( BoxFunctionDeclaration.class, BoxClosure.class, BoxLambda.class ) != null ) {
			throw new BoxRuntimeException(
			    "Class definitions are not allowed inside function bodies. Move class [" + localName + "] outside of any function body." );
		}
		return new EmptyStmt();
	}
}
