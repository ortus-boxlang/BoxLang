package ortus.boxlang.runtime.bifs.global.math;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.DoubleCaster;
import ortus.boxlang.runtime.types.exceptions.ApplicationException;

public class RandRange extends BIF {

    /**
     * 
     * Return a random int between number1 and number 2
     * 
     * @param context
     * @param number1 A numeric value that represents the range minimum
     * @param number2 A numeric value that represents the range maximum (not inclusive)
     * 
     * @return
     */
    public static Object invoke( IBoxContext context, Object number1, Object number2 ) {
        Double numA = DoubleCaster.cast( number1 );
        Double numB = DoubleCaster.cast( number2 );

        return ( int ) ( numA + ( ( Double ) Rand.invoke( context ) * ( numB - numA ) ) );
    }

    public static Object invoke( IBoxContext context, Object number1, Object number2, String alg ) {
        throw new ApplicationException( "The algorithm argument has not yet been implemented" );
    }

}
