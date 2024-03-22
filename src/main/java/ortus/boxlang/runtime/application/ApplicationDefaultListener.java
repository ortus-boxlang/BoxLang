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
package ortus.boxlang.runtime.application;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;

/**
 * I represent a default Application listener
 */
public class ApplicationDefaultListener extends ApplicationListener {

	/**
	 * Constructor
	 *
	 * @param listener An Application class instance
	 */
	public ApplicationDefaultListener( RequestBoxContext context ) {
		super( context );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	public void onRequest( IBoxContext context, Object[] args ) {
		// Then include the requested template
		context.includeTemplate( ( String ) args[ 0 ] );
	}

	public boolean onRequestStart( IBoxContext context, Object[] args ) {
		return true;
	}

	public void onSessionStart( IBoxContext context, Object[] args ) {
	}

	public boolean onApplicationStart( IBoxContext context, Object[] args ) {
		return true;
	}

}
