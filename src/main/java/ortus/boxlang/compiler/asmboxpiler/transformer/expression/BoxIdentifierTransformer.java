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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;

import java.util.ArrayList;
import java.util.List;

public class BoxIdentifierTransformer extends AbstractTransformer {

	public BoxIdentifierTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxIdentifier			identifier	= ( BoxIdentifier ) node;

		List<AbstractInsnNode>	nodes		= new ArrayList<>();

		if ( transpiler.matchesImport( identifier.getName() ) && transpiler.getProperty( "sourceType" ).toLowerCase().startsWith( "box" ) ) {
			nodes.add( new VarInsnNode( Opcodes.ALOAD, 2 ) );
			nodes.add( new VarInsnNode( Opcodes.ALOAD, 1 ) );
			nodes.add( new LdcInsnNode( identifier.getName() ) );
			nodes.add( new FieldInsnNode( Opcodes.GETSTATIC,
			    transpiler.getProperty( "packageName" ).replace( '.', '/' )
			        + "/"
			        + transpiler.getProperty( "classname" ),
			    "imports",
			    Type.getDescriptor( List.class ) ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKEVIRTUAL,
			    Type.getInternalName( ClassLocator.class ),
			    "load",
			    Type.getMethodDescriptor( Type.getType( DynamicObject.class ), Type.getType( IBoxContext.class ), Type.getType( String.class ),
			        Type.getType( List.class ) ),
			    false ) );
		} else {
			nodes.add( new VarInsnNode( Opcodes.ALOAD, 1 ) );
			nodes.addAll( transpiler.createKey( identifier.getName() ) );
			if ( context == TransformerContext.SAFE ) {
				nodes.add( new VarInsnNode( Opcodes.ALOAD, 1 ) );
				nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
				    Type.getInternalName( IBoxContext.class ),
				    "getDefaultAssignmentScope",
				    Type.getMethodDescriptor( Type.getType( IScope.class ) ),
				    true ) );
			} else {
				nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
			}
			nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
			    Type.getInternalName( IBoxContext.class ),
			    "scopeFindNearby",
			    Type.getMethodDescriptor( Type.getType( IBoxContext.ScopeSearchResult.class ), Type.getType( Key.class ), Type.getType( IScope.class ) ),
			    true ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKEVIRTUAL,
			    Type.getInternalName( IBoxContext.ScopeSearchResult.class ),
			    "value",
			    Type.getMethodDescriptor( Type.getType( Object.class ) ),
			    false ) );
		}
		return nodes;
	}
}
