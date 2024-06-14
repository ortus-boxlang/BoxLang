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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.context.ApplicationBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.types.Function;

/**
 * This is a BoxLang proxy class for functional interfaces so we can use them in BoxLang
 * via type casting and coercion.
 */
public class BaseProxy {

	/**
	 * The target function that this proxy is wrapping.
	 */
	protected Function				target;

	/**
	 * The context that created this proxy.
	 */
	protected IBoxContext			context;

	/**
	 * App context if any
	 */
	protected ApplicationBoxContext	appContext;

	/**
	 * The logger for this proxy.
	 */
	protected static final Logger	logger	= LoggerFactory.getLogger( BaseProxy.class );

	/**
	 * Constructor for the proxy.
	 */
	public BaseProxy( Function target, IBoxContext context ) {
		this.target		= target;
		this.context	= context;
		this.appContext	= context.getParentOfType( ApplicationBoxContext.class );

	}

	/**
	 * Utility Methods
	 */

	/**
	 * Get the current thread
	 */
	protected Thread getCurrentThread() {
		return Thread.currentThread();
	}

	/**
	 * Get the current thread name
	 */
	protected String getCurrentThreadName() {
		return Thread.currentThread().getName();
	}

	/**
	 * Check if I am in the fork join pool
	 */
	protected boolean isInForkJoinPool() {
		return getCurrentThread().getName().startsWith( "ForkJoinPool" );
	}
}
