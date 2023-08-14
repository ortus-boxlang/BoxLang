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
package ortus.boxlang.runtime.testing;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
// BoxLang Auto Imports
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.BaseTemplate;
import ortus.boxlang.runtime.interop.ClassInvoker;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.IScope;

// Classes Auto-Imported on all Templates and Classes by BoxLang
import java.time.LocalDateTime;
import java.time.Instant;
import java.lang.System;
import java.lang.String;
import java.lang.Character;
import java.lang.Boolean;
import java.lang.Double;
import java.lang.Integer;

public class MockTemplate extends BaseTemplate {

	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */

	private static MockTemplate instance;

	private MockTemplate() {
	}

	public static synchronized MockTemplate getInstance() {
		if ( instance == null ) {
			instance = new MockTemplate();
		}
		return instance;
	}

	@Override
	public void invoke( IBoxContext context ) throws Exception {
		IScope variablesScope = context.getScopeLocal( Key.of( "variables" ) );

		// I can store variables in the context
		variablesScope.put( Key.of( "MockTemplate" ), "Yea baby!!" );
		// Case sensitive set
		variablesScope.put( Key.of( "system" ), ClassLocator.getInstance().load( context, "java.lang.System" ) );

		System.out.println( "MockTemplate invoked, woot woot!" );
	}

	public static void main( String[] args ) {
		// This is the main method, it will be invoked when the template is executed
		// You can use this
		// Get a runtime going
		BoxRuntime.startup();

		try {
			BoxRuntime.getInstance().executeTemplate( "" );
		} catch ( Exception e ) {
			e.printStackTrace();
			System.exit( 1 );
		}

		// Bye bye! Ciao Bella!
		BoxRuntime.shutdown();
	}
}
