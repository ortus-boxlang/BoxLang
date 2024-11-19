/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.bifs.global.java;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.javaproxy.InterfaceProxyService;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
public class CreateDynamicProxy extends BIF {

	ClassLocator classLocator = BoxRuntime.getInstance().getClassLocator();

	/**
	 * Constructor
	 */
	public CreateDynamicProxy() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, "any", Key._CLASS ),
		    new Argument( true, "any", Key.interfaces )
		};
	}

	/**
	 * Creates a dynamic proxy of the Box Class that is passed to a Java library.
	 *
	 * Dynamic proxy lets you pass Box Classes to Java objects.
	 *
	 * Java objects can work with the Box Class seamlessly as if they are native
	 * Java objects.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.class The Box Class to create a dynamic proxy of.
	 *
	 * @argument.interfaces The interfaces that the dynamic proxy should implement.
	 *
	 * @return A dynamic proxy of the Box Class.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object			oClass		= arguments.get( Key._CLASS );
		Object			oInterfaces	= arguments.get( Key.interfaces );
		IClassRunnable	classToProxy;
		Array			interfacesToImplement;

		// Class can be a string name or a Box Class instance
		if ( oClass instanceof IClassRunnable classRunnable ) {
			classToProxy = classRunnable;
		} else {
			String className = StringCaster.cast( oClass );
			classToProxy = ( IClassRunnable ) classLocator
			    .load( context, className, ClassLocator.BX_PREFIX, false, context.getCurrentImports() )
			    .invokeConstructor( context )
			    .unWrapBoxLangClass();
		}

		// Interfaces can be a string name or an array of string names
		CastAttempt<Array> arrayAttemp = ArrayCaster.attempt( oInterfaces );
		if ( arrayAttemp.wasSuccessful() ) {
			interfacesToImplement = arrayAttemp.get();
		} else {
			interfacesToImplement = Array.of( StringCaster.cast( oInterfaces ) );
		}

		// assert, we now have a Box Class instance to proxy and an array of interface names to implement

		// valiate at least one interface was passed
		if ( interfacesToImplement.isEmpty() ) {
			throw new BoxRuntimeException( "At least one interface must be passed to create a dynamic proxy" );
		}

		// Build it out. Note: We use the request class loader to make sure that
		// the proxy can see the Box Class that is being proxied.
		return InterfaceProxyService.buildGenericProxy(
		    context,
		    classToProxy,
		    null,
		    interfacesToImplement,
		    context.getParentOfType( RequestBoxContext.class ).getRequestClassLoader()
		);
	}
}
