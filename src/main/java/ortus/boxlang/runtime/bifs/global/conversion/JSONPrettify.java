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
package ortus.boxlang.runtime.bifs.global.conversion;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.StringUtil;

@BoxBIF
@BoxMember( type = BoxLangType.STRING_STRICT, name = "JSONPrettify" )
public class JSONPrettify extends BIF {

	/**
	 * Constructor
	 */
	public JSONPrettify() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key.var )
		};
	}

	/**
	 * Prettifies a JSON string.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.var The JSON string to prettify.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return StringUtil.prettyJson( arguments.getAsString( Key.var ) );
	}
}
