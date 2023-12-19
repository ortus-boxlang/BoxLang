package ortus.boxlang.runtime.bifs.global.array;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.Function;

@BoxBIF
@BoxMember( type = BoxLangType.ARRAY )
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
        Array    actualArray = ArrayCaster.cast( arguments.dereference( Key.array, false ) );
        Object   accumulator = arguments.get( Key.initialValue );
        Function func        = ( Function ) arguments.get( Key.callback );

        for ( int i = 0; i < actualArray.size(); i++ ) {
            accumulator = context.invokeFunction( func, new Object[] { accumulator, actualArray.get( i ), i + 1, actualArray } );
        }

        return accumulator;
    }
}
