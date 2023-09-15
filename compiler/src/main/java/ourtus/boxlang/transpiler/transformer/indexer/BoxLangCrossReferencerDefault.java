package ourtus.boxlang.transpiler.transformer.indexer;

import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.DataKey;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.stmt.Statement;
import ourtus.boxlang.ast.BoxNode;

import static ourtus.boxlang.transpiler.transformer.indexer.BoxNodeKey.BOX_NODE_DATA_KEY;

/**
 * Cross Reference default implementation
 */
public class BoxLangCrossReferencerDefault extends BoxLangCrossReferencer {

	/**
	 * Creates the cross-reference between the Java AST node and BoxNone
	 * @param javaNode Java Parser AST Node
	 * @param boxNode  BoxLang AST Node
	 *
	 * @return the Java AST with a BoxNode in the data collection
	 */
	public Node storeReference( Node javaNode, ourtus.boxlang.ast.BoxNode boxNode ) {
		if (this.enabled) {
			if (javaNode instanceof Statement) {
				javaNode.setData(BOX_NODE_DATA_KEY, boxNode);
			}
		}
		return javaNode;
	}
}
