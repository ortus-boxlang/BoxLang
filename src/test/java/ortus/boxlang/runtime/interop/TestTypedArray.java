package ortus.boxlang.runtime.interop;

public class TestTypedArray {

	static public void test( String name ) {

	}

	static public void test( String name, Object[] stuff, TestTypedArraySubtype[] types ) {
		System.out.println( "Testing String, Object[], TestTypedArraySubtype[] " + name + " with size: " + types.length );
	}

}
