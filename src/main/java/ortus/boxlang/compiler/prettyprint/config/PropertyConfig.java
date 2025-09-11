package ortus.boxlang.compiler.prettyprint.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PropertyConfig {

	private Multiline	multiline	= new Multiline();
	@JsonProperty( "key_value" )
	private KeyValue	keyValue	= new KeyValue();

	public Multiline getMultiline() {
		return multiline;
	}

	public PropertyConfig setMultiline( Multiline multiline ) {
		this.multiline = multiline;
		return this;
	}

	public KeyValue getKeyValue() {
		return keyValue;
	}

	public PropertyConfig setKeyValue( KeyValue keyValue ) {
		this.keyValue = keyValue;
		return this;
	}

	public static class Multiline {

		private int	element_count	= 4;
		private int	min_length		= 40;

		public int getElement_count() {
			return element_count;
		}

		public Multiline setElement_count( int element_count ) {
			this.element_count = element_count;
			return this;
		}

		public int getMin_length() {
			return min_length;
		}

		public Multiline setMin_length( int min_length ) {
			this.min_length = min_length;
			return this;
		}
	}

	public static class KeyValue {

		private boolean padding = false;

		public boolean getPadding() {
			return padding;
		}

		public KeyValue setPadding( boolean padding ) {
			this.padding = padding;
			return this;
		}
	}

}
