package ortus.boxlang.transpiler.transformer.statement;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.statement.BoxAssert;
import ortus.boxlang.ast.statement.BoxThrow;
import ortus.boxlang.transpiler.BoxLangTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

import java.util.HashMap;
import java.util.Map;

public class BoxThrowTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxThrowTransformer.class );

	/**
	 * Transform a throw statement
	 *
	 * @param node    a BoxThrow instance
	 * @param context transformation context
	 *
	 * @return Generates a throw
	 *
	 * @throws IllegalStateException
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxThrow			boxAssert	= ( BoxThrow ) node;
		Expression			expr		= ( Expression ) resolveScope( BoxLangTranspiler.transform( boxAssert.getExpression(), TransformerContext.RIGHT ),
		    context );
		Map<String, String>	values		= new HashMap<>() {

											{
												put( "expr", expr.toString() );

											}
										};
		String				template	= "throw (${expr});";
		Node				javaStmt	= parseStatement( template, values );
		logger.info( node.getSourceText() + " -> " + javaStmt );
		addIndex( javaStmt, node );
		return javaStmt;

	}
}
