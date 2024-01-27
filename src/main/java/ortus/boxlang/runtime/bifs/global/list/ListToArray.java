
package ortus.boxlang.runtime.bifs.global.list;

import org.apache.commons.lang3.StringUtils;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicJavaInteropService;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;

@BoxBIF

public class ListToArray extends BIF {

	private static final String	fn_Split				= "split";
	private static final String	fn_splitWholePreserve	= "splitByWholeSeparatorPreserveAllTokens";
	private static final String	fn_splitWhole			= "splitByWholeSeparator";
	private static final String	fn_splitPreserve		= "splitPreserveAllTokens";

	/**
	 * Constructor
	 */
	public ListToArray() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "string", Key.list ),
		    new Argument( false, "string", Key.delimiter, "," ),
		    new Argument( false, "boolean", Key.includeEmptyFields, false ),
		    new Argument( false, "boolean", Key.multiCharacterDelimiter, false ),

		};
	}

	/**
	 * Describe what the invocation of your bif function does
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.foo Describe any expected arguments
	 */
	public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
		String	list			= arguments.getAsString( Key.list );
		String	delimiter		= arguments.getAsString( Key.delimiter );
		Boolean	wholeDelimiter	= arguments.getAsBoolean( Key.multiCharacterDelimiter );
		Boolean	includeEmpty	= arguments.getAsBoolean( Key.includeEmptyFields );
		String	utilFn			= fn_Split;
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

}
