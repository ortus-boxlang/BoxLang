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
package ortus.boxlang.runtime.types.util;

import java.util.Map;
import java.util.stream.Collector;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.Struct;

public class BLCollector {

	private BLCollector() {
	}

	/**
	 * Returns a Collector that collects the input elements into an Array
	 *
	 * @return the populated array
	 */
	public static Collector<Object, ?, Array> toArray() {
		return Collector.of(
		    Array::new, // supplier
		    Array::add, // accumulator
		    ( left, right ) -> {
			    left.addAll( right );
			    return left;
		    }, // combiner
		    Collector.Characteristics.IDENTITY_FINISH,
		    Collector.Characteristics.CONCURRENT
		);
	}

	/**
	 * Returns a Collector that collects the input elements into an Struct of the default type
	 *
	 * @return The populated Struct
	 */
	public static Collector<Map.Entry<Key, Object>, ?, Struct> toStruct() {
		return Collector.of(
		    Struct::new, // supplier
		    ( struct, entry ) -> struct.put( entry.getKey(), entry.getValue() ), // accumulator
		    ( left, right ) -> {
			    left.putAll( right );
			    return left;
		    }, // combiner
		    Collector.Characteristics.IDENTITY_FINISH,
		    Collector.Characteristics.CONCURRENT,
		    Collector.Characteristics.UNORDERED
		);
	}

	/**
	 * Returns a Collector that collects the input elements into an Struct of the specified type
	 *
	 * @param type The type of the Struct
	 *
	 * @return The populated Struct
	 */
	public static Collector<Map.Entry<Key, Object>, ?, Struct> toStruct( IStruct.TYPES type ) {
		Collector.Characteristics[] characteristics;
		if ( type == IStruct.TYPES.LINKED ) {
			characteristics = new Collector.Characteristics[] { Collector.Characteristics.IDENTITY_FINISH, Collector.Characteristics.CONCURRENT };
		} else {
			characteristics = new Collector.Characteristics[] { Collector.Characteristics.IDENTITY_FINISH, Collector.Characteristics.CONCURRENT,
			    Collector.Characteristics.UNORDERED };
		}
		return Collector.of(
		    () -> new Struct( type ), // supplier
		    ( struct, entry ) -> struct.put( entry.getKey(), entry.getValue() ), // accumulator
		    ( left, right ) -> {
			    left.putAll( right );
			    return left;
		    }, // combiner
		    characteristics
		);
	}

	/**
	 * Returns a Collector that collects the input elements into a Query
	 *
	 * @param newQuery The query to populate with data
	 *
	 * @return the populated query
	 */
	public static Collector<IStruct, Query, Query> toQuery( Query newQuery ) {
		return Collector.of(
		    () -> newQuery, // supplier
		    Query::add, // accumulator
		    ( left, right ) -> {
			    left.addAll( right );
			    return left;
		    }, // combiner
		    Collector.Characteristics.IDENTITY_FINISH,
		    Collector.Characteristics.CONCURRENT
		);
	}

}
