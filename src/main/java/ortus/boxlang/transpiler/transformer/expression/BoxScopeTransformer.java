package ortus.boxlang.transpiler.transformer.expression;

import com.github.javaparser.ast.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxScope;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

import java.util.HashMap;
import java.util.Map;

public class BoxScopeTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxScopeTransformer.class );

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxScope			scope		= ( BoxScope ) node;
		String				side		= context == TransformerContext.NONE ? "" : "(" + context.toString() + ") ";
		Map<String, String>	values		= new HashMap<>() {

											{
												put( "scope", scope.getName() );
											}
										};
		String				template	= "";
		if ( "local".equalsIgnoreCase( scope.getName() ) ) {
			template = "context.getScopeNearby(  LocalScope.name )";
		} else {
			// template = "${scope}Scope";
			template = "context.getDefaultAssignmentScope()";
		}

		Node javaExpr = parseExpression( template, values );
		logger.info( side + node.getSourceText() + " -> " + javaExpr );
		return javaExpr;
	}
}
