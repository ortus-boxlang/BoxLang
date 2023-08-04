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
package ortus.boxlang.runtime.interop;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

import ortus.boxlang.runtime.BoxRunner;

import java.lang.invoke.MethodHandles;

public class InvokeDynamicTest {

	public static void main( String[] args ) throws Throwable {
		// cf code => javaObject.method( arg1:string, arg 2:int )
		// InvokeJava( class, method, arguments, argumentSignatures )
		// The incoming arguments from CF, transform to an array of objects
		final Object[] arguments = { "Hello" };
		// The incoming argument types from CF, transform to an array of object types
		final Class<?>[] argumentSignatures = { String.class };
		// The dynamic invocation of the class and method, remember, we don't know the return type
		final Optional<?> results = invokeJava( new MyClass(), "myMethod", arguments, argumentSignatures );
		// Present Results
		results.ifPresentOrElse( System.out::println, () -> System.out.println( "No results" ) );
	}

	public static Optional<?> invokeJava( Object javaObject, String methodName, Object[] arguments,
			Class<?>[] argumentSignatures ) throws Throwable {
		MethodHandles.Lookup lookup = MethodHandles.lookup();
		Class<?> targetClass = javaObject.getClass();
		Class<?> returnTypeClass = discoverMethodReturnType( javaObject, methodName );

		System.out.println( "Target Class: " + targetClass.toString() + "." + methodName );
		System.out.println( "Method Return Type: " + returnTypeClass.toString() );

		MethodType methodType = MethodType.methodType( returnTypeClass, argumentSignatures );
		MethodHandle methodHandle = lookup.findVirtual( targetClass, methodName, methodType );
		return Optional.of( methodHandle.invokeExact( arguments ) );
	}

	private static Class<?> discoverMethodReturnType( Object javaObject, String methodName )
			throws NoSuchMethodException, SecurityException {
		Class<?> clazz = javaObject.getClass();
		final Method method = clazz.getMethod( methodName );
		return method.getReturnType().getClass();
	}

	public static class MyClass {

		public void myMethod( String message ) {
			System.out.println( "Virtual method called: " + message );
		}

		public static void anotherMethod( String message ) {
			System.out.println( "Static method called: " + message );
		}
	}
}
