package ortus.boxlang.transpiler.transformer.expression;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxAssignment;
import ortus.boxlang.ast.statement.BoxAssignmentOperator;
import ortus.boxlang.runtime.config.util.PlaceholderHelper;
import ortus.boxlang.runtime.types.exceptions.ApplicationException;
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
		logger.info( node.getSourceText() );
		BoxAssignment		assigment	= ( BoxAssignment ) node;
		Expression			left		= ( Expression ) transpiler.transform( assigment.getLeft(), TransformerContext.LEFT );
		Expression			right		= ( Expression ) transpiler.transform( assigment.getRight(), TransformerContext.RIGHT );

		Map<String, String>	values		= new HashMap<>() {

											{
												put( "contextName", transpiler.peekContextName() );
											}
										};
		String				template;

		if ( left instanceof MethodCallExpr method ) {
			if ( "assign".equalsIgnoreCase( method.getName().asString() ) ) {
				method.getArguments().add( right );
			}
			if ( "setDeep".equalsIgnoreCase( method.getName().asString() ) ) {
				method.getArguments().add( 1, right );
			}
			values.put( "expr", method.getScope().orElseThrow().toString() );
			values.put( "key", method.getArguments().get( 0 ).toString() );
			values.put( "right", right.toString() );
			template = getMethodCallTemplate( assigment.getOp() );

		} else if ( left instanceof NameExpr name ) {
			values.put( "key", left.toString() );
			values.put( "right", right.toString() );
			if ( right instanceof NameExpr rname ) {
				String tmp = PlaceholderHelper
				    .resolve( "${contextName}.scopeFindNearby( Key.of( \"" + rname + "\" ), ${contextName}.getDefaultAssignmentScope() ).value()", values );
				values.put( "right", tmp );
			}

			template = getNameExpressionTemplate( assigment.getOp() );
		} else {
			throw new ApplicationException( "Unimplemented assignment operator type assignment operator type", left.toString() );
		}
		return parseExpression( template, values );
	}

	private String getNameExpressionTemplate( BoxAssignmentOperator operator ) {
		return switch ( operator ) {
			case PlusEqual -> "Plus.invoke( ${contextName}.scopeFindNearby( Key.of( \"${key}\" ),null).scope(),Key.of( \"${key}\"), ${right} )";
			case MinusEqual -> "Minus.invoke( ${contextName}.scopeFindNearby( Key.of( \"${key}\" ),null).scope(),Key.of( \"${key}\"), ${right} )";
			case StarEqual -> "Multiply.invoke( ${contextName}.scopeFindNearby( Key.of( \"${key}\" ),null).scope(),Key.of( \"${key}\"), ${right} )";
			case SlashEqual -> "Divide.invoke( ${contextName}.scopeFindNearby( Key.of( \"${key}\" ),null).scope(),Key.of( \"${key}\"), ${right} )";
			case ConcatEqual -> "Concat.invoke( ${contextName}.scopeFindNearby( Key.of( \"${key}\" ),null).scope(),Key.of( \"${key}\"), ${right} )";
			case ModEqual -> "Modulus.invoke( ${contextName}.scopeFindNearby( Key.of( \"${key}\" ),null).scope(),Key.of( \"${key}\"), ${right} )";
			default -> """
			           ${contextName}.scopeFindNearby( Key.of( "${key}" ), ${contextName}.getDefaultAssignmentScope() ).scope().assign( Key.of( "${key}" ), ${right} )
			                     """;
		};
	}

	private String getMethodCallTemplate( BoxAssignmentOperator operator ) {
		return switch ( operator ) {
			case PlusEqual -> "Plus.invoke( ${expr}, ${key}, ${right} )";
			case MinusEqual -> "Minus.invoke( ${expr}, ${key}, ${right} )";
			case StarEqual -> "Multiply.invoke( ${expr}, ${key}, ${right} )";
			case SlashEqual -> "Divide.invoke( ${expr}, ${key}, ${right} )";
			case ModEqual -> "Modulus.invoke( ${expr}, ${key}, ${right} )";
			case ConcatEqual -> "Concat.invoke( ${expr}, ${key}, ${right} )";
			case Equal -> "${expr}.assign(${key}, ${right} )";
		};
	}

}
