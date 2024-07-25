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
package ortus.boxlang.runtime.bifs.global.stream;

import java.util.stream.Stream;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.util.ListUtil;

@BoxMember( type = BoxLangType.STREAM )
public class ToBXList extends BIF {

	/**
	 * Constructor
	 */
	public ToBXList() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "stream", Key.stream ),
		    new Argument( false, "string", Key.delimiter, "," )
		};
	}

	/**
	 * Collect a Java stream into a BoxLang delimited list. Each item in the stream will cast
	 * to a string and then be joined with the delimiter.
	 * 
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 * 
	 * @argument.stream The stream to collect.
	 * 
	 * @argument.delimiter The delimiter to use.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Stream<?>	stream		= arguments.getAsStream( Key.stream );
		String		delimiter	= arguments.getAsString( Key.delimiter );
		return ListUtil.asString( Array.fromList( stream.toList() ), delimiter );
	}

}
