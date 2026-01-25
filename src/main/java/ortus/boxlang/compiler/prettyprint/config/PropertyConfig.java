package ortus.boxlang.compiler.prettyprint.config;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PropertyConfig {

	private MultilineConfig	multiline	= new MultilineConfig();
	@JsonProperty( "key_value" )
	private KeyValue		keyValue	= new KeyValue();

	public MultilineConfig getMultiline() {
		return multiline;
	}

	public PropertyConfig setMultiline( MultilineConfig multiline ) {
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

	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "multiline", multiline.toMap() );
		map.put( "key_value", keyValue.toMap() );
		return map;
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

		public Map<String, Object> toMap() {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put( "padding", padding );
			return map;
		}
	}

}
