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

import java.util.Objects;
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
	 * Collect a Java stream into a BoxLang Query.
	 * Provide a template query to match the columns and types of the stream.
	 * Once the stream collects, it will return a Query object that can be used in BoxLang.
	 *
	 * <h2>Usage</h2>
	 *
	 * <pre>
	 * // Create a query template
	 * templateQuery = queryNew( "id,name,age" );
	 * // Create a stream from an array of structs
	 * stream = [ {id:1, name:"John", age:30}, {id:2, name:"Jane", age:25} ].toStream();
	 * // Convert the stream to a query
	 * result = stream.toBXQuery( templateQuery );
	 * </pre>
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.stream The stream that will be collected into a Query.
	 *
	 * @argument.query The query template that defines the structure of the resulting Query.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		@SuppressWarnings( "unchecked" )
		// If this is not a stream of IStruct, then the cast will fail
		Stream<IStruct> stream = ( Stream<IStruct> ) arguments.getAsStream( Key.stream );
		Objects.requireNonNull( stream, "Stream cannot be null" );
		Query template = arguments.getAsQuery( Key.query );
		return stream.collect( BLCollector.toQuery( template ) );
	}

}
