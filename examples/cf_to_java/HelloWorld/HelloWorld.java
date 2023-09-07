// Auto package creation according to file path on disk
package c_drive.projects.examples;

// BoxLang Auto Imports

import ortus.boxlang.runtime.context.TemplateContext;
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

// Imports based on AST Tree
import ortus.boxlang.runtime.operators.EqualsEquals;
import ortus.boxlang.runtime.operators.Concat;

public class HelloWorld$cfm extends BaseTemplate {

	// Auto-Generated Singleton Helpers
	private static HelloWorld$cfm instance;

	private HelloWorld$cfm() {
		this.name         = "HelloWorld";
		this.extension    = "cfm";
		this.path         = "c:/projects/examples";
		this.lastModified = "2023-07-26T17:45:16.669276";
		this.compiledOn   = "2023-07-26T17:45:16.669276";
		// this.ast = ???
	}

	public static synchronized HelloWorld$cfm getInstance() {
		if ( instance == null ) {
			instance = new HelloWorld$cfm();
		}
		return instance;
	}

	/**
	 * Each template must implement the invoke() method which executes the template
	 *
	 * @param context The execution context requesting the execution
	 */
	public void invoke( ExecutionContext context ) throws Throwable {

		// Reference to the variables scope
		IScope variablesScope = context.getScopeNearby( Key.of( "variables" ) );

		ClassLocator JavaLoader = ClassLocator.getInstance();

		// Case sensitive set
		variablesScope.put( Key.of( "system" ), JavaLoader.load( context, "java.lang.System" ) );

		variablesScope.put(
			// Case insensitive set
			Key.of( "GREETING" ),

			// Invoke callsite
			ClassLoader.load( context, "java.lang.String" ).invokeConstructor(
				// Argument Values
				new Object[] { "Hello" } ) );

		if ( EqualsEquals.invoke( context, variablesScope.get( Key.of( "GREETING" ) ), "Hello" ) ) {

			Referencer.getAndInvoke(
				context,
				// Object
				Referencer.get( variablesScope.get( Key.of( "SYSTEM" ) ), Key.of( "OUT" ) ),

				// Method
				"println",

				// Arguments
				new Object[] {

					Concat.invoke( context, context.scopeFindNearby( Key.of( "GREETING" ), null ).value(), " world" )

				}

			);
		}
	}

}