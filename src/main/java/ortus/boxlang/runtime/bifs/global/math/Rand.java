package ortus.boxlang.runtime.bifs.global.math;

import java.util.Random;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class Rand extends BIF {

    private final static Key algorithm = Key.of( "algorithm" );

    public static Argument[] arguments = new Argument[] {
        new Argument( algorithm )
    };

    static Random            rand      = new Random();

    /**
     * 
     * Return a random double between 0 and 1
     * 
     * @param context
     * 
     * @return
     */
    public static double invoke( IBoxContext context, ArgumentsScope arguments ) {
        if ( arguments.containsKey( algorithm ) && arguments.dereference( algorithm, false ) != null ) {
            throw new BoxRuntimeException( "The algorithm argument has not yet been implemented" );
        }
        return rand.nextDouble();
    }

}
