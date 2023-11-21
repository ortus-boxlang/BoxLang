package ortus.boxlang.transpiler.transformer.statement;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.statement.BoxImport;
import ortus.boxlang.transpiler.JavaTranspiler;
import ortus.boxlang.transpiler.transformer.AbstractTransformer;
import ortus.boxlang.transpiler.transformer.TransformerContext;

import java.util.HashMap;
import java.util.Map;

public class BoxImportTransformer extends AbstractTransformer {

	Logger logger = LoggerFactory.getLogger( BoxImportTransformer.class );

	public BoxImportTransformer( JavaTranspiler transpiler ) {
		super( transpiler );
	}

	/**
	 * Transform an import statement
	 *
	 * @param node    a BoxImport instance
	 * @param context transformation context
	 *
	 * @return Generates an entry in the list of import
	 *
	 * @throws IllegalStateException
	 */
	@Override
	public Node transform( BoxNode node, TransformerContext context ) throws IllegalStateException {
		BoxImport			boxImport	= ( BoxImport ) node;
		Expression			namespace	= ( Expression ) transpiler.transform( boxImport.getExpression(), TransformerContext.RIGHT );
		String				alias		= boxImport.getAlias() != null
		    ? " as " + transpiler.transform( boxImport.getAlias(), TransformerContext.RIGHT ).toString()
		    : "";

		Map<String, String>	values		= new HashMap<>() {

											{
												put( "namespace", namespace.toString() + alias );
												put( "contextName", transpiler.peekContextName() );

											}
										};
		String				template	= "ImportDefinition.parse( \"${namespace}\" )";

		Node				javaStmt	= parseExpression( template, values );
		logger.info( node.getSourceText() + " -> " + javaStmt );
		addIndex( javaStmt, node );
		return javaStmt;
	}
}
