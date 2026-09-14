/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.compiler.parser;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;

/**
 * Base class for the generated {@code GroovyGrammar} parser (see {@code GroovyGrammar.g4}'s
 * {@code superClass} option). Mirrors {@link CFParserControl}'s role for the CF grammar.
 * <p>
 * Phase 1 of the Groovy grammar doesn't yet need any semantic predicates (unlike CFParserControl,
 * which resolves CFML's component-vs-identifier ambiguity), so this is currently a thin base.
 * Predicates for Groovy-specific ambiguities (e.g. command-style calls) will land here as the
 * grammar grows in later phases.
 */
public abstract class GroovyParserControl extends Parser {

	public GroovyParserControl( TokenStream input ) {
		super( input );
	}

}
