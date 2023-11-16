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
import com.github.javaparser.ast.expr.MethodCallExpr;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxFunctionInvocation;
import ortus.boxlang.ast.expression.BoxIdentifier;
import ortus.boxlang.ast.expression.BoxObjectAccess;
import ortus.boxlang.ast.expression.BoxScope;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

public class BoxObjectAccessTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxObjectAccessTransformer.class );

	public BoxObjectAccessTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxObjectAccess	objectAccess	= ( BoxObjectAccess ) node;
		String			side			= context == TransformerContext.NONE ? "" : "(" + context.toString() + ") ";

		if ( objectAccess.getContext() instanceof BoxScope && objectAccess.getAccess() instanceof BoxObjectAccess ) {
			Expression	scope		= ( Expression ) transpiler.transform( objectAccess.getContext(), TransformerContext.LEFT );
			Node		variable	= transpiler.transform( objectAccess.getAccess(), context );

			if ( variable instanceof MethodCallExpr method ) {
				if ( "setDeep".equalsIgnoreCase( method.getName().asString() ) ) {
					method.getArguments().remove( 0 );
					method.getArguments().add( 0, scope );
					return method;
				}

				// TODO: Why not use Map.ofEntries() instead?
				Map<String, String>	values		= new HashMap<>() {

													{
														put( "scope", scope.toString() );
														put( "variable", variable.toString() );
														// put( "var1", vars.getValues().get( 1 ).toString() );
													}
												};

				String				template	= switch ( context ) {
													case LEFT -> """
													             ${scope}.dereference( ${variable} )
													             """;
													default -> """
													           ${scope}.dereference( ${variable} , false )
													           """;
													// default -> """
													// Referencer.get(${scope}.dereference(${variable},false)
													// """;
												};
				Node				javaExpr	= parseExpression( template, values );

				logger.info( "{} -> {}", side + node.getSourceText(), javaExpr );
				addIndex( javaExpr, node );
				return method;

			} else {
				Map<String, String>	values		= new HashMap<>() {

													{
														put( "scope", scope.toString() );
														put( "variable", variable.toString() );
													}
												};

				String				template	= switch ( context ) {
													case LEFT -> """
													             ${scope}.assign(Key.of("${variable}"))
													             """;
													default -> """
													           ${scope}.dereference( Key.of( "${variable}" ) , false )
													           """;

												};
				Node				javaExpr	= parseExpression( template, values );
				logger.info( side + node.getSourceText() + " -> " + javaExpr );
				addIndex( javaExpr, node );
				return javaExpr;
			}
		} else if ( objectAccess.getContext() instanceof BoxScope && objectAccess.getAccess() instanceof BoxIdentifier ) {
			Expression			scope		= ( Expression ) transpiler.transform( objectAccess.getContext(), TransformerContext.LEFT );
			Expression			variable	= ( Expression ) transpiler.transform( objectAccess.getAccess(), TransformerContext.RIGHT );
			Map<String, String>	values		= new HashMap<>() {

												{
													put( "scope", scope.toString() );
													put( "variable", variable.toString() );
													put( "safe", objectAccess.isSafe().toString() );
												}
											};
			String				template	= switch ( context ) {
												case LEFT -> """
												             ${scope}.assign(Key.of("${variable}"))
												             """;
												default -> """
												           ${scope}.dereference( Key.of( "${variable}" ) , ${safe} )
												           """;
											};
			Node				javaExpr	= parseExpression( template, values );
			logger.info( side + node.getSourceText() + " -> " + javaExpr );
			addIndex( javaExpr, node );
			return javaExpr;
		} else if ( objectAccess.getContext() instanceof BoxIdentifier ) {

			Node javaExpr = accessWithDeep( objectAccess, context );
			logger.info( side + node.getSourceText() + " -> " + javaExpr );
			addIndex( javaExpr, node );

			return javaExpr;
		} else if ( objectAccess.getContext() instanceof BoxFunctionInvocation && objectAccess.getAccess() instanceof BoxIdentifier ) {
			Expression			function	= ( Expression ) transpiler.transform( objectAccess.getContext(), TransformerContext.LEFT );
			Expression			member		= ( Expression ) transpiler.transform( objectAccess.getAccess(), TransformerContext.RIGHT );
			Map<String, String>	values		= new HashMap<>() {

												{
													put( "function", function.toString() );
													put( "member", member.toString() );
												}
											};
			String				template	= """
			                                  			${function}.getField( "${member}" ).get()
			                                  """;
			Node				javaExpr	= parseExpression( template, values );
			logger.info( side + node.getSourceText() + " -> " + javaExpr );
			addIndex( javaExpr, node );

			return javaExpr;
		}
		throw new IllegalStateException( "" );
	}

	private Node accessWithDeep( BoxObjectAccess objectAccess, TransformerContext context ) {
		List<Node>		keys	= new ArrayList<>();
		List<Boolean>	safe	= new ArrayList<>();

		if ( context == TransformerContext.LEFT ) {

			for ( ortus.boxlang.ast.Node id : objectAccess.getAccess().walk() ) {
				if ( id instanceof BoxIdentifier boxId ) {
					keys.add( transpiler.transform( boxId, TransformerContext.DEREFERENCING ) );
				}
			}

			String				args		= keys.stream().map( Node::toString ).collect( Collectors.joining( ", " ) );
			Expression			ctx			= ( Expression ) transpiler.transform( objectAccess.getContext(), TransformerContext.DEREFERENCING );

			String				template	= """
			                                  Referencer.setDeep(
			                                  	context.scopeFindNearby(
			                                  		${ctx},
			                                  		context.getDefaultAssignmentScope()
			                                  	  ).scope(),
			                                  	${ctx},
			                                  	${acs}
			                                  )
			                                  """;
			Map<String, String>	values		= new HashMap<>() {

												{
													put( "ctx", ctx.toString() );
													put( "acs", args );
												}
											};
			Node				javaExpr	= parseExpression( template, values );
			return javaExpr;

		} else {
			Expression ctx = ( Expression ) transpiler.transform( objectAccess.getContext(), TransformerContext.DEREFERENCING );

			for ( ortus.boxlang.ast.Node id : objectAccess.getAccess().walk() ) {
				if ( id instanceof BoxIdentifier boxId ) {
					keys.add( transpiler.transform( boxId, TransformerContext.DEREFERENCING ) );
					if ( id.getParent() != null && id.getParent() instanceof BoxObjectAccess access ) {
						safe.add( access.isSafe() );
					}
				}
			}
			String	template	= "";
			Node	javaExpr	= null;
			for ( int i = 0; i < keys.size(); i++ ) {
				Node	key		= keys.get( i );
				Boolean	isSafe	= safe.get( i );
				if ( i == 0 ) {
					Map<String, String> values = new HashMap<>() {

						{
							put( "ctx", ctx.toString() );
							put( "safe1", objectAccess.isSafe().toString() );
							put( "safe2", isSafe.toString() );
							put( "key", key.toString() );
						}
					};
					template	= """
					              Referencer.get(
					              	context.scopeFindNearby(
					              		${ctx},
					              		null
					              	).value(),
					              	${key},
					              	${safe2}
					              )
					              """;
					javaExpr	= parseExpression( template, values );
				} else {
					Node				finalJavaExpr	= javaExpr;
					Map<String, String>	values			= new HashMap<>() {

															{
																put( "ctx", finalJavaExpr.toString() );
																put( "safe", isSafe.toString() );
																put( "key", key.toString() );
															}
														};
					template	= """
					              Referencer.get(
					              	${ctx},
					              	${key},
					              	${safe}
					              )
					              """;
					javaExpr	= parseExpression( template, values );
				}

			}

			return javaExpr;

		}
	}
}
