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
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.operators.*;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.IScope;

// Classes Auto-Imported on all Templates and Classes by BoxLang
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.time.Instant;

public class Phase1 extends BaseTemplate {

	private static Phase1						instance;

	private final static List<ImportDefinition>	imports	= List.of();

	private Phase1() {
	}

	public static synchronized Phase1 getInstance() {
		if ( instance == null ) {
			instance = new Phase1();
		}
		return instance;
	}

	/**
	 * <pre>
	<cfscript>
	  // Static reference to System (java proxy?)
	  variables['system'] = create java:java.lang.System;
	  // call constructor to create instance
	  variables.greeting = new java:java.lang.String( 'Hello' );
	
	
	  // Conditional, requires operation support
	  if( variables.greeting == 'Hello' ) {
	    // De-referencing "out" and "println" and calling Java method via invoke dynamic
	    variables.system.out.println(
	      // Multi-line statement, expression requires concat operator and possible casting
	      // Unscoped lookup requires scope search
	      greeting & " world"
	    )
	  }
	</cfscript>
	 * </pre>
	 */

	@Override
	public void invoke( IBoxContext context ) throws Throwable {
		ClassLocator	classLocator	= ClassLocator.getInstance();

		// Reference to the variables scope
		IScope			variablesScope	= context.getScopeNearby( Key.of( "variables" ) );

		// Case sensitive set
		variablesScope.put( Key.of( "system" ), classLocator.load( context, "java:java.lang.System", imports ) );

		variablesScope.put(
		    // Case insensitive set
		    Key.of( "GREETING" ),

		    // Every class (box|java) is represented as a DynamicObject
		    classLocator
		        .load( context, "java:java.lang.String", imports )
		        .invokeConstructor( new Object[] { "Hello" } ) );

		if ( EqualsEquals.invoke( variablesScope.get( Key.of( "GREETING" ) ), "Hello" ) ) {

			Referencer.getAndInvoke(

			    // Object
			    Referencer.get(
			        variablesScope.get( Key.of( "SYSTEM" ) ),
			        Key.of( "out" ),
			        false ),

			    // Method
			    Key.of( "println" ),

			    // Arguments
			    new Object[] {

			        Concat.invoke(
			            context.scopeFindNearby( Key.of( "GREETING" ), null ).value(),
			            " world" )

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
			BoxRuntime.executeTemplate( Phase1.getInstance() );
		} catch ( Throwable e ) {
			e.printStackTrace();
			System.exit( 1 );
		}

		// Bye bye! Ciao Bella!
		BoxRuntime.shutdown();
	}
}
