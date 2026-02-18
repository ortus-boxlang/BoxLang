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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import ortus.boxlang.compiler.asmboxpiler.AsmTranspiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.BoxTemplate;
import ortus.boxlang.compiler.ast.statement.BoxAccessModifier;
import ortus.boxlang.compiler.ast.statement.BoxFunctionDeclaration;
import ortus.boxlang.compiler.ast.statement.BoxMethodDeclarationModifier;
import ortus.boxlang.compiler.ast.statement.BoxReturnType;
import ortus.boxlang.compiler.ast.statement.BoxScriptIsland;
import ortus.boxlang.compiler.ast.statement.BoxType;
import ortus.boxlang.compiler.ast.statement.component.BoxTemplateIsland;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.UDF;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * Transform a Function Declaration using the static method + method handle pattern.
 * Instead of generating a separate inner class per function, this generates:
 * 1. A static invoker method on the enclosing class
 * 2. An instantiation of UDF with a method reference to the static invoker
 */
public class BoxFunctionDeclarationTransformer extends AbstractTransformer {

	public BoxFunctionDeclarationTransformer( AsmTranspiler transpiler ) {
		super( transpiler );
	}

	// @formatter:on
	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxFunctionDeclaration	function	= ( BoxFunctionDeclaration ) node;
		TransformerContext		safe		= function.getName().equalsIgnoreCase( "isnull" ) ? TransformerContext.SAFE : context;

		if ( transpiler.hasCompiledFunction( function.getName() ) ) {
			if ( transpiler.getProperty( "sourceType" ).equals( BoxSourceType.BOXSCRIPT.name() )
			    || transpiler.getProperty( "sourceType" ).equals( BoxSourceType.BOXTEMPLATE.name() ) ) {
				throw new IllegalStateException( "Cannot define multiple functions with the same name: " + function.getName() );
			} else {
				ArrayList<AbstractInsnNode> blankResult = new ArrayList<>();

				if ( returnContext == ReturnValueContext.VALUE || returnContext == ReturnValueContext.VALUE_OR_NULL ) {
					blankResult.add( new InsnNode( Opcodes.ACONST_NULL ) );
				}

				return blankResult;
			}

		}

		BoxReturnType	boxReturnType	= function.getType();
		BoxType			returnType		= BoxType.Any;
		String			fqn				= null;
		if ( boxReturnType != null ) {
			returnType = boxReturnType.getType();
			if ( returnType.equals( BoxType.Fqn ) ) {
				fqn = boxReturnType.getFqn();
			}
		}
		String				returnTypeName		= returnType.equals( BoxType.Fqn ) ? fqn : returnType.name();

		BoxAccessModifier	access				= function.getAccessModifier() == null ? BoxAccessModifier.Public : function.getAccessModifier();

		String				invokerMethodName	= "invokeFunction" + function.getName();

		Type				declaringType		= Type.getType( "L" + transpiler.getProperty( "packageName" ).replace( '.', '/' )
		    + "/" + transpiler.getProperty( "classname" )
		    + ";" );

		// Generate the static invoker method on the owning class
		ClassNode owningClass = transpiler.getOwningClass();

		transpiler.incrementfunctionBodyCounter();
		AsmHelper.methodWithContextAndClassLocator( owningClass, invokerMethodName, Type.getType( FunctionBoxContext.class ), Type.getType( Object.class ),
		    true,
		    transpiler, true,
		    () -> {

			    if ( function.getBody() == null ) {
				    return new ArrayList<AbstractInsnNode>();
			    }

			    return function.getBody()
			        .stream()
			        .flatMap( statement -> transpiler.transform( statement, safe, ReturnValueContext.EMPTY ).stream() )
			        .collect( Collectors.toList() );
		    } );
		transpiler.decrementfunctionBodyCounter();

		// Generate the UDF instantiation bytecode: new UDF(name, arguments, returnType, access, annotations, documentation, modifiers, defaultOutput,
		// imports, sourceType, path, ClassName::invokerMethodName)
		List<AbstractInsnNode> instantiation = new ArrayList<>();

		// NEW UDF
		instantiation.add( new TypeInsnNode( Opcodes.NEW, Type.getInternalName( UDF.class ) ) );
		instantiation.add( new InsnNode( Opcodes.DUP ) );

		// Arg 1: name (Key)
		instantiation.addAll( transpiler.createKey( function.getName() ) );

		// Arg 2: arguments (Argument[])
		instantiation.addAll(
		    AsmHelper.array( Type.getType( Argument.class ), function.getArgs(), ( arg, i ) -> transpiler.transform( arg, safe ) )
		);

