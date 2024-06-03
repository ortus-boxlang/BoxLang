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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import ortus.boxlang.compiler.IBoxpiler;
import ortus.boxlang.compiler.javaboxpiler.JavaBoxpiler;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.runnables.IProxyRunnable;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.util.EncryptionUtil;

/**
 * I handle creating interface proxies
 */
public class InterfaceProxyService {

	private static final ClassLocator	classLocator	= ClassLocator.getInstance();

	/**
	 * BoxPiler
	 */
	private static final IBoxpiler		boxpiler		= JavaBoxpiler.getInstance();

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
	 * @param context    The context
	 * @param interfaces The interfaces
	 *
	 * @return The proxy definition
	 */
	public static InterfaceProxyDefinition generateDefinition( IBoxContext context, Array interfaces ) {
		String			name			= generateName( interfaces );
		List<Method>	methods			= new ArrayList<Method>();
		List<String>	interfaceNames	= new ArrayList<String>();
		// For each interface in the array
		for ( Object iface : interfaces ) {
			DynamicObject iClass;
			// Load the class, and add the methods to our list
			if ( iface instanceof Class ic ) {
				interfaceNames.add( ic.getName() );
				iClass = DynamicObject.of( ic );
			} else {
				iClass = classLocator.load( context, ( String ) iface, "java", true, context.getCurrentImports() );
				interfaceNames.add( ( String ) iface );
			}
			methods.addAll( iClass.getMethods() );
		}
		// TODO: validate overlapping methods? Remove duplicate method signatures?

		return new InterfaceProxyDefinition( name, methods, interfaceNames );
	}

	private static String generateName( Array interfaces ) {
		return EncryptionUtil.hash( interfaces.toString(), "MD5" );
	}

}