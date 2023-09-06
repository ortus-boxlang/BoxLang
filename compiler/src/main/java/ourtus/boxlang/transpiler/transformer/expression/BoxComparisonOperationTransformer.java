package ourtus.boxlang.transpiler.transformer.expression;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NameExpr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ourtus.boxlang.ast.BoxNode;
import ourtus.boxlang.ast.expression.BoxComparisonOperation;
import ourtus.boxlang.ast.expression.BoxComparisonOperator;
import ourtus.boxlang.transpiler.BoxLangTranspiler;
import ourtus.boxlang.transpiler.transformer.AbstractTransformer;
import ourtus.boxlang.transpiler.transformer.TransformerContext;

import java.util.HashMap;
import java.util.Map;

public class BoxComparisonOperationTransformer extends AbstractTransformer {
	Logger logger = LoggerFactory.getLogger( BoxComparisonOperationTransformer.class );
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxComparisonOperation operation = ( BoxComparisonOperation ) node;
		Expression left = ( Expression ) resolveScope( BoxLangTranspiler.transform( operation.getLeft()),context );
		Expression right = ( Expression ) resolveScope(BoxLangTranspiler.transform( operation.getRight()),context );

		Map<String, String> values = new HashMap<>() {{
			put( "left", left.toString() );
			put( "right", right.toString() );

		}};
		String template = "";

		if ( operation.getOperator() == BoxComparisonOperator.Equal ) {
			template = "EqualsEquals.invoke(${left},${right})";
		} else if(operation.getOperator() == BoxComparisonOperator.NotEqual) {
			template = "!EqualsEquals.invoke(${left},${right})";
		} else if(operation.getOperator() == BoxComparisonOperator.TEqual) {
			template = "EqualsEqualsEquals.invoke(${left},${right})";
		} else if(operation.getOperator() == BoxComparisonOperator.GreaterThan) {
			template = "GreaterThan.invoke(${left},${right})";
		} else if(operation.getOperator() == BoxComparisonOperator.GreaterThanEquals) {
			template = "GreaterThanEqual.invoke(${left},${right})";
		} else if(operation.getOperator() == BoxComparisonOperator.LessThan) {
			template = "LessThan.invoke(${left},${right})";
		} else if(operation.getOperator() == BoxComparisonOperator.LesslThanEqual) {
			template = "LessThanEqual.invoke(${left},${right})";
		} else {
			throw new IllegalStateException("not implemented");
		}
		Node javaExpr = parseExpression( template, values );
		logger.info(node.getSourceText() + " -> " + javaExpr);
		return javaExpr;
	}


}
