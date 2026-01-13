package ortus.boxlang.compiler.prettyprint.config;

public class ForLoopSemicolons {

	private boolean padding = true;

	public ForLoopSemicolons() {
	}

	public boolean getPadding() {
		return padding;
	}

	public ForLoopSemicolons setPadding( boolean padding ) {
		this.padding = padding;
		return this;
	}
}
