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
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.util.BLCollector;

@BoxMember( type = BoxLangType.STREAM )
public class ToBXQuery extends BIF {

	/**
	 * Constructor
	 */
	public ToBXQuery() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "stream", Key.stream ),
		    new Argument( true, "query", Key.query )
		};
	}

	/**
	 * Collect a Java stream into a BoxLang Query. Provde an empty query to populate.
	 * 
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 * 
	 * @argument.stream The stream to collect.
	 * 
	 * @argument.query The query to populate.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		@SuppressWarnings( "unchecked" )
		// If this is not a stream of IStruct, then the cast will fail
		Stream<IStruct>	stream	= ( Stream<IStruct> ) arguments.getAsStream( Key.stream );
		Query			query	= arguments.getAsQuery( Key.query );
		return stream.collect( BLCollector.toQuery( query ) );
	}

}
