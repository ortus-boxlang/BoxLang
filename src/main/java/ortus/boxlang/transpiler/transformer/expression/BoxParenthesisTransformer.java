package ortus.boxlang.transpiler.transformer.expression;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxParenthesis;
import ortus.boxlang.transpiler.BoxLangTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

import java.util.HashMap;
import java.util.Map;

public class BoxParenthesisTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxParenthesisTransformer.class );

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxParenthesis		parenthesis	= ( BoxParenthesis ) node;
		Expression			expr		= ( Expression ) BoxLangTranspiler.transform( parenthesis.getExpression() );
		String				side		= context == TransformerContext.NONE ? "" : "(" + context.toString() + ") ";
		Map<String, String>	values		= new HashMap<>() {

											{
												put( "expr", expr.toString() );
											}
										};
		String				template	= "(${expr})";
		Node				javaExpr	= parseExpression( template, values );
		logger.info( side + node.getSourceText() + " -> " + javaExpr );
		return javaExpr;

	}
}
