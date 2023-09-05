package ourtus.boxlang.transpiler.transformer.expression;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import ourtus.boxlang.ast.BoxNode;
import ourtus.boxlang.ast.expression.BoxObjectAccess;
import ourtus.boxlang.ast.expression.BoxScope;
import ourtus.boxlang.transpiler.BoxLangTranspiler;
import ourtus.boxlang.transpiler.transformer.AbstractTransformer;
import ourtus.boxlang.transpiler.transformer.TransformerContext;

import java.util.HashMap;
import java.util.Map;

public class BoxObjectAccessTransformer extends AbstractTransformer {

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxObjectAccess objectAccess = ( BoxObjectAccess ) node;
		Expression scope = ( Expression ) BoxLangTranspiler.transform( objectAccess.getContext() );
		Expression variable = ( Expression ) BoxLangTranspiler.transform( objectAccess.getAccess() );

		Map<String, String> values = new HashMap<>() {{
			put( "scope", scope.toString() );
			put( "variable", variable.toString() );
		}};

		String template;

		if ( objectAccess.getContext() instanceof BoxScope ) {
			template = """
				context.getScopeNearby( Key.of( "${scope}" ) ).get( Key.of( "${variable}" ) )
				""";
		} else {
			template = """
				Referencer.get(context.scopeFindNearby(Key.of("${scope}"), null).value(), Key.of("${variable}"), false)
				""";
		}

		template = switch ( context ) {
			case LEFT -> """
							${scope}.put(${variable})
				""";
			case RIGHT -> """
							Referencer.get(context.scopeFindNearby(Key.of("${scope}"), null).value(), Key.of("${variable}"), false)
				""";
			default -> template;
		};

		return parseExpression( template, values );
	}
}
