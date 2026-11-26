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

@BoxBIF
@BoxMember( type = BoxLangType.STRING_BUILDER_STRICT, name = "mid" )
public class StringBuilderMid extends BIF {

	public StringBuilderMid() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING_BUILDER, Key.stringBuilder ),
		    new Argument( true, Argument.INTEGER, Key.start ),
		    new Argument( false, Argument.INTEGER, Key.count )
		};
	}

	/**
	 * Returns a segment from the middle of a StringBuilder as a string, without modifying the buffer, using 1-based indexing. A start
	 * less than 1 is treated as 1; a start beyond the buffer length returns an empty string.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.stringBuilder The StringBuilder to read from.
	 *
	 * @argument.start The 1-based start position.
	 *
	 * @argument.count The number of characters to return. If omitted, returns everything from start to the end of the buffer.
	 *
	 * @return The extracted substring.
	 */
	@Override
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		BoxStringBuilder sb = arguments.getAsBoxStringBuilder( Key.stringBuilder );
		return sb.mid( arguments.getAsInteger( Key.start ), arguments.getAsInteger( Key.count ) );
	}

}