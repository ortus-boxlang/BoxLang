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

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxNew;
import ortus.boxlang.compiler.ast.expression.BoxStringInterpolation;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;

public class BoxNewTransformer extends AbstractTransformer {

	public BoxNewTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxNew					boxNew	= ( BoxNew ) node;

		List<AbstractInsnNode>	nodes	= new ArrayList<>();
		// nodes.add( new VarInsnNode( Opcodes.ALOAD, 2 ) );
		nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
		    Type.getInternalName( ClassLocator.class ),
		    "getInstance",
		    Type.getMethodDescriptor( Type.getType( ClassLocator.class ) ),
		    false ) );

		nodes.addAll( transpiler.getCurrentMethodContextTracker().get().loadCurrentContext() );

		// Extract the class name at compile time
		String	prefix	= boxNew.getPrefix() == null ? "" : boxNew.getPrefix().getName() + ":";
		String	fqn;
		if ( boxNew.getExpression() instanceof BoxStringLiteral bsl ) {
			fqn = bsl.getValue();
			nodes.add( new LdcInsnNode( prefix + fqn.replace( "java:", "" ) ) );
		} else if ( boxNew.getExpression() instanceof BoxFQN bFqn ) {
			fqn = bFqn.getValue();
			nodes.add( new LdcInsnNode( prefix + fqn.replace( "java:", "" ) ) );
		} else if ( boxNew.getExpression() instanceof BoxStringInterpolation bsi ) {
			nodes.addAll( transpiler.transform( bsi, TransformerContext.NONE, ReturnValueContext.VALUE ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( StringCaster.class ),
			    "cast",
			    Type.getMethodDescriptor( Type.getType( String.class ), Type.getType( Object.class ) ),
			    false ) );
		} else {
			throw new ExpressionException( "BoxNew expression must be a string literal or FQN, but was a " + boxNew.getExpression().getClass().getSimpleName(),
			    boxNew.getExpression() );
		}

		nodes.add( new FieldInsnNode( Opcodes.GETSTATIC,
		    transpiler.getProperty( "packageName" ).replace( '.', '/' )
		        + "/"
		        + transpiler.getProperty( "classname" ),
		    "imports",
		    Type.getDescriptor( List.class ) ) );

		nodes.add( new MethodInsnNode( Opcodes.INVOKEVIRTUAL,
		    Type.getInternalName( ClassLocator.class ),
		    "load",
		    Type.getMethodDescriptor( Type.getType( DynamicObject.class ),
		        Type.getType( IBoxContext.class ),
		        Type.getType( String.class ),
		        Type.getType( List.class ) ),
		    false ) );

		nodes.addAll( transpiler.getCurrentMethodContextTracker().get().loadCurrentContext() );

		nodes.addAll( AsmHelper.callDynamicObjectInvokeConstructor( transpiler, boxNew.getArguments(), context ) );

		nodes.add( new MethodInsnNode( Opcodes.INVOKEVIRTUAL,
		    Type.getInternalName( DynamicObject.class ),
		    "unWrapBoxLangClass",
		    Type.getMethodDescriptor( Type.getType( Object.class ) ),
		    false ) );

		if ( returnContext.empty ) {
			nodes.add( new InsnNode( Opcodes.POP ) );
		}

		return AsmHelper.addLineNumberLabels( nodes, node );
	}
}
