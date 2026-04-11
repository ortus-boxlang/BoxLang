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

import ortus.boxlang.compiler.IBoxpiler;
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
import ortus.boxlang.compiler.ast.expression.BoxClosure;
import ortus.boxlang.compiler.ast.expression.BoxLambda;
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
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.LocalScope;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Closure;
import ortus.boxlang.runtime.types.ClosureDefinition;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.UDF;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * Transform a Function Declaration using the static method + method handle pattern.
 * Supports both top-level UDF declarations (hoisted) and nested function declarations
 * inside other functions/closures (compiled as closures and assigned to the local scope).
 */
public class BoxFunctionDeclarationTransformer extends AbstractTransformer {

	public BoxFunctionDeclarationTransformer( AsmTranspiler transpiler ) {
		super( transpiler );
	}

	// @formatter:on
	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxFunctionDeclaration function = ( BoxFunctionDeclaration ) node;

		if ( isNestedFunction( function ) ) {
			return transformNestedFunction( function, context, returnContext );
		}

		return transformTopLevelFunction( function, context, returnContext );
	}

	/**
	 * Detect if this function declaration is nested inside another function, closure, or lambda body.
	 */
	private boolean isNestedFunction( BoxFunctionDeclaration function ) {
		return function.getFirstAncestorOfType( BoxFunctionDeclaration.class ) != null
		    || function.getFirstAncestorOfType( BoxClosure.class ) != null
		    || function.getFirstAncestorOfType( BoxLambda.class ) != null;
	}

	/**
	 * Transform a nested function declaration as a named closure assigned to the parent's local scope.
	 * Generates a ClosureDefinition (stored in the static closures list) and inline bytecode that
	 * calls newInstance(context) and puts the resulting Closure into the local scope.
	 */
	private List<AbstractInsnNode> transformNestedFunction( BoxFunctionDeclaration function, TransformerContext context,
	    ReturnValueContext returnContext ) {
		TransformerContext	safe			= function.getName().equalsIgnoreCase( "isnull" ) ? TransformerContext.SAFE : context;

		BoxReturnType		boxReturnType	= function.getType();
		BoxType				returnType		= BoxType.Any;
		String				fqn				= null;
		if ( boxReturnType != null ) {
			returnType = boxReturnType.getType();
			if ( returnType.equals( BoxType.Fqn ) ) {
				fqn = boxReturnType.getFqn();
			}
		}
		String				returnTypeName	= returnType.equals( BoxType.Fqn ) ? fqn : returnType.name();
		BoxAccessModifier	access			= function.getAccessModifier() == null ? BoxAccessModifier.Public : function.getAccessModifier();

		int					closureIndex	= transpiler.getClosureInstantiations().size();
		transpiler.getClosureInstantiations().add( null );
		String		invokerMethodName	= "invokeClosure_" + closureIndex;

		Type		declaringType		= Type.getType( "L" + transpiler.getProperty( "packageName" ).replace( '.', '/' )
		    + "/" + transpiler.getProperty( "classname" )
		    + ";" );

		ClassNode	owningClass			= transpiler.getOwningClass();

		// Generate the static invoker method for the nested function body
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

		// Generate the ClosureDefinition instantiation bytecode
		List<AbstractInsnNode> instantiation = new ArrayList<>();

		instantiation.add( new TypeInsnNode( Opcodes.NEW, Type.getInternalName( ClosureDefinition.class ) ) );
		instantiation.add( new InsnNode( Opcodes.DUP ) );

		// Arg 1: name (Key) - use the function's actual name
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

		// Arg 9: imports (List)
		instantiation.add( new FieldInsnNode( Opcodes.GETSTATIC,
		    declaringType.getInternalName(), "imports", Type.getDescriptor( List.class ) ) );

		// Arg 10: sourceType (BoxSourceType)
		instantiation.add( new FieldInsnNode( Opcodes.GETSTATIC,
		    declaringType.getInternalName(), "sourceType", Type.getDescriptor( BoxSourceType.class ) ) );

		// Arg 11: path (ResolvedFilePath)
		instantiation.add( new FieldInsnNode( Opcodes.GETSTATIC,
		    declaringType.getInternalName(), "path", Type.getDescriptor( ResolvedFilePath.class ) ) );

		// Arg 12: method reference
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

		// INVOKESPECIAL ClosureDefinition.<init>(...)
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

		transpiler.getClosureInstantiations().set( closureIndex, instantiation );

		// Generate inline bytecode: context.getScopeNearby(LocalScope.name, false).put(Key.of("name"), closures.get(index).newInstance(context))
		List<AbstractInsnNode> nodes = new ArrayList<>();

		// context.getScopeNearby(LocalScope.name, false)
		nodes.addAll( transpiler.getCurrentMethodContextTracker().get().loadCurrentContext() );
		nodes.add( new FieldInsnNode( Opcodes.GETSTATIC,
		    Type.getInternalName( LocalScope.class ),
		    "name",
		    Type.getDescriptor( Key.class ) ) );
		nodes.add( new InsnNode( Opcodes.ICONST_0 ) );
		nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
		    Type.getInternalName( IBoxContext.class ),
		    "getScopeNearby",
		    Type.getMethodDescriptor( Type.getType( IScope.class ), Type.getType( Key.class ), Type.BOOLEAN_TYPE ),
		    true ) );

		// .put(Key.of("name"), closures.get(index).newInstance(context))
		// Stack: [IScope]
		nodes.addAll( transpiler.createKey( function.getName() ) );
		// Stack: [IScope, Key]

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

		// Stack: [IScope, Key, Closure]
		// IScope.put(Key, Object) -> returns Object
		nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
		    Type.getInternalName( IScope.class ),
		    "put",
		    Type.getMethodDescriptor( Type.getType( Object.class ), Type.getType( Object.class ), Type.getType( Object.class ) ),
		    true ) );

		// put() returns the previous value; discard it unless a value is expected
		if ( returnContext != ReturnValueContext.VALUE && returnContext != ReturnValueContext.VALUE_OR_NULL ) {
			nodes.add( new InsnNode( Opcodes.POP ) );
		}

		return nodes;
	}

	/**
	 * Transform a top-level function declaration as a hoisted UDF registration (original behavior).
	 */
	private List<AbstractInsnNode> transformTopLevelFunction( BoxFunctionDeclaration function, TransformerContext context,
	    ReturnValueContext returnContext ) {
		TransformerContext safe = function.getName().equalsIgnoreCase( "isnull" ) ? TransformerContext.SAFE : context;

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

		String				invokerMethodName	= IBoxpiler.INVOKE_FUNCTION_PREFIX + IBoxpiler.sanitizeForJavaIdentifier( function.getName() );

		Type				declaringType		= Type.getType( "L" + transpiler.getProperty( "packageName" ).replace( '.', '/' )
		    + "/" + transpiler.getProperty( "classname" )
		    + ";" );

		ClassNode			owningClass			= transpiler.getOwningClass();

		transpiler.markFunctionCompiled( function.getName() );
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

		List<AbstractInsnNode> instantiation = new ArrayList<>();

		instantiation.add( new TypeInsnNode( Opcodes.NEW, Type.getInternalName( UDF.class ) ) );
		instantiation.add( new InsnNode( Opcodes.DUP ) );

		instantiation.addAll( transpiler.createKey( function.getName() ) );

		instantiation.addAll(
		    AsmHelper.array( Type.getType( Argument.class ), function.getArgs(), ( arg, i ) -> transpiler.transform( arg, safe ) )
		);

		instantiation.add( new LdcInsnNode( returnTypeName ) );

		instantiation.add( new FieldInsnNode( Opcodes.GETSTATIC,
		    Type.getInternalName( Function.Access.class ),
		    access.name().toUpperCase(),
		    Type.getDescriptor( Function.Access.class ) ) );

		instantiation.addAll( transpiler.transformAnnotations( function.getAnnotations() ) );

		instantiation.addAll( transpiler.transformDocumentation( function.getDocumentation() ) );

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

		instantiation.add( new InsnNode( shouldDefaultOutput( function ) ? Opcodes.ICONST_1 : Opcodes.ICONST_0 ) );

		instantiation.add( new FieldInsnNode( Opcodes.GETSTATIC,
		    declaringType.getInternalName(), "imports", Type.getDescriptor( List.class ) ) );

		instantiation.add( new FieldInsnNode( Opcodes.GETSTATIC,
		    declaringType.getInternalName(), "sourceType", Type.getDescriptor( BoxSourceType.class ) ) );

		instantiation.add( new FieldInsnNode( Opcodes.GETSTATIC,
		    declaringType.getInternalName(), "path", Type.getDescriptor( ResolvedFilePath.class ) ) );

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

		transpiler.getUDFInstantiations().put( Key.of( function.getName() ), instantiation );

		List<AbstractInsnNode> registrationNodes = new ArrayList<>();

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
		@SuppressWarnings( "unchecked" )
		BoxNode	ancestor		= function.getFirstNodeOfTypes( BoxTemplate.class, BoxScript.class, BoxExpression.class, BoxScriptIsland.class,
		    BoxTemplateIsland.class );
		boolean	defaultOutput	= ancestor == null || !Set.of( BoxTemplate.class, BoxTemplateIsland.class ).contains( ancestor.getClass() );

		return defaultOutput;
	}
}
