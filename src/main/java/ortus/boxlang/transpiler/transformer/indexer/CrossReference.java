package ortus.boxlang.transpiler.transformer.indexer;

import com.github.javaparser.Range;
import ortus.boxlang.ast.BoxNode;

public class CrossReference {

	public BoxNode	origin;
	public Range	destination;

	public CrossReference( BoxNode origin, Range destination ) {
		this.origin			= origin;
		this.destination	= destination;
	}
}
