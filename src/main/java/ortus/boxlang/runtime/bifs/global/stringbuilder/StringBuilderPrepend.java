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
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.BoxStringBuilder;

@BoxBIF( description = "Inserts the string representation of value at the beginning of the StringBuilder buffer. Returns the StringBuilder for chaining." )
@BoxMember( type = BoxLangType.STRING_BUILDER_STRICT, name = "prepend" )
public class StringBuilderPrepend extends BIF {

	public StringBuilderPrepend() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING_BUILDER, Key.stringBuilder ),
		    new Argument( true, Argument.ANY, Key.value )
		};
	}

	/**
	 * Inserts the string representation of value at the beginning (position 1) of the buffer.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.stringBuilder The StringBuilder to prepend to.
	 *
	 * @argument.value The value to insert at the start. Coerced to string.
	 */
	@Override
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		BoxStringBuilder sb = arguments.getAsBoxStringBuilder( Key.stringBuilder );
		return sb.prepend( arguments.get( Key.value ) );
	}

}
