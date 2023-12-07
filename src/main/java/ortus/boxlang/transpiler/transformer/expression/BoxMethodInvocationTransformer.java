package ortus.boxlang.transpiler.transformer.expression;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxIdentifier;
import ortus.boxlang.ast.expression.BoxMethodInvocation;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

public class BoxMethodInvocationTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxScopeTransformer.class );

	public BoxMethodInvocationTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxMethodInvocation	invocation	= ( BoxMethodInvocation ) node;
		String				side		= context == TransformerContext.NONE ? "" : "(" + context.toString() + ") ";

		Expression			expr		= ( Expression ) resolveScope( transpiler.transform( invocation.getObj(),
		    TransformerContext.RIGHT ), TransformerContext.RIGHT );

		String				args		= invocation.getArguments().stream()
		    .map( it -> resolveScope( transpiler.transform( it ), context ).toString() )
		    .collect( Collectors.joining( ", " ) );

		Map<String, String>	values		= new HashMap<>() {

											{
												put( "contextName", transpiler.peekContextName() );
											}
										};

		String				target		= null;
		if ( invocation.getName() instanceof BoxIdentifier id ) {
			target = BoxBuiltinRegistry.getInstance().getRegistry().get( id.getName() );
		}

		values.put( "expr", expr.toString() );
		values.put( "args", args );

		String template;

		if ( target != null ) {
			template = "${expr}." + target;
		} else {
			Node accessKey;
			// DotAccess just uses the string directly, array access allows any expression
			if ( invocation.getUsedDotAccess() ) {
				accessKey = createKey( ( ( BoxIdentifier ) invocation.getName() ).getName() );
			} else {
				accessKey = createKey( invocation.getName() );
			}
			values.put( "methodKey", accessKey.toString() );
			template = """
			           Referencer.getAndInvoke(
			             context,
			             ${expr},
			             ${methodKey},
			             new Object[] { ${args} },
			             false
			           )
			           """;
		}
		Node javaExpr = parseExpression( template, values );
		logger.info( side + node.getSourceText() + " -> " + javaExpr );
		addIndex( javaExpr, node );
		return javaExpr;
	}
}
