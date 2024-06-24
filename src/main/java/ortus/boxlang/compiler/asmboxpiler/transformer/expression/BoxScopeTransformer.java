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
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxScope;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.BaseScope;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.RequestScope;
import ortus.boxlang.runtime.scopes.ServerScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;

public class BoxScopeTransformer extends AbstractTransformer {

	public BoxScopeTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxScope					boxScope	= ( BoxScope ) node;

		List<AbstractInsnNode>		nodes		= new ArrayList<>();
		Class<? extends BaseScope>	scopeClass	= null;

		if ( "variables".equalsIgnoreCase( boxScope.getName() ) ) {
			scopeClass = VariablesScope.class;
		} else if ( "request".equalsIgnoreCase( boxScope.getName() ) ) {
			scopeClass = RequestScope.class;
		} else if ( "server".equalsIgnoreCase( boxScope.getName() ) ) {
			scopeClass = ServerScope.class;
		} else {
			throw new ExpressionException( "Scope transformation not implemented: " + boxScope.getName(), boxScope );
		}

		return List.of(
		    new VarInsnNode( Opcodes.ALOAD, 1 ),
		    new FieldInsnNode(
		        Opcodes.GETSTATIC,
		        Type.getInternalName( scopeClass ),
		        "name",
		        Type.getDescriptor( Key.class )
		    ),
		    new MethodInsnNode(
		        Opcodes.INVOKEINTERFACE,
		        Type.getInternalName( IBoxContext.class ),
		        "getScopeNearby",
		        Type.getMethodDescriptor( Type.getType( IScope.class ), Type.getType( Key.class ) ),
		        true
		    )
		);
	}

}
