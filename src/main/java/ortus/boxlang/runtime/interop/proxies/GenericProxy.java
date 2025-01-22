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
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.BooleanRef;

/**
 * A generic proxy allows you to wrap any object and call any method on it from Java/BoxLang
 */
public class GenericProxy extends BaseProxy implements InvocationHandler {

	public GenericProxy( Object target, IBoxContext context, String method ) {
		super( target, context, method );
		prepLogger( GenericProxy.class );
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
				return coerceReturnValue( invoke( Key.of( method.getName() ), args ), method.getReturnType(), method.getName() );
			} else {
				return invoke( args );
			}
		} catch ( Exception e ) {
			getLogger().error( "Error invoking GenericProxy", e );
			throw new BoxRuntimeException( "Error invoking GenericProxy", e );
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
		if ( returnType == null ) {
			return returnValue;
		}
		Object[]	args	= new Object[] { returnValue };
		boolean		success	= DynamicInteropService.coerceArguments( context, new Class<?>[] { returnType }, new Class<?>[] { returnValue.getClass() }, args,
		    false, BooleanRef.of( true ) );
		if ( !success ) {
			throw new BoxRuntimeException( "Proxied method [ " + methodName + "() ] returned a value of type [ " + returnValue.getClass().getName()
			    + " ] which could not be coerced to [ " + returnType.getName() + " ] in order to match the interface method signature." );
		}
		return args[ 0 ];
	}

}
