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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.operators.Concat;
import ortus.boxlang.runtime.operators.Elvis;
import ortus.boxlang.runtime.operators.EqualsEquals;
import ortus.boxlang.runtime.operators.Increment;
import ortus.boxlang.runtime.runnables.BoxTemplate;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;

public class Phase1 extends BoxTemplate {

	private static Phase1						instance;

	private static final List<ImportDefinition>	imports			= List.of();

	private static final Path					path			= Paths.get( "runtime\\src\\main\\java\\ortus\\boxlang\\runtime\\testing\\Phase1.java" );
	private static final long					compileVersion	= 1L;
	private static final LocalDateTime			compiledOn		= LocalDateTime.parse( "2023-09-27T10:15:30" );
	private static final Object					ast				= null;

	private Phase1() {
	}

	public static synchronized Phase1 getInstance() {
		if ( instance == null ) {
			instance = new Phase1();
		}
		return instance;
	}

	/*
	 * <pre>
	 * <cfscript>
	 * // Static reference to System (java proxy?)
	 * variables['system'] = create java:java.lang.System;
	 *
	 * server.counter = server.counter ?: 0'
	 * request.running = true;
	 *
	 * // call constructor to create instance
	 * variables.greeting = new java:java.lang.String( 'Hello' );
	 *
	 *
	 * // Conditional, requires operation support
	 * if( variables.greeting == 'Hello' ) {
	 * // De-referencing "out" and "println" and calling Java method via invoke dynamic
	 * variables.system.out.println(
	 * // Multi-line statement, expression requires concat operator and possible casting
	 * // Unscoped lookup requires scope search
	 * greeting & " world " & server.counter
	 * )
	 * }
	 * </cfscript>
	 * </pre>
	 */

	@Override
	public void _invoke( IBoxContext context ) {
		ClassLocator	classLocator	= ClassLocator.getInstance();

		// Reference to the variables scope
		IScope			variablesScope	= context.getScopeNearby( Key.of( "variables" ) );
		IScope			serverScope		= context.getScopeNearby( Key.of( "server" ) );
		IScope			requestScope	= context.getScopeNearby( Key.of( "server" ) );

		// Case sensitive set
		variablesScope.assign( context, Key.of( "system" ), classLocator.load( context, "java:java.lang.System", imports ) );

		serverScope.assign(
		    context,
		    Key.of( "counter" ),
		    Elvis.invoke( serverScope.dereference( context, Key.of( "counter" ), true ), 0 )
		);

		requestScope.assign( context, Key.of( "running" ), true );

		variablesScope.assign(
		    context,
		    // Case insensitive set
		    Key.of( "GREETING" ),

		    // Every class (box|java) is represented as a DynamicObject
		    classLocator
		        .load( context, "java:java.lang.String", imports )
		        .invokeConstructor( context, new Object[] { "Hello" } ) );

		if ( EqualsEquals.invoke( variablesScope.get( Key.of( "GREETING" ) ), "Hello" ) ) {

			Referencer.getAndInvoke(
			    context,
			    // Object
			    Referencer.get(
			        context,
			        variablesScope.get( Key.of( "SYSTEM" ) ),
			        Key.of( "out" ),
			        false ),

			    // Method
			    Key.of( "println" ),

			    // Arguments
			    new Object[] {

			        Concat.invoke(
			            context.scopeFindNearby( Key.of( "GREETING" ), null ).value(),
			            Concat.invoke( " world ", serverScope.get( Key.of( "counter" ) ) )
			        )

				},
			    false

			);

			Increment.invokePost( context, serverScope, Key.of( "counter" ) );
		}

	}

	// ITemplateRunnable implementation methods

	/**
	 * The version of the BoxLang runtime
	 */
	public long getRunnableCompileVersion() {
		return Phase1.compileVersion;
	}

	/**
	 * The date the template was compiled
	 */
	public LocalDateTime getRunnableCompiledOn() {
		return Phase1.compiledOn;
	}

	/**
	 * The AST (abstract syntax tree) of the runnable
	 */
	public Object getRunnableAST() {
		return Phase1.ast;
	}

	/**
	 * The path to the template
	 */
	public Path getRunnablePath() {
		return Phase1.path;
	}

	/**
	 * The original source type
	 */
	public BoxSourceType getSourceType() {
		return BoxSourceType.BOXSCRIPT;
	}

	/**
	 * The imports for this runnable
	 */
	public List<ImportDefinition> getImports() {
		return imports;
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
			boxRuntime.executeTemplate( Phase1.getInstance() );
		} catch ( Throwable e ) {
			e.printStackTrace();
			System.exit( 1 );
		}

		// Bye bye! Ciao Bella!
		boxRuntime.shutdown();
	}
}
