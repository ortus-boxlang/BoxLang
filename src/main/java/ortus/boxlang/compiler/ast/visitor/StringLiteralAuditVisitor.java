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
package ortus.boxlang.compiler.ast.visitor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;

import ortus.boxlang.compiler.ast.expression.BoxStringInterpolation;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;

/**
 * 
 * THIS IS A TEST CLASS WHICH IS ONLY USED FOR DEBUGGING.
 * IT'S NOT INTEDED TO BE USER FACING AND MAY BE REMOVED AT ANY TIME.
 * 
 * I am an AST visitor that collects every string literal found in the parsed source,
 * including both standalone {@code BoxStringLiteral} nodes and the literal segments
 * inside {@code BoxStringInterpolation} nodes.
 *
 * Whitespace characters in the collected strings are replaced with readable
 * placeholders ({@code <space>}, {@code <LF>}, {@code <CR>}, {@code <tab>}, etc.)
 * so the console report is unambiguous.
 */
public class StringLiteralAuditVisitor extends VoidBoxVisitor {

	/**
	 * Thread-safe map of "sanitized string" → occurrence count.
	 */
	private final Map<String, LongAdder> stringCounts = new ConcurrentHashMap<>();

	/**
	 * Returns the collected (sanitized → count) map.
	 *
	 * @return the map
	 */
	public Map<String, LongAdder> getStringCounts() {
		return this.stringCounts;
	}

	/**
	 * Total number of string literals encountered
	 */
	private long totalStrings = 0;

	/**
	 * Returns the total count of string literals processed.
	 *
	 * @return total
	 */
	public long getTotalStrings() {
		return this.totalStrings;
	}

	/**
	 * Visit a standalone string literal.
	 */
	@Override
	public void visit( BoxStringLiteral node ) {
		addString( node.getValue() );
	}

	/**
	 * Visit an interpolated string. We collect the literal segments
	 * ({@code BoxStringLiteral}) from the values list directly, and
	 * manually continue traversal for embedded expressions so nested
	 * string literals are also discovered.
	 */
	@Override
	public void visit( BoxStringInterpolation node ) {
		for ( var child : node.getValues() ) {
			if ( child instanceof BoxStringLiteral literal ) {
				addString( literal.getValue() );
			} else {
				child.accept( this );
			}
		}
	}

	/**
	 * Record one occurrence of {@code raw} after sanitizing whitespace.
	 */
	private void addString( String raw ) {
		String sanitized = sanitize( raw );
		this.stringCounts.computeIfAbsent( sanitized, k -> new LongAdder() ).increment();
		this.totalStrings++;
	}

	/**
	 * Replace common whitespace characters with readable placeholders.
	 * <ul>
	 * <li>{@code } (space) → {@code <space>}</li>
	 * <li>{@code \n} → {@code <LF>}</li>
	 * <li>{@code \r} → {@code <CR>}</li>
	 * <li>{@code \t} → {@code <tab>}</li>
	 * <li>{@code \f} → {@code <FF>}</li>
	 * </ul>
	 *
	 * @param input the raw string value
	 *
	 * @return the sanitized string
	 */
	public static String sanitize( String input ) {
		String result = input
		    .replace( " ", "<space>" )
		    .replace( "\t", "<tab>" )
		    .replace( "\n", "<LF>" )
		    .replace( "\r", "<CR>" )
		    .replace( "\f", "<FF>" );
		if ( result.isEmpty() ) {
			return "<empty string>";
		}
		return result;
	}
}