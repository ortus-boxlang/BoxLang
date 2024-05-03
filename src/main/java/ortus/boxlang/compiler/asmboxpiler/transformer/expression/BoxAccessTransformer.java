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
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.*;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.scopes.Key;

import java.util.ArrayList;
import java.util.List;

public class BoxAccessTransformer extends AbstractTransformer {

	public BoxAccessTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxAccess				objectAccess	= ( BoxAccess ) node;

		List<AbstractInsnNode>	accessKey;
		// DotAccess just uses the string directly, array access allows any expression
		if ( objectAccess instanceof BoxDotAccess dotAccess ) {
			if ( dotAccess.getAccess() instanceof BoxIdentifier id ) {
				accessKey = transpiler.createKey( ( id ).getName() );
			} else if ( dotAccess.getAccess() instanceof BoxIntegerLiteral il ) {
				accessKey = transpiler.createKey( il );
			} else {
				throw new IllegalStateException( "Unsupported access type: " + dotAccess.getAccess().getClass().getName() );
			}
		} else {
			accessKey = transpiler.createKey( objectAccess.getAccess() );
		}

		// An access expression starting a scope can be optimized
		if ( objectAccess.getContext() instanceof BoxScope ) {
			// List<AbstractInsnNode> jContext = transpiler.transform( objectAccess.getContext() );
			// values.put( "scopeReference", jContext.toString() );
			//
			// String template = """
			// ${scopeReference}.dereference(
			// ${contextName},
			// ${accessKey},
			// ${safe}
			// )
			// """;
			// Node javaExpr = parseExpression( template, values );
			// logger.atTrace().log( node.getSourceText() + " -> " + javaExpr );
			// addIndex( javaExpr, node );
			// return javaExpr;
			throw new UnsupportedOperationException();

		} else {
			// BoxNode parent = ( BoxNode ) objectAccess.getParent();
			List<AbstractInsnNode> nodes = new ArrayList<>();
			nodes.add( new VarInsnNode( Opcodes.ALOAD, 1 ) );
			nodes.addAll( transpiler.transform( objectAccess.getContext(), TransformerContext.NONE ) );
			nodes.addAll( accessKey );
			nodes.add( new FieldInsnNode( Opcodes.GETSTATIC,
			    Type.getInternalName( Boolean.class ),
			    "FALSE",
			    Type.getDescriptor( Boolean.class ) ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( Referencer.class ),
			    "get",
			    Type.getMethodDescriptor( Type.getType( Object.class ),
			        Type.getType( IBoxContext.class ),
			        Type.getType( Object.class ),
			        Type.getType( Key.class ),
			        Type.getType( Boolean.class ) ),
			    false ) );
			BoxNode parent = objectAccess.getParent();
			if ( ! ( parent instanceof BoxAccess )
			    // I don't know if this will work, but I'm trying to make an exception for query columns being passed to array BIFs
			    // This prolly won't work if a query column is passed as a second param that isn't the array
			    && ! ( parent instanceof BoxArgument barg && barg.getParent() instanceof BoxFunctionInvocation bfun
			        && bfun.getName().toLowerCase().contains( "array" ) ) ) {
				nodes.add( 0, new VarInsnNode( Opcodes.ALOAD, 1 ) );
				nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
				    Type.getInternalName( IBoxContext.class ),
				    "unwrapQueryColumn",
				    Type.getMethodDescriptor( Type.getType( Object.class ), Type.getType( Object.class ) ),
				    true ) );
			}

			return nodes;
		}
	}

}
