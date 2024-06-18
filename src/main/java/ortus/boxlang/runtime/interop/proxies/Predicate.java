package ortus.boxlang.runtime.interop.proxies;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * https://docs.oracle.com/en%2Fjava%2Fjavase%2F21%2Fdocs%2Fapi%2F%2F/java.base/java/util/function/Predicate.html
 */
public class Predicate<T> extends BaseProxy implements java.util.function.Predicate<T> {

	public Predicate( Function target, IBoxContext context ) {
		super( target, context );
		prepLogger( Predicate.class );
	}

	@Override
	public boolean test( T t ) {
		try {
			return BooleanCaster.cast( this.context.invokeFunction(
			    this.target,
			    new Object[] { t }
			) );
		} catch ( Exception e ) {
			getLogger().error( "Error invoking Predicate", e );
			throw new BoxRuntimeException( "Error invoking Predicate", e );
		}
	}

}
