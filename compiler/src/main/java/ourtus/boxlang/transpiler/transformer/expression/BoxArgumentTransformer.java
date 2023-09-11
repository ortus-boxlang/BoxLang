package ourtus.boxlang.transpiler.transformer.expression;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ourtus.boxlang.ast.BoxNode;
import ourtus.boxlang.ast.expression.BoxArgument;
import ourtus.boxlang.transpiler.BoxLangTranspiler;
import ourtus.boxlang.transpiler.transformer.AbstractTransformer;
import ourtus.boxlang.transpiler.transformer.TransformerContext;

import java.util.HashMap;
import java.util.Map;

public class BoxArgumentTransformer extends AbstractTransformer {
	Logger logger = LoggerFactory.getLogger( BoxArrayAccessTransformer.class );
	@Override
	public Node transform(BoxNode node, TransformerContext context) throws IllegalStateException {
		BoxArgument arg = (BoxArgument)node;
		String side = context == TransformerContext.NONE ? "" : "(" + context.toString() + ") ";
		Expression expr = (Expression) BoxLangTranspiler.transform(arg.getValue());
		// TODO handle named parameters
		Map<String, String> values = new HashMap<>() {{
			put( "expr", expr.toString() );
		}};

		String template = "${expr}";

		Node javaExpr = parseExpression( template, values ) ;
		logger.info(side + node.getSourceText() + " -> " + javaExpr);

		return javaExpr;
	}
}
