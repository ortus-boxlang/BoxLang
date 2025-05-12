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

import ortus.boxlang.runtime.interop.DynamicObject;

/**
 * This represents the interceptor object and it's invoker to avoid runtime lookups
 * when invoking the interceptor.
 */
public class InterceptorEntry {

	public final DynamicObject		interceptor;
	public final InterceptorInvoker	invoker;

	/**
	 * Constructor
	 *
	 * @param interceptor The interceptor object
	 * @param invoker     The interceptor invoker
	 */
	InterceptorEntry( DynamicObject interceptor, InterceptorInvoker invoker ) {
		this.interceptor	= interceptor;
		this.invoker		= invoker;
	}

	/**
	 * Equals method. Verifies if the incoming object is the same as this interceptor.
	 * This is used to verify if the interceptor is already registered in the interceptor state
	 *
	 * @param obj The object to compare
	 *
	 * @return true if the object is the same as this interceptor
	 */
	public boolean equals( DynamicObject obj ) {
		return this.interceptor.equals( obj );
	}

}
