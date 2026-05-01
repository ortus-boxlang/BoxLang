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
 * Configuration options for embedded SQL formatting within {@code queryExecute} and
 * {@code <bx:query>} blocks.
 */
public class SqlConfig {

	/**
	 * Convert SQL keywords to uppercase.
	 *
	 * <pre>
	 * // uppercaseKeywords: true (default)
	 * SELECT name FROM users WHERE id = :id
	 *
	 * // uppercaseKeywords: false
	 * select name from users where id = :id
	 * </pre>
	 */
	@JsonProperty( "uppercase_keywords" )
	private boolean	uppercaseKeywords	= true;

	/**
	 * Indent SQL clauses ({@code SELECT}, {@code FROM}, {@code WHERE}, etc.)
	 * relative to the opening tag or function call.
	 *
	 * <pre>
	 * // indentClauses: true (default)
	 * queryExecute( "
	 *     SELECT name
	 *     FROM users
	 *     WHERE id = :id
	 * " );
	 *
	 * // indentClauses: false
	 * queryExecute( "
	 * SELECT name
	 * FROM users
	 * WHERE id = :id
	 * " );
	 * </pre>
	 */
	@JsonProperty( "indent_clauses" )
	private boolean	indentClauses		= true;

	/** Default constructor. */
	public SqlConfig() {
	}

	/**
	 * Get whether SQL keywords are uppercased.
	 *
	 * @return true if keywords are uppercased
	 */
	public boolean getUppercaseKeywords() {
		return uppercaseKeywords;
	}

	/**
	 * Set whether SQL keywords are uppercased.
	 *
	 * @param uppercaseKeywords true to uppercase keywords
	 *
	 * @return this config for chaining
	 */
	public SqlConfig setUppercaseKeywords( boolean uppercaseKeywords ) {
		this.uppercaseKeywords = uppercaseKeywords;
		return this;
	}

	/**
	 * Get whether SQL clauses are indented.
	 *
	 * @return true if clause indentation is enabled
	 */
	public boolean getIndentClauses() {
		return indentClauses;
	}

	/**
	 * Set whether SQL clauses are indented.
	 *
	 * @param indentClauses true to indent clauses
	 *
	 * @return this config for chaining
	 */
	public SqlConfig setIndentClauses( boolean indentClauses ) {
		this.indentClauses = indentClauses;
		return this;
	}

	/**
	 * Convert this configuration to a map for JSON serialization.
	 *
	 * @return a map representation of this configuration
	 */
	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "uppercase_keywords", uppercaseKeywords );
		map.put( "indent_clauses", indentClauses );
		return map;
	}
}
