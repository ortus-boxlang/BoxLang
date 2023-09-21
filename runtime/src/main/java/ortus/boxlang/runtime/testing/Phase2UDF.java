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

import java.util.List;
import java.util.Map;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
// BoxLang Auto Imports
import ortus.boxlang.runtime.dynamic.BaseTemplate;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Phase 2 BoxLang
 * Example of UDF delcaration and execution
 */
public class Phase2UDF extends BaseTemplate {

	private static Phase2UDF					instance;

	private static final List<ImportDefinition>	imports	= List.of();

	private Phase2UDF() {
	}

	public static synchronized Phase2UDF getInstance() {
		if ( instance == null ) {
			instance = new Phase2UDF();
		}
		return instance;
	}

	/*
	 * <pre>
	 * <cfscript>
	 * string function greet( required string name='Brad' ) hint="My Function Hint" {
	 * local.race = "Local scope value";
	 * arguments.race = "Arguments scope value";
	 * 
	 * var greeting = "Hello " & name;
	 * 
	 * // Reach "into" parent context and get "out" from variables scope
	 * out.println( "Inside UDF, race scope lookup finds: " & race )
	 * 
	 * return greeting;
	 * }
	 * 
	 * variables.out = (create java.lang.System).out;
	 * 
	 * // Positional args
	 * variables.out.println( greet( 'John' ) );
	 * 
	 * // named args
	 * variables.out.println( greet( name='John' ) );
	 * </cfscript>
	 * </pre>
	 */

	@Override
	public void _invoke( IBoxContext context ) throws Throwable {
		ClassLocator	classLocator	= ClassLocator.getInstance();
		IScope			variablesScope	= context.getScopeNearby( Key.of( "variables" ) );

		// Create instance of UDF and register in the variables scope
		context.registerUDF( Phase2UDF$greet.getInstance() );

		variablesScope.put(
		    Key.of( "out" ),
		    Referencer.get(
		        classLocator.load( context, "java:java.lang.System", imports ),
		        Key.of( "out" ),
		        false )
		);

		// Positional args
		Referencer.getAndInvoke(
		    context,
		    // Object
		    variablesScope.get( Key.of( "out" ) ),
		    // Method
		    Key.of( "println" ),
		    // Arguments
		    new Object[] {
		        context.invokeFunction( Key.of( "greet" ), new Object[] { "John" } )
		    },
		    false
		);

		// named args
		Referencer.getAndInvoke(
		    context,
		    // Object
		    variablesScope.get( Key.of( "out" ) ),
		    // Method
		    Key.of( "println" ),
		    // Arguments
		    new Object[] {
		        context.invokeFunction( Key.of( "greet" ), Map.of( Key.of( "name" ), "Bob" ) )
		    },
		    false
		);

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
		BoxRuntime boxRuntime = BoxRuntime.getInstance( true );

		try {
			boxRuntime.executeTemplate( Phase2UDF.getInstance() );
		} catch ( Throwable e ) {
			e.printStackTrace();
			System.exit( 1 );
		}

		// Bye bye! Ciao Bella!
		boxRuntime.shutdown();
	}
}
