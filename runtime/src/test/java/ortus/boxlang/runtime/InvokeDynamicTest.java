package ortus.boxlang.runtime;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles;

import org.junit.jupiter.api.Test;

public class InvokeDynamicTest {

	@Test
	void canInvokeDynamic() {

		// cf code => javaObject.method( arg1:string, arg 2:int )
		// Bytecode : 2 arguments : String.class, Integer.class

		// Define the target method to be invoked
		MethodHandles.Lookup lookup = MethodHandles.lookup();
		String methodName = "getGreeting";

		try {
			// Construct an array of argument classes
			Class<?>[] arguments = { String.class };
			// Find the target method handle with the right type and arguments
			MethodHandle targetMethod = lookup.findVirtual( Bootstrap.class, methodName,
					MethodType.methodType( String.class, arguments ) );

			// Create a CallSite with the dynamic invocation strategy
			CallSite callSite = new ConstantCallSite( targetMethod );

			// Invoke the dynamic method using invokedynamic
			MethodHandle dynamicInvoker = callSite.dynamicInvoker();
			String results = ( String ) dynamicInvoker.invokeExact( "luis" );

			System.out.println( "===> Results are " + results );

		} catch ( NoSuchMethodException | IllegalAccessException e ) {
			e.printStackTrace();
		} catch ( Throwable e ) {
			e.printStackTrace();
		}

	}
}
