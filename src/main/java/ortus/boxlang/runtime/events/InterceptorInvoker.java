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
package ortus.boxlang.runtime.events;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.types.IStruct;

@FunctionalInterface
interface InterceptorInvoker {

	/**
	 * Invokes the interceptor with the given data and context.
	 * This is done in order to avoid runtime lookups/instance checks when invoking the interceptor.
	 *
	 * @param data     The data to be passed to the interceptor.
	 * @param context  The context in which the interceptor is invoked.
	 * @param observer The observer that is being invoked.
	 *
	 * @return The result of the interceptor invocation, which can be null or boolean
	 */
	Object invoke( IStruct data, IBoxContext context, DynamicObject observer );
}
