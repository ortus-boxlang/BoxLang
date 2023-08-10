package ortus.boxlang.runtime.interop;

import java.lang.invoke.*;
import java.lang.reflect.Field;

public class Test {

	public static MethodHandle getField( Class targetClass, MethodHandles.Lookup caller, String fieldName )
	        throws NoSuchFieldException, IllegalAccessException {
		Field field = targetClass.getField( fieldName );
		System.out.println( "field: " + field );
		MethodHandle mh = caller.unreflectGetter( field );
		return mh;
	}

	public static void main( String[] args ) throws Throwable {
		FieldClass				instance	= new FieldClass();
		MethodHandles.Lookup	lookup		= MethodHandles.lookup();
		MethodHandle			getter		= getField( FieldClass.class, lookup, "publicField" );
		int						fieldValue	= ( int ) getter.invokeExact( instance );
		System.out.println( "Value of publicField: " + fieldValue );

		getter = getField( FieldClass.class, lookup, "CONSTANT" );
		String constantValue = ( String ) getter.invokeExact();
		System.out.println( "Value of CONSTANT: " + constantValue );
	}
}

class FieldClass {

	public static final String	CONSTANT	= "Hello World";
	public int					publicField	= 42;
}
