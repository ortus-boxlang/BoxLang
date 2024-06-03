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
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.StringUtil;

@BoxBIF
@BoxMember( type = BoxLangType.STRING )
public class Slugify extends BIF {

	/**
	 * Constructor
	 */
	public Slugify() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING, Key.string ),
		    new Argument( false, Argument.NUMERIC, Key.maxLength, 0 ),
		    new Argument( false, Argument.STRING, Key.allow, "" )
		};
	}

	/**
	 * Slugify a string for URL safety
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.string The target string
	 *
	 * @argument.maxLength The maximum number of chracters to allow, 0 is all
	 *
	 * @argument.allow A regex safe list of additional characters to allow. The default is <code>[^a-z0-9]</code>
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		return StringUtil.slugify(
		    arguments.getAsString( Key.string ),
		    IntegerCaster.cast( arguments.getAsDouble( Key.maxLength ) ),
		    arguments.getAsString( Key.allow )
		);
	}
}
