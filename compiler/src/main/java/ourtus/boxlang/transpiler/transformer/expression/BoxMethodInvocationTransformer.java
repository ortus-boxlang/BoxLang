package ourtus.boxlang.transpiler.transformer.expression;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;

import ourtus.boxlang.ast.BoxNode;
import ourtus.boxlang.ast.expression.BoxMethodInvocation;
import ourtus.boxlang.transpiler.BoxLangTranspiler;
import ourtus.boxlang.transpiler.transformer.AbstractTransformer;
import ourtus.boxlang.transpiler.transformer.TransformerContext;

public class BoxMethodInvocationTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxScopeTransformer.class );

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxMethodInvocation	invocation	= ( BoxMethodInvocation ) node;
		String				side		= context == TransformerContext.NONE ? "" : "(" + context.toString() + ") ";

		Expression			expr		= ( Expression ) BoxLangTranspiler.transform( invocation.getObj(),
		    TransformerContext.RIGHT );

		String				args		= invocation.getArguments().stream()
		    .map( it -> BoxLangTranspiler.transform( it ).toString() )
		    .collect( Collectors.joining( ", " ) );

		Map<String, String>	values		= new HashMap<>();

		String				target		= BoxBuiltinRegistry.getInstance().getRegistry().get( invocation.getName().getName() );

		values.put( "expr", expr.toString() );
		values.put( "args", args );

		String template;

		if ( target != null ) {
			template = "${expr}." + target;
		} else {
			values.put( "method", invocation.getName().getName().toString() );
			template = """
			           Referencer.getAndInvoke(
			             context,
			             ${expr},
			             Key.of( "${method}" ),
			             new Object[] { ${args} },
			             false
			           )
			           """;
		}
		Node javaExpr = parseExpression( template, values );
		logger.info( side + node.getSourceText() + " -> " + javaExpr );
		return javaExpr;
	}
}
