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

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.ClassUtils;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.runnables.BoxClassSupport;
import ortus.boxlang.runtime.runnables.BoxInterface;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxLangException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.NoMethodException;

/**
 * This class is used to represent a BX/Java Class and invoke methods on classes using invoke dynamic.
 *
 * This class is not in charge of casting the results. That is up to the caller to determine.
 * We basically just invoke and return the results!
 *
 * To create a new class invoker you can use the following:
 *
 * <pre>{@code
 *
 * ClassInvoker target = new ClassInvoker( String.class );
 * ClassInvoker target = ClassInvoker.of( String.class );
 * ClassInvoker target = new ClassInvoker( new String() );
 * ClassInvoker target = ClassInvoker.of( new String() );
 * }
 * </pre>
 *
 * You can then use the following methods to invoke methods on the class:
 * - {@code invokeConstructor( Object... args )} - Invoke a constructor on the class, and store the instance for future method calls
 * - {@code invokeStaticMethod( String methodName, Object... args )} - Invoke a static method on the class
 * - {@code invoke( String methodName, Object... args )} - Invoke a method on the instance of the class
 */
public class DynamicObject implements IReferenceable {

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Helper for all class utility methods from apache commons lang 3
	 */
	public static final Class<ClassUtils>	CLASS_UTILS		= ClassUtils.class;

	/**
	 * Empty arguments array
	 */
	public static final Object[]			EMPTY_ARGS		= new Object[] {};

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	Set<Key>								exceptionKeys	= new HashSet<>( Arrays.asList(
	    BoxLangException.messageKey,
	    BoxLangException.detailKey,
	    BoxLangException.typeKey,
	    BoxLangException.tagContextKey,
	    BoxRuntimeException.ExtendedInfoKey
	) );

	/**
	 * The bound class for this invoker
	 */
	private Class<?>						targetClass;

	/**
	 * The bound instance for this invoker (if any)
	 * If this is null, then we are invoking static methods or a constructor has not been called on it yet.
	 */
	private Object							targetInstance	= null;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Create a new class invoker for the given class
	 *
	 * @param targetClass The class to create the invoker for
	 */
	public DynamicObject( Class<?> targetClass ) {
		this.targetClass = targetClass;
		if ( BoxInterface.class.isAssignableFrom( targetClass ) ) {
			targetInstance = invoke( "getInstance" );
		}
	}

