package ortus.boxlang.transpiler.transformer.statement;

import com.github.javaparser.ast.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.statement.BoxRethrow;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Transform a Rethrow statement in the equivalent Java Parser AST nodes
 */
public class BoxRethrowTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxRethrowTransformer.class );

	public BoxRethrowTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxRethrow			boxRethrow	= ( BoxRethrow ) node;

		String				template	= "${contextName}.rethrow();";
		Map<String, String>	values		= new HashMap<>() {

											{
												put( "contextName", transpiler.peekContextName() );
											}
										};
		Node				javaStmt	= parseStatement( template, values );
		logger.info( node.getSourceText() + " -> " + javaStmt );
		addIndex( javaStmt, node );
		return javaStmt;
	}
}
