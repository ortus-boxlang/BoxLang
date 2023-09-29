package ortus.boxlang.transpiler.transformer.indexer;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.Statement;
import static ortus.boxlang.transpiler.transformer.indexer.BoxNodeKey.BOX_NODE_DATA_KEY;

/**
 * Cross Reference default implementation
 */
public class BoxLangCrossReferencerDefault extends BoxLangCrossReferencer {

	/**
	 * Creates the cross-reference between the Java AST node and BoxNone
	 *
	 * @param javaNode Java Parser AST Node
	 * @param boxNode  BoxLang AST Node
	 *
	 * @return the Java AST with a BoxNode in the data collection
	 */
	public Node storeReference( Node javaNode, ortus.boxlang.ast.BoxNode boxNode ) {
		if ( this.enabled ) {
			if ( javaNode instanceof Statement ) {
				javaNode.setData( BOX_NODE_DATA_KEY, boxNode );
			}
		}
		return javaNode;
	}
}
