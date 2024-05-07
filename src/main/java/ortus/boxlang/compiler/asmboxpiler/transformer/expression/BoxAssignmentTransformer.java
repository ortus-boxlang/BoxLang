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
import ortus.boxlang.compiler.asmboxpiler.AsmTranspiler;
import ortus.boxlang.compiler.asmboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.asmboxpiler.transformer.TransformerContext;
import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.*;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.operators.*;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;

import java.util.ArrayList;
import java.util.List;

public class BoxAssignmentTransformer extends AbstractTransformer {

	public BoxAssignmentTransformer( AsmTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public List<AbstractInsnNode> transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxAssignment assigment = ( BoxAssignment ) node;
		if ( assigment.getOp() == BoxAssignmentOperator.Equal ) {
			List<AbstractInsnNode> jRight = transpiler.transform( assigment.getRight(), TransformerContext.NONE );
			return transformEquals( assigment.getLeft(), jRight, assigment.getOp(), assigment.getModifiers(), assigment.getSourceText() );
		} else {
			return transformCompoundEquals( assigment );
		}

	}

	public List<AbstractInsnNode> transformEquals( BoxExpression left, List<AbstractInsnNode> jRight, BoxAssignmentOperator op,
	    List<BoxAssignmentModifier> modifiers, String sourceText ) throws IllegalStateException {
		String	template;
		boolean	hasVar	= hasVar( modifiers );

		// "#arguments.scope#.#arguments.propertyName#" = arguments.propertyValue;
		if ( left instanceof BoxStringInterpolation || left instanceof BoxStringLiteral ) {
			// values.put( "left", transpiler.transform( left ).toString() );
			// template = """
			// ExpressionInterpreter.setVariable(
			// ${contextName},
			// ${left},
			// ${right}
			// )
			// """;
			//
			// Node javaExpr = parseExpression( template, values );
			// logger.atTrace().log( sourceText + " -> " + javaExpr.toString() );
			// return javaExpr;
			throw new UnsupportedOperationException();
		}

		List<List<AbstractInsnNode>>	accessKeys		= new ArrayList<>();
		BoxExpression					furthestLeft	= left;

		while ( furthestLeft instanceof BoxAccess currentObjectAccess ) {
			// DotAccess just uses the string directly, array access allows any expression
			if ( currentObjectAccess instanceof BoxDotAccess dotAccess ) {
				if ( dotAccess.getAccess() instanceof BoxIdentifier id ) {
					accessKeys.add( 0, transpiler.createKey( id.getName() ) );
				} else if ( dotAccess.getAccess() instanceof BoxIntegerLiteral intl ) {
					accessKeys.add( 0, transpiler.createKey( intl.getValue() ) );
				} else {
					throw new ExpressionException(
					    "Unexpected element [" + currentObjectAccess.getAccess().getClass().getSimpleName() + "] in dot access expression.",
					    currentObjectAccess.getAccess().getPosition(), currentObjectAccess.getAccess().getSourceText() );
				}
			} else {
				accessKeys.add( 0, transpiler.createKey( currentObjectAccess.getAccess() ) );
			}
			furthestLeft = currentObjectAccess.getContext();
		}

		// If this assignment was var foo = 1, then we need into insert the scope as the furthest left and shift the key
		if ( hasVar ) {
			// This is for the edge case of
			// var variables = 5
			// or
			// var variables.foo = 5
			// in which case it's not really a scope but just an identifier
			// I'd rather do this check when building the AST but the parse tree is more of a pain to deal with
			if ( furthestLeft instanceof BoxScope scope ) {
				accessKeys.add( 0, transpiler.createKey( scope.getName() ) );
			} else if ( furthestLeft instanceof BoxIdentifier id ) {
				accessKeys.add( 0, transpiler.createKey( id.getName() ) );
			} else {
				throw new ExpressionException( "You cannot use the [var] keyword before " + furthestLeft.getClass().getSimpleName(), furthestLeft.getPosition(),
				    furthestLeft.getSourceText() );
			}
			furthestLeft = new BoxIdentifier( "local", null, null );
		}

		List<AbstractInsnNode> nodes = new ArrayList<>();
		if ( furthestLeft instanceof BoxIdentifier id ) {
			/*
			 * Referencer.setDeep(
			 * ${contextName},
			 * ${contextName}.scopeFindNearby( ${accessKey}, ${contextName}.getDefaultAssignmentScope() ),
			 * ${right}
			 * ${accessKeys});
			 */
			nodes.add( new VarInsnNode( Opcodes.ALOAD, 1 ) );

			nodes.add( new VarInsnNode( Opcodes.ALOAD, 1 ) );
			List<AbstractInsnNode> keyNode = transpiler.createKey( id.getName() );
			nodes.addAll( keyNode );
			nodes.add( new VarInsnNode( Opcodes.ALOAD, 1 ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
			    Type.getInternalName( IBoxContext.class ),
			    "getDefaultAssignmentScope",
			    Type.getMethodDescriptor( Type.getType( IScope.class ) ),
			    true ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
			    Type.getInternalName( IBoxContext.class ),
			    "scopeFindNearby",
			    Type.getMethodDescriptor( Type.getType( IBoxContext.ScopeSearchResult.class ), Type.getType( Key.class ), Type.getType( IScope.class ) ),
			    true ) );

			nodes.addAll( jRight );

			nodes.addAll( AsmHelper.array( Type.getType( Key.class ), accessKeys ) );

			nodes.add( new MethodInsnNode(
			    Opcodes.INVOKESTATIC,
			    Type.getInternalName( Referencer.class ),
			    "setDeep",
			    Type.getMethodDescriptor( Type.getType( Object.class ),
			        Type.getType( IBoxContext.class ),
			        Type.getType( IBoxContext.ScopeSearchResult.class ),
			        Type.getType( Object.class ),
			        Type.getType( Key[].class ) ),
			    false ) );
			nodes.add( new InsnNode( Opcodes.POP ) );
		} else {
			if ( accessKeys.size() == 0 ) {
				throw new ExpressionException( "You cannot assign a value to " + left.getClass().getSimpleName(), left.getPosition(), left.getSourceText() );
			}
			/*
			 * Referencer.setDeep(
			 * ${contextName},
			 * ${furthestLeft},
			 * ${right},
			 * ${accessKeys})
			 */
			nodes.add( new VarInsnNode( Opcodes.ALOAD, 1 ) );

			nodes.addAll( transpiler.transform( furthestLeft, TransformerContext.NONE ) );

			nodes.addAll( jRight );

			nodes.addAll( AsmHelper.array( Type.getType( Object.class ), accessKeys ) );

			nodes.add( new MethodInsnNode(
			    Opcodes.INVOKESTATIC,
			    Type.getInternalName( Referencer.class ),
			    "setDeep",
			    Type.getMethodDescriptor( Type.getType( Object.class ),
			        Type.getType( IBoxContext.class ),
			        Type.getType( Object.class ),
			        Type.getType( Object.class ),
			        Type.getType( Key[].class ) ),
			    false ) );
			nodes.add( new InsnNode( Opcodes.POP ) );
		}

		return nodes;
	}

