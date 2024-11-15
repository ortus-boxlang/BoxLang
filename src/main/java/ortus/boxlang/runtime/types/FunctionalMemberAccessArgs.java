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
package ortus.boxlang.runtime.types;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * I represent a functional call to a memeber method on the passed instance with args.
 */
public class FunctionalMemberAccessArgs extends Closure {

	private static final Argument[]										EMPTY_ARGUMENTS	= new Argument[ 0 ];
	private static final IStruct										documentation	= Struct.of( "hint",
	    "I am a functional wrapper that calls a pre-determined member method on the first argument passed." );

	private final Key													name;
	private final String												returnType		= "any";
	private java.util.function.Function<IBoxContext, Map<Key, Object>>	namedArgumentsGenerator;
	private java.util.function.Function<IBoxContext, Object[]>			positionalArgumentsGenerator;

	/**
	 * Constructor
	 * Create a new abstract function. There is no body to execute, just the metadata
	 */
	public FunctionalMemberAccessArgs(
	    Key name,
	    IBoxContext declaringContext,
	    java.util.function.Function<IBoxContext, Map<Key, Object>> namedArgumentsGenerator,
	    java.util.function.Function<IBoxContext, Object[]> positionalArgumentsGenerator ) {
		super( declaringContext );
		this.name							= name;
		this.namedArgumentsGenerator		= namedArgumentsGenerator;
		this.positionalArgumentsGenerator	= positionalArgumentsGenerator;
	}

	/**
	 * Get the name of the function.
	 *
	 * @return function name
	 */
	public Key getName() {
		return this.name;
	}

	/**
	 * We return nothing here since the methos handle isn't resolved yet so we don't know what
	 * overloaded args we may have. We will resolve this when we actually call the method
	 *
	 * @return array of arguments
	 */

	public Argument[] getArguments() {
		return EMPTY_ARGUMENTS;
	}

	/**
	 * We don't actually know the return type of the method until we call it
	 *
	 * @return return type
	 */
	public String getReturnType() {
		return returnType;
	}

	/**
	 * Get any annotations declared for this function, both the @annotation syntax and inline.
	 *
	 * @return function metadata
	 */
	public IStruct getAnnotations() {
		return Struct.EMPTY;
	}

	/**
	 * Get the contents of the documentation comment for this function.
	 *
	 * @return function metadata
	 */
	public IStruct getDocumentation() {
		return documentation;
	}

	/**
	 * Get access modifier of the function
	 *
	 * @return function access modifier
	 */
	public Access getAccess() {
		return Access.PUBLIC;
	}

	/**
	 * Implement this method to invoke the actual function logic
	 *
	 * @param context
	 *
	 * @return
	 */
	public Object _invoke( FunctionBoxContext context ) {
		if ( context.getArgumentsScope().isEmpty() ) {
			throw new BoxValidationException( "No arguments passed to functional member access" );
		}
		// Object to invoke the member method on
		Object obj = context.getArgumentsScope().get( Key.of( 1 ) );
		if ( namedArgumentsGenerator != null ) {
			return Referencer.getAndInvoke( context, obj, name, namedArgumentsGenerator.apply( context ), false );
		} else if ( positionalArgumentsGenerator != null ) {
			return Referencer.getAndInvoke( context, obj, name, positionalArgumentsGenerator.apply( context ), false );
		} else {
			throw new BoxValidationException( "No arguments generator provided for functional member access" );
		}
	}

	// ITemplateRunnable implementation methods

	/**
	 * Get the version of the BoxLang runtime
	 */
	public long getRunnableCompileVersion() {
		return 0;
	}

	/**
	 * Get the date the template was compiled
	 */
	public LocalDateTime getRunnableCompiledOn() {
		return null;
	}

	/**
	 * The AST (abstract syntax tree) of the runnable
	 */
	public Object getRunnableAST() {
		return null;
	}

	@Override
	public List<ImportDefinition> getImports() {
		return List.of();
	}

	@Override
	public ResolvedFilePath getRunnablePath() {
		return ResolvedFilePath.of( Path.of( "unknown" ) );
	}

	@Override
	public BoxSourceType getSourceType() {
		return BoxSourceType.BOXSCRIPT;
	}

	/**
	 * True if the function requires strict arguments (basically a java method)
	 * or false if this is a Boxlang method which can accept additional arbitrary arguments
	 * 
	 * @return true if strict arguments are required
	 */
	public boolean requiresStrictArguments() {
		return true;
	}

	/**
	 * If we are wrapping a method that outputs something, don't block it
	 * 
	 * @param context The context in which the function is being invoked
	 */
	public boolean canOutput( FunctionBoxContext context ) {
		return true;
	}

}
