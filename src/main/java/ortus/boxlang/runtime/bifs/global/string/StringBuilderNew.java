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
package ortus.boxlang.runtime.bifs.global.string;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxStringBuilder;

@BoxBIF( description = "Creates and returns a new mutable StringBuilder instance, optionally seeded with an initial string value and/or capacity." )
public class StringBuilderNew extends BIF {

	private static final Key	keyInitialValue	= Key.of( "initialValue" );
	private static final Key	keyCapacity		= Key.of( "capacity" );

	public StringBuilderNew() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( false, Argument.STRING, keyInitialValue, "" ),
		    new Argument( false, Argument.INTEGER, keyCapacity, 0 )
		};
	}

	/**
	 * Creates a new StringBuilder, optionally seeded with an initial value and/or initial buffer capacity.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.initialValue Optional string to seed the buffer with. Defaults to empty string.
	 *
	 * @argument.capacity Optional initial internal buffer capacity in characters. Defaults to the Java StringBuilder default.
	 */
	@Override
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	initialValue	= arguments.getAsString( keyInitialValue );
		int		capacity		= arguments.getAsInteger( keyCapacity );
		if ( capacity > 0 ) {
			return new BoxStringBuilder( initialValue, capacity );
		}
		return new BoxStringBuilder( initialValue );
	}

}