	/**
	 * Create a new class invoker for the given instance
	 *
	 * @param targetInstance The instance to create the invoker for
	 */
	public DynamicObject( Object targetInstance ) {
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
	public static DynamicObject of( Class<?> targetClass ) {
		return new DynamicObject( targetClass );
	}

	/**
	 * Static factory method to create a new class invoker for the given instance. Mostly used for nice fluent chaining
	 *
	 * @param targetInstance The instance to create the invoker for
	 *
	 * @return The class invoker
	 */
	public static DynamicObject of( Object targetInstance ) {
		return new DynamicObject( targetInstance );
	}

	/**
	 * @return the targetClass
	 */
	public Class<?> getTargetClass() {
		return targetClass;
	}

	/**
	 * @param targetClass the targetClass to set
	 *
	 * @return The Dynamic Object
	 */
	public DynamicObject setTargetClass( Class<?> targetClass ) {
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
	 *
	 * @return The Dynamic Object
	 */
	public DynamicObject setTargetInstance( Object targetInstance ) {
		this.targetInstance = targetInstance;
		return this;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Invokers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Invokes the constructor for the class with the given arguments and stores the instance of the object
	 * into the {@code targetInstance} property for future method calls.
	 *
	 * @param args The arguments to pass to the constructor
	 *
	 * @return The instance of the class
	 */
	public DynamicObject invokeConstructor( IBoxContext context, Object... args ) {
		this.targetInstance = DynamicInteropService.invokeConstructor( context, this.targetClass, args );
		return this;
	}

	/**
	 * Invokes the constructor for the class with the given arguments and stores the instance of the object
	 * into the {@code targetInstance} property for future method calls.
	 *
	 * @param args The arguments to pass to the constructor
	 *
	 * @return The instance of the class
	 */
	public DynamicObject invokeConstructor( IBoxContext context, Map<Key, Object> args ) {
		this.targetInstance = DynamicInteropService.invokeConstructor( context, this.targetClass, args );
		return this;
	}

	/**
	 * Invokes the no-arg constructor for the class with the given arguments and stores the instance of the object
	 * into the {@code targetInstance} property for future method calls.
	 *
	 * @return The instance of the class
	 */
	public DynamicObject invokeConstructor( IBoxContext context ) {
		return invokeConstructor( context, EMPTY_ARGS );
	}

	/**
	 * Invoke can be used to invoke public methods on instances, or static methods on classes/interfaces.
	 *
	 * If it's determined that the method handle is static, then the target instance is ignored.
	 * If it's determined that the method handle is not static, then the target instance is used.
	 *
	 * @param methodName The name of the method to invoke
	 * @param arguments  The arguments to pass to the method
	 *
	 * @return The result of the method invocation
	 */
	public Object invoke( String methodName, Object... arguments ) {
		return DynamicInteropService.invoke( this.getTargetClass(), this.getTargetInstance(), methodName, false, arguments );
	}

	/**
	 * Invokes a static method with the given name and arguments on a class or an interface
	 *
	 * @param methodName The name of the method to invoke
	 * @param arguments  The arguments to pass to the method
	 *
	 * @return The result of the method invocation
	 *
	 */
	public Object invokeStatic( String methodName, Object... arguments ) {
		return DynamicInteropService.invoke( this.targetClass, methodName, false, arguments );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Field Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the value of a public or public static field on a class or instance
	 *
	 * @param fieldName The name of the field to get
	 *
	 * @return The value of the field wrapped in an Optional
	 *
	 */
	public Optional<Object> getField( String fieldName ) {
		return DynamicInteropService.getField( this.targetClass, this.targetInstance, fieldName );
	}

	/**
	 * Get the value of a public or public static field on a class or instance but if it doesn't exist
	 * return the default value passed in.
	 *
	 * @param fieldName    The name of the field to get
	 * @param defaultValue The default value to return if the field doesn't exist
	 *
	 *
	 * @return The value of the field or the default value wrapped in an Optional
	 */
	public Optional<Object> getField( String fieldName, Object defaultValue ) {
		return DynamicInteropService.getField( this.targetClass, this.targetInstance, fieldName, defaultValue );
	}

	/**
	 * Set the value of a public or public static field on a class or instance
	 *
	 * @param fieldName The name of the field to set
	 * @param value     The value to set the field to
	 *
	 * @return The class invoker
	 *
	 */
	public DynamicObject setField( String fieldName, Object value ) {
		DynamicInteropService.setField( this.targetClass, this.targetInstance, fieldName, value );

		return this;
	}

	/**
	 * Find a field by name with no case-sensitivity (upper case) in the class
	 *
	 * @param fieldName The name of the field to find
	 *
	 * @return The field if discovered
	 *
	 */
	public Field findField( String fieldName ) {
		return DynamicInteropService.findField( this.targetClass, fieldName );
	}

	/**
	 * Verifies if the class has a public or public static field with the given name
	 *
	 * @param fieldName The name of the field to check
	 *
	 * @return True if the field exists, false otherwise
	 */
	public Boolean hasField( String fieldName ) {
		return DynamicInteropService.hasField( this.targetClass, fieldName );
	}

	/**
	 * Verifies if the class has a public or public static field with the given name and no case-sensitivity (upper case)
	 *
	 * @param fieldName The name of the field to check
	 *
	 * @return True if the field exists, false otherwise
	 */
	public Boolean hasFieldNoCase( String fieldName ) {
		return DynamicInteropService.hasFieldNoCase( this.targetClass, fieldName );
	}

	/**
	 * Get an array of fields of all the public fields for the given class
	 *
	 * @return The fields in the class
	 */
	public Field[] getFields() {
		return DynamicInteropService.getFields( this.targetClass );
	}

	/**
	 * Get a stream of fields of all the public fields for the given class
	 *
	 * @return The stream of fields in the class
	 */
	public Stream<Field> getFieldsAsStream() {
		return DynamicInteropService.getFieldsAsStream( this.targetClass );
	}

	/**
	 * Get a list of field names for the given class with case-sensitivity
	 *
	 * @return A list of field names
	 */
	public List<String> getFieldNames() {
		return DynamicInteropService.getFieldNames( this.targetClass );

	}

	/**
	 * Get a list of field names for the given class with no case-sensitivity (upper case)
	 *
	 * @return A list of field names
	 */
	public List<String> getFieldNamesNoCase() {
		return DynamicInteropService.getFieldNamesNoCase( this.targetClass );

	}

	/**
	 * --------------------------------------------------------------------------
	 * Helpers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get a method by name for the given class
	 *
	 * @return The method object
	 */
	public Method getMethod( String name ) {
		return DynamicInteropService.getMethod( this.targetClass, name );
	}

	/**
	 * Get a HashSet of methods of all the unique callable method signatures for the given class
	 *
	 * @return A unique set of callable methods
	 */
	public Set<Method> getMethods() {
		return DynamicInteropService.getMethods( this.targetClass );
	}

	/**
	 * Get a stream of methods of all the unique callable method signatures for the given class
	 *
	 * @return A stream of unique callable methods
	 */
	public Stream<Method> getMethodsAsStream() {
		return DynamicInteropService.getMethodsAsStream( this.targetClass );
	}

	/**
	 * Get a list of method names for the given class
	 *
	 * @return A list of method names
	 */
	public List<String> getMethodNames() {
		return DynamicInteropService.getMethodNames( this.targetClass );
	}

	/**
	 * Get a list of method names for the given class with no case-sensitivity (upper case)
	 *
	 * @return A list of method names with no case
	 */
	public List<String> getMethodNamesNoCase() {
		return DynamicInteropService.getMethodNamesNoCase( this.targetClass );
	}

	/**
	 * Verifies if the class has a public or public static method with the given name
	 *
	 * @param methodName The name of the method to check
	 *
	 * @return True if the method exists, false otherwise
	 */
	public Boolean hasMethod( String methodName ) {
		return DynamicInteropService.hasMethod( this.targetClass, methodName );
	}

	/**
	 * Verifies if the class has a public or public static method with the given name and no case-sensitivity (upper case)
	 *
	 * @param methodName The name of the method to check
	 *
	 * @return True if the method exists, false otherwise
	 */
	public Boolean hasMethodNoCase( String methodName ) {
		return DynamicInteropService.hasMethodNoCase( this.targetClass, methodName );
	}

	/**
	 * This method is used to verify if the class has the same method signature as the incoming one with no case-sensitivity (upper case)
	 *
	 * @param methodName         The name of the method to check
	 * @param argumentsAsClasses The parameter types of the method to check
	 *
	 * @throws NoMethodException If the method is not found and safe is false
	 *
	 * @return The matched method signature. If not found and safe is true, it returns null, otherwise it throws an exception
	 *
	 */
	public Method findMatchingMethod( String methodName, Class<?>[] argumentsAsClasses ) {
		return DynamicInteropService.findMatchingMethod( this.targetClass, methodName, argumentsAsClasses );
	}

	/**
	 * Utility method to convert a method to a method handle
	 *
	 * @param method The method to convert
	 *
	 * @return The method handle representing the method or an exception if it fails
	 *
	 */
	public static MethodHandle toMethodHandle( Method method ) {
		return DynamicInteropService.toMethodHandle( method );
	}

	/**
	 * Verifies if the target calss is an interface or not
	 *
	 * @return
	 */
	public boolean isInterface() {
		return DynamicInteropService.isInterface( this.targetClass );
	}

	/**
	 * Converts the argument(s) to a class representation according to Java casting rules
	 *
	 * @param thisArg The argument to convert
	 *
	 * @return The class representation of the argument
	 */
	public static Class<?> argumentToClass( Object thisArg ) {
		return DynamicInteropService.argumentToClass( thisArg );
	}

	/**
	 * Converts the arguments to an array of classes
	 *
	 * @param args The arguments to convert
	 *
	 * @return The array of classes
	 */
	public static Class<?>[] argumentsToClasses( Object... args ) {
		return DynamicInteropService.argumentsToClasses( args );
	}

	/**
	 * Unwrap an object if it's inside a ClassInvoker instance
	 *
	 * @param param The object to unwrap
	 *
	 * @return The target instance or class, depending which one is set
	 */
	public static Object unWrap( Object param ) {
		return DynamicInteropService.unWrap( param );
	}

	/**
	 * Instance method to unwrap itself
	 *
	 * @return The target instance or class, depending which one is set
	 */
	public Object unWrap() {
		if ( hasInstance() ) {
			return getTargetInstance();
		} else {
			return getTargetClass();
		}
	}

	/**
	 * Instance method to unwrap itself if it's a BoxLang class
	 *
	 * @return The target instance or class, depending which one is set
	 */
	public Object unWrapBoxLangClass() {
		if ( hasInstance() && ( getTargetInstance() instanceof IClassRunnable || getTargetInstance() instanceof BoxInterface ) ) {
			return getTargetInstance();
		} else {
			return this;
		}
	}

	/**
	 * Verifies if the class invoker has an instance or not
	 *
	 * @return True if it has an instance, false otherwise
	 */
	public Boolean hasInstance() {
		return this.targetInstance != null;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Implementation of IReferencable
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Dereference this object by a key and return the value, or throw exception
	 *
	 * @param name The name of the key to dereference
	 * @param safe If true, return null if the method is not found, otherwise throw an exception
	 *
	 * @return The requested object
	 */
	public Object dereference( IBoxContext context, Key name, Boolean safe ) {

		// If this dynamic object represnts a Box Class (not an instance), then get static field
		if ( IClassRunnable.class.isAssignableFrom( targetClass ) && targetInstance == null ) {
			return BoxClassSupport.dereferenceStatic( this, context, name, safe );
		}

		return DynamicInteropService.dereference( context, this.targetClass, this.targetInstance, name, safe );
	}

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method)
	 *
	 * @param name                The name of the key to dereference, which becomes the method name
	 * @param positionalArguments The arguments to pass to the invokable
	 * @param safe                If true, return null if the method is not found, otherwise throw an exception
	 *
	 * @return The requested return value or null
	 */
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Object[] positionalArguments, Boolean safe ) {
		if ( name.equals( Key.init ) ) {
			this.targetInstance = DynamicInteropService.invokeConstructor( context, this.targetClass, positionalArguments );
			return this.targetInstance;
		}

		// If this dynamic object represnts a Box Class (not an instance), then invoke static
		if ( IClassRunnable.class.isAssignableFrom( targetClass ) && targetInstance == null ) {
			return BoxClassSupport.dereferenceAndInvokeStatic( this, context, name, positionalArguments, safe );
		}

		return DynamicInteropService.dereferenceAndInvoke( this.targetClass, this.targetInstance, context, name, positionalArguments, safe );
	}

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method)
	 *
	 * @param name           The name of the key to dereference, which becomes the method name
	 * @param namedArguments The arguments to pass to the invokable
	 * @param safe           If true, return null if the method is not found, otherwise throw an exception
	 *
	 * @return The requested return value or null
	 */
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Map<Key, Object> namedArguments, Boolean safe ) {

		// If this dynamic object represnts a Box Class (not an instance), then invoke static
		if ( IClassRunnable.class.isAssignableFrom( targetClass ) && targetInstance == null ) {
			return BoxClassSupport.dereferenceAndInvokeStatic( this, context, name, namedArguments, safe );
		}

		return DynamicInteropService.dereferenceAndInvoke( this.targetClass, this.targetInstance, context, name, namedArguments, safe );
	}

	/**
	 * Assign a value to a field
	 *
	 * @param name  The name of the field to assign
	 * @param value The value to assign
	 */
	public Object assign( IBoxContext context, Key name, Object value ) {

		// If this dynamic object represnts a Box Class (not an instance), then set static field
		if ( IClassRunnable.class.isAssignableFrom( targetClass ) && targetInstance == null ) {
			return BoxClassSupport.assignStatic( this, context, name, value );
		}

		return DynamicInteropService.assign( context, this.targetClass, this.targetInstance, name, value );
	}
}
