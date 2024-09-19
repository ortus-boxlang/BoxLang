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
import java.util.Optional;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.statement.BoxArgumentDeclaration;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.DefaultExpression;
import ortus.boxlang.runtime.types.IStruct;

public class BoxArgumentDeclarationTransformer extends AbstractTransformer {

	public BoxArgumentDeclarationTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxArgumentDeclaration	boxArgument			= ( BoxArgumentDeclaration ) node;

		/* Process default value */
		List<AbstractInsnNode>	defaultLiteral		= List.of( new InsnNode( Opcodes.ACONST_NULL ) );
		List<AbstractInsnNode>	defaultExpression	= List.of( new InsnNode( Opcodes.ACONST_NULL ) );
		if ( boxArgument.getValue() != null ) {
			if ( boxArgument.getValue().isLiteral() ) {
				defaultLiteral = transpiler.transform( boxArgument.getValue(), TransformerContext.NONE );
			} else {
				defaultExpression = transpiler.transform( boxArgument.getValue(), TransformerContext.NONE, ReturnValueContext.VALUE );
			}
		}

		List<AbstractInsnNode> nodes = new ArrayList<>();
		nodes.add( new TypeInsnNode( Opcodes.NEW, Type.getInternalName( Argument.class ) ) );
		nodes.add( new InsnNode( Opcodes.DUP ) );
		nodes.add( new LdcInsnNode( boxArgument.getRequired() ? 1 : 0 ) );
		nodes.add( new LdcInsnNode( Optional.ofNullable( boxArgument.getType() ).orElse( "any" ) ) );
		nodes.addAll( transpiler.createKey( boxArgument.getName() ) );
		nodes.addAll( defaultLiteral );
		nodes.addAll( defaultExpression );
		nodes.addAll( transpiler.transformAnnotations( boxArgument.getAnnotations() ) );
		nodes.addAll( transpiler.transformDocumentation( boxArgument.getDocumentation() ) );
		nodes.add( new MethodInsnNode( Opcodes.INVOKESPECIAL,
		    Type.getInternalName( Argument.class ),
		    "<init>",
		    Type.getMethodDescriptor( Type.VOID_TYPE,
		        Type.BOOLEAN_TYPE,
		        Type.getType( String.class ),
		        Type.getType( Key.class ),
		        Type.getType( Object.class ),
		        Type.getType( DefaultExpression.class ),
		        Type.getType( IStruct.class ),
		        Type.getType( IStruct.class ) ),
		    false ) );
		return nodes;
	}
}
