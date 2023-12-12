package ortus.boxlang.transpiler.transformer.expression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxAccess;
import ortus.boxlang.ast.expression.BoxAssignment;
import ortus.boxlang.ast.expression.BoxAssignmentModifier;
import ortus.boxlang.ast.expression.BoxDotAccess;
import ortus.boxlang.ast.expression.BoxIdentifier;
import ortus.boxlang.ast.expression.BoxScope;
import ortus.boxlang.ast.statement.BoxAssignmentOperator;
import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.runtime.types.exceptions.ApplicationException;
import ortus.boxlang.runtime.types.exceptions.ExpressionException;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

public class BoxAssignmentTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxAssignmentTransformer.class );

	public BoxAssignmentTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxAssignment assigment = ( BoxAssignment ) node;
		if ( assigment.getOp() == BoxAssignmentOperator.Equal ) {
			return transformEquals( assigment, context );
		} else {
			return transformCompoundEquals( assigment, context );
		}

	}

	private Node transformEquals( BoxAssignment assigment, TransformerContext context ) throws IllegalStateException {
		Expression			right			= ( Expression ) transpiler.transform( assigment.getRight(), TransformerContext.NONE );
		String				template;
		boolean				hasVar			= hasVar( assigment );

		Map<String, String>	values			= new HashMap<>() {

												{
													put( "contextName", transpiler.peekContextName() );
													put( "right", right.toString() );
												}
											};

		List<Node>			accessKeys		= new ArrayList<Node>();
		BoxExpr				furthestLeft	= assigment.getLeft();

		while ( furthestLeft instanceof BoxAccess currentObjectAccess ) {
			// DotAccess just uses the string directly, array access allows any expression
			if ( currentObjectAccess instanceof BoxDotAccess dotAccess ) {
				accessKeys.add( 0, createKey( ( ( BoxIdentifier ) dotAccess.getAccess() ).getName() ) );
			} else {
				accessKeys.add( 0, createKey( currentObjectAccess.getAccess() ) );
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
				accessKeys.add( 0, createKey( scope.getName() ) );
			} else if ( furthestLeft instanceof BoxIdentifier id ) {
				accessKeys.add( 0, createKey( id.getName() ) );
			} else {
				throw new ExpressionException( "You cannot use the [var] keyword before " + furthestLeft.getClass().getSimpleName() );
			}
			furthestLeft = new BoxScope( "local", null, null );
		}

		if ( furthestLeft instanceof BoxIdentifier id ) {
			if ( transpiler.matchesImport( id.getName() ) ) {
				throw new ApplicationException( "You cannot assign a variable with the same name as an import: [" + id.getName() + "]" );
			}

			Node	keyNode	= createKey( id.getName() );
			String	thisKey	= keyNode.toString();
			accessKeys.add( 0, keyNode );
			values.put( "accessKey", thisKey );
			values.put( "furthestLeft",
			    PlaceholderHelper.resolve( "${contextName}.scopeFindNearby( ${accessKey}, ${contextName}.getDefaultAssignmentScope() ).scope()",
			        values ) );

		} else {
			if ( accessKeys.size() == 0 ) {
				throw new ExpressionException( "You cannot assign a value to " + assigment.getLeft().getClass().getSimpleName() );
			}
			values.put( "furthestLeft", transpiler.transform( furthestLeft, TransformerContext.NONE ).toString() );

		}

		values.put( "accessKeys", accessKeys.stream().map( it -> it.toString() ).collect( Collectors.joining( "," ) ) );
		template = """
		                          Referencer.setDeep(
		           ${furthestLeft},
		           ${right},
		           ${accessKeys}
		           )
		                """;

		Node javaExpr = parseExpression( template, values );
		logger.info( assigment.getSourceText() + " -> " + javaExpr.toString() );
		return javaExpr;
	}

	private Node transformCompoundEquals( BoxAssignment assigment, TransformerContext context ) throws IllegalStateException {
		// Note any var keyword is completley ignored in this code path!

		Expression			right	= ( Expression ) transpiler.transform( assigment.getRight(), TransformerContext.NONE );
		String				template;
		Node				accessKey;

		Map<String, String>	values	= new HashMap<>() {

										{
											put( "contextName", transpiler.peekContextName() );
											put( "right", right.toString() );
										}
									};

		if ( assigment.getLeft() instanceof BoxIdentifier id ) {
			accessKey = createKey( id.getName() );
			values.put( "accessKey", accessKey.toString() );
			String obj = PlaceholderHelper.resolve(
			    "${contextName}.scopeFindNearby( ${accessKey}, ${contextName}.getDefaultAssignmentScope() ).scope()",
			    values );
			values.put( "obj", obj );

		} else if ( assigment.getLeft() instanceof BoxAccess objectAccess ) {
			values.put( "obj", transpiler.transform( objectAccess.getContext() ).toString() );
			// DotAccess just uses the string directly, array access allows any expression
			if ( objectAccess instanceof BoxDotAccess dotAccess ) {
				accessKey = createKey( ( ( BoxIdentifier ) dotAccess.getAccess() ).getName() );
			} else {
				accessKey = createKey( objectAccess.getAccess() );
			}
			values.put( "accessKey", accessKey.toString() );
		} else {
			throw new ExpressionException( "You cannot assign a value to " + assigment.getLeft().getClass().getSimpleName() );
		}

		template = getMethodCallTemplate( assigment.getOp() );
		Node javaExpr = parseExpression( template, values );
		logger.info( assigment.getSourceText() + " -> " + javaExpr.toString() );
		return javaExpr;
	}

	private boolean hasVar( BoxAssignment assigment ) {
		return assigment.getModifiers().stream().anyMatch( it -> it == BoxAssignmentModifier.VAR );
	}

	private String getMethodCallTemplate( BoxAssignmentOperator operator ) {
		return switch ( operator ) {
			case PlusEqual -> "Plus.invoke( ${obj}, ${accessKey}, ${right} )";
			case MinusEqual -> "Minus.invoke( ${obj}, ${accessKey}, ${right} )";
			case StarEqual -> "Multiply.invoke( ${obj}, ${accessKey}, ${right} )";
			case SlashEqual -> "Divide.invoke( ${obj}, ${accessKey}, ${right} )";
			case ModEqual -> "Modulus.invoke( ${obj}, ${accessKey}, ${right} )";
			case ConcatEqual -> "Concat.invoke( ${obj}, ${accessKey}, ${right} )";
			default -> throw new ExpressionException( "Unknown assingment operator " + operator.toString() );
		};
	}

}
