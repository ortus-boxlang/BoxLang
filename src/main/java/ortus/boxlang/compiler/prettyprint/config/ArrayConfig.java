package ortus.boxlang.compiler.prettyprint.config;

public class ArrayConfig {

	private boolean			padding			= false;
	private boolean			empty_padding	= false;
	private MultilineConfig	multiline		= new MultilineConfig();

	public MultilineConfig getMultiline() {
		return multiline;
	}

	public ArrayConfig setMultiline( MultilineConfig multiline ) {
		this.multiline = multiline;
		return this;
	}

	public boolean getPadding() {
		return padding;
	}

	public ArrayConfig setPadding( boolean padding ) {
		this.padding = padding;
		return this;
	}

	public boolean getEmpty_padding() {
		return empty_padding;
	}

	public ArrayConfig setEmpty_padding( boolean empty_padding ) {
		this.empty_padding = empty_padding;
		return this;
	}

}
