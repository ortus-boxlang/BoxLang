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
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

import ortus.boxlang.compiler.asmboxpiler.Transpiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.ReturnValueContext;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxAccess;
import ortus.boxlang.compiler.ast.expression.BoxDotAccess;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxParenthesis;
import ortus.boxlang.compiler.ast.expression.BoxScope;
import ortus.boxlang.compiler.ast.expression.BoxUnaryOperation;
import ortus.boxlang.compiler.ast.expression.BoxUnaryOperator;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.operators.BitwiseComplement;
import ortus.boxlang.runtime.operators.Decrement;
import ortus.boxlang.runtime.operators.Increment;
import ortus.boxlang.runtime.operators.Negate;
import ortus.boxlang.runtime.operators.Not;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;

public class BoxUnaryOperationTransformer extends AbstractTransformer {

	public BoxUnaryOperationTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	/**
	 * Transform a unary operator
	 *
	 * @param node    a BoxUnaryOperator instance
	 * @param context transformation context
	 *
	 * @return Generates a Method invocation to the Runtime Increment/Increment
	 *
	 * @throws IllegalStateException
	 */
	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context, ReturnValueContext returnContext ) throws IllegalStateException {
		BoxUnaryOperation	operation	= ( BoxUnaryOperation ) node;
		BoxUnaryOperator	operator	= operation.getOperator();
		BoxExpression		expr		= operation.getExpr();

		// Outer parenthesis are useless at this point, so unwrap them until we get to something other than BoxParenthesis
		while ( expr instanceof BoxParenthesis boxparen ) {
			expr = boxparen.getExpression();
		}

		List<AbstractInsnNode> nodes = new ArrayList<>();
		// for non literals, we need to identify the key being incremented/decremented and the object it lives in (which may be a scope)
		if ( expr instanceof BoxIdentifier id && operator != BoxUnaryOperator.Not && operator != BoxUnaryOperator.Minus && operator != BoxUnaryOperator.Plus ) {
			nodes.add( new VarInsnNode( Opcodes.ALOAD, 1 ) );
			nodes.add( new VarInsnNode( Opcodes.ALOAD, 1 ) );
			nodes.addAll( transpiler.createKey( id.getName() ) );
			nodes.add( new InsnNode( Opcodes.ACONST_NULL ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
			    Type.getInternalName( IBoxContext.class ),
			    "scopeFindNearby",
			    Type.getMethodDescriptor( Type.getType( IBoxContext.ScopeSearchResult.class ), Type.getType( Key.class ), Type.getType( IScope.class ) ),
			    true ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKEVIRTUAL,
			    Type.getInternalName( IBoxContext.ScopeSearchResult.class ),
			    "scope",
			    Type.getMethodDescriptor( Type.getType( IReferenceable.class ) ),
			    false ) );
			nodes.addAll( transpiler.createKey( id.getName() ) );
			nodes.add( getMethodCallTemplateCompound( operation ) );
		} else if ( expr instanceof BoxAccess objectAccess && operator != BoxUnaryOperator.Not && operator != BoxUnaryOperator.Minus
		    && operator != BoxUnaryOperator.Plus ) {
			nodes.add( new VarInsnNode( Opcodes.ALOAD, 1 ) );
			nodes.addAll( transpiler.transform( objectAccess.getContext(), TransformerContext.NONE ) );
			// DotAccess just uses the string directly, array access allows any expression>
			List<AbstractInsnNode> accessKey;
			if ( objectAccess instanceof BoxDotAccess dotAccess ) {
				accessKey = transpiler.createKey( ( ( BoxIdentifier ) dotAccess.getAccess() ).getName() );
			} else {
				accessKey = transpiler.createKey( objectAccess.getAccess() );
			}
			nodes.addAll( accessKey );
			nodes.add( getMethodCallTemplateCompound( operation ) );
		} else if ( expr instanceof BoxScope ) {
			throw new ExpressionException( "You cannot perform an increment/decrement operation on a " + expr.getClass().getSimpleName() + " expression.",
			    expr.getPosition(), expr.getSourceText() );
		} else {
			nodes.addAll( transpiler.transform( expr, TransformerContext.NONE ) );
			// +5, -6, or !true are "simple" use cases, same with ++5, --5, 5++, 5--, (something)++ (-5)-- ++foo() foo.bar()--
			switch ( operation.getOperator() ) {
				// +5 or +tmp is the same as just 5 or tmp
				case Plus, PostPlusPlus, PostMinusMinus :
					break;
				case Minus :
					nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
					    Type.getInternalName( Negate.class ),
					    "invoke",
					    Type.getMethodDescriptor( Type.getType( Number.class ), Type.getType( Object.class ) ),
					    false ) );
					break;
				case Not :
					nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
					    Type.getInternalName( Not.class ),
					    "invoke",
					    Type.getMethodDescriptor( Type.getType( Boolean.class ), Type.getType( Object.class ) ),
					    false ) );
					break;
				case PrePlusPlus :
					nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
					    Type.getInternalName( Increment.class ),
					    "invoke",
					    Type.getMethodDescriptor( Type.getType( Double.class ), Type.getType( Object.class ) ),
					    false ) );
					break;
				// 5++ is the same as 5
				case PreMinusMinus :
					nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
					    Type.getInternalName( Decrement.class ),
					    "invoke",
					    Type.getMethodDescriptor( Type.getType( Double.class ), Type.getType( Object.class ) ),
					    false ) );
					break;
				// 5-- is the same as 5
				case BitwiseComplement :
					nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
					    Type.getInternalName( BitwiseComplement.class ),
					    "invoke",
					    Type.getMethodDescriptor( Type.getType( Number.class ), Type.getType( Object.class ) ),
					    false ) );
					break;
				default :
					throw new ExpressionException( "Unknown unary operator " + operator.toString(), operation.getPosition(), operation.getSourceText() );
			}
			;
		}

		return nodes;
	}

	private AbstractInsnNode getMethodCallTemplateCompound( BoxUnaryOperation operation ) {
		BoxUnaryOperator operator = operation.getOperator();
		// These all refernce variable names which need retrieved, modified, and then re-assigned
		return switch ( operator ) {
			case PrePlusPlus -> new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( Increment.class ),
			    "invokePre",
			    Type.getMethodDescriptor( Type.getType( Double.class ), Type.getType( IBoxContext.class ), Type.getType( Object.class ),
			        Type.getType( Key.class ) ),
			    false );
			case PostPlusPlus -> new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( Increment.class ),
			    "invokePost",
			    Type.getMethodDescriptor( Type.getType( Double.class ), Type.getType( IBoxContext.class ), Type.getType( Object.class ),
			        Type.getType( Key.class ) ),
			    false );
			case PreMinusMinus -> new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( Decrement.class ),
			    "invokePre",
			    Type.getMethodDescriptor( Type.getType( Double.class ), Type.getType( IBoxContext.class ), Type.getType( Object.class ),
			        Type.getType( Key.class ) ),
			    false );
			case PostMinusMinus -> new MethodInsnNode( Opcodes.INVOKESTATIC,
			    Type.getInternalName( Decrement.class ),
			    "invokePost",
			    Type.getMethodDescriptor( Type.getType( Double.class ), Type.getType( IBoxContext.class ), Type.getType( Object.class ),
			        Type.getType( Key.class ) ),
			    false );
			default -> throw new ExpressionException( "Unknown unary compound operator " + operator.toString(), operation.getPosition(),
			    operation.getSourceText() );
		};
	}

}
