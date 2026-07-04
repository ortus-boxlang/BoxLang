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
package ortus.boxlang.runtime.bifs.global.stringbuilder;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxStringBuilder;

@BoxBIF( description = "Create a new StringBuilder" )
public class StringBuilderNew extends BIF {

	public StringBuilderNew() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, Argument.ANY, Key.value ),
		    new Argument( false, Argument.INTEGER, Key.capacity, 0 )
		};
	}

	/**
	 * Create a new StringBuilder instance or wrap an existing Java StringBuilder.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.value Optional initial value (String) or existing Java StringBuilder to wrap. Empty if not provided.
	 *
	 * @argument.capacity Optional initial capacity for newly created BoxStringBuilder instances.
	 */
	@Override
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		int		capacity	= arguments.getAsInteger( Key.capacity );
		Object	value		= arguments.get( Key.value );
		if ( value != null ) {
			if ( value instanceof java.lang.StringBuilder sb ) {
				return new BoxStringBuilder( sb );
			}
			if ( capacity > 0 ) {
				return new BoxStringBuilder( StringCaster.cast( value ), capacity );
			}
			return new BoxStringBuilder( StringCaster.cast( value ) );
		}
		if ( capacity > 0 ) {
			return new BoxStringBuilder( capacity );
		}
		return new BoxStringBuilder();
	}

}
