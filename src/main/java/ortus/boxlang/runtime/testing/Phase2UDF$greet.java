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
import java.time.LocalDateTime;
import java.util.List;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.operators.Concat;
import ortus.boxlang.runtime.runnables.IBoxRunnable;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.LocalScope;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.UDF;

/**
 * Phase 2 BoxLang
 * Example of UDF delcaration and execution
 */
public class Phase2UDF$greet extends UDF {

	private static Phase2UDF$greet		instance;

	/**
	 * The name of the function
	 */
	private final static Key			name				= Key.of( "greet" );

	/**
	 * The arguments of the function
	 */
	private final static Argument[]		arguments			= new Argument[] {
	    new Argument( true, "String", Key.of( "name" ), "Brad" )
	};

	/**
	 * The return type of the function
	 */
	private final static String			returnType			= "String";

	/**
	 * The access modifier of the function
	 */
	private Access						access				= Access.PUBLIC;

	// TODO: cachedwithin, modifier, localmode, return format

	/**
	 * Additional abitrary metadata about this function.
	 */
	private final static IStruct		annotations			= Struct.of( Key.of( "hint" ), "My Function Hint" );

	private final static IStruct		documentation		= Struct.EMPTY;

	/**
	 * The Box Runnable that declared this function
	 */
	private static final IBoxRunnable	declaringRunnable	= Phase2UDF.getInstance();
	private static final Object			ast					= null;

	public Key getName() {
		return name;
	}

	public Argument[] getArguments() {
		return arguments;
	}

	public String getReturnType() {
		return returnType;
	}

	public IStruct getAnnotations() {
		return annotations;
	}

	public IStruct getDocumentation() {
		return documentation;
	}

	public Access getAccess() {
		return access;
	}

	/**
	 * The imports for this runnable
	 */
	public List<ImportDefinition> getImports() {
		return declaringRunnable.getImports();
	}

	private Phase2UDF$greet() {
		super();
	}

	public static synchronized Phase2UDF$greet getInstance() {
		if ( instance == null ) {
			instance = new Phase2UDF$greet();
		}
		return instance;
	}

	/*
	 * <pre>
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
	 * </pre>
	 */
	@Override
	public Object _invoke( FunctionBoxContext context ) {

		// Create local.race and arguments.race to show scope lookup
		context.getScopeNearby( LocalScope.name ).assign(
		    context,
		    Key.of( "race" ),
		    "Local scope value"
		);

		context.getScopeNearby( ArgumentsScope.name ).assign(
		    context,
		    Key.of( "race" ),
		    "Arguments scope value"
		);

		context.getScopeNearby( LocalScope.name ).assign(
		    context,
		    Key.of( "Greeting" ),
		    Concat.invoke(
		        "Hello ",
		        context.scopeFindNearby( Key.of( "name" ), null ).value()
		    )
		);

		// Reach "into" parent context and get "out" from variables scope
		Referencer.getAndInvoke(
		    context,
		    // Object
		    context.scopeFindNearby( Key.of( "out" ), null ).value(),
		    // Method
		    Key.of( "println" ),
		    // Arguments
		    new Object[] {
		        "Inside UDF, race scope lookup finds: " + context.scopeFindNearby( Key.of( "race" ), null ).value()
		    },
		    false
		);

		return context.scopeFindNearby( Key.of( "greeting" ), null ).value();
	}

	// ITemplateRunnable implementation methods

	/**
	 * The version of the BoxLang runtime
	 */
	public long getRunnableCompileVersion() {
		return Phase2UDF$greet.declaringRunnable.getRunnableCompileVersion();
	}

	/**
	 * The date the template was compiled
	 */
	public LocalDateTime getRunnableCompiledOn() {
		return Phase2UDF$greet.declaringRunnable.getRunnableCompiledOn();
	}

	/**
	 * The AST (abstract syntax tree) of the runnable
	 */
	public Object getRunnableAST() {
		return Phase2UDF$greet.ast;
	}

	public Path getRunnablePath() {
		return Path.of( "unknown" );
	}

	/**
	 * The original source type
	 */
	public BoxSourceType getSourceType() {
		return BoxSourceType.BOXSCRIPT;
	}

}
