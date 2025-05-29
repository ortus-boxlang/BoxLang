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
package ortus.boxlang.runtime.bifs.global.system;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.events.InterceptorPool;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

@BoxBIF
public class BoxRegisterInterceptor extends BIF {

	protected InterceptorPool interceptorTarget;

	/**
	 * Constructor
	 */
	public BoxRegisterInterceptor() {
		super();
		declaredArguments		= new Argument[] {
		    new Argument( true, Argument.ANY, Key.interceptor ),
		    new Argument( false, Argument.ANY, Key.states, new Array() )
		};
		this.interceptorTarget	= this.interceptorService;
	}

	/**
	 * Registers a global interceptor that can listen to global runtime events.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.interceptor This can be a class or a closure/lambda that will listen to global events
	 *
	 * @argument.states An array of events to listen to along side the points discovered in the incoming interceptor
	 *
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object	interceptor	= arguments.get( Key.interceptor );
		Key[]	states		= InterceptorPool.inflateStates( arguments.get( Key.states ) );

		switch ( interceptor ) {
			// BoxLang Classes
			case IClassRunnable boxClass -> {
				this.interceptorTarget
				    .registerInterceptionPoint( states )
				    .register( boxClass );
			}

			// Any Java Class
			case DynamicObject boxObject -> {
				this.interceptorTarget
				    .register( boxObject, states );
			}

			// Any BoxLang Closure/UDF/Lambda
			case Function castedFunction -> {
				// If the states are empty, throw an exception, closures need at least one state to listen to
				if ( states.length == 0 ) {
					throw new BoxRuntimeException( "Closures/Lambdas need at least one state to listen to" );
				}

				this.interceptorTarget
				    .register(
				        new ortus.boxlang.runtime.interop.proxies.IInterceptorLambda( castedFunction, context, null ),
				        states
				    );
			}

			default -> {
				throw new BoxRuntimeException(
				    "Invalid interceptor type. Expected a class, dynamic object, or closure/lambda, but got: "
				        + interceptor.getClass().getName()
				);
			}
		}

		return true;
	}

}
