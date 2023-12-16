package ortus.boxlang.transpiler.transformer.expression;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxExpressionInvocation;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

public class BoxExpressionInvocationTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxExpressionInvocationTransformer.class );

	public BoxExpressionInvocationTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxExpressionInvocation	invocation	= ( BoxExpressionInvocation ) node;

		Expression				expr		= ( Expression ) transpiler.transform( invocation.getExpr(), context );

		String					args		= invocation.getArguments().stream()
		    .map( it -> transpiler.transform( it ).toString() )
		    .collect( Collectors.joining( ", " ) );

		String					template	= """
		                                      ${contextName}.invokeFunction(
		                                        ${expr},
		                                        new Object[] { ${args} }
		                                      )
		                                      """;

		Map<String, String>		values		= new HashMap<>() {

												{
													put( "contextName", transpiler.peekContextName() );
													put( "expr", expr.toString() );
													put( "args", args );
												}
											};

		Node					javaExpr	= parseExpression( template, values );
		logger.info( node.getSourceText() + " -> " + javaExpr );
		addIndex( javaExpr, node );
		return javaExpr;
	}
}
