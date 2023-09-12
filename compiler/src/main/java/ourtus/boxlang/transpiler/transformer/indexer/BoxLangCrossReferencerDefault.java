package ourtus.boxlang.transpiler.transformer.indexer;

import com.github.javaparser.Position;
import com.github.javaparser.Range;
import com.github.javaparser.ast.Node;

public class BoxLangCrossReferencerDefault extends BoxLangCrossReferencer {

	public Node storeReference( Node javaNode, ourtus.boxlang.ast.BoxNode boxNode ) {
		if ( this.enabled ) {
			Range range = new Range(
			    new Position( boxNode.getPosition().getStart().getLine(), boxNode.getPosition().getStart().getColumn() ),
			    new Position( boxNode.getPosition().getEnd().getLine(), boxNode.getPosition().getEnd().getColumn() )
			);
			javaNode.setRange( range );
		}
		return javaNode;
	}

}
