package ortus.boxlang.runtime.bifs.global.array;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Function;

public class ArrayReduce extends BIF {

    /**
     * Constructor
     */
    public ArrayReduce() {
        super();
        arguments = new Argument[] {
            new Argument( true, "array", Key.array ),
            new Argument( true, "function", Key.callback ),
            new Argument( Key.initialValue )
        };
    }

    /**
     * Run the provided udf over the array to reduce the values to a single output
     *
     * @param context   The context in which the BIF is being invoked.
     * @param arguments Argument scope for the BIF.
     * 
     * @argument.array The array to reduce
     * 
     * @argument.callback The function to invoke for each item. The function will be passed 3 arguments: the accumulator, the current item, and the
     *                    current index. The function should return the new accumulator value.
     * 
     * @argument.initialValue The initial value of the accumulator
     */
    public Object invoke( IBoxContext context, ArgumentsScope arguments ) {
        Array          actualArray     = arguments.getAsArray( Key.array );
        Object         accumulator     = arguments.get( Key.initialValue );
        Function       func            = arguments.getAsFunction( Key.callback );
        ArgumentsScope closureArgScope = func.createArgumentsScope( new Object[] { accumulator, null, null, actualArray } );

        for ( int i = 0; i < actualArray.size(); i++ ) {
            closureArgScope.put( Key._1, accumulator );
            closureArgScope.put( Key._2, actualArray.get( i ) );
            closureArgScope.put( Key._3, i + 1 );

            FunctionBoxContext fbc = Function.generateFunctionContext( func, context, Key.callback, closureArgScope );

            accumulator = func.invoke( fbc );
        }

        return accumulator;
    }
}
