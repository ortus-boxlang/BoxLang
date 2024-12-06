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
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;

public class BoxIdentifierTransformer extends AbstractTransformer {

	public BoxIdentifierTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxIdentifier			identifier	= ( BoxIdentifier ) node;

		List<AbstractInsnNode>	nodes		= new ArrayList<>();

		if ( transpiler.matchesImport( identifier.getName() ) && transpiler.getProperty( "sourceType" ).toLowerCase().startsWith( "box" ) ) {
			// If id is an imported class name, load the class directly instead of searching scopes for it
			nodes.addAll( AsmHelper.loadClass( transpiler, identifier ) );
		} else {
			transpiler.getCurrentMethodContextTracker().ifPresent( ( t ) -> nodes.addAll( t.loadCurrentContext() ) );
			nodes.addAll( transpiler.createKey( identifier.getName() ) );
			if ( context == TransformerContext.SAFE ) {
				transpiler.getCurrentMethodContextTracker().ifPresent( ( t ) -> nodes.addAll( t.loadCurrentContext() ) );
				nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
				    Type.getInternalName( IBoxContext.class ),
				    "getDefaultAssignmentScope",
				    Type.getMethodDescriptor( Type.getType( IScope.class ) ),
				    true ) );
			} else {
				nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
			}
			nodes.add( new LdcInsnNode( true ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
			    Type.getInternalName( IBoxContext.class ),
			    "scopeFindNearby",
			    Type.getMethodDescriptor( Type.getType( IBoxContext.ScopeSearchResult.class ), Type.getType( Key.class ), Type.getType( IScope.class ),
			        Type.BOOLEAN_TYPE ),
			    true ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKEVIRTUAL,
			    Type.getInternalName( IBoxContext.ScopeSearchResult.class ),
			    "value",
			    Type.getMethodDescriptor( Type.getType( Object.class ) ),
			    false ) );
		}

		if ( returnContext.empty ) {
			nodes.add( new InsnNode( Opcodes.POP ) );
		}
		return AsmHelper.addLineNumberLabels( nodes, node );
	}
}
