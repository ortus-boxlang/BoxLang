package ortus.boxlang.runtime.types.util;

import java.util.stream.Collector;

import ortus.boxlang.runtime.types.Array;

public class BLCollector {

	private BLCollector() {
	}

	public static Collector<Object, ?, Array> toArray() {
		return Collector.of(
		    Array::new, // supplier
		    Array::add, // accumulator
		    ( left, right ) -> {
			    left.addAll( right );
			    return left;
		    }, // combiner
		    Collector.Characteristics.IDENTITY_FINISH
		);
	}
}