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
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import ortus.boxlang.compiler.asmboxpiler.AsmHelper;
import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxScope;
import ortus.boxlang.compiler.ast.expression.BoxSpreadExpression;
import ortus.boxlang.compiler.ast.expression.BoxStructLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStructType;
import ortus.boxlang.runtime.dynamic.LiteralSpreadUtil;
import ortus.boxlang.runtime.types.IStruct;

public class BoxStructLiteralTransformer extends AbstractTransformer {

	public BoxStructLiteralTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) {
		BoxStructLiteral				structLiteral	= ( BoxStructLiteral ) node;
		List<AbstractInsnNode>			nodes			= new ArrayList<>();
		String							structTypeField	= structLiteral.getType() == BoxStructType.Ordered ? "LINKED" : "DEFAULT";
		List<List<AbstractInsnNode>>	structArguments	= transformStructArguments( structLiteral, context );

		nodes.add( new FieldInsnNode( Opcodes.GETSTATIC,
		    Type.getInternalName( IStruct.TYPES.class ),
		    structTypeField,
		    Type.getDescriptor( IStruct.TYPES.class ) ) );
		nodes.addAll( AsmHelper.array( Type.getType( Object.class ), structArguments ) );
		nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
		    Type.getInternalName( LiteralSpreadUtil.class ),
		    "struct",
		    Type.getMethodDescriptor( Type.getType( IStruct.class ), Type.getType( IStruct.TYPES.class ), Type.getType( Object[].class ) ),
		    false ) );
		return AsmHelper.addLineNumberLabels( nodes, node );
	}

	private List<List<AbstractInsnNode>> transformStructArguments( BoxStructLiteral structLiteral, TransformerContext context ) {
		List<List<AbstractInsnNode>>	arguments	= new ArrayList<>();
		List<BoxExpression>				values		= structLiteral.getValues();

		for ( int i = 0; i < values.size(); ) {
			BoxExpression current = values.get( i );
			if ( current instanceof BoxSpreadExpression spread ) {
				arguments.add( transformSpread( spread, context ) );
				i++;
				continue;
			}

			if ( i + 1 >= values.size() ) {
				throw new IllegalStateException( "Invalid struct literal data while transforming spread values." );
			}

			arguments.add( transformKey( current, context ) );
			arguments.add( transpiler.transform( values.get( i + 1 ), context, ReturnValueContext.VALUE ) );
			i += 2;
		}

		return arguments;
	}

	private List<AbstractInsnNode> transformKey( BoxExpression keyExpr, TransformerContext context ) {
		if ( keyExpr instanceof BoxIdentifier identifier ) {
			return List.of( new LdcInsnNode( identifier.getName() ) );
		}
		if ( keyExpr instanceof BoxScope scope ) {
			return List.of( new LdcInsnNode( scope.getName() ) );
		}
		return transpiler.transform( keyExpr, context, ReturnValueContext.VALUE );
	}

	private List<AbstractInsnNode> transformSpread( BoxSpreadExpression spread, TransformerContext context ) {
		List<AbstractInsnNode> spreadNodes = new ArrayList<>( transpiler.transform( spread.getExpression(), context, ReturnValueContext.VALUE_OR_NULL ) );
		spreadNodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
		    Type.getInternalName( LiteralSpreadUtil.class ),
		    "spread",
		    Type.getMethodDescriptor( Type.getType( LiteralSpreadUtil.SpreadValue.class ), Type.getType( Object.class ) ),
		    false ) );
		return spreadNodes;
	}
}
