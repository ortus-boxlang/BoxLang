/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.compiler.asmboxpiler.transformer.statement;

import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;

import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxClosure;
import ortus.boxlang.compiler.ast.expression.BoxLambda;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxLocalClass;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Named local classes — whether in a script/template or as an inner class inside a BoxClass —
 * are pre-compiled before body transformation. The source declaration statement itself emits
 * no runtime instructions.
 * If the class is nested inside a function body, a compile-time error is thrown since class
 * definitions inside functions are not supported.
 */
public class BoxLocalClassTransformer extends AbstractTransformer {

	public BoxLocalClassTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxLocalClass	localClass	= ( BoxLocalClass ) node;
		String			localName	= localClass.getName().getName();
		// This check is a failsafe. Our BoxVisitor also validates this at parse time.
		if ( localClass.getFirstNodeOfTypes( BoxFunctionDeclaration.class, BoxClosure.class, BoxLambda.class ) != null ) {
			throw new BoxRuntimeException(
			    "Class definitions are not allowed inside function bodies. Move class [" + localName + "] outside of any function body." );
		}
		// Local class was already pre-compiled. Emit no instructions for the statement itself.
		// However, callers with a non-EMPTY return context expect something on the stack (the
		// contract of VALUE_OR_NULL). Push null so that callers like transformBodyExpressionsFromScript
		// can safely pop it and replace it with their own null return value.
		if ( returnContext != ReturnValueContext.EMPTY && returnContext != ReturnValueContext.EMPTY_UNLESS_JUMPING ) {
			return List.of( new InsnNode( Opcodes.ACONST_NULL ) );
		}
		return List.of();
	}

}
