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
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.operators.Concat;
import ortus.boxlang.runtime.runnables.IBoxRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.LocalScope;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Closure;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * Phase 2 BoxLang
 * Example of Closure delcaration and execution
 */
public class Phase2Closure$closure1 extends Closure {

	/**
	 * The name of the function
	 */
	private final static Key			name				= Closure.defaultName;

	/**
	 * The arguments of the function
	 */
	private final static Argument[]		arguments			= new Argument[] {
	    new Argument( true, "String", Key.of( "name" ), "Brad" )
	};

	/**
	 * The return type of the function
	 */
	private final static String			returnType			= "any";

	// TODO: cachedwithin, modifier, localmode, return format

	/**
	 * Additional abitrary metadata about this function.
	 */
	private final static IStruct		annotations			= Struct.EMPTY;

	private final static IStruct		documentation		= Struct.EMPTY;

	/**
	 * The access modifier of the function
	 */
	private Access						access				= Access.PUBLIC;

	/**
	 * The Box Runnable that declared this function
	 */
	private static final IBoxRunnable	declaringRunnable	= Phase2Closure.getInstance();
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

	public ResolvedFilePath getRunnablePath() {
		return ResolvedFilePath.of( Path.of( "unknown" ) );
	}

	/**
	 * The original source type
	 */
	public BoxSourceType getSourceType() {
		return BoxSourceType.BOXSCRIPT;
	}

	public Phase2Closure$closure1( IBoxContext declaringContext ) {
		super( declaringContext );
	}

	// ITemplateRunnable implementation methods

	/**
	 * The version of the BoxLang runtime
	 */
	public long getRunnableCompileVersion() {
		return Phase2Closure$closure1.declaringRunnable.getRunnableCompileVersion();
	}

	/**
	 * The date the template was compiled
	 */
	public LocalDateTime getRunnableCompiledOn() {
		return Phase2Closure$closure1.declaringRunnable.getRunnableCompiledOn();
	}

	/**
	 * The AST (abstract syntax tree) of the runnable
	 */
	public Object getRunnableAST() {
		return Phase2Closure$closure1.ast;
	}

	/**
	 * The imports for this runnable
	 */
	public List<ImportDefinition> getImports() {
		return declaringRunnable.getImports();
	}

	/*
	 * <pre>
	 * ( required string name='Brad' ) => {
	 * var greeting = "Hello " & name;
	 *
	 * out.println( "Inside Closure, outside lookup finds: " & outside )
	 *
	 * return greeting;
	 * }
	 * </pre>
	 */
	@Override
	public Object _invoke( FunctionBoxContext context ) {

		context.getScopeNearby( LocalScope.name ).assign(
		    context,
		    Key.of( "Greeting" ),
		    Concat.invoke(
		        "Hello ",
		        context.scopeFindNearby( Key.of( "name" ), null, false ).value()
		    )
		);

		// Reach "into" parent context and get "out" from variables scope
		Referencer.getAndInvoke(
		    context,
		    // Object
		    context.scopeFindNearby( Key.of( "out" ), null, false ).value(),
		    // Method
		    Key.of( "println" ),
		    // Arguments
		    new Object[] {
		        "Inside Closure, outside lookup finds: " + context.scopeFindNearby( Key.of( "outside" ), null, false ).value()
		    },
		    false
		);

		return context.scopeFindNearby( Key.of( "greeting" ), null, false ).value();
	}

}
