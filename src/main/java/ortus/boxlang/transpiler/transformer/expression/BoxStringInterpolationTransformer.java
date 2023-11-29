
package ortus.boxlang.transpiler.transformer.expression;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxStringInterpolation;
import ortus.boxlang.transpiler.Transpiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

/**
 * Transform a String Interpolatiion the equivalent Java Parser AST nodes
 */
public class BoxStringInterpolationTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxStringInterpolationTransformer.class );

	public BoxStringInterpolationTransformer( Transpiler transpiler ) {
		super( transpiler );
	}

	/**
	 * Transform a String interpolation expression
	 *
	 * @param node    a BoxStringInterpolation
	 * @param context transformation context
	 *
	 * @return a Java Parser Expression
	 *
	 * @throws IllegalStateException
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxStringInterpolation	interpolation	= ( BoxStringInterpolation ) node;
		String					operands		= interpolation.getValues()
		    .stream()
		    .map( it -> resolveScope( transpiler.transform( it, TransformerContext.RIGHT ), context ).toString() )
		    .collect( Collectors.joining( "," ) );

		String					expr			= "Concat.invoke(" + operands + ")";

		Map<String, String>		values			= new HashMap<>() {

													{
														put( "contextName", transpiler.peekContextName() );
													}
												};
		Node					javaExpr		= parseExpression( expr.toString(), values );
		logger.info( "{} -> {}", node.getSourceText(), javaExpr );
		addIndex( javaExpr, node );
		return javaExpr;
	}
}
