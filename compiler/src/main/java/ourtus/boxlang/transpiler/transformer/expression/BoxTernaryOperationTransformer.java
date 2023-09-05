package ourtus.boxlang.transpiler.transformer.expression;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import ourtus.boxlang.ast.BoxNode;
import ourtus.boxlang.ast.expression.BoxTernaryOperation;
import ourtus.boxlang.transpiler.BoxLangTranspiler;
import ourtus.boxlang.transpiler.transformer.AbstractTransformer;
import ourtus.boxlang.transpiler.transformer.TransformerContext;

import java.util.HashMap;
import java.util.Map;

public class BoxTernaryOperationTransformer extends AbstractTransformer {

	@Override
	public Node transform(BoxNode node, TransformerContext context) throws IllegalStateException {
		BoxTernaryOperation operation = (BoxTernaryOperation)node;
		Expression condition = (Expression) BoxLangTranspiler.transform(operation.getCondition());
		Expression whenTrue = (Expression) BoxLangTranspiler.transform(operation.getWhenTrue());
		Expression whenFalse = (Expression) BoxLangTranspiler.transform(operation.getWhenFalse());

		Map<String, String> values = new HashMap<>() {{
			put("condition", condition.toString());
			put("whenTrue", whenTrue.toString());
			put("whenFalse", whenFalse.toString());
		}};
		String template = "Ternary.invoke(${condition},${whenTrue},${whenFalse})";;

		return parseExpression(template,values);
	}
}
