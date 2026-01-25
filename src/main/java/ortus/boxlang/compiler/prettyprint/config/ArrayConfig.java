package ortus.boxlang.compiler.prettyprint.config;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ArrayConfig {

	private boolean			padding			= false;
	@JsonProperty( "empty_padding" )
	private boolean			emptyPadding	= false;
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

	public boolean getEmptyPadding() {
		return emptyPadding;
	}

	public ArrayConfig setEmptyPadding( boolean emptyPadding ) {
		this.emptyPadding = emptyPadding;
		return this;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "padding", padding );
		map.put( "empty_padding", emptyPadding );
		map.put( "multiline", multiline.toMap() );
		return map;
	}

}
