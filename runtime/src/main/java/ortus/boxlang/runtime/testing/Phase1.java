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
import ortus.boxlang.runtime.dynamic.BaseTemplate;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.operators.*;
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

public class Phase1 extends BaseTemplate {

	private static Phase1 instance;

	private Phase1() {
	}

	public static synchronized Phase1 getInstance() {
		if ( instance == null ) {
			instance = new Phase1();
		}
		return instance;
	}

	@Override
	public void invoke( IBoxContext context ) throws Throwable {
		ClassLocator	classLocator	= ClassLocator.getInstance();

		// Reference to the variables scope
		IScope			variablesScope	= context.getScopeLocal( Key.of( "variables" ) );

		// Case sensitive set
		variablesScope.put( Key.of( "system" ), classLocator.load( context, "java.lang.System", "java" ) );

		variablesScope.put(
		        // Case insensitive set
		        Key.of( "GREETING" ),

		        // Every class (box|java) is represented as a DynamicObject
		        classLocator
		                .load( context, "java.lang.String", "java" )
		                .invokeConstructor( new Object[] { "Hello" } )
		);

		if ( EqualsEquals.invoke( variablesScope.get( Key.of( "GREETING" ) ), "Hello" ) ) {

			Referencer.getAndInvoke(

			        // Object
			        Referencer.get(
			                variablesScope.get( Key.of( "SYSTEM" ) ),
			                Key.of( "out" ),
			                false
			        ),

			        // Method
			        Key.of( "println" ),

			        // Arguments
			        new Object[] {

			                Concat.invoke(
			                        context.scopeFindLocal( Key.of( "GREETING" ) ),
			                        " world"
			                )

					},
			        false

			);

		}

	}

	/**
	 * Main method
	 *
	 * @param args
	 */
	public static void main( String[] args ) {
		// This is the main method, it will be invoked when the template is executed
		// You can use this
		// Get a runtime going
		BoxRuntime.startup( true );

		try {
			BoxRuntime.executeTemplate( "" );
		} catch ( Throwable e ) {
			e.printStackTrace();
			System.exit( 1 );
		}

		// Bye bye! Ciao Bella!
		BoxRuntime.shutdown();
	}
}
