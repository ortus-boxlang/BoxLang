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

import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;

@BoxBIF( description = "Register a request-level interceptor" )
public class BoxRegisterRequestInterceptor extends BoxRegisterInterceptor {

	/**
	 * Constructor
	 */
	public BoxRegisterRequestInterceptor() {
		super();
	}

	/**
	 * Registers a request interceptor with the application listener's interceptor pool, so it only listens to events for the application
	 * request lifecycle. Behaves like boxRegisterInterceptor() otherwise: the interceptor can be a BoxLang class, a Java object, or a
	 * closure/lambda (which needs at least one state).
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.interceptor A BoxLang class, a Java object, or a closure/lambda that will listen to request events.
	 *
	 * @argument.states An array of events to listen to alongside the points discovered in the incoming interceptor. Required for closures/lambdas.
	 *
	 * @return true once the interceptor is registered.
	 */
	@Override
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		this.interceptorTarget = context.getRequestContextOrFail()
		    .getApplicationListener()
		    .getInterceptorPool();

		return super._invoke( context, arguments );
	}

}
