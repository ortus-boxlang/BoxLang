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
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

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
				return invoke( Key.of( method.getName() ), args );
			}

			// Use use the default invocations
			return invoke( args );
		} catch ( Exception e ) {
			getLogger().error( "Error invoking GenericProxy", e );
			throw new BoxRuntimeException( "Error invoking GenericProxy", e );
		}
	}

}
