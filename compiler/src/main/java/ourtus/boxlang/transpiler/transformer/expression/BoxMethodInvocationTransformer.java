package ourtus.boxlang.transpiler.transformer.expression;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import ourtus.boxlang.ast.BoxNode;
import ourtus.boxlang.ast.expression.BoxMethodInvocation;
import ourtus.boxlang.transpiler.BoxLangTranspiler;
import ourtus.boxlang.transpiler.transformer.AbstractTransformer;
import ourtus.boxlang.transpiler.transformer.TransformerContext;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class BoxMethodInvocationTransformer extends AbstractTransformer {

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxMethodInvocation invocation = ( BoxMethodInvocation ) node;
		Expression expr = ( Expression ) BoxLangTranspiler.transform( invocation.getObj() );

		String args = invocation.getArguments().stream().map( it -> BoxLangTranspiler.transform( it ).toString() )
			.collect( Collectors.joining( ", " ) );

		Map<String, String> values = new HashMap<>();

		String target = BoxBuiltinRegistry.getInstance().getRegistry().get( invocation.getName().getName() );

		values.put( "expr", expr.toString() );
		values.put( "args", args );

		String template;

		if ( target != null ) {
			template = "${expr}." + target;
		} else {
			values.put( "method", invocation.getName().getName().toString() );
			template = """
				Referencer.getAndInvoke(
				  ${expr},
				  Key.of( "${method}" ),
				  new Object[] { ${args} },
				  false
				)
				""";
		}

		return parseExpression( template, values );
	}
}
