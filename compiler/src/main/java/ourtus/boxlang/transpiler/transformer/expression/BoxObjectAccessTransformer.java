package ourtus.boxlang.transpiler.transformer.expression;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import ourtus.boxlang.ast.BoxNode;
import ourtus.boxlang.ast.expression.BoxObjectAccess;
import ourtus.boxlang.transpiler.BoxLangTranspiler;
import ourtus.boxlang.transpiler.transformer.AbstractTransformer;
import ourtus.boxlang.transpiler.transformer.TransformerContext;

import java.util.HashMap;
import java.util.Map;

public class BoxObjectAccessTransformer extends AbstractTransformer {
	@Override
	public Node transform(BoxNode node, TransformerContext context) throws IllegalStateException {
		BoxObjectAccess objectAccess = (BoxObjectAccess)node;
		Expression scope = (Expression) BoxLangTranspiler.transform(objectAccess.getContext());
		Expression variable = (Expression) BoxLangTranspiler.transform(objectAccess.getAccess());

		Map<String, String> values = new HashMap<>() {{
			put("scope",scope.toString());
			put("variable",variable.toString());
		}};

		if(context == TransformerContext.LEFT) {
			String template = """
    			${scope}.put(${variable})
			""";
			return parseExpression(template,values);
		} else {
			String template = """
    			${scope}.get(${variable})
			""";
			return parseExpression(template,values);
		}
	}
}
