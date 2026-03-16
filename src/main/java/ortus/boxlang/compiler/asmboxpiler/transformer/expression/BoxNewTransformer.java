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
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxNew;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.ClassLocator;

public class BoxNewTransformer extends AbstractTransformer {

	public BoxNewTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxNew					boxNew			= ( BoxNew ) node;

		List<AbstractInsnNode>	nodes			= new ArrayList<>();

		// Fast path: if the target is a simple identifier that names a local class in this script/template,
		// emit a direct class reference (LDC + DynamicObject.of) instead of going through ClassLocator.
		// This avoids classloader-hierarchy problems because the local class is already defined in the
		// compile-time DiskClassLoader, not the request-time DynamicClassLoader.
		//
		// The grammar rule for `new` uses `fqn` internally, so `new Counter()` produces a BoxFQN("Counter"),
		// not a BoxIdentifier. We therefore check for both: a bare BoxIdentifier, AND a single-segment
		// BoxFQN (one with no dots, meaning there is no package qualifier).
		String					localClassAlias	= null;
		if ( boxNew.getExpression() instanceof BoxIdentifier identifier ) {
			localClassAlias = identifier.getName();
		} else if ( boxNew.getExpression() instanceof BoxFQN fqn && !fqn.getValue().contains( "." ) ) {
			// Single-segment FQN like "Counter" — no package qualifier, treat as a simple name
			localClassAlias = fqn.getValue();
		}
		if ( localClassAlias != null ) {
			String localClassJvmName = transpiler.getLocalClassJvmName( localClassAlias );
			if ( localClassJvmName != null ) {
				// Push Class<?> literal: equivalent to MyScript$LocalClass$Foo.class
				nodes.add( new LdcInsnNode( Type.getType( "L" + localClassJvmName + ";" ) ) );
				// Push IBoxContext
				nodes.addAll( transpiler.getCurrentMethodContextTracker().get().loadCurrentContext() );
				// DynamicObject.of(Class<?>, IBoxContext) → DynamicObject
				nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
				    Type.getInternalName( DynamicObject.class ),
				    "of",
				    Type.getMethodDescriptor( Type.getType( DynamicObject.class ),
				        Type.getType( Class.class ),
				        Type.getType( IBoxContext.class ) ),
				    false ) );
				// Push context + args, call invokeConstructor → DynamicObject
				nodes.addAll( transpiler.getCurrentMethodContextTracker().get().loadCurrentContext() );
				nodes.addAll( AsmHelper.callDynamicObjectInvokeConstructor( transpiler, boxNew.getArguments(), context ) );
				// Unwrap to the BoxLang class instance
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

		// Default path: resolve class at runtime using ClassLocator (imports + Java / BoxLang class loading)
		// nodes.add( new VarInsnNode( Opcodes.ALOAD, 2 ) );
		nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
		    Type.getInternalName( ClassLocator.class ),
		    "getInstance",
		    Type.getMethodDescriptor( Type.getType( ClassLocator.class ) ),
		    false ) );

		nodes.addAll( transpiler.getCurrentMethodContextTracker().get().loadCurrentContext() );
		nodes.add( new LdcInsnNode( "" ) ); // TODO: how to set this?
		nodes.addAll( transpiler.transform( boxNew.getExpression(), TransformerContext.NONE, ReturnValueContext.VALUE ) );
		nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
		    Type.getInternalName( StringCaster.class ),
		    "cast",
		    Type.getMethodDescriptor( Type.getType( String.class ), Type.getType( Object.class ) ),
		    false ) );
		nodes.add( new MethodInsnNode( Opcodes.INVOKEVIRTUAL,
		    Type.getInternalName( String.class ),
		    "concat",
		    Type.getMethodDescriptor( Type.getType( String.class ), Type.getType( String.class ) ),
		    false ) );
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
