package ortus.boxlang.transpiler.transformer.expression;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxScope;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

public class BoxScopeTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxScopeTransformer.class );

	public BoxScopeTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxScope			scope		= ( BoxScope ) node;
		String				side		= context == TransformerContext.NONE ? "" : "(" + context.toString() + ") ";
		Map<String, String>	values		= new HashMap<>() {

											{
												put( "scope", scope.getName() );
												put( "contextName", transpiler.peekContextName() );
											}
										};
		String				template	= "";
		if ( "local".equalsIgnoreCase( scope.getName() ) ) {
			template = "${contextName}.getScopeNearby( LocalScope.name )";
		} else if ( "variables".equalsIgnoreCase( scope.getName() ) ) {
			template = "${contextName}.getScopeNearby( VariablesScope.name )";
			// This is assuming all class templates' invoke method gets the varaiblesScope reference first
			// Nevermind-- this doens't work in a catch block where I need the variables scope to come from the CATCHBOXCONTEXT instead
			// template = "variablesScope";
		} else if ( "request".equalsIgnoreCase( scope.getName() ) ) {
			template = "${contextName}.getScopeNearby( RequestScope.name )";
		} else if ( "server".equalsIgnoreCase( scope.getName() ) ) {
			template = "${contextName}.getScopeNearby( ServerScope.name )";
		} else if ( "arguments".equalsIgnoreCase( scope.getName() ) ) {
			template = "${contextName}.getScopeNearby( ArgumentsScope.name )";
		} else if ( "this".equalsIgnoreCase( scope.getName() ) ) {
			template = "${contextName}.getScopeNearby( ThisScope.name )";
		} else {
			throw new IllegalStateException( "Scope transformation not implemented: " + scope.getName() );
		}

		Node javaExpr = parseExpression( template, values );
		logger.info( side + node.getSourceText() + " -> " + javaExpr );
		return javaExpr;
	}
}
