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

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxScope;
import ortus.boxlang.compiler.ast.expression.BoxStructLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStructType;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

import java.util.ArrayList;
import java.util.List;

public class BoxStructLiteralTransformer extends AbstractTransformer {

	public BoxStructLiteralTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context ) {
		BoxStructLiteral	structLiteral	= ( BoxStructLiteral ) node;
		boolean				empty			= structLiteral.getValues().isEmpty();

		if ( structLiteral.getType() == BoxStructType.Unordered ) {
			if ( empty ) {
				return List.of(
				    new TypeInsnNode( Opcodes.NEW, Type.getInternalName( Struct.class ) ),
				    new InsnNode( Opcodes.DUP ),
				    new MethodInsnNode( Opcodes.INVOKESPECIAL,
				        Type.getInternalName( Struct.class ),
				        "<init>",
				        Type.getMethodDescriptor( Type.VOID_TYPE ),
				        false )
				);
			}

			List<AbstractInsnNode> nodes = new ArrayList<>();

			nodes.addAll( AsmHelper.array( Type.getType( Object.class ), structLiteral.getValues(), ( value, i ) -> {
				if ( value instanceof BoxIdentifier && i % 2 != 1 ) {
					// { foo : "bar" }
					return List.of( new LdcInsnNode( value.getSourceText() ) );
				} else if ( value instanceof BoxScope && i % 2 != 1 ) {
					// { this : "bar" }
					return List.of( new LdcInsnNode( value.getSourceText() ) );
				} else {
					// { "foo" : "bar" }
					return transpiler.transform( value, context );
				}
			} ) );

			nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( Struct.class ),
			    "of",
			    Type.getMethodDescriptor( Type.getType( IStruct.class ), Type.getType( Object[].class ) ),
			    false ) );
			return nodes;
		} else {
			if ( empty ) {
				return List.of(
				    new TypeInsnNode( Opcodes.NEW, Type.getInternalName( Struct.class ) ),
				    new InsnNode( Opcodes.DUP ),
				    new FieldInsnNode( Opcodes.GETSTATIC,
				        Type.getInternalName( IStruct.TYPES.class ),
				        "LINKED",
				        Type.getDescriptor( IStruct.TYPES.class ) ),
				    new MethodInsnNode( Opcodes.INVOKESPECIAL,
				        Type.getInternalName( Struct.class ),
				        "<init>",
				        Type.getMethodDescriptor( Type.VOID_TYPE, Type.getType( IStruct.TYPES.class ) ),
				        false )
				);
			}
			List<AbstractInsnNode> nodes = new ArrayList<>();
			nodes.add( new LdcInsnNode( structLiteral.getValues() ) );
			nodes.add( new TypeInsnNode( Opcodes.ANEWARRAY, Type.getInternalName( Object.class ) ) );
			int i = 1;
			for ( BoxExpression expr : structLiteral.getValues() ) {
				nodes.add( new InsnNode( Opcodes.DUP ) );
				nodes.add( new LdcInsnNode( i - 1 ) );
				if ( expr instanceof BoxIdentifier && i % 2 != 0 ) {
					// { foo : "bar" }
					nodes.add( new LdcInsnNode( expr.getSourceText() ) );
				} else if ( expr instanceof BoxScope && i % 2 != 0 ) {
					// { this : "bar" }
					nodes.add( new LdcInsnNode( expr.getSourceText() ) );
				} else {
					// { "foo" : "bar" }
					nodes.addAll( transpiler.transform( expr, context ) );
				}
				nodes.add( new InsnNode( Opcodes.AASTORE ) );
				i++;
			}
			nodes.add( new MethodInsnNode( Opcodes.INVOKESPECIAL,
			    Type.getInternalName( Struct.class ),
			    "linkedOf",
			    Type.getMethodDescriptor( Type.getType( IStruct.class ), Type.getType( Object[].class ) ),
			    false ) );

			return nodes;
		}
	}
}
