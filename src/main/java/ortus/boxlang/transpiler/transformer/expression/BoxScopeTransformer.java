package ortus.boxlang.transpiler.transformer.expression;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxScope;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

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
			template = "context.getScopeNearby( LocalScope.name )";
		} else if ( "variables".equalsIgnoreCase( scope.getName() ) ) {
			// This is assuming all class templates' invoke method gets the varaiblesScope reference first
			template = "variablesScope";
		} else if ( "request".equalsIgnoreCase( scope.getName() ) ) {
			template = "context.getScopeNearby( RequestScope.name )";
		} else if ( "server".equalsIgnoreCase( scope.getName() ) ) {
			template = "context.getScopeNearby( ServerScope.name )";
		} else if ( "arguments".equalsIgnoreCase( scope.getName() ) ) {
			template = "context.getScopeNearby( ArgumentsScope.name )";
		} else if ( "this".equalsIgnoreCase( scope.getName() ) ) {
			template = "context.getScopeNearby( ThisScope.name )";
		} else {
			throw new IllegalStateException( "Scope transformation not implemented: " + scope.getName() );
		}

		Node javaExpr = parseExpression( template, values );
		logger.info( side + node.getSourceText() + " -> " + javaExpr );
		return javaExpr;
	}
}
