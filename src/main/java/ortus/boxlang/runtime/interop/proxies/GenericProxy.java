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
package ortus.boxlang.runtime.interop.proxies;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicInteropService;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxCastException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;
import ortus.boxlang.runtime.types.util.TypeUtil;

/**
 * A generic proxy allows you to wrap any object and call any method on it from Java/BoxLang
 */
public class GenericProxy extends BaseProxy implements InvocationHandler {

	public GenericProxy( Object target, IBoxContext context, String method ) {
		super( target, context, method );
	}

	/**
	 * @InheritDoc
	 */
	@Override
	public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable {
		try {
			// Verify we have args or default them to an empty array
			if ( args == null ) {
				args = new Object[] {};
			}

			// If we have a class and an incoming method proxy, run it
			if ( isClassRunnableTarget() && method != null ) {
				// Invoke the method
				return coerceReturnValue( invoke( Key.of( method.getName() ), args ), method.getReturnType(), method.getName() );
			}

			// Use use the default invocations
			if ( method != null ) {
				return coerceReturnValue( invoke( args ), method.getReturnType(), method.getName() );
			} else {
				return invoke( args );
			}
		} catch ( Exception e ) {
			getLogger().error( "Error invoking GenericProxy", e );
			ExceptionUtil.throwException( e );
			return null; // Unreachable code, but required for compilation
		}
	}

	/**
	 * Force return value to be what the interface requires
	 *
	 * @param returnValue The value being returned
	 * @param returnType  The return type of the method
	 * @param methodName  The name of the method (or error handling)
	 *
	 * @return The coerced return value
	 */
	private Object coerceReturnValue( Object returnValue, Class<?> returnType, String methodName ) {

		// If our proxy returned a value, but we needed void, then just skip the casting check and return null.
		if ( Void.class.equals( returnType ) || void.class.equals( returnType ) ) {
			return null;
		}

		// If there is nothing to cast, just return it....
		if ( returnValue == null ) {
			// ... UNLESS we required a primitive type, which cannot be null!
			if ( returnType.isPrimitive() ) {
				throw new BoxCastException(
				    "Proxied method [" + methodName + "()] returned null, but the interface method signature requires a primitive type ["
				        + returnType.getName() + "] which cannot be null." );
			}
			return returnValue;
		}

		try {
			return DynamicInteropService.coerceValue( context, returnValue, returnType );
		} catch ( BoxCastException bce ) {
			// Throw an error message that's as useful as possible
			throw new BoxRuntimeException( "Proxied method [ " + methodName + "() ] returned a value of type [ " + TypeUtil.getObjectName( returnValue )
			    + " ] which could not be coerced to [ " + returnType.getName() + " ] in order to match the interface method signature.", bce );
		}
	}

}
