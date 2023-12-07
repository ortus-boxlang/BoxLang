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
import ortus.boxlang.ast.expression.BoxDotAccess;
import ortus.boxlang.ast.expression.BoxIdentifier;
import ortus.boxlang.ast.statement.BoxAssignmentOperator;
import ortus.boxlang.runtime.config.util.PlaceholderHelper;
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
		Expression			right	= ( Expression ) transpiler.transform( assigment.getRight(), TransformerContext.NONE );
		String				template;

		Map<String, String>	values	= new HashMap<>() {

										{
											put( "contextName", transpiler.peekContextName() );
											put( "right", right.toString() );
										}
									};

		if ( assigment.getLeft() instanceof BoxIdentifier id ) {
			Node accessKey = createKey( id.getName() );
			values.put( "accessKey", accessKey.toString() );
			template = """
			                     ${contextName}.scopeFindNearby( ${accessKey}, ${contextName}.getDefaultAssignmentScope() ).scope().assign( ${accessKey}, ${right} )
			           """;
		} else if ( assigment.getLeft() instanceof BoxAccess objectAccess ) {
			List<Node>	accessKeys	= new ArrayList<Node>();
			BoxExpr		current		= objectAccess;

			while ( current instanceof BoxAccess currentObjectAccess ) {
				// DotAccess just uses the string directly, array access allows any expression
				if ( currentObjectAccess instanceof BoxDotAccess dotAccess ) {
					accessKeys.add( 0, createKey( ( ( BoxIdentifier ) dotAccess.getAccess() ).getName() ) );
				} else {
					accessKeys.add( 0, createKey( currentObjectAccess.getAccess() ) );
				}
				current = currentObjectAccess.getContext();
			}
			values.put( "accessKeys", accessKeys.stream().map( it -> it.toString() ).collect( Collectors.joining( "," ) ) );
			values.put( "furthestLeft", transpiler.transform( current, TransformerContext.NONE ).toString() );
			template = """
			                          Referencer.setDeep(
			           ${furthestLeft},
			           ${right},
			           ${accessKeys}
			           )
			                """;
		} else {
			throw new ExpressionException( "You cannot assign a value to " + assigment.getLeft().getClass().getSimpleName() );
		}

		Node javaExpr = parseExpression( template, values );
		logger.info( assigment.getSourceText() + " -> " + javaExpr.toString() );
		return javaExpr;
	}

	private Node transformCompoundEquals( BoxAssignment assigment, TransformerContext context ) throws IllegalStateException {
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
