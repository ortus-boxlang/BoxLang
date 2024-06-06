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
package ortus.boxlang.compiler.asmboxpiler.transformer.expression;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxComparisonOperation;
import ortus.boxlang.compiler.ast.expression.BoxComparisonOperator;
import ortus.boxlang.runtime.operators.*;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;

import java.util.ArrayList;
import java.util.List;

public class BoxComparisonOperationTransformer extends AbstractTransformer {

	public BoxComparisonOperationTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxComparisonOperation	operation	= ( BoxComparisonOperation ) node;
		List<AbstractInsnNode>	left		= transpiler.transform( operation.getLeft(), TransformerContext.NONE );
		List<AbstractInsnNode>	right		= transpiler.transform( operation.getRight(), TransformerContext.NONE );

		Class<?>				dispatcher;
		if ( operation.getOperator() == BoxComparisonOperator.Equal ) {
			dispatcher = EqualsEquals.class;
		} else if ( operation.getOperator() == BoxComparisonOperator.NotEqual ) {
			dispatcher = NotEqualsEquals.class;
		} else if ( operation.getOperator() == BoxComparisonOperator.TEqual ) {
			dispatcher = EqualsEqualsEquals.class;
		} else if ( operation.getOperator() == BoxComparisonOperator.GreaterThan ) {
			dispatcher = GreaterThan.class;
		} else if ( operation.getOperator() == BoxComparisonOperator.GreaterThanEquals ) {
			dispatcher = GreaterThanEqual.class;
		} else if ( operation.getOperator() == BoxComparisonOperator.LessThan ) {
			dispatcher = LessThan.class;
		} else if ( operation.getOperator() == BoxComparisonOperator.LesslThanEqual ) {
			dispatcher = LessThanEqual.class;
		} else {
			throw new ExpressionException( "not implemented", operation );
		}

		List<AbstractInsnNode> nodes = new ArrayList<>();
		nodes.addAll( left );
		nodes.addAll( right );
		nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
		    Type.getInternalName( dispatcher ),
		    "invoke",
		    Type.getMethodDescriptor( Type.getType( Boolean.class ), Type.getType( Object.class ), Type.getType( Object.class ) ),
		    false ) );
		return nodes;
	}

}
