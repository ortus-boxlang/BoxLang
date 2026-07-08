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
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.BoxStringBuilder;

@BoxBIF( description = "Removes characters from start to end (1-based, inclusive) from the StringBuilder buffer. Returns the StringBuilder for chaining." )
@BoxMember( type = BoxLangType.STRING_BUILDER_STRICT, name = "delete" )
public class StringBuilderDelete extends BIF {

	public StringBuilderDelete() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.STRING_BUILDER, Key.stringBuilder ),
		    new Argument( true, Argument.INTEGER, Key.start ),
		    new Argument( true, Argument.INTEGER, Key.end )
		};
	}

	/**
	 * Removes characters from start to end, both 1-based and inclusive.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.stringBuilder The StringBuilder to delete from.
	 *
	 * @argument.start The 1-based start position (inclusive).
	 *
	 * @argument.end The 1-based end position (inclusive).
	 */
	@Override
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		BoxStringBuilder	sb		= arguments.getAsBoxStringBuilder( Key.stringBuilder );
		int					start	= IntegerCaster.cast( arguments.get( Key.start ) );
		int					end		= IntegerCaster.cast( arguments.get( Key.end ) );
		return sb.delete( start, end );
	}

}
