/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.compiler.prettyprint.config;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration options for {@code property} declaration formatting within classes and components.
 */
public class PropertyConfig {

	/**
	 * Multiline formatting thresholds for property annotations. Controls when
	 * property attributes are printed on separate lines vs a single line.
	 *
	 * @see MultilineConfig
	 */
	private MultilineConfig	multiline	= new MultilineConfig();

	/**
	 * Configuration for key-value pair formatting within property declarations.
	 */
	@JsonProperty( "key_value" )
	private KeyValue		keyValue	= new KeyValue();

	/**
	 * Get the multiline formatting configuration for properties.
	 *
	 * @return the multiline configuration
	 */
	public MultilineConfig getMultiline() {
		return multiline;
	}

	/**
	 * Set the multiline formatting configuration for properties.
	 *
	 * @param multiline the multiline configuration to set
	 *
	 * @return this config for chaining
	 */
	public PropertyConfig setMultiline( MultilineConfig multiline ) {
		this.multiline = multiline;
		return this;
	}

	/**
	 * Get the key-value pair formatting configuration.
	 *
	 * @return the key-value configuration
	 */
	public KeyValue getKeyValue() {
		return keyValue;
	}

	/**
	 * Set the key-value pair formatting configuration.
	 *
	 * @param keyValue the key-value configuration to set
	 *
	 * @return this config for chaining
	 */
	public PropertyConfig setKeyValue( KeyValue keyValue ) {
		this.keyValue = keyValue;
		return this;
	}

	/**
	 * Convert this configuration to a map for JSON serialization.
	 *
	 * @return a map representation of this configuration
	 */
	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "multiline", multiline.toMap() );
		map.put( "key_value", keyValue.toMap() );
		return map;
	}

	/**
	 * Configuration for key-value formatting in property attributes.
	 */
	public static class KeyValue {

		/**
		 * Add spaces around the {@code =} sign in property attributes.
		 *
		 * <pre>
		 * // padding: true
		 * property name = "id" type = "numeric";
		 *
		 * // padding: false (default)
		 * property name="id" type="numeric";
		 * </pre>
		 */
		private boolean padding = false;

		/**
		 * Get whether spaces are added around the equals sign.
		 *
		 * @return true if padding is enabled
		 */
		public boolean getPadding() {
			return padding;
		}

		/**
		 * Set whether spaces are added around the equals sign.
		 *
		 * @param padding true to enable padding
		 *
		 * @return this config for chaining
		 */
		public KeyValue setPadding( boolean padding ) {
			this.padding = padding;
			return this;
		}

		/**
		 * Convert this configuration to a map for JSON serialization.
		 *
		 * @return a map representation of this configuration
		 */
		public Map<String, Object> toMap() {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put( "padding", padding );
			return map;
		}
	}

}
