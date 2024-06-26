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
package ortus.boxlang.runtime.dynamic.javaproxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ClassUtils;

import ortus.boxlang.compiler.IBoxpiler;
import ortus.boxlang.compiler.javaboxpiler.JavaBoxpiler;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.interop.proxies.BaseProxy;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.runnables.IProxyRunnable;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.util.EncryptionUtil;

/**
 * I handle creating interface proxies
 */
public class InterfaceProxyService {

	/**
	 * Our class locator
	 */
	private static final ClassLocator	classLocator	= ClassLocator.getInstance();

	/**
	 * BoxPiler
	 */
	private static final IBoxpiler		boxpiler		= JavaBoxpiler.getInstance();

	/**
	 * We create several proxies for core Java classes to make them easier to work with.
	 * If you find one that is used often, add it here and to the core.
	 */
	private static final List<String>	CORE_PROXIES	= Arrays.asList(
	    "BiConsumer",
	    "BiFunction",
	    "BinaryOperator",
	    "Callable",
	    "Comparator",
	    "Consumer",
	    "Function",
	    "IInterceptorLambda",
	    "Predicate",
	    "Runnable",
	    "Supplier",
	    "UnaryOperator",
	    "ToDoubleFunction",
	    "ToIntFunction",
	    "ToLongFunction"
	);

	/**
	 * --------------------------------------------------------------------------
	 * Proxy Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Create a proxy class that wraps a Box class and implements the given interfaces
	 *
	 * @param context    The current context
	 * @param boxClass   The box class to wrap
	 * @param interfaces The array of interfaces to implement
	 *
	 * @return The proxy
	 */
	public static IProxyRunnable createProxy( IBoxContext context, IClassRunnable boxClass, Array interfaces ) {
		var				definition	= generateDefinition( context, interfaces );
		DynamicObject	proxyClass	= DynamicObject.of( boxpiler.compileInterfaceProxy( context, definition ) );
		proxyClass.invokeConstructor( context );
		IProxyRunnable proxy = ( IProxyRunnable ) proxyClass.getTargetInstance();
		proxy.setBXProxy( boxClass );
		return proxy;
	}

	/**
	 * Turn an array of interfaces into a proxy definition
	 *
	 * @param context    The context to bind with
	 * @param interfaces The interfaces to implement
	 *
	 * @return The proxy definition
	 */
	public static InterfaceProxyDefinition generateDefinition( IBoxContext context, Array interfaces ) {
		String			name			= generateName( interfaces );
		List<Method>	methods			= new ArrayList<>();
		List<String>	interfaceNames	= new ArrayList<>();
		// For each interface in the array
		for ( Object iface : interfaces ) {
			DynamicObject iClass;
			// Load the class, and add the methods to our list
			if ( iface instanceof Class ic ) {
				interfaceNames.add( ic.getName() );
				iClass = DynamicObject.of( ic );
			} else {
				iClass = classLocator.load( context, ( String ) iface, ClassLocator.JAVA_PREFIX, true, context.getCurrentImports() );
				interfaceNames.add( ( String ) iface );
			}
			methods.addAll( iClass.getMethods() );
		}
		// TODO: validate overlapping methods? Remove duplicate method signatures?

		return new InterfaceProxyDefinition( name, methods, interfaceNames );
	}

	/**
	 * This will build a generic dynamic proxy for functional interfaces according to the passed clazz.
	 * The target will be the target of execution of the proxy, either a Function or IClassRunnable.
	 *
	 * We will wrap the target into a {@Link ortus.boxlang.runtime.interop.proxies.GenericProxy} which
	 * implements a {@link InvocationHandler} and will handle the method invocation with logging
	 * and security.
	 *
	 * @param context     The box context to bind with
	 * @param target      The target object to wrap, either a Funcion or IClassRunnable
	 * @param method      The method name to invoke in the IClassRunable if any
	 * @param interfaces  One or more functional interfaces to implement
	 * @param classLoader The class loader to use
	 *
	 * @throws IllegalArgumentException if the clazz is not a functional interface
	 *                                  - All of Class objects in the given interfaces array must represent non-hidden and non-sealed interfaces, not classes or primitive types.
	 *                                  - No two elements in the interfaces array may refer to identical Class objects.
	 *
	 * @return A proxy object implementing the functional interfaces passed.
	 */
	public static Object buildGenericProxy(
	    IBoxContext context,
	    Object target,
	    String method,
	    Array interfaces,
	    ClassLoader classLoader ) {

		Class<?>[] interfacesArray = interfaces
		    .stream()
		    .map( iface -> {
			    if ( iface instanceof Class interfaceClass ) {
				    return interfaceClass;
			    } else {
				    return classLocator.load( context, ( String ) iface, ClassLocator.JAVA_PREFIX, true, context.getCurrentImports() ).getTargetClass();
			    }
		    } )
		    .toArray( Class[]::new );

		return buildGenericProxy(
		    context,
		    target,
		    method,
		    interfacesArray,
		    classLoader
		);
	}

