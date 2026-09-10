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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum representing the separator style between keys and values in struct literals.
 * Each value defines the exact string used to separate a key from its value.
 *
 * <pre>
 * // COLON: {key:"value"}
 * // EQUALS: {key="value"}
 * // COLON_SPACE: {key: "value"} (default)
 * // COLON_BOTH_SPACE: {key : "value"}
 * // EQUALS_SPACE: {key= "value"}
 * // EQUALS_BOTH_SPACE:{key = "value"}
 * </pre>
 */
public enum Separator {

	/** Colon with no spacing: {@code ":"} */
	@JsonProperty( ":" )
	COLON(":" ),

	/** Equals with no spacing: {@code "="} */
	@JsonProperty( "=" )
	EQUALS("=" ),

	/** Colon with trailing space: {@code ": "} */
	@JsonProperty( ": " )
	COLON_SPACE(": " ),

	/** Colon with spaces on both sides: {@code " : "} */
	@JsonProperty( " : " )
	COLON_BOTH_SPACE(" : " ),

	/** Equals with trailing space: {@code "= "} */
	@JsonProperty( "= " )
	EQUALS_SPACE("= " ),

	/** Equals with spaces on both sides: {@code " = "} */
	@JsonProperty( " = " )
	EQUALS_BOTH_SPACE(" = " );

	private final String symbol;

	/**
	 * Construct a Separator with the given symbol string.
	 *
	 * @param symbol the separator string
	 */
	Separator( String symbol ) {
		this.symbol = symbol;
	}

	/**
	 * Get the separator symbol string.
	 *
	 * @return the separator string
	 */
	@JsonValue
	public String getSymbol() {
		return symbol;
	}
}
