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
import ortus.boxlang.runtime.context.CatchBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;

// BoxLang Auto Imports
import ortus.boxlang.runtime.dynamic.BaseTemplate;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.operators.*;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;
import ortus.boxlang.runtime.scopes.IScope;

// Classes Auto-Imported on all Templates and Classes by BoxLang
import java.time.LocalDateTime;
import java.util.List;
import java.time.Instant;

public class Phase1TryCatch extends BaseTemplate {

	private static Phase1TryCatch				instance;

	private final static List<ImportDefinition>	imports	= List.of();

	private Phase1TryCatch() {
	}

	public static synchronized Phase1TryCatch getInstance() {
		if ( instance == null ) {
			instance = new Phase1TryCatch();
		}
		return instance;
	}

	/**
	 * <pre>
	<cfscript>
	  system = create java:java.lang.System;
	
	  try {
		1/0
	  } catch (any e) {
	    variables.system.out.println(e.message);
	  } finally {
	    variables.system.out.println("Finally");
	  }
	
	  try {
		throw new java:ortus.boxlang.runtime.types.exceptions.BoxLangException( "My Message", "My detail", "com.foo.type" );
	  } catch ("com.foo.type" e) {
	   variables.system.out.println(e.message);
	  } catch (java.lang.RuntimeException e) {
	   variables.system.out.println(e.message);
	  }
	</cfscript>
	 * </pre>
	 */

	@Override
	public void invoke( IBoxContext context ) throws Throwable {
		ClassLocator	classLocator	= ClassLocator.getInstance();
		IBoxContext		catchContext;

		// Reference to the variables scope
		IScope			variablesScope	= context.getScopeNearby( Key.of( "variables" ) );
		variablesScope.put(
		    Key.of( "system" ),
		    classLocator.load( context, "java:java.lang.System", imports )
		);

		try {
			Divide.invoke( 1, 0 );
		} catch ( Throwable e ) {
			// This this context for all code in the catch block
			catchContext = new CatchBoxContext( context, Key.of( "e" ), e );
			Referencer.getAndInvoke(
			    // Object
			    Referencer.get(
			        variablesScope.get( Key.of( "system" ) ),
			        Key.of( "out" ),
			        false
			    ),
			    // Method
			    Key.of( "println" ),
			    // Arguments
			    new Object[] {
			        Referencer.get(
			            catchContext.scopeFindNearby( Key.of( "e" ), null ).value(),
			            Key.of( "message" ),
			            false
			        )

				},
			    false

			);

		} finally {

			Referencer.getAndInvoke(
			    // Object
			    Referencer.get(
			        variablesScope.get( Key.of( "system" ) ),
			        Key.of( "out" ),
			        false
			    ),
			    // Method
			    Key.of( "println" ),
			    // Arguments
			    new Object[] { "Finally" },
			    false
			);
		}

		try {
			throw ( Throwable ) classLocator
			    .load( context, "java:ortus.boxlang.runtime.types.exceptions.BoxLangException", imports )
			    .invokeConstructor( new Object[] { "My Message", "My detail", "com.foo.type" } ).getTargetInstance();
		} catch ( Throwable e ) {
			// This this context for all code in the catch block
			catchContext = new CatchBoxContext( context, Key.of( "e" ), e );

			if ( ExceptionUtil.exceptionIsOfType( catchContext, e, "com.foo.type" ) ) {
				Referencer.getAndInvoke(
				    // Object
				    Referencer.get(
				        variablesScope.get( Key.of( "system" ) ),
				        Key.of( "out" ),
				        false
				    ),
				    // Method
				    Key.of( "println" ),
				    // Arguments
				    new Object[] {
				        Referencer.get(
				            catchContext.scopeFindNearby( Key.of( "e" ), null ).value(),
				            Key.of( "message" ),
				            false
				        )
				    },
				    false
				);
			} else if ( ExceptionUtil.exceptionIsOfType( catchContext, e, "java.lang.RuntimeException" ) ) {
				Referencer.getAndInvoke(
				    // Object
				    Referencer.get(
				        variablesScope.get( Key.of( "system" ) ),
				        Key.of( "out" ),
				        false
				    ),
				    // Method
				    Key.of( "println" ),
				    // Arguments
				    new Object[] {
				        Referencer.get(
				            catchContext.scopeFindNearby( Key.of( "e" ), null ),
				            Key.of( "message" ),
				            false
				        )
				    },
				    false
				);
			} else {
				// Because there is no "any" catch block, we rethrow if we didn't match the type above.
				throw e;
			}
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
			BoxRuntime.executeTemplate( Phase1TryCatch.getInstance() );
		} catch ( Throwable e ) {
			e.printStackTrace();
			System.exit( 1 );
		}

		// Bye bye! Ciao Bella!
		BoxRuntime.shutdown();
	}
}
