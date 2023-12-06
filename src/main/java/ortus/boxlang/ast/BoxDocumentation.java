package ortus.boxlang.ast;

import java.util.List;

public class BoxDocumentation extends BoxNode {

	private final List<BoxNode> annotations;

	/**
	 * Create a instance of a BoxDocumentation
	 *
	 * @param annotations
	 * @param position    position within the source code
	 * @param sourceText  source code
	 */
	public BoxDocumentation( List<BoxNode> annotations, Position position, String sourceText ) {
		super( position, sourceText );
		this.annotations = annotations;
	}

	public List<BoxNode> getAnnotations() {
		return annotations;
	}
}