		// Arg 3: returnType (String)
		instantiation.add( new LdcInsnNode( returnTypeName ) );

		// Arg 4: access (Function.Access)
		instantiation.add( new FieldInsnNode( Opcodes.GETSTATIC,
		    Type.getInternalName( Function.Access.class ),
		    access.name().toUpperCase(),
		    Type.getDescriptor( Function.Access.class ) ) );

		// Arg 5: annotations (IStruct)
		instantiation.addAll( transpiler.transformAnnotations( function.getAnnotations() ) );

		// Arg 6: documentation (IStruct)
		instantiation.addAll( transpiler.transformDocumentation( function.getDocumentation() ) );

		// Arg 7: modifiers (List)
		instantiation.addAll(
		    AsmHelper.array(
		        Type.getType( BoxMethodDeclarationModifier.class ),
		        function.getModifiers(),
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

		// Arg 8: defaultOutput (boolean)
		instantiation.add( new InsnNode( shouldDefaultOutput( function ) ? Opcodes.ICONST_1 : Opcodes.ICONST_0 ) );

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

		// INVOKESPECIAL UDF.<init>(Key, Argument[], String, Access, IStruct, IStruct, List, boolean, List, BoxSourceType, ResolvedFilePath, Function)V
		instantiation.add( new MethodInsnNode( Opcodes.INVOKESPECIAL,
		    Type.getInternalName( UDF.class ),
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

		// Store the UDF instantiation bytecode in the transpiler for later clinit population
		transpiler.getUDFInstantiations().put( Key.of( function.getName() ), instantiation );

		// Generate registration bytecode: context.registerUDF( udfs.get( Key.of("name") ) )
		// NOTE: Do NOT add loadCurrentContext() here. The registration nodes may be generated inside a
		// static component body method (e.g., componentBody_N) but consumed inside the non-static _invoke method,
		// which has a different context slot. The context loading is deferred to getUDFRegistrations().
		List<AbstractInsnNode> registrationNodes = new ArrayList<>();

		// udfs.get( Key.of("name") )
		registrationNodes.add( new FieldInsnNode( Opcodes.GETSTATIC,
		    declaringType.getInternalName(),
		    "udfs",
		    Type.getDescriptor( Map.class ) ) );
		registrationNodes.addAll( transpiler.createKey( function.getName() ) );
		registrationNodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
		    Type.getInternalName( Map.class ),
		    "get",
		    Type.getMethodDescriptor( Type.getType( Object.class ), Type.getType( Object.class ) ),
		    true ) );
		registrationNodes.add( new TypeInsnNode( Opcodes.CHECKCAST, Type.getInternalName( UDF.class ) ) );

		// context.registerUDF( udf )
		registrationNodes.add(
		    new MethodInsnNode( Opcodes.INVOKEINTERFACE,
		        Type.getInternalName( IBoxContext.class ),
		        "registerUDF",
		        Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( UDF.class ) ),
		        true )
		);

		if ( !function.getModifiers().contains( BoxMethodDeclarationModifier.STATIC )
		    && !function.getModifiers().contains( BoxMethodDeclarationModifier.ABSTRACT ) ) {
			transpiler.addUDFRegistration( function.getName(), registrationNodes );
		}

		if ( function.getModifiers().contains( BoxMethodDeclarationModifier.STATIC ) ) {
			// Static UDF registrations are returned inline (not deferred), so they need
			// context loading from the current (correct) tracker right here.
			List<AbstractInsnNode> staticResult = new ArrayList<>();
			staticResult.addAll( transpiler.getCurrentMethodContextTracker().get().loadCurrentContext() );
			staticResult.addAll( registrationNodes );
			return staticResult;
		}

		ArrayList<AbstractInsnNode> blankResult = new ArrayList<>();

		if ( returnContext == ReturnValueContext.VALUE || returnContext == ReturnValueContext.VALUE_OR_NULL ) {
			blankResult.add( new InsnNode( Opcodes.ACONST_NULL ) );
		}

		return blankResult;
	}

	private boolean shouldDefaultOutput( BoxFunctionDeclaration function ) {
		// If the closest ancestor is a script, then the default output is true
		@SuppressWarnings( "unchecked" )
		BoxNode	ancestor		= function.getFirstNodeOfTypes( BoxTemplate.class, BoxScript.class, BoxExpression.class, BoxScriptIsland.class,
		    BoxTemplateIsland.class );
		boolean	defaultOutput	= ancestor == null || !Set.of( BoxTemplate.class, BoxTemplateIsland.class ).contains( ancestor.getClass() );

		return defaultOutput;
	}
}
