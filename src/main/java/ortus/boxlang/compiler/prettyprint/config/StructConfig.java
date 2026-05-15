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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSON.Feature;

/**
 * Configuration options for struct literal formatting.
 */
public class StructConfig {

	/**
	 * Add spaces inside the curly braces of struct literals that contain values.
	 * When flat, adds a space; when broken across lines, adds a newline.
	 *
	 * <pre>
	 * // padding: true
	 * data = { key: "value" };
	 *
	 * // padding: false (default)
	 * data = {key: "value"};
	 * </pre>
	 */
	private boolean			padding			= true;

	/**
	 * Add a space inside empty struct literals and ordered struct markers.
	 *
	 * <pre>
	 * // emptyPadding: true
	 * data = { };
	 * ordered = [ : ];
	 *
	 * // emptyPadding: false (default)
	 * data = {};
	 * ordered = [:];
	 * </pre>
	 */
	@JsonProperty( "empty_padding" )
	private boolean			emptyPadding	= false;

	/**
	 * Quote the keys in struct literals using the configured quote style
	 * ({@code singleQuote} setting on the parent {@link Config}).
	 *
	 * <pre>
	 * // quoteKeys: true
	 * data = { "name": "Brad", "age": 30 };
	 *
	 * // quoteKeys: false (default)
	 * data = { name: "Brad", age: 30 };
	 * </pre>
	 */
	@JsonProperty( "quote_keys" )
	private boolean			quoteKeys		= false;

	/**
	 * The separator character(s) between keys and values in struct literals.
	 * Controls spacing around the colon or equals sign.
	 *
	 * <pre>
	 * // separator: ": " (default)
	 * { name: "Brad" }
	 *
	 * // separator: " = "
	 * { name = "Brad" }
	 *
	 * // separator: " : "
	 * { name : "Brad" }
	 * </pre>
	 *
	 * @see Separator
	 */
	private Separator		separator		= Separator.COLON_BOTH_SPACE;

	/**
	 * Multiline formatting thresholds for struct literals. Controls when structs
	 * switch from single-line to multi-line formatting based on element count
	 * or total length.
	 *
	 * @see MultilineConfig
	 */
	private MultilineConfig	multiline		= new MultilineConfig();

	/** Default constructor. */
	public StructConfig() {
		this.multiline.setElementCount( 2 );
		this.multiline.setMinLength( 60 );
	}

	/**
	 * Get the multiline formatting configuration for struct literals.
	 *
	 * @return the multiline configuration
	 */
	public MultilineConfig getMultiline() {
		return multiline;
	}

	/**
	 * Set the multiline formatting configuration for struct literals.
	 *
	 * @param multiline the multiline configuration to set
	 *
	 * @return this config for chaining
	 */
	public StructConfig setMultiline( MultilineConfig multiline ) {
		this.multiline = multiline;
		return this;
	}

	/**
	 * Get the key-value separator for struct literals.
	 *
	 * @return the separator
	 */
	public Separator getSeparator() {
		return separator;
	}

	/**
	 * Set the key-value separator for struct literals.
	 *
	 * @param separator the separator to use
	 *
	 * @return this config for chaining
	 */
	public StructConfig setSeparator( Separator separator ) {
		this.separator = separator;
		return this;
	}

	/**
	 * Get whether struct keys should be quoted.
	 *
	 * @return true if keys should be quoted
	 */
	public boolean getQuoteKeys() {
		return quoteKeys;
	}

	/**
	 * Set whether struct keys should be quoted.
	 *
	 * @param quoteKeys true to quote keys
	 *
	 * @return this config for chaining
	 */
	public StructConfig setQuoteKeys( boolean quoteKeys ) {
		this.quoteKeys = quoteKeys;
		return this;
	}

	/**
	 * Get whether spaces are added inside struct braces.
	 *
	 * @return true if padding is enabled
	 */
	public boolean getPadding() {
		return padding;
	}

	/**
	 * Set whether spaces are added inside struct braces.
	 *
	 * @param padding true to enable padding
	 *
	 * @return this config for chaining
	 */
	public StructConfig setPadding( boolean padding ) {
		this.padding = padding;
		return this;
	}

	/**
	 * Get whether spaces are added inside empty struct braces.
	 *
	 * @return true if empty padding is enabled
	 */
	public boolean getEmptyPadding() {
		return emptyPadding;
	}

	/**
	 * Set whether spaces are added inside empty struct braces.
	 *
	 * @param emptyPadding true to enable empty padding
	 *
	 * @return this config for chaining
	 */
	public StructConfig setEmptyPadding( boolean emptyPadding ) {
		this.emptyPadding = emptyPadding;
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
		map.put( "empty_padding", emptyPadding );
		map.put( "quote_keys", quoteKeys );
		map.put( "separator", separator.getSymbol() );
		map.put( "multiline", multiline.toMap() );
		return map;
	}

	/**
	 * Convert this configuration to a JSON string.
	 *
	 * @return a formatted JSON string
	 */
	public String toJSON() {
		try {
			return JSON.std.with( Feature.PRETTY_PRINT_OUTPUT, Feature.WRITE_NULL_PROPERTIES )
			    .asString( toMap() );
		} catch ( IOException e ) {
			e.printStackTrace();
			throw new RuntimeException( "Failed to convert to JSON", e );
		}
	}

	/**
	 * Create a deep copy of this configuration.
	 *
	 * @return a new StructConfig with the same settings
	 */
	public StructConfig clone() {
		StructConfig clone = new StructConfig();
		clone.padding		= this.padding;
		clone.emptyPadding	= this.emptyPadding;
		clone.quoteKeys		= this.quoteKeys;
		clone.separator		= this.separator;
		clone.multiline		= this.multiline.clone();
		return clone;
	}
}