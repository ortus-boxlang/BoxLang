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

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.util.BLCollector;

@BoxMember( type = BoxLangType.STREAM )
public class ToBXStruct extends BIF {

	/**
	 * Constructor
	 */
	public ToBXStruct() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "stream", Key.stream ),
		    new Argument( false, "string", Key.type, "default" )
		};
	}

	/**
	 * Collect a Java stream into a BoxLang Struct. The stream must be a stream of <code>Map.Entry</code> instances, for example
	 * <code>myStruct.entrySet().stream().toBXStruct()</code>. If the same key appears more than once, the last value wins. A stream of
	 * anything else fails when it is collected. The stream is consumed by this call.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.stream The Java stream of Map.Entry instances to collect. This is the object the member function is called on.
	 *
	 * @argument.type The type of struct to create: "default", "ordered" (insertion order), "sorted", "casesensitive", "ordered-casesensitive", "soft" or
	 *                "weak". Defaults to "default". An unknown type throws a BoxRuntimeException.
	 *
	 * @return A BoxLang Struct of the requested type with the entries of the stream.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		@SuppressWarnings( "unchecked" )
		// If this is not a stream of Map.Entry, then the cast will fail
		Stream<Map.Entry<Key, Object>>	stream	= ( Stream<Entry<Key, Object>> ) arguments.getAsStream( Key.stream );
		IStruct.TYPES					type	= IStruct.TYPES.fromString( arguments.getAsString( Key.type ) );
		return stream.collect( BLCollector.toStruct( type ) );
	}

}
