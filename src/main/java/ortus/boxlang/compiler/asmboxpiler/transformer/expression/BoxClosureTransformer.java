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

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxClosure;
import ortus.boxlang.compiler.ast.statement.BoxExpressionStatement;
import ortus.boxlang.compiler.ast.statement.BoxMethodDeclarationModifier;
import ortus.boxlang.compiler.ast.statement.BoxStatementBlock;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Closure;
import ortus.boxlang.runtime.types.ClosureDefinition;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * Transform a Closure using the static method + method handle pattern.
 * Instead of generating a separate inner class, this generates:
 * 1. A static invoker method on the enclosing class
 * 2. An instantiation of ClosureDefinition with a method reference to the static invoker
 * 3. At runtime, closures.get(index).newInstance(context) creates a Closure wrapping the declaring context
 */
public class BoxClosureTransformer extends AbstractTransformer {

	public BoxClosureTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxClosure	boxClosure		= ( BoxClosure ) node;

		// Reserve a slot in the closureInstantiations list to prevent nested closures from getting the same index
		int			closureIndex	= transpiler.getClosureInstantiations().size();
		transpiler.getClosureInstantiations().add( null );

		String		invokerMethodName	= "invokeClosure_" + closureIndex;

		Type		declaringType		= Type.getType( "L" + transpiler.getProperty( "packageName" ).replace( '.', '/' )
		    + "/" + transpiler.getProperty( "classname" )
		    + ";" );

		// Generate the static invoker method on the owning class
		ClassNode	owningClass			= transpiler.getOwningClass();

		boolean		isBlock				= boxClosure.getBody() instanceof BoxStatementBlock;

		int			componentCounter	= transpiler.getComponentCounter();
		transpiler.setComponentCounter( 0 );
		transpiler.incrementfunctionBodyCounter();

		ReturnValueContext closureReturnContext = isBlock ? ReturnValueContext.EMPTY : ReturnValueContext.VALUE_OR_NULL;
		AsmHelper.methodWithContextAndClassLocator( owningClass, invokerMethodName, Type.getType( FunctionBoxContext.class ), Type.getType( Object.class ),
		    true,
		    transpiler, isBlock,
		    () -> {
			    List<AbstractInsnNode> bodyNodes = new ArrayList<AbstractInsnNode>();

			    BoxNode				body		= boxClosure.getBody();

			    if ( body instanceof BoxExpressionStatement boxExpr ) {
				    bodyNodes.addAll( transpiler.transform( boxExpr.getExpression(), TransformerContext.NONE, closureReturnContext ) );
			    } else {
				    bodyNodes.addAll( transpiler.transform( body, TransformerContext.NONE, closureReturnContext ) );
			    }

			    if ( isBlock ) {
				    bodyNodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
			    }

			    return bodyNodes;
		    } );
		transpiler.decrementfunctionBodyCounter();
		transpiler.setComponentCounter( componentCounter );

		// Generate the ClosureDefinition instantiation bytecode
		List<AbstractInsnNode> instantiation = new ArrayList<>();

		// NEW ClosureDefinition
		instantiation.add( new TypeInsnNode( Opcodes.NEW, Type.getInternalName( ClosureDefinition.class ) ) );
		instantiation.add( new InsnNode( Opcodes.DUP ) );

		// Arg 1: name (Key) - Closure.defaultName
		instantiation.add( new FieldInsnNode( Opcodes.GETSTATIC,
		    Type.getInternalName( Closure.class ),
		    "defaultName",
		    Type.getDescriptor( Key.class ) ) );

		// Arg 2: arguments (Argument[])
		instantiation.addAll(
		    AsmHelper.array(
		        Type.getType( Argument.class ),
		        boxClosure.getArgs().stream().map( decl -> transpiler.transform( decl, TransformerContext.NONE, ReturnValueContext.VALUE ) ).toList()
		    )
		);

		// Arg 3: returnType (String) - "any"
		instantiation.add( new LdcInsnNode( "any" ) );

		// Arg 4: access (Function.Access) - PUBLIC
		instantiation.add( new FieldInsnNode( Opcodes.GETSTATIC,
		    Type.getInternalName( Function.Access.class ),
		    "PUBLIC",
		    Type.getDescriptor( Function.Access.class ) ) );

		// Arg 5: annotations (IStruct)
		instantiation.addAll( transpiler.transformAnnotations( boxClosure.getAnnotations() ) );

		// Arg 6: documentation (IStruct) - Struct.EMPTY
		instantiation.add( new FieldInsnNode( Opcodes.GETSTATIC,
		    Type.getInternalName( Struct.class ),
		    "EMPTY",
		    Type.getDescriptor( IStruct.class ) ) );

