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

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.CatchBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.IBoxContext.ScopeSearchResult;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.operators.Divide;
import ortus.boxlang.runtime.runnables.BoxTemplate;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;

public class Phase1TryCatch extends BoxTemplate {

	private static Phase1TryCatch				instance;

	private final static List<ImportDefinition>	imports			= List.of();

	private static final Path					path			= Paths
	    .get( "runtime\\src\\main\\java\\ortus\\boxlang\\runtime\\testing\\Phase1TryCatch.java" );
	private static final long					compileVersion	= 1L;
	private static final LocalDateTime			compiledOn		= LocalDateTime.parse( "2023-09-27T10:15:30" );
	private static final Object					ast				= null;

	private Phase1TryCatch() {
	}

	public static synchronized Phase1TryCatch getInstance() {
		if ( instance == null ) {
			instance = new Phase1TryCatch();
		}
		return instance;
	}

	/*
	 * <pre>
	 * <cfscript>
	 * system = create java:java.lang.System;
	 *
	 * try {
	 * 1/0
	 * } catch (any e) {
	 * variables.system.out.println(e.message);
	 * } finally {
	 * variables.system.out.println("Finally");
	 * }
	 *
	 * try {
	 * throw new java:ortus.boxlang.runtime.types.exceptions.BoxRuntimeException( "My Message", "My detail", "com.foo.type" );
	 * } catch ("com.foo.type" e) {
	 * variables.system.out.println(e.message);
	 * } catch (java.lang.RuntimeException e) {
	 * variables.system.out.println(e.message);
	 * }
	 * </cfscript>
	 * </pre>
	 */

	@Override
	public void _invoke( IBoxContext context ) {
		ClassLocator		classLocator	= ClassLocator.getInstance();
		IBoxContext			catchContext;

		// Reference to the variables scope
		IScope				variablesScope	= context.getScopeNearby( Key.of( "variables" ) );

		// unscoped assignment performs lookup to find the scope to assign to
		ScopeSearchResult	result			= context.scopeFindNearby(
		    Key.of( "system" ),
		    context.getDefaultAssignmentScope()
		);
		result.scope().assign(
		    context,
		    Key.of( "system" ),
		    classLocator.load( context, "java:java.lang.System", imports )
		);

		try {
			Divide.invoke( 1, 0 );
		} catch ( Throwable e ) {
			// This this context for all code in the catch block
			catchContext = new CatchBoxContext( context, Key.of( "e" ), e );
			Referencer.getAndInvoke(
			    context,
			    // Object
			    Referencer.get(
			        context,
			        variablesScope.get( Key.of( "system" ) ),
			        Key.of( "out" ),
			        false
			    ),
			    // Method
			    Key.of( "println" ),
			    // Arguments
			    new Object[] {
			        Referencer.get(
			            context,
			            catchContext.scopeFindNearby( Key.of( "e" ), null ).value(),
			            Key.of( "message" ),
			            false
			        )

				},
			    false

			);

		} finally {

			Referencer.getAndInvoke(
			    context,
			    // Object
			    Referencer.get(
			        context,
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
			    .load( context, "java:ortus.boxlang.runtime.types.exceptions.BoxRuntimeException", imports )
			    .invokeConstructor( context, new Object[] { "My Message", "My detail", "com.foo.type" } ).getTargetInstance();
		} catch ( Throwable e ) {
			// This this context for all code in the catch block
			catchContext = new CatchBoxContext( context, Key.of( "e" ), e );

			if ( ExceptionUtil.exceptionIsOfType( catchContext, e, "com.foo.type" ) ) {
				Referencer.getAndInvoke(
				    context,
				    // Object
				    Referencer.get(
				        context,
				        variablesScope.get( Key.of( "system" ) ),
				        Key.of( "out" ),
				        false
				    ),
				    // Method
				    Key.of( "println" ),
				    // Arguments
				    new Object[] {
				        Referencer.get(
				            context,
				            catchContext.scopeFindNearby( Key.of( "e" ), null ).value(),
				            Key.of( "message" ),
				            false
				        )
				    },
				    false
				);
			} else if ( ExceptionUtil.exceptionIsOfType( catchContext, e, "java.lang.RuntimeException" ) ) {
				Referencer.getAndInvoke(
				    context,
				    // Object
				    Referencer.get(
				        context,
				        variablesScope.get( Key.of( "system" ) ),
				        Key.of( "out" ),
				        false
				    ),
				    // Method
				    Key.of( "println" ),
				    // Arguments
				    new Object[] {
				        Referencer.get(
				            context,
				            catchContext.scopeFindNearby( Key.of( "e" ), null ).value(),
				            Key.of( "message" ),
				            false
				        )
				    },
				    false
				);
			} else {
				// Because there is no "any" catch block, we rethrow if we didn't match the type above.)
				if ( e instanceof RuntimeException re ) {
					throw re;
				} else {
					// Pretty sure this can never be reached, but the compiler won't let me blindly rethrow a Throwable
					throw new BoxRuntimeException( e.getMessage(), e );
				}
			}
		}

	}

	// ITemplateRunnable implementation methods

	/**
	 * The version of the BoxLang runtime
	 */
	public long getRunnableCompileVersion() {
		return Phase1TryCatch.compileVersion;
	}

	/**
	 * The date the template was compiled
	 */
	public LocalDateTime getRunnableCompiledOn() {
		return Phase1TryCatch.compiledOn;
	}

	/**
	 * The AST (abstract syntax tree) of the runnable
	 */
	public Object getRunnableAST() {
		return Phase1TryCatch.ast;
	}

	/**
	 * The path to the template
	 */
	public Path getRunnablePath() {
		return Phase1TryCatch.path;
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
			boxRuntime.executeTemplate( Phase1TryCatch.getInstance() );
		} catch ( Throwable e ) {
			e.printStackTrace();
			System.exit( 1 );
		}

		// Bye bye! Ciao Bella!
		boxRuntime.shutdown();
	}
}
