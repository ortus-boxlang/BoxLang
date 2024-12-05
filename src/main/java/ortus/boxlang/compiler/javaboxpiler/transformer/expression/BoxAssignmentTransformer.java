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
package ortus.boxlang.compiler.javaboxpiler.transformer.expression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxAccess;
import ortus.boxlang.compiler.ast.expression.BoxAssignment;
import ortus.boxlang.compiler.ast.expression.BoxAssignmentModifier;
import ortus.boxlang.compiler.ast.expression.BoxAssignmentOperator;
import ortus.boxlang.compiler.ast.expression.BoxDotAccess;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxIntegerLiteral;
import ortus.boxlang.compiler.ast.expression.BoxScope;
import ortus.boxlang.compiler.ast.expression.BoxStringInterpolation;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.javaboxpiler.JavaTranspiler;
import ortus.boxlang.compiler.javaboxpiler.transformer.AbstractTransformer;
import ortus.boxlang.compiler.javaboxpiler.transformer.TransformerContext;
import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;

public class BoxAssignmentTransformer extends AbstractTransformer {

	public BoxAssignmentTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxAssignment	assignment	= ( BoxAssignment ) node;
		Expression		key;
		if ( assignment.getOp() == null ) {
			if ( assignment.getLeft() instanceof BoxIdentifier id ) {
				key = createKey( id.getName() );
			} else {
				throw new ExpressionException( "You cannot declare a variable using " + assignment.getLeft().getClass().getSimpleName(),
				    assignment.getPosition(),
				    assignment.getSourceText() );
			}

			Map<String, String>	values		= new HashMap<>() {

												{
													put( "contextName", transpiler.peekContextName() );
													put( "accessKey", key.toString() );
												}
											};
			String				template	= """
			                                  Referencer.setDeep(
			                                  	${contextName},
			                                  	${contextName}.scopeFindNearby( LocalScope.name, null ),
			                                  	null,
			                                  	${accessKey}
			                                  )
			                                  """;

			Node				javaExpr	= parseExpression( template, values );
			addIndex( javaExpr, node );
			return javaExpr;

		} else if ( assignment.getOp() == BoxAssignmentOperator.Equal ) {
			Expression jRight = ( Expression ) transpiler.transform( assignment.getRight(), TransformerContext.NONE );
			return transformEquals( assignment.getLeft(), jRight, assignment.getOp(), assignment.getModifiers(), assignment.getSourceText(),
			    context );
		} else {
			return transformCompoundEquals( assignment, context );
		}

	}

	public Node transformEquals( BoxExpression left, Expression jRight, BoxAssignmentOperator op, List<BoxAssignmentModifier> modifiers, String sourceText,
	    TransformerContext context ) throws IllegalStateException {
		String				template;
		boolean				hasVar			= hasVar( modifiers );
		boolean				hasStatic		= hasStatic( modifiers );
		boolean				hasFinal		= hasFinal( modifiers );
		String				mustBeScopeName	= null;

		Map<String, String>	values			= new HashMap<>() {

												{
													put( "contextName", transpiler.peekContextName() );
													put( "right", jRight.toString() );
												}
											};

		// "#arguments.scope#.#arguments.propertyName#" = arguments.propertyValue;
		if ( left instanceof BoxStringInterpolation || left instanceof BoxStringLiteral ) {
			// It may be possible to support these, but they are edge cases and follow a different code path, so let's just validate it for now
			if ( hasVar ) {
				throw new ExpressionException( "You cannot use the [var] keyword with a quoted string on the left hand side of your assignment",
				    left.getPosition(), left.getSourceText() );
			}
			if ( hasStatic ) {
				throw new ExpressionException( "You cannot use the [static] keyword with a quoted string on the left hand side of your assignment",
				    left.getPosition(), left.getSourceText() );
			}
			if ( hasFinal ) {
				throw new ExpressionException( "You cannot use the [final] keyword with a quoted string on the left hand side of your assignment",
				    left.getPosition(), left.getSourceText() );
			}

			values.put( "left", transpiler.transform( left ).toString() );
			template = """
			           ExpressionInterpreter.setVariable(
			           ${contextName},
			           ${left},
			           ${right}
			           )
			           	""";

			Node javaExpr = parseExpression( template, values );
			// logger.trace( sourceText + " -> " + javaExpr.toString() );
			return javaExpr;
		}

		List<Node>		accessKeys		= new ArrayList<Node>();
		BoxExpression	furthestLeft	= left;

		while ( furthestLeft instanceof BoxAccess currentObjectAccess ) {
			// DotAccess just uses the string directly, array access allows any expression
			if ( currentObjectAccess instanceof BoxDotAccess dotAccess ) {
				if ( dotAccess.getAccess() instanceof BoxIdentifier id ) {
					accessKeys.add( 0, createKey( id.getName() ) );
				} else if ( dotAccess.getAccess() instanceof BoxIntegerLiteral intl ) {
					accessKeys.add( 0, createKey( intl.getValue() ) );
				} else {
					throw new ExpressionException(
					    "Unexpected element [" + currentObjectAccess.getAccess().getClass().getSimpleName() + "] in dot access expression.",
					    currentObjectAccess.getAccess().getPosition(), currentObjectAccess.getAccess().getSourceText() );
				}
			} else {
				accessKeys.add( 0, createKey( currentObjectAccess.getAccess() ) );
			}
			furthestLeft = currentObjectAccess.getContext();
		}

		if ( hasStatic && hasVar ) {
			throw new ExpressionException( "You cannot use the [var] and [static] keywords together", left.getPosition(), left.getSourceText() );
		}

		// If this assignment was var foo = 1, then we need into insert the scope as the furthest left and shift the key
		if ( hasVar ) {
			mustBeScopeName = "local";
			// This is for the edge case of
			// var variables = 5
			// or
			// var variables.foo = 5
			// in which case it's not really a scope but just an identifier
			// I'd rather do this check when building the AST but the parse tree is more of a pain to deal with
			if ( furthestLeft instanceof BoxScope scope ) {
				accessKeys.add( 0, createKey( scope.getName() ) );
			} else if ( furthestLeft instanceof BoxIdentifier id ) {
				accessKeys.add( 0, createKey( id.getName() ) );
			} else {
				throw new ExpressionException( "You cannot use the [var] keyword before " + furthestLeft.getClass().getSimpleName(), furthestLeft.getPosition(),
				    furthestLeft.getSourceText() );
			}
			furthestLeft = new BoxIdentifier( "local", null, null );
		}

		// If this assignment was static foo = 1, then we need into insert the scope as the furthest left and shift the key
		if ( hasStatic ) {
			mustBeScopeName = "static";
			// This is for the edge case of
			// static variables = 5
			// or
			// static variables.foo = 5
			// in which case it's not really a scope but just an identifier
			// I'd rather do this check when building the AST but the parse tree is more of a pain to deal with
			if ( furthestLeft instanceof BoxScope scope ) {
				accessKeys.add( 0, createKey( scope.getName() ) );
			} else if ( furthestLeft instanceof BoxIdentifier id ) {
				accessKeys.add( 0, createKey( id.getName() ) );
			} else {
				throw new ExpressionException( "You cannot use the [static] keyword before " + furthestLeft.getClass().getSimpleName(),
				    furthestLeft.getPosition(),
				    furthestLeft.getSourceText() );
			}
			furthestLeft = new BoxIdentifier( "static", null, null );
		}

		if ( furthestLeft instanceof BoxIdentifier id ) {
			boolean isBoxSyntax = transpiler.getProperty( "sourceType" ).toLowerCase().startsWith( "box" );
			// imported.foo = 5 is ok, but imported = 5 is not
			if ( left instanceof BoxIdentifier idl && transpiler.matchesImport( idl.getName() ) && isBoxSyntax ) {
				throw new ExpressionException( "You cannot assign a variable with the same name as an import: [" + idl.getName() + "]",
				    idl.getPosition(), idl.getSourceText() );
			}

			String baseObjTemplate = "${contextName}.scopeFindNearby( ${accessKey}, ${contextName}.getDefaultAssignmentScope() ),";
			// imported.foo needs to swap out the furthest left object
			if ( transpiler.matchesImport( id.getName() ) && isBoxSyntax ) {
				baseObjTemplate = "classLocator.load( ${contextName}, \"${accessName}\", imports ),";
			}

			Node	keyNode	= createKey( id.getName() );
			String	thisKey	= keyNode.toString();
			values.put( "accessName", id.getName() );
			values.put( "accessKey", thisKey );
			values.put( "mustBeScopeName", mustBeScopeName == null ? "null" : createKey( mustBeScopeName ).toString() );
			values.put( "hasFinal", hasFinal ? "true" : "false" );
			values.put( "furthestLeft",
			    PlaceholderHelper.resolve( ".scope()",
			        values ) );

			values.put( "accessKeys",
			    ( accessKeys.size() > 0 ? "," : "" ) + accessKeys.stream().map( it -> it.toString() ).collect( Collectors.joining( "," ) ) );
			template = """
			           Referencer.setDeep(
			           	${contextName},
			           	${hasFinal},
			           	${mustBeScopeName},
			           	""" + baseObjTemplate + """
			                                    	${right}
			                                    	${accessKeys}
			                                    )
			                                    """;
		} else {
			if ( accessKeys.size() == 0 && ! ( left instanceof BoxScope ) ) {
				throw new ExpressionException( "You cannot assign a value to " + left.getClass().getSimpleName(), left.getPosition(), left.getSourceText() );
			}
			values.put( "furthestLeft", transpiler.transform( furthestLeft, TransformerContext.NONE ).toString() );
			values.put( "accessKeys", accessKeys.stream().map( it -> it.toString() ).collect( Collectors.joining( "," ) ) );
			values.put( "mustBeScopeName", mustBeScopeName == null ? "null" : createKey( mustBeScopeName ).toString() );
			values.put( "hasFinal", hasFinal ? "true" : "false" );

			template = """
			           Referencer.setDeep(
			           	${contextName},
			           	${hasFinal},
			           	${mustBeScopeName},
			           	${furthestLeft},
			           	${right},
			           	${accessKeys}
			           )
			           """;
			if ( accessKeys.size() == 0 ) {
				template = """
				           Referencer.setDeep(
				           	${contextName},
				           	${hasFinal},
				           	${mustBeScopeName},
				           	${furthestLeft},
				           	${right}
				           )
				           """;
			}
		}

		Node javaExpr = parseExpression( template, values );
		// logger.trace( sourceText + " -> " + javaExpr.toString() );
		return javaExpr;
	}

	private Node transformCompoundEquals( BoxAssignment assignment, TransformerContext context ) throws IllegalStateException {
		// Note any var keyword is completley ignored in this code path!

		Expression			right	= ( Expression ) transpiler.transform( assignment.getRight(), TransformerContext.NONE );
		String				template;
		Node				accessKey;

		Map<String, String>	values	= new HashMap<>() {

										{
											put( "contextName", transpiler.peekContextName() );
											put( "right", right.toString() );
										}
									};

		if ( assignment.getLeft() instanceof BoxIdentifier id ) {
			accessKey = createKey( id.getName() );
			values.put( "accessKey", accessKey.toString() );
			String obj = PlaceholderHelper.resolve(
			    "${contextName}.scopeFindNearby( ${accessKey}, ${contextName}.getDefaultAssignmentScope() ).scope()",
			    values );
			values.put( "obj", obj );

		} else if ( assignment.getLeft() instanceof BoxAccess objectAccess ) {
			values.put( "obj", transpiler.transform( objectAccess.getContext() ).toString() );
			// DotAccess just uses the string directly, array access allows any expression
			if ( objectAccess instanceof BoxDotAccess dotAccess ) {
				if ( dotAccess.getAccess() instanceof BoxIdentifier id ) {
					accessKey = createKey( id.getName() );
				} else if ( dotAccess.getAccess() instanceof BoxIntegerLiteral intl ) {
					accessKey = createKey( intl.getValue() );
				} else {
					throw new ExpressionException(
					    "Unexpected element [" + dotAccess.getAccess().getClass().getSimpleName() + "] in dot access expression.",
					    dotAccess.getAccess().getPosition(), dotAccess.getAccess().getSourceText() );
				}
			} else {
				accessKey = createKey( objectAccess.getAccess() );
			}
			values.put( "accessKey", accessKey.toString() );
		} else {
			throw new ExpressionException( "You cannot assign a value to " + assignment.getLeft().getClass().getSimpleName(), assignment.getPosition(),
			    assignment.getSourceText() );
		}

		template = getMethodCallTemplate( assignment );
		Node javaExpr = parseExpression( template, values );
		// logger.trace( assignment.getSourceText() + " -> " + javaExpr.toString() );
		return javaExpr;
	}

	private boolean hasVar( List<BoxAssignmentModifier> modifiers ) {
		return modifiers.stream().anyMatch( it -> it == BoxAssignmentModifier.VAR );
	}

	private boolean hasStatic( List<BoxAssignmentModifier> modifiers ) {
		return modifiers.stream().anyMatch( it -> it == BoxAssignmentModifier.STATIC );
	}

	private boolean hasFinal( List<BoxAssignmentModifier> modifiers ) {
		return modifiers.stream().anyMatch( it -> it == BoxAssignmentModifier.FINAL );
	}

	private String getMethodCallTemplate( BoxAssignment assignment ) {
		BoxAssignmentOperator operator = assignment.getOp();
		return switch ( operator ) {
			case PlusEqual -> "Plus.invoke( ${contextName}, ${obj}, ${accessKey}, ${right} )";
			case MinusEqual -> "Minus.invoke( ${contextName}, ${obj}, ${accessKey}, ${right} )";
			case StarEqual -> "Multiply.invoke( ${contextName}, ${obj}, ${accessKey}, ${right} )";
			case SlashEqual -> "Divide.invoke( ${contextName}, ${obj}, ${accessKey}, ${right} )";
			case ModEqual -> "Modulus.invoke( ${contextName}, ${obj}, ${accessKey}, ${right} )";
			case ConcatEqual -> "Concat.invoke( ${contextName}, ${obj}, ${accessKey}, ${right} )";
			default -> throw new ExpressionException( "Unknown assingment operator " + operator.toString(), assignment.getPosition(),
			    assignment.getSourceText() );
		};
	}

}
