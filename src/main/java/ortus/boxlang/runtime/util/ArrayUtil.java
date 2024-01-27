package ortus.boxlang.runtime.util;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.function.IntPredicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.interop.DynamicJavaInteropService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Function;

public class ArrayUtil {

	private static final String	fn_Split				= "split";
	private static final String	fn_splitWholePreserve	= "splitByWholeSeparatorPreserveAllTokens";
	private static final String	fn_splitWhole			= "splitByWholeSeparator";
	private static final String	fn_splitPreserve		= "splitPreserveAllTokens";

	public static Array ofList(
	    String list,
	    String delimiter,
	    Boolean includeEmpty,
	    Boolean wholeDelimiter ) {

		String utilFn = fn_Split;
		if ( wholeDelimiter ) {
			if ( includeEmpty ) {
				utilFn = fn_splitWholePreserve;
			} else {
				utilFn = fn_splitWhole;
			}
		} else if ( includeEmpty ) {
			utilFn = fn_splitPreserve;
		}
		return new Array(
		    ( String[] ) DynamicJavaInteropService.invokeStatic( StringUtils.class, utilFn, list, delimiter )
		);
	}

	public static String toList(
	    Array array,
	    String delimiter ) {
		return array.stream()
		    .map( StringCaster::cast )
		    .collect( Collectors.joining( ( delimiter ) ) );
	}

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
