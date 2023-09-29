package ortus.boxlang.transpiler.transformer.expression;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.expression.BoxNewOperation;
import ortus.boxlang.ast.statement.BoxThrow;
import ortus.boxlang.transpiler.BoxLangTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;
import ortus.boxlang.transpiler.transformer.statement.BoxThrowTransformer;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class BoxNewOperationTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxThrowTransformer.class );

	/**
	 * Transform a new expression
	 *
	 * @param node    a BoxNewOperation instance
	 * @param context transformation context
	 *
	 * @return Generates a throw
	 *
	 * @throws IllegalStateException
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxNewOperation		boxNew		= ( BoxNewOperation ) node;
		Expression			expr		= ( Expression ) BoxLangTranspiler.transform( boxNew.getExpression(), TransformerContext.RIGHT );

		String				args		= boxNew.getArguments().stream()
		    .map( it -> resolveScope( BoxLangTranspiler.transform( it ), context ).toString() )
		    .collect( Collectors.joining( ", " ) );

		Map<String, String>	values		= new HashMap<>() {

											{
												put( "expr", expr.toString() );
												put( "args", args );

											}
										};
		String				template	= "new ${expr}(${args})";
		Node				javaStmt	= parseExpression( template, values );
		logger.info( node.getSourceText() + " -> " + javaStmt );
		addIndex( javaStmt, node );
		return javaStmt;

	}
}
