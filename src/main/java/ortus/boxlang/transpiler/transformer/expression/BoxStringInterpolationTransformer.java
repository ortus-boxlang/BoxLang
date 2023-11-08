
package ortus.boxlang.transpiler.transformer.expression;

import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxStringInterpolation;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

/**
 * Transform a String Interpolatiion the equivalent Java Parser AST nodes
 */
public class BoxStringInterpolationTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxStringInterpolationTransformer.class );

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
		List<Node>				operands		= interpolation.getValues()
		    .stream()
		    .map( it -> resolveScope( JavaTranspiler.transform( it, TransformerContext.RIGHT ), context ) )
		    .toList();
		// .collect( Collectors.joining( "+" ) );
		String					expr			= operands.get( operands.size() - 1 ).toString();
		for ( int i = operands.size() - 1; i-- > 0; ) {
			expr = "Concat.invoke(" + operands.get( i ) + "," + expr + ")";
		}

		Node javaExpr = parseExpression( expr.toString(), new HashMap<>() );
		logger.info( "{} -> {}", node.getSourceText(), javaExpr );
		addIndex( javaExpr, node );
		return javaExpr;
	}
}
