package ortus.boxlang.runtime.bifs.global.math;

import java.util.Random;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.types.exceptions.ApplicationException;

public class Rand extends BIF {

    static Random rand = new Random();

    /**
     * 
     * Return a random double between 0 and 1
     * 
     * @param context
     * 
     * @return
     */
    public static Object invoke( IBoxContext context ) {
        return rand.nextDouble();
    }

    public static Object invoke( IBoxContext context, String alg ) {
        throw new ApplicationException( "The algorithm argument has not yet been implemented" );
    }
}
