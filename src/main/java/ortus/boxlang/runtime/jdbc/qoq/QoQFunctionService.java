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
package ortus.boxlang.runtime.jdbc.qoq;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.QueryColumnType;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * I handle executing functions in query of queries
 */
public class QoQFunctionService {

	private static Map<Key, QoQFunction> functions = new HashMap<Key, QoQFunction>();

	static {
		register( Key.of( "upper" ), args -> StringCaster.cast( args.get( 0 ) ).toUpperCase(), QueryColumnType.VARCHAR );
	}

	private QoQFunctionService() {

	}

	public static void register( Key name, java.util.function.Function<List<Object>, Object> function, QueryColumnType returnType ) {
		functions.put( name, QoQFunction.of( function, returnType ) );
	}

	public static void unregister( Key name ) {
		functions.remove( name );
	}

	public static QoQFunction getFunction( Key name ) {
		if ( !functions.containsKey( name ) ) {
			throw new BoxRuntimeException( "Function [" + name + "] not found" );
		}
		return functions.get( name );
	}

	public record QoQFunction( java.util.function.Function<List<Object>, Object> callable, QueryColumnType returnType ) {

		static QoQFunction of( java.util.function.Function<List<Object>, Object> callable, QueryColumnType returnType ) {
			return new QoQFunction( callable, returnType );
		}

		public Object invoke( List<Object> arguments ) {
			return callable.apply( arguments );
		}

		public QueryColumnType getReturnType() {
			return returnType;
		}

	}
}
