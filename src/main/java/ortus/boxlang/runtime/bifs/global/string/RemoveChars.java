
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

import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.ListUtil;

@BoxBIF
@BoxMember( type = BoxLangType.STRING, name = "RemoveChars" )

public class RemoveChars extends BIF {

	/**
	 * Constructor
	 */
	public RemoveChars() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.string ),
		    new Argument( true, "integer", Key.start ),
		    new Argument( true, "integer", Key.count )
		};
	}

	/**
	 * Describe what the invocation of your bif function does
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.foo Describe any expected arguments
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		String			ref		= arguments.getAsString( Key.string );
		Integer			rmStart	= arguments.getAsInteger( Key.start ) - 1;
		Integer			rmEnd	= rmStart + arguments.getAsInteger( Key.count ) - 1;
		Range<Integer>	scope	= Range.of( rmStart, rmEnd );
		return ListUtil.asString(
		    new Array(
		        ListUtil.asList(
		            arguments.getAsString( Key.string ),
		            ""
		        ).intStream().filter( idx -> !scope.contains( idx ) ).mapToObj( idx -> StringUtils.substring( ref, idx, idx + 1 ) ).toArray()
		    ),
		    ""
		);
	}

}
