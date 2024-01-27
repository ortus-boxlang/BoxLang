package ortus.boxlang.runtime.util;

import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.interop.DynamicJavaInteropService;
import ortus.boxlang.runtime.types.Array;

public class StringUtil {

	private static final String	fn_Split				= "split";
	private static final String	fn_splitWholePreserve	= "splitByWholeSeparatorPreserveAllTokens";
	private static final String	fn_splitWhole			= "splitByWholeSeparator";
	private static final String	fn_splitPreserve		= "splitPreserveAllTokens";

	public static Array toArray(
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

}
