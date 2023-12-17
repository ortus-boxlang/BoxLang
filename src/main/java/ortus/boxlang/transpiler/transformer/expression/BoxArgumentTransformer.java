package ortus.boxlang.transpiler.transformer.expression;

import java.util.HashMap;
import java.util.Map;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;

import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxArgument;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

/**
 * Transform a BoxArgument Node the equivalent Java Parser AST nodes
 */
public class BoxArgumentTransformer extends AbstractTransformer {

	public BoxArgumentTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	/**
	 * Transform a function/method argument
	 *
	 * @param node    a BoxArgument instance
	 * @param context transformation context
	 *
	 * @return Generates a Java Parser Expression
	 *
	 * @throws IllegalStateException
	 *
	 * @see BoxArgument
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxArgument			arg			= ( BoxArgument ) node;
		String				side		= context == TransformerContext.NONE ? "" : "(" + context.toString() + ") ";
		Expression			expr		= ( Expression ) transpiler.transform( arg.getValue() );
		// TODO handle named parameters
		Map<String, String>	values		= new HashMap<>() {

											{
												put( "expr", expr.toString() );
												put( "contextName", transpiler.peekContextName() );
											}
										};

		String				template	= "${expr}";

		Node				javaExpr	= parseExpression( template, values );
		logger.info( side + node.getSourceText() + " -> " + javaExpr );

		return javaExpr;
	}
}
