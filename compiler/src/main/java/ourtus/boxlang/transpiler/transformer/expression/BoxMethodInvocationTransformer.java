package ourtus.boxlang.transpiler.transformer.expression;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import org.apache.commons.text.StringSubstitutor;
import ourtus.boxlang.ast.BoxNode;
import ourtus.boxlang.ast.expression.BoxFunctionInvocation;
import ourtus.boxlang.ast.expression.BoxMethodInvocation;
import ourtus.boxlang.transpiler.BoxLangTranspiler;
import ourtus.boxlang.transpiler.transformer.AbstractTransformer;
import ourtus.boxlang.transpiler.transformer.TransformerContext;

import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class BoxMethodInvocationTransformer extends AbstractTransformer {

	@Override
	public Node transform(BoxNode node, TransformerContext context) throws IllegalStateException {
		BoxMethodInvocation invocation = (BoxMethodInvocation)node;
		Expression expr = (Expression) BoxLangTranspiler.transform(invocation.getObj());

		String args =  invocation.getArguments().stream()
			.map(it -> BoxLangTranspiler.transform(it).toString())
			.collect(Collectors.joining(", "));

		Map<String, String> values = new HashMap<>();

		String target = BoxBuiltinRegistry.getInstance().getRegistry().get(invocation.getName().getName());
		String methodTemplate = target != null ? target : invocation.getName().getName() + "(${args})";

		values.put("expr",expr.toString());
		values.put("args",args);

		String template = "${expr}." + methodTemplate;


		return parseExpression(template,values);
	}
}
