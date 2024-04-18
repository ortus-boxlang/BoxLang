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
package ortus.boxlang.web.scopes;

import java.io.IOException;
import java.util.stream.Collectors;

import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import ortus.boxlang.runtime.scopes.BaseScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Variables scope implementation in BoxLang
 */
public class FormScope extends BaseScope {

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */
	public static final Key name = Key.of( "form" );

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	public FormScope( HttpServerExchange exchange ) {
		super( FormScope.name );

		FormParserFactory	parserFactory	= FormParserFactory.builder().build();
		FormDataParser		parser			= parserFactory.createParser( exchange );

		FormData			formData;

		// If there is no parser for the request content type, this will be null
		if ( parser != null ) {

			try {
				formData = parser.parseBlocking();
			} catch ( IOException e ) {
				throw new BoxRuntimeException( "Could not parse form data", e );
			}
			for ( String key : formData ) {
				this.put(
				    Key.of( key ),
				    formData.get( key )
				        .stream()
				        .filter( f -> !f.isFileItem() )
				        .map( f -> f.getValue() )
				        .collect( Collectors.joining( "," ) )
				);
			}
		}

	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */
}
