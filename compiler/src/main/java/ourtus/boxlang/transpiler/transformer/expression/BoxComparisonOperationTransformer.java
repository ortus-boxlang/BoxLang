package ourtus.boxlang.transpiler.transformer.expression;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import ourtus.boxlang.ast.BoxNode;
import ourtus.boxlang.ast.expression.BoxComparisonOperation;
import ourtus.boxlang.ast.expression.BoxComparisonOperator;
import ourtus.boxlang.transpiler.BoxLangTranspiler;
import ourtus.boxlang.transpiler.transformer.AbstractTransformer;
import ourtus.boxlang.transpiler.transformer.TransformerContext;

import java.util.HashMap;
import java.util.Map;

public class BoxComparisonOperationTransformer extends AbstractTransformer {
	@Override
	public Node transform(BoxNode node, TransformerContext context) throws IllegalStateException {
		BoxComparisonOperation operation = (BoxComparisonOperation) node;
		Expression left = (Expression) BoxLangTranspiler.transform(operation.getLeft());
		Expression right = (Expression) BoxLangTranspiler.transform(operation.getRight());

		Map<String, String> values = new HashMap<>() {{
			put("left", left.toString());
			put("right", right.toString());

		}};
		String template = "";

		if (operation.getOperator() == BoxComparisonOperator.Equal) {
			template = "EqualsEquals.invoke(context,${left},${right})";
		}
		return parseExpression(template,values);
	}
}
