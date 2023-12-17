package ortus.boxlang.runtime.bifs.global.array;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Function;

public class ArrayReduce extends BIF {

    private final static Key arr          = Key.of( "arr" );
    private final static Key callback     = Key.of( "callback" );
    private final static Key initialValue = Key.of( "initialValue" );

    /**
     * Constructor
     */
    public ArrayReduce() {
        super();
        arguments = new Argument[] {
            new Argument( true, "any", arr ),
            new Argument( true, "any", callback ),
            new Argument( initialValue )
        };
    }

    /**
     * Run the provided udf over the array to reduce the values to a single output
     *
     * @param context
     * @param arguments Argument scope defining the array and value to append.
     */
    public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
        Array          actualArray     = ArrayCaster.cast( arguments.dereference( arr, false ) );
        Object         accumulator     = arguments.get( initialValue );
        Function       func            = ( Function ) arguments.get( callback );
        ArgumentsScope closureArgScope = func.createArgumentsScope( new Object[] { accumulator, null, null, actualArray } );

        for ( int i = 0; i < actualArray.size(); i++ ) {
            closureArgScope.put( Key._1, accumulator );
            closureArgScope.put( Key._2, actualArray.get( i ) );
            closureArgScope.put( Key._3, i + 1 );

            FunctionBoxContext fbc = Function.generateFunctionContext( func, context, callback, closureArgScope );

            accumulator = func.invoke( fbc );
        }

        return accumulator;
    }
}
