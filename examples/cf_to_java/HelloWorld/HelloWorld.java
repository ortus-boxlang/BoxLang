// Auto package creation according to file path on disk
package c_drive.projects.examples;

// Interface for object representing a .cfm template

import ortus.boxlang.runtime.dynamic.ITemplate;
import ortus.boxlang.runtime.ExecutionContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;

// This class reponsible for finding and loading Java classes
// If we don't know the type: java or boxlang, we use ClassLoader so it can figure it out via the ClassLocator class
import ortus.boxlang.runtime.interop.JavaLoader;
// Called anytime we have (expression)() in our code, which invokes the BIF, UDF, or Java method (callsite)
import ortus.boxlang.runtime.interop.Invoker;

// Need better name, but generically handles de-referncing of `foo.bar` or `foo['bar']` based on runtime values
import ortus.boxlang.runtime.core.Derefrencer;

// Operators-- do we have a class for ecah one or one huge class of all operators?
import ortus.boxlang.runtime.operators.EqualsEquals;
import ortus.boxlang.runtime.operators.Concat;

// Classes Auto-Imported on all Templates and Classes by BoxLang
// TODO: Determine which classes will be auto-imported
import java.time.LocalDateTime;
import java.lang.System;
import java.lang.String;
import java.lang.Character;
import java.lang.Boolean;
import java.lang.Double;
import java.lang.Integer;

public class HelloWorld$cfm implements ITemplate {

	// The name of the current template
	public static final String name = "HelloWorld";
	// The type of extension the template has
	public static final String extension = "cfm";
	// The absolute path of the current template
	public static final String path = "c:/projects/examples";
	// The last modified date of the current template
	public static final LocalDateTime lastModified = "2023-07-26T17:45:16.669276";
	// The date time of compilation of this template
	public static final LocalDateTime compiledOn = "2023-07-26T17:45:16.669276";
	// The AST that models this template
	public static final CFScript ast = astRep;

	/**
	 * Each template must implement the invoke() method which executes the template
	 *
	 * @param context The execution context requesting the execution
	 */
	public void invoke( ExecutionContext context ) throws Throwable {
		// Reference to the variables scope
		// TODO: How will we track this brad, since this belongs to THIS template, shouldn't each template get
		// it's own `variables` scope
		IScope variablesScope = context.getVariablesScope();

		// Case sensitive set
		variablesScope.set( Key.of( "system" ), JavaLoader.load( context, "java.lang.System" ) );

		// This is a Class, or a Java proxy instance?
		// Let's start with a direct class for now
		Class oString = JavaLoader.load( context, "java.lang.String" );

		variablesScope.set(
			// Case insensitive set
			Key.of( "GREETING" ),

			// Invoke callsite
			Invoker.invokeConstructor( context,

				// Method: De-reference String.init
				Derefrencer.derefrence( context, oString, "init" ),

				// Argument Values
				new Object[] { "Hello" },

				// Argument Class Types
				new Object[] { String.class } ) );

		if ( EqualsEquals.invoke( context, variablesScope.get( Key.of( "GREETING" ) ), "Hello" ) ) {

			Invoker.invoke(
				// Context
				context,

				// Method
				Derefrencer.derefrence( context,
					Derefrencer.derefrence( context, variablesScope.get( Key.of( "system" ) ), "out" ), "println" ),

				// Argument Values
				new Object[] { Concat.invoke( context, context.scopeFind( Key.of( "GREETING" ) ), " world" ) },

				// Argument Type Classes
				new Object[] { String.class } );

		}

	}

}