package ortus.boxlang.runtime.types.util;

import java.util.Map;
import java.util.stream.Collector;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
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

}