/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