	private List<AbstractInsnNode> transformCompoundEquals( BoxAssignment assigment ) throws IllegalStateException {
		// Note any var keyword is completley ignored in this code path!

		List<AbstractInsnNode>	nodes	= new ArrayList<>();
		List<AbstractInsnNode>	right	= transpiler.transform( assigment.getRight(), TransformerContext.NONE );
		String					template;

		/*
		 * ${operation}.invoke(${contextName},
		 * ${contextName}.scopeFindNearby( ${accessKey}, ${contextName}.getDefaultAssignmentScope() ).scope(),
		 * ${accessKey},
		 * ${right})
		 */

		nodes.add( new VarInsnNode( Opcodes.ALOAD, 1 ) );

		if ( assigment.getLeft() instanceof BoxIdentifier id ) {
			List<AbstractInsnNode> accessKey = transpiler.createKey( id.getName() );

			nodes.add( new VarInsnNode( Opcodes.ALOAD, 1 ) );

			nodes.addAll( accessKey );

			nodes.add( new VarInsnNode( Opcodes.ALOAD, 1 ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
			    Type.getInternalName( IBoxContext.class ),
			    "getDefaultAssignmentScope",
			    Type.getMethodDescriptor( Type.getType( IScope.class ) ),
			    true ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKEINTERFACE,
			    Type.getInternalName( IBoxContext.class ),
			    "scopeFindNearby",
			    Type.getMethodDescriptor( Type.getType( IBoxContext.ScopeSearchResult.class ),
			        Type.getType( Key.class ),
			        Type.getType( IScope.class ) ),
			    true ) );
			nodes.add( new MethodInsnNode( Opcodes.INVOKEVIRTUAL,
			    Type.getInternalName( IBoxContext.ScopeSearchResult.class ),
			    "scope",
			    Type.getMethodDescriptor( Type.getType( IReferenceable.class ) ),
			    false ) );

			nodes.addAll( accessKey );

			nodes.addAll( right );
		} else if ( assigment.getLeft() instanceof BoxAccess objectAccess ) {
			// values.put( "obj", transpiler.transform( objectAccess.getContext() ).toString() );
			// // DotAccess just uses the string directly, array access allows any expression
			// if ( objectAccess instanceof BoxDotAccess dotAccess ) {
			// if ( dotAccess.getAccess() instanceof BoxIdentifier id ) {
			// accessKey = createKey( id.getName() );
			// } else if ( dotAccess.getAccess() instanceof BoxIntegerLiteral intl ) {
			// accessKey = createKey( intl.getValue() );
			// } else {
			// throw new ExpressionException(
			// "Unexpected element [" + dotAccess.getAccess().getClass().getSimpleName() + "] in dot access expression.",
			// dotAccess.getAccess().getPosition(), dotAccess.getAccess().getSourceText() );
			// }
			// } else {
			// accessKey = createKey( objectAccess.getAccess() );
			// }
			// values.put( "accessKey", accessKey.toString() );

			throw new UnsupportedOperationException();
		} else {
			throw new ExpressionException( "You cannot assign a value to " + assigment.getLeft().getClass().getSimpleName(), assigment.getPosition(),
			    assigment.getSourceText() );
		}

		nodes.add( new MethodInsnNode( Opcodes.INVOKESTATIC,
		    Type.getInternalName( getMethodCallTemplate( assigment ) ),
		    "invoke",
		    Type.getMethodDescriptor( Type.getType( Double.class ),
		        Type.getType( IBoxContext.class ),
		        Type.getType( Object.class ),
		        Type.getType( Key.class ),
		        Type.getType( Object.class ) ),
		    false ) );

		return nodes;
	}

	private boolean hasVar( List<BoxAssignmentModifier> modifiers ) {
		return modifiers.stream().anyMatch( it -> it == BoxAssignmentModifier.VAR );
	}

	private Class<?> getMethodCallTemplate( BoxAssignment assignment ) {
		BoxAssignmentOperator operator = assignment.getOp();
		return switch ( operator ) {
			case PlusEqual -> Plus.class;
			case MinusEqual -> Minus.class;
			case StarEqual -> Multiply.class;
			case SlashEqual -> Divide.class;
			case ModEqual -> Modulus.class;
			case ConcatEqual -> Concat.class;
			default -> throw new ExpressionException( "Unknown assingment operator " + operator.toString(), assignment.getPosition(),
			    assignment.getSourceText() );
		};
	}

}
