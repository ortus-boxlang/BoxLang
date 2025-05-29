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

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.events.Interceptor;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * IInterceptorLambda is a proxy class that implements the IInterceptorLambda interface.
 * It is used to intercept events in the BoxLang runtime.
 * This is used to wrap closures or lambdas that are registered as interceptors.
 */
@Interceptor( autoLoad = false )
public class IInterceptorLambda extends BaseProxy implements ortus.boxlang.runtime.events.IInterceptorLambda {

	/**
	 * Default constructor for IInterceptorLambda.
	 * Initializes the proxy without any target or context.
	 */
	public IInterceptorLambda() {
		super();
	}

	/**
	 * Constructor for IInterceptorLambda.
	 * Initializes the proxy with a target object, context, and method name.
	 *
	 * @param target  The target object that this proxy will wrap.
	 * @param context The context in which this proxy operates.
	 * @param method  The method name to be invoked on the target.
	 */
	public IInterceptorLambda( Object target, IBoxContext context, String method ) {
		super( target, context, method );
	}

	@Override
	public Boolean intercept( IStruct data ) {
		// Does the data have a Function
		if ( data.containsKey( Key.function ) && isFunctionTarget() ) {
			// If so, then if the incoming function is the same as the target, then just return true
			// If not, we will do recursive invocation
			if ( data.get( Key.function ) == getAsFunction() ) {
				return true;
			}
		}

		try {
			return BooleanCaster.cast( invoke( data ) );
		} catch ( Exception e ) {
			getLogger().error( "Error invoking IInterceptorLambda", e );
			throw new BoxRuntimeException( "Error invoking IInterceptorLambda", e );
		}
	}

}
