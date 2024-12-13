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
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxFunctionalMemberAccess;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.FunctionalMemberAccess;
import ortus.boxlang.runtime.types.FunctionalMemberAccessArgs;
import ortus.boxlang.runtime.types.util.MapHelper;

public class BoxFunctionalMemberAccessTransformer extends AbstractTransformer {

	public BoxFunctionalMemberAccessTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxFunctionalMemberAccess	memberAccess	= ( BoxFunctionalMemberAccess ) node;

		List<AbstractInsnNode>		nodes			= new ArrayList<>();

		if ( memberAccess.getArguments() == null || memberAccess.getArguments().isEmpty() ) {
			nodes.addAll( transpiler.createKey( memberAccess.getName() ) );

			nodes.add( new MethodInsnNode(
			    Opcodes.INVOKESTATIC,
			    Type.getInternalName( FunctionalMemberAccess.class ),
			    "of",
			    Type.getMethodDescriptor( Type.getType( FunctionalMemberAccess.class ),
			        Type.getType( Key.class ) ),
			    false ) );

			return AsmHelper.addLineNumberLabels( nodes, node );
		}
		nodes.add( new TypeInsnNode( Opcodes.NEW, Type.getInternalName( FunctionalMemberAccessArgs.class ) ) );
		nodes.add( new InsnNode( Opcodes.DUP ) );

		nodes.addAll( transpiler.createKey( memberAccess.getName() ) );

		nodes.addAll( transpiler.getCurrentMethodContextTracker().get().loadCurrentContext() );

		boolean usesNamedArguments = memberAccess.getArguments().get( 0 ).getName() != null;

		if ( usesNamedArguments ) {
			nodes.addAll( generateNamedArgumentLambda( memberAccess ) );
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
		} else {
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
			nodes.addAll( generatePositionalArgumentLambda( memberAccess ) );
		}

		nodes.add( new MethodInsnNode(
		    Opcodes.INVOKESPECIAL,
		    Type.getInternalName( FunctionalMemberAccessArgs.class ),
		    "<init>",
		    Type.getMethodDescriptor(
		        Type.VOID_TYPE,
		        Type.getType( Key.class ),
		        Type.getType( IBoxContext.class ),
		        Type.getType( Function.class ),
		        Type.getType( Function.class )
		    ),
		    false ) );

		return AsmHelper.addLineNumberLabels( nodes, node );
	}

	private List<AbstractInsnNode> generateNamedArgumentLambda( BoxFunctionalMemberAccess memberAccess ) {
		return generateArgumentProducerLambda( memberAccess, () -> {
			List<AbstractInsnNode>			nodes		= new ArrayList<>();

			List<List<AbstractInsnNode>>	argNodes	= memberAccess.getArguments().stream()
			    .map( arg -> {

															    return List.of(
															        transpiler.createKey( arg.getName() ),
															        transpiler.transform( arg, TransformerContext.NONE, ReturnValueContext.VALUE )
															    );
														    } )
			    .flatMap( l -> l.stream() )
			    .collect( Collectors.toList() );

			nodes.addAll( AsmHelper.array( Type.getType( Object.class ), argNodes ) );

			nodes.add(
			    new MethodInsnNode( Opcodes.INVOKESTATIC,
			        Type.getInternalName( MapHelper.class ),
			        "LinkedHashMapOfAny",
			        Type.getMethodDescriptor( Type.getType( Map.class ), Type.getType( Object[].class ) ),
			        false )
			);

			return nodes;
		} );
	}

	private List<AbstractInsnNode> generatePositionalArgumentLambda( BoxFunctionalMemberAccess memberAccess ) {
		return generateArgumentProducerLambda( memberAccess, () -> {
			return AsmHelper.array( Type.getType( Object.class ), memberAccess.getArguments(),
			    ( argument, i ) -> transpiler.transform( memberAccess.getArguments().get( i ), TransformerContext.NONE, ReturnValueContext.VALUE ) );
		} );
	}

	private List<AbstractInsnNode> generateArgumentProducerLambda( BoxExpression body, Supplier<List<AbstractInsnNode>> nodeSupplier ) {
		Type		type		= Type.getType( "L" + transpiler.getProperty( "packageName" ).replace( '.', '/' )
		    + "/" + transpiler.getProperty( "classname" )
		    + "$Lambda_" + transpiler.incrementAndGetLambdaCounter() + ";" );

		ClassNode	classNode	= new ClassNode();

		classNode.visit(
		    Opcodes.V21,
		    Opcodes.ACC_PUBLIC,
		    type.getInternalName(),
		    null,
		    Type.getInternalName( Object.class ),
		    new String[] { Type.getInternalName( Function.class ) } );

		MethodVisitor initVisitor = classNode.visitMethod( Opcodes.ACC_PUBLIC,
		    "<init>",
		    Type.getMethodDescriptor( Type.VOID_TYPE ),
		    null,
		    null );
		initVisitor.visitCode();
		initVisitor.visitVarInsn( Opcodes.ALOAD, 0 );
		initVisitor.visitMethodInsn( Opcodes.INVOKESPECIAL,
		    Type.getInternalName( Object.class ),
		    "<init>",
		    Type.getMethodDescriptor( Type.VOID_TYPE ),
		    false );
		initVisitor.visitInsn( Opcodes.RETURN );
		initVisitor.visitEnd();

		// MethodContextTracker t = new MethodContextTracker( false );
		// transpiler.addMethodContextTracker( t );
		// Object evaluate( IBoxContext context );
		MethodVisitor methodVisitor = classNode.visitMethod(
		    Opcodes.ACC_PUBLIC,
		    "apply",
		    Type.getMethodDescriptor( Type.getType( Object.class ), Type.getType( Object.class ) ),
		    null,
		    null );
		methodVisitor.visitCode();

		// t.trackNewContext();

		nodeSupplier.get().forEach( n -> n.accept( methodVisitor ) );

		methodVisitor.visitInsn( Opcodes.ARETURN );
		methodVisitor.visitMaxs( 0, 0 );
		methodVisitor.visitEnd();

		// transpiler.popMethodContextTracker();

		transpiler.setAuxiliary( type.getClassName(), classNode );

		List<AbstractInsnNode> nodes = new ArrayList<AbstractInsnNode>();

		nodes.add( new TypeInsnNode( Opcodes.NEW, type.getInternalName() ) );
		nodes.add( new InsnNode( Opcodes.DUP ) );
		nodes.add( new MethodInsnNode( Opcodes.INVOKESPECIAL,
		    type.getInternalName(),
		    "<init>",
		    Type.getMethodDescriptor( Type.VOID_TYPE ),
		    false ) );
		return nodes;
	}
}