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
 * limitations under the License.8
 */
package ortus.boxlang.runtime.types;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.function.BiFunction;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * This represents a BoxLang Function that will be created dynamically either by Java or BoxLang.
 * It can be used to generate using a Java Lambda, or subclass with no compiling or parsing.
 * The impetus of this class is to be able to have an injected mixin that can pivot according to
 * the name of the function as it's injected into a BoxLang class.
 */
public class DynamicFunction extends UDF {

	private static final Argument[]									EMPTY_ARGUMENTS	= new Argument[ 0 ];

	/**
	 * |--------------------------------------------------------------------------
	 * | Properties
	 * |--------------------------------------------------------------------------
	 */

	/**
	 * The injectable name of the function
	 */
	private Key														name;

	/**
	 * The hint of the function
	 */
	private String													hint			= "Dynamic BoxLang Function";

	/**
	 * The return type of the function, default is 'any'
	 */
	private String													returnType		= "any";

	/**
	 * The arguments of the function
	 */
	private Argument[]												arguments		= EMPTY_ARGUMENTS;

	/**
	 * The annotations of the function
	 */
	private IStruct													annotations		= Struct.EMPTY;

	/**
	 * The documentation of the function
	 */
	private IStruct													documentation	= Struct.of(
	    "hint",
	    "I am a dynamic BoxLang function. I can be used to generate a function dynamically using Java or BoxLang."
	);

	/**
	 * The created on date of the function
	 */
	private Instant													createdOn		= Instant.now();

	/**
	 * A Java BiFunction lambda that will proxy the function
	 * Receives the context and this class as arguments, can return anything
	 */
	private BiFunction<FunctionBoxContext, DynamicFunction, Object>	target;

	/**
	 * |--------------------------------------------------------------------------
	 * | Constructors
	 * |--------------------------------------------------------------------------
	 */

	/**
	 * Full Constructor
	 *
	 * @param name        The name of the function that will be used to register it
	 * @param hint        The hint of the function
	 * @param returnType  The return type of the function
	 * @param arguments   The arguments of the function
	 * @param annotations The annotations of the function
	 */
	public DynamicFunction(
	    Key name,
	    BiFunction<FunctionBoxContext, DynamicFunction, Object> target,
	    Argument[] arguments,
	    String returnType,
	    String hint,
	    IStruct annotations ) {
		this.name			= name;
		this.target			= target;
		this.returnType		= returnType;
		this.arguments		= arguments;
		this.annotations	= annotations;
		this.documentation.put( "hint", hint );
	}

	/**
	 * Simple Constructor of just the name and the target and arguments
	 *
	 * @param name      The name of the function that will be used to register it
	 * @param target    The target lambda that will be executed when the function is called
	 * @param arguments The arguments of the function
	 */
	public DynamicFunction(
	    Key name,
	    BiFunction<FunctionBoxContext, DynamicFunction, Object> target,
	    Argument[] arguments ) {
		this.name		= name;
		this.target		= target;
		this.arguments	= arguments;
	}

	/**
	 * Simple Constructor of just the name and the target
	 *
	 * @param name   The name of the function that will be used to register it
	 * @param target The target lambda that will be executed when the function is called
	 */
	public DynamicFunction(
	    Key name,
	    BiFunction<FunctionBoxContext, DynamicFunction, Object> target ) {
		this.name	= name;
		this.target	= target;
	}

	/**
	 * Simple Constructor of just the name, target, and return type
	 *
	 * @param name       The name of the function that will be used to register it
	 * @param target     The target lambda that will be executed when the function is called
	 * @param returnType The return type of the function
	 */
	public DynamicFunction(
	    Key name,
	    BiFunction<FunctionBoxContext, DynamicFunction, Object> target, String returnType ) {
		this.name		= name;
		this.target		= target;
		this.returnType	= returnType;
	}

	/**
	 * |--------------------------------------------------------------------------
	 * | Abstract Methods
	 * |--------------------------------------------------------------------------
	 */

	/**
	 * This is our proxy to the lambda we generate the function with
	 *
	 * @inheritDoc
	 */
	@Override
	public Object _invoke( FunctionBoxContext context ) {
		return this.target.apply( context, this );
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public Key getName() {
		return this.name;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public Argument[] getArguments() {
		return this.arguments;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public List<ImportDefinition> getImports() {
		return List.of();
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public ResolvedFilePath getRunnablePath() {
		return ResolvedFilePath.of( Path.of( "dynamic-boxlang-function" ) );
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public BoxSourceType getSourceType() {
		return BoxSourceType.BOXSCRIPT;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public String getReturnType() {
		return this.returnType;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public IStruct getAnnotations() {
		return this.annotations;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public IStruct getDocumentation() {
		return this.documentation;
	}

	/**
	 * All dynamic functions are usually public
	 *
	 * @inheritDoc
	 */
	@Override
	public Access getAccess() {
		return Access.PUBLIC;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public long getRunnableCompileVersion() {
		return 0;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public LocalDateTime getRunnableCompiledOn() {
		return LocalDateTime.ofInstant( this.createdOn, ZoneId.systemDefault() );
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public Object getRunnableAST() {
		return null;
	}

}
