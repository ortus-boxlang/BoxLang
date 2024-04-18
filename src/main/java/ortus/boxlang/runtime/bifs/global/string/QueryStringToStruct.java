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
package ortus.boxlang.runtime.bifs.global.string;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.StructUtil;

@BoxBIF
@BoxMember( type = BoxLangType.STRING, name = "toStruct" )
public class QueryStringToStruct extends BIF {

	/**
	 * Constructor
	 */
	public QueryStringToStruct() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.target ),
		    new Argument( false, "string", Key.delimiter, "&" ),
		};
	}

	/**
	 * Convert a query string to a struct.
	 * Each key-value pair in the query string is separated by a delimiter.
	 * The default delimiter is "&".
	 * <p>
	 * Example:
	 *
	 * <pre>
	 * queryStringToStruct( "foo=bar&baz=qux" );
	 * "foo=bar&baz=qux".toStruct();
	 * </pre>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.target The query string to convert.
	 *
	 * @argument.delimiter The delimiter used to separate key-value pairs in the query string.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	target		= arguments.getAsString( Key.target );
		String	delimiter	= arguments.getAsString( Key.delimiter );

		return StructUtil.fromQueryString( target, delimiter );
	}
}
