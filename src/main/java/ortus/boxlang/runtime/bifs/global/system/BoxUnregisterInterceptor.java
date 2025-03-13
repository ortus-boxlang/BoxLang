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
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;

@BoxBIF
public class BoxUnregisterInterceptor extends BIF {

	protected InterceptorPool interceptorTarget;

	/**
	 * Constructor
	 */
	public BoxUnregisterInterceptor() {
		super();
		declaredArguments		= new Argument[] {
		    new Argument( true, Argument.ANY, Key.interceptor ),
		    new Argument( false, Argument.ANY, Key.states, new Array() )
		};
		this.interceptorTarget	= this.interceptorService;
	}

	/**
	 * UnRegisters a global interceptor from all or specific states
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.interceptor The reference to the interceptor to unregister
	 *
	 * @argument.states An array of events to unregister to along side the points discovered in the incoming interceptor. If not passed, all states
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Object			interceptor	= arguments.get( Key.interceptor );
		Key[]			states		= InterceptorPool.inflateStates( arguments.get( Key.states ) );
		DynamicObject	target		= ( interceptor instanceof DynamicObject castedObject )
		    ? castedObject
		    : DynamicObject.of( interceptor );

		if ( states.length == 0 ) {
			this.interceptorTarget.unregister( target );
		} else {
			this.interceptorTarget.unregister( target, states );
		}

		return true;
	}

}
