package ortus.boxlang.transpiler.transformer.expression;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxAssignmentExpression;
import ortus.boxlang.ast.expression.BoxIdentifier;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

public class BoxAssignmentExpressionTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxAssignmentExpressionTransformer.class );

	public BoxAssignmentExpressionTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxAssignmentExpression	assignment	= ( BoxAssignmentExpression ) node;

		Expression				left		= ( Expression ) transpiler.transform( assignment.getLeft(), TransformerContext.LEFT );
		Expression				right		= ( Expression ) transpiler.transform( assignment.getRight() );

		if ( left instanceof MethodCallExpr javaExpr && javaExpr.getName().asString().equalsIgnoreCase( "assign" ) ) {
			javaExpr.getArguments().add( right );
			logger.info( "{} -> {}", node.getSourceText(), javaExpr );
			return javaExpr;
		}

		if ( left instanceof NameExpr ) {

			Map<String, String>	values		= Map.of( "idKey", createKey( ( ( BoxIdentifier ) assignment.getLeft() ).getName() ).toString(), "contextName",
			    transpiler.peekContextName() );
			String				template	= """
			                                  ${contextName}.scopeFindNearby(
			                                                                   	${idKey},
			                                                                   	${contextName}.getDefaultAssignmentScope()).scope().assign()
			                                                                   """;

			MethodCallExpr		javaExpr	= ( MethodCallExpr ) parseExpression( template, values );
			javaExpr.getArguments().add( right );
			logger.info( "{} -> {}", node.getSourceText(), javaExpr );
			return javaExpr;

		}
		return left;
	}
}
