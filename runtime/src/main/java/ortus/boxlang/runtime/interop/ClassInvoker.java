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
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Arrays;
import java.util.Map;

/**
 * This class is used to represent a BX/Java Class and invoke methods on classes using invoke dynamic.
 *
 * This class is not in charge of casting the results. That is up to the caller to determine.
 * We basically just invoke and forget!
 *
 * TODO:
 * - Is there a way to cache method handles?
 */
public class ClassInvoker {

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * This is a map of primitive types to their native Java counterparts
	 * so we can do the right casting for primitives
	 */
	// @formatter:off
	private static final Map<Class<?>, Class<?>> PRIMITIVE_MAP = Map.of(
            Boolean.class, boolean.class,
            Byte.class, byte.class,
            Character.class, char.class,
            Short.class, short.class,
            Integer.class, int.class,
            Long.class, long.class,
            Float.class, float.class,
            Double.class, double.class
    );
	// @formatter:on

	/**
	 * This invokers works of this targeted class
	 */
	private Class<?>								targetClass;

	/**
	 * This is the instance of the targeted class if it exists, since it could be static method calls or interface calls
	 */
	private Object									targetInstance	= null;

	/**
	 * This is the method handle lookup for the class
	 */
	private final MethodHandles.Lookup				lookup			= MethodHandles.lookup();

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Create a new class invoker for the given class
	 *
	 * @param targetClass
	 */
	public ClassInvoker( Class<?> targetClass ) {
		this.targetClass = targetClass;
	}

	/**
	 * Create a new class invoker for the given instance
	 *
	 * @param targetInstance
	 */
	public ClassInvoker( Object targetInstance ) {
		this.targetInstance	= targetInstance;
		this.targetClass	= targetInstance.getClass();
	}

	/**
	 * Static factory method to create a new class invoker for the given class. Mostly used for nice fluent chaining
	 *
	 * @param targetClass The class to create the invoker for
	 *
	 * @return The class invoker
	 */
	public static ClassInvoker of( Class<?> targetClass ) {
		return new ClassInvoker( targetClass );
	}

	/**
	 * Static factory method to create a new class invoker for the given instance. Mostly used for nice fluent chaining
	 *
	 * @param targetInstance The instance to create the invoker for
	 *
	 * @return The class invoker
	 */
	public static ClassInvoker of( Object targetInstance ) {
		return new ClassInvoker( targetInstance );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * @return the targetClass
	 */
	public Class<?> getTargetClass() {
		return targetClass;
	}

	/**
	 * @param targetClass the targetClass to set
	 */
	public ClassInvoker setTargetClass( Class<?> targetClass ) {
		this.targetClass = targetClass;
		return this;
	}

	/**
	 * @return the targetInstance
	 */
	public Object getTargetInstance() {
		return targetInstance;
	}

	/**
	 * @param targetInstance the targetInstance to set
	 */
	public ClassInvoker setTargetInstance( Object targetInstance ) {
		this.targetInstance = targetInstance;
		return this;
	}

	/**
	 * Verifies if the target calss is an interface or not
	 *
	 * @return
	 */
	boolean isInterface() {
		return this.targetClass.isInterface();
	}

	/**
	 * Invokes the constructor for the class with the given arguments
	 *
	 * @param args The arguments to pass to the constructor
	 *
	 * @return The instance of the class
	 *
	 * @throws Throwable             If the constructor cannot be invoked
	 * @throws IllegalStateException If the class is an interface
	 */
	public Object invokeConstructor( Object... args ) throws Throwable {

		if ( isInterface() ) {
			throw new IllegalStateException( "Cannot invoke a constructor on an interface" );
		}

		// Method signature for a constructor is void (Object...)
		MethodType		constructorType		= MethodType.methodType( void.class, argumentsToClasses( args ) );
		// Define the bootstrap method
		MethodHandle	constructorHandle	= lookup.findConstructor( this.targetClass, constructorType );
		// Create a callsite using the constructor handle
		CallSite		callSite			= new ConstantCallSite( constructorHandle );
		// Bind the CallSite and invoke the constructor with the provided arguments
		// Invoke Dynamic tries to do argument coercion, so we need to convert the arguments to the right types
		MethodHandle	constructorInvoker	= callSite.dynamicInvoker();
		this.targetInstance = constructorInvoker.invokeWithArguments( args );

		return this.targetInstance;
	}

	/**
	 * Invokes a public method on a class or interface with the given name and arguments
	 *
	 * @param methodName The name of the method to invoke
	 * @param args       The arguments to pass to the method
	 *
	 * @return The result of the method invocation or null if the method is void
	 *
	 * @throws Throwable
	 */
	public Object invoke( String methodName, Object... args ) throws Throwable {
		// Find the method handle for the method
		MethodHandle method = lookup.findVirtual(
		        this.targetClass,
		        methodName,
		        MethodType.methodType( Object.class, argumentsToClasses( args ) )
		);

		// Execute it baby!
		return method.invokeWithArguments( args );
	}

	/**
	 * Invokes a static method with the given name and arguments
	 *
	 * @param methodName The name of the method to invoke
	 * @param args       The arguments to pass to the method
	 *
	 * @return The result of the method invocation or null if the method is void
	 *
	 * @throws Throwable
	 */
	public Object invokeStatic( String methodName, Object... args ) throws Throwable {
		MethodHandle method = lookup.findStatic(
		        this.targetClass,
		        methodName,
		        MethodType.methodType( Object.class, argumentsToClasses( args ) )
		);
		return method.invokeWithArguments( args );
	}

	/**
	 * Converts the argument(s) to a class representation according to Java casting rules
	 *
	 * @param thisArg The argument to convert
	 *
	 * @return The class representation of the argument
	 */
	public static Class<?> argumentToClass( Object thisArg ) {
		// nulls are always null, why is this even a thing?
		if ( thisArg == null ) {
			return Object.class;
		}
		// If it's a primitive, we need to convert it to the wrapper class
		Class<?> clazz = thisArg.getClass();
		return PRIMITIVE_MAP.getOrDefault( clazz, clazz );
	}

	/**
	 * Converts the arguments to an array of classes
	 *
	 * @param args The arguments to convert
	 *
	 * @return The array of classes
	 */
	public static Class<?>[] argumentsToClasses( Object... args ) {
		// Convert the arguments to an array of classes
		return Arrays.stream( args )
		        .map( ClassInvoker::argumentToClass )
		        .toArray( Class<?>[]::new );
	}
}