	/**
	 * This will build a generic dynamic proxy for functional interfaces according to the passed clazz.
	 * The target will be the target of execution of the proxy, either a Function or IClassRunnable.
	 *
	 * We will wrap the target into a {@Link ortus.boxlang.runtime.interop.proxies.GenericProxy} which
	 * implements a {@link InvocationHandler} and will handle the method invocation with logging
	 * and security.
	 *
	 * @param context     The box context to bind with
	 * @param target      The target object to wrap, either a Funcion or IClassRunnable
	 * @param method      The method name to invoke in the IClassRunable if any
	 * @param interfaces  One or more functional interfaces to implement
	 * @param classLoader The class loader to use
	 *
	 * @throws IllegalArgumentException if the clazz is not a functional interface
	 *                                  - All of Class objects in the given interfaces array must represent non-hidden and non-sealed interfaces, not classes or primitive types.
	 *                                  - No two elements in the interfaces array may refer to identical Class objects.
	 *
	 * @return A proxy object implementing the functional interfaces passed.
	 */
	public static Object buildGenericProxy(
	    IBoxContext context,
	    Object target,
	    String method,
	    Class<?>[] interfaces,
	    ClassLoader classLoader ) {
		return Proxy.newProxyInstance(
		    // The class loader to use
		    classLoader,
		    // The interface(s) to implement
		    interfaces,
		    // The invocation handler, which is our base generic proxy
		    // This gives us logging, context binding, and ability to target Functions and classes
		    new ortus.boxlang.runtime.interop.proxies.GenericProxy( target, context, method )
		);
	}

	/**
	 * Build a functional interface proxy for a core proxy
	 *
	 * @param clazz   The functional interface class
	 * @param context The box context
	 * @param target  The target object to wrap, either a Funcion or IClassRunnable
	 * @param method  The method name to invoke if any
	 *
	 * @return The proxy to coerce the target object to the functional interface
	 */
	@SuppressWarnings( "rawtypes" )
	public static BaseProxy buildCoreProxy( Class<?> clazz, IBoxContext context, Object target, String method ) {
		String targetName = clazz.getSimpleName();
		return switch ( targetName ) {
			case "BiConsumer" -> new ortus.boxlang.runtime.interop.proxies.BiConsumer( target, context, method );
			case "BiFunction" -> new ortus.boxlang.runtime.interop.proxies.BiFunction( target, context, method );
			case "BinaryOperator" -> new ortus.boxlang.runtime.interop.proxies.BinaryOperator( target, context, method );
			case "Callable" -> new ortus.boxlang.runtime.interop.proxies.Callable( target, context, method );
			case "Comparator" -> new ortus.boxlang.runtime.interop.proxies.Comparator( target, context, method );
			case "Consumer" -> new ortus.boxlang.runtime.interop.proxies.Consumer( target, context, method );
			case "Function" -> new ortus.boxlang.runtime.interop.proxies.Function( target, context, method );
			case "IInterceptorLambda" -> new ortus.boxlang.runtime.interop.proxies.IInterceptorLambda( target, context, method );
			case "Predicate" -> new ortus.boxlang.runtime.interop.proxies.Predicate( target, context, method );
			case "Runnable" -> new ortus.boxlang.runtime.interop.proxies.Runnable( target, context, method );
			case "Supplier" -> new ortus.boxlang.runtime.interop.proxies.Supplier( target, context, method );
			case "UnaryOperator" -> new ortus.boxlang.runtime.interop.proxies.UnaryOperator( target, context, method );
			case "ToDoubleFunction" -> new ortus.boxlang.runtime.interop.proxies.ToDoubleFunction( target, context, method );
			case "ToIntFunction" -> new ortus.boxlang.runtime.interop.proxies.ToIntFunction( target, context, method );
			case "ToLongFunction" -> new ortus.boxlang.runtime.interop.proxies.ToLongFunction( target, context, method );
			default -> null;
		};
	}

	/**
	 * Checks if the clazz is a functional interface or a SAM interface
	 *
	 * @param clazz The class to check
	 *
	 * @return True if it's a functional interface or SAM, false otherwise
	 */
	public static Boolean isFunctionalInterface( Class<?> clazz ) {
		if ( clazz.isInterface() && clazz.isAnnotationPresent( FunctionalInterface.class ) ) {
			return true;
		} else {
			return isSAMInterface( clazz );
		}
	}

	/**
	 * Checks if the clazz is an interface and if it has 1 abstract method
	 *
	 * @param clazz The class to check
	 *
	 * @return True if it's a SAM interface, false otherwise
	 */
	public static Boolean isSAMInterface( Class<?> clazz ) {
		// If it's not an interface, it can't be a SAM
		if ( !clazz.isInterface() ) {
			return false;
		}

		// If it's an interface, it can be a SAM if it has only 1 abstract method
		return Arrays.stream( clazz.getDeclaredMethods() )
		    .filter( method -> Modifier.isAbstract( method.getModifiers() ) )
		    .count() == 1;
	}

	/**
	 * Checks if the classname is a core proxy. We only use the class name not the full path.
	 *
	 * @param className The class name to check if it's a core proxy
	 *
	 * @return True if it's a core proxy, false otherwise
	 */
	public static Boolean isCoreProxy( String className ) {
		return CORE_PROXIES.contains( className );
	}

	/**
	 * Inspect the target class to see if it's a functional interface and return the functional interface class
	 *
	 * @param clazz The class to inspect
	 *
	 * @return The functional interface class or null if it's not a functional interface
	 */
	public static Class<?> getFunctionalInterface( Class<?> clazz ) {
		// If the clazz is a functional interface, or a SAM, return it
		if ( isFunctionalInterface( clazz ) ) {
			return clazz;
		}

		// Default, check all implemented interfaces for a functional interface
		return ClassUtils.getAllInterfaces( clazz )
		    .stream()
		    .filter( InterfaceProxyService::isFunctionalInterface )
		    .findFirst()
		    .orElse( null );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Private Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Generate a name for the proxy using the interfaces array
	 *
	 * @param interfaces The interfaces to use for proxying
	 *
	 * @return A unique name for the proxy using a hash
	 */
	private static String generateName( Array interfaces ) {
		return EncryptionUtil.hash( interfaces.toString(), "MD5" );
	}

}
