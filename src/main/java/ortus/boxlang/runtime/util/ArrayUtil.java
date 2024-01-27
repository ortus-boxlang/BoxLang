package ortus.boxlang.runtime.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.IntPredicate;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Function;

public class ArrayUtil {

	public static Array filter(
	    Array array,
	    Function callback,
	    IBoxContext callbackContext,
	    Boolean parallel,
	    Integer maxThreads ) {

		IntPredicate	test	= idx -> ( boolean ) callbackContext.invokeFunction( callback,
		    new Object[] { array.get( idx ), idx + 1, array } );

		ForkJoinPool	pool	= null;
		if ( parallel ) {
			pool = new ForkJoinPool( maxThreads );
		}

		return ArrayCaster.cast(
		    pool == null
		        ? array.intStream()
		            .filter( test )
		            .mapToObj( idx -> array.get( idx ) )
		            .toArray()

		        : CompletableFuture.supplyAsync(
		            () -> array.intStream().parallel().filter( test ).mapToObj( idx -> array.get( idx ) ),
		            pool
		        ).join().toArray()
		);
	}

}
