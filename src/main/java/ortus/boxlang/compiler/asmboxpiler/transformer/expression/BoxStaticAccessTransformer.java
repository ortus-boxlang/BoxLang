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

package ortus.boxlang.compiler.asmboxpiler.transformer.expression;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStaticAccess;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.BoxClassSupport;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;

public class BoxStaticAccessTransformer extends AbstractTransformer {

	public BoxStaticAccessTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxStaticAccess			objectAccess	= ( BoxStaticAccess ) node;
		List<AbstractInsnNode>	nodes			= new ArrayList<>();
		Boolean					safe			= objectAccess.isSafe() || context == TransformerContext.SAFE;

		transpiler.getCurrentMethodContextTracker().ifPresent( t -> nodes.addAll( t.loadCurrentContext() ) );

		// does this go here or above?

		// get the imports for the current class
		transpiler.getCurrentMethodContextTracker().ifPresent( t -> nodes.addAll( t.loadCurrentContext() ) );
		// nodes.add( new VarInsnNode( Opcodes.ALOAD, 0 ) );
		// push scope reference onto the stack
		if ( objectAccess.getContext() instanceof BoxFQN fqn ) {
			nodes.add( new LdcInsnNode( fqn.getValue() ) );
		} else if ( objectAccess.getContext() instanceof BoxIdentifier id ) {
			if ( transpiler.matchesImport( id.getName() ) && transpiler.getProperty( "sourceType" ).toLowerCase().startsWith( "box" ) ) {
				nodes.addAll( transpiler.transform( id, context, ReturnValueContext.VALUE ) );
			} else {
				nodes.add( new LdcInsnNode( id.getName() ) );
			}
		} else {
			throw new ExpressionException( "Unexpected base token in static access.", objectAccess.getContext() );
		}
		nodes.add( new FieldInsnNode( Opcodes.GETSTATIC, transpiler.getProperty( "classTypeInternal" ), "imports", Type.getDescriptor( List.class ) ) );

		// invoke BoxClassSupport.ensureClass(${contextName},${scopeReference},imports),
		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( BoxClassSupport.class ),
		    "ensureClass",
		    Type.getMethodDescriptor( Type.getType( DynamicObject.class ),
		        Type.getType( IBoxContext.class ),
		        Type.getType( Object.class ),
		        Type.getType( List.class )
		    ),
		    false )
		);

		// Node accessKey;
		// objectAccess just uses the string directly, array access allows any expression
		if ( objectAccess.getAccess() instanceof BoxIdentifier id ) {
			nodes.addAll( transpiler.createKey( id.getName() ) );
		} else if ( objectAccess.getAccess() instanceof BoxIntegerLiteral il ) {
			nodes.addAll( transpiler.createKey( il ) );
		} else {
			throw new ExpressionException( "Unsupported access type: " + objectAccess.getAccess().getClass().getName(), objectAccess.getAccess() );
		}

		// Expression jContext;

		// // "scope" here isn't a BoxLang proper scope, it's just whatever Java source represents the context of the access expression
		// values.put( "scopeReference", jContext.toString() );

		// String template = """
		// Referencer.get(
		// ${contextName},
		// BoxClassSupport.ensureClass(${contextName},${scopeReference},imports),
		// ${accessKey},
		// ${safe}
		// )
		// """;

		nodes.add( new FieldInsnNode( Opcodes.GETSTATIC, Type.getInternalName( Boolean.class ), safe ? "TRUE" : "FALSE",
		    Type.getDescriptor( Boolean.class ) ) );

		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESTATIC,
		    Type.getInternalName( Referencer.class ),
		    "get",
		    Type.getMethodDescriptor( Type.getType( Object.class ),
		        Type.getType( IBoxContext.class ),
		        Type.getType( Object.class ),
		        Type.getType( Key.class ),
		        Type.getType( Boolean.class )
		    ),
		    false )
		);

		// Node javaExpr = parseExpression( template, values );
		// logger.trace( node.getSourceText() + " -> " + javaExpr );
		// addIndex( javaExpr, node );
		return nodes;
	}

}