		// Arg 7: modifiers (List) - empty list
		instantiation.addAll(
		    AsmHelper.array(
		        Type.getType( BoxMethodDeclarationModifier.class ),
		        List.<BoxMethodDeclarationModifier> of(),
		        ( bmdm, i ) -> List.of(
		            new FieldInsnNode(
		                Opcodes.GETSTATIC,
		                Type.getInternalName( BoxMethodDeclarationModifier.class ),
		                bmdm.toString().toUpperCase(),
		                Type.getDescriptor( BoxMethodDeclarationModifier.class )
		            )
		        )
		    )
		);
		instantiation.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
		    Type.getInternalName( List.class ),
		    "of",
		    Type.getMethodDescriptor( Type.getType( List.class ), Type.getType( Object[].class ) ),
		    true ) );

		// Arg 8: defaultOutput (boolean) - true
		instantiation.add( new InsnNode( Opcodes.ICONST_1 ) );

		// Arg 9: imports (List) - from enclosing class static field
		instantiation.add( new FieldInsnNode( Opcodes.GETSTATIC,
		    declaringType.getInternalName(),
		    "imports",
		    Type.getDescriptor( List.class ) ) );

		// Arg 10: sourceType (BoxSourceType) - from enclosing class static field
		instantiation.add( new FieldInsnNode( Opcodes.GETSTATIC,
		    declaringType.getInternalName(),
		    "sourceType",
		    Type.getDescriptor( BoxSourceType.class ) ) );

		// Arg 11: path (ResolvedFilePath) - from enclosing class static field
		instantiation.add( new FieldInsnNode( Opcodes.GETSTATIC,
		    declaringType.getInternalName(),
		    "path",
		    Type.getDescriptor( ResolvedFilePath.class ) ) );

		// Arg 12: method reference - ClassName::invokerMethodName as Function<FunctionBoxContext, Object>
		instantiation.add( new InvokeDynamicInsnNode(
		    "apply",
		    "()Ljava/util/function/Function;",
		    new Handle(
		        Opcodes.H_INVOKESTATIC,
		        "java/lang/invoke/LambdaMetafactory",
		        "metafactory",
		        "(Ljava/lang/invoke/MethodHandles$Lookup;"
		            + "Ljava/lang/String;"
		            + "Ljava/lang/invoke/MethodType;"
		            + "Ljava/lang/invoke/MethodType;"
		            + "Ljava/lang/invoke/MethodHandle;"
		            + "Ljava/lang/invoke/MethodType;)"
		            + "Ljava/lang/invoke/CallSite;",
		        false
		    ),
		    Type.getMethodType( "(Ljava/lang/Object;)Ljava/lang/Object;" ),
		    new Handle(
		        Opcodes.H_INVOKESTATIC,
		        declaringType.getInternalName(),
		        invokerMethodName,
		        "(Lortus/boxlang/runtime/context/FunctionBoxContext;)Ljava/lang/Object;",
		        false
		    ),
		    Type.getMethodType( "(Lortus/boxlang/runtime/context/FunctionBoxContext;)Ljava/lang/Object;" )
		) );

		// INVOKESPECIAL ClosureDefinition.<init>(Key, Argument[], String, Access, IStruct, IStruct, List, boolean, List, BoxSourceType, ResolvedFilePath,
		// Function)V
		instantiation.add( new MethodInsnNode( Opcodes.INVOKESPECIAL,
		    Type.getInternalName( ClosureDefinition.class ),
		    "<init>",
		    Type.getMethodDescriptor(
		        Type.VOID_TYPE,
		        Type.getType( Key.class ),
		        Type.getType( Argument[].class ),
		        Type.getType( String.class ),
		        Type.getType( Function.Access.class ),
		        Type.getType( IStruct.class ),
		        Type.getType( IStruct.class ),
		        Type.getType( List.class ),
		        Type.BOOLEAN_TYPE,
		        Type.getType( List.class ),
		        Type.getType( BoxSourceType.class ),
		        Type.getType( ResolvedFilePath.class ),
		        Type.getType( java.util.function.Function.class )
		    ),
		    false ) );

		// Store the ClosureDefinition instantiation bytecode in the reserved slot
		transpiler.getClosureInstantiations().set( closureIndex, instantiation );

		// Return: closures.get(closureIndex).newInstance(context)
		List<AbstractInsnNode> nodes = new ArrayList<>();

		// closures.get(closureIndex)
		nodes.add( new FieldInsnNode( Opcodes.GETSTATIC,
		    declaringType.getInternalName(),
		    "closures",
		    Type.getDescriptor( List.class ) ) );
		nodes.add( new LdcInsnNode( closureIndex ) );
		nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
		    Type.getInternalName( List.class ),
		    "get",
		    Type.getMethodDescriptor( Type.getType( Object.class ), Type.INT_TYPE ),
		    true ) );
		nodes.add( new TypeInsnNode( Opcodes.CHECKCAST, Type.getInternalName( ClosureDefinition.class ) ) );

		// .newInstance(context)
		nodes.addAll( transpiler.getCurrentMethodContextTracker().get().loadCurrentContext() );
		nodes.add( new MethodInsnNode( Opcodes.INVOKEVIRTUAL,
		    Type.getInternalName( ClosureDefinition.class ),
		    "newInstance",
		    Type.getMethodDescriptor( Type.getType( Closure.class ), Type.getType( IBoxContext.class ) ),
		    false ) );

		return AsmHelper.addLineNumberLabels( nodes, node );
	}
}
