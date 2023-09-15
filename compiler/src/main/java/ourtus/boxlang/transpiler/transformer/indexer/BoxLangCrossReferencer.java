package ourtus.boxlang.transpiler.transformer.indexer;

import com.github.javaparser.ast.Node;

import java.io.File;

/**
 * Cross-reference abstract class
 */
public abstract class BoxLangCrossReferencer {

	protected File		source;
	protected File		destination;
	protected boolean	enabled;

	/**
	 * Store a reference into the index
	 *
	 * @param javaNode Java Parser AST Node
	 * @param boxNode  BoxLang AST Node
	 *
	 * @return
	 */
	public abstract Node storeReference( Node javaNode, ourtus.boxlang.ast.BoxNode boxNode );

	public BoxLangCrossReferencer() {
		this.enabled = true;
	}

	public BoxLangCrossReferencer( File source, File destination ) {
		this.source			= source;
		this.destination	= destination;
		this.enabled		= true;
	}

	public File getSource() {
		return source;
	}

	public void setSource( File source ) {
		this.source = source;
	}

	public File getDestination() {
		return destination;
	}

	public void setDestination( File destination ) {
		this.destination = destination;
	}
}
