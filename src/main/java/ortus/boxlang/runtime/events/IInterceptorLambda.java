/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http: //www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.runtime.events;

import ortus.boxlang.runtime.types.IStruct;

/**
 * This interface is used to define a Java interceptor.
 * An interceptor is a class that can be used to intercept events in BoxLang and discovered by Module Services.
 */
@FunctionalInterface
public interface IInterceptorLambda extends IInterceptor {

	/**
	 * This method is called by the BoxLang runtime to intercept an event
	 *
	 * @param data The data to intercept
	 *
	 * @return True, if you want to stop the chain. False or null if you want to continue the chain.
	 */
	public Boolean intercept( IStruct data );

	/**
	 * This method is called by the BoxLang runtime to configure the interceptor
	 * with a Struct of properties
	 *
	 * @param properties The properties to configure the interceptor with (if any)
	 */
	public default void configure( IStruct properties ) {
		// not needed
	}

}
