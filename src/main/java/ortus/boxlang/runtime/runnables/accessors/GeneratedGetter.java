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
package ortus.boxlang.runtime.runnables.accessors;

import java.nio.file.Path;
import java.util.List;

import ortus.boxlang.compiler.ast.statement.BoxMethodDeclarationModifier;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.UDF;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * A generated getter method
 * I am a concrete class and will not be extended by a compiled runnable
 */
public class GeneratedGetter extends UDF {

	private static final Argument[]			EMPTY_ARGUMENTS		= new Argument[ 0 ];
	private static final IStruct			documentation		= Struct.of( "hint", "I am a generated getter method." );
	private static final ResolvedFilePath	unknownResolvedPath	= ResolvedFilePath.of( Path.of( "unknown" ) );

	private final Key						name;
	private final Key						variable;
	private final String					returnType;
	private final Key						returnTypeKey;
	private final BoxSourceType				sourceType;

	/**
	 * Constructor
	 * Create a new abstract function. There is no body to execute, just the metadata
	 * 
	 * This constructor is deprecated. Use GeneratedGetter(Key, Key, String, BoxSourceType) instead.
	 */
	@Deprecated
	public GeneratedGetter( Key name, Key variable, String type ) {
		this.name			= name;
		this.variable		= variable;
		this.returnType		= type;
		this.returnTypeKey	= Key.of( type );
		this.sourceType		= BoxSourceType.BOXSCRIPT;
	}

	/**
	 * Constructor
	 * Create a new abstract function. There is no body to execute, just the metadata
	 */
	public GeneratedGetter( Key name, Key variable, String type, BoxSourceType sourceType ) {
		this.name			= name;
		this.variable		= variable;
		this.returnType		= type;
		this.returnTypeKey	= Key.of( type );
		this.sourceType		= sourceType;
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
	 * Get the arguments of the function.
	 *
	 * @return array of arguments
	 */

	public Argument[] getArguments() {
		return EMPTY_ARGUMENTS;
	}

	/**
	 * Get the return type of the function.
	 *
	 * @return return type
	 */
	public String getReturnType() {
		return returnType;
	}

	@Override
	public Key getReturnTypeKey() {
		return returnTypeKey;
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
		// We want null to be returned if the variable is not found to match CF
		return context.getScopeNearby( VariablesScope.name ).get( variable );
	}

	// ITemplateRunnable implementation methods

	@Override
	public List<ImportDefinition> getImports() {
		return List.of();
	}

	@Override
	public ResolvedFilePath getRunnablePath() {
		return unknownResolvedPath;
	}

	@Override
	public BoxSourceType getSourceType() {
		return sourceType;
	}

	/**
	 * Ensure the return value of the function is the correct type
	 *
	 * @param context the context to ensure the return type in
	 * @param value   the value to ensure the type of
	 *
	 * @return the value, cast to the correct type if necessary
	 */
	@Override
	protected Object ensureReturnType( IBoxContext context, Object value ) {
		// Compat-- return types are not enforced on generated getters in CFML.
		if ( sourceType.equals( BoxSourceType.CFSCRIPT ) || sourceType.equals( BoxSourceType.CFTEMPLATE ) ) {
			return value;
		}
		return super.ensureReturnType( context, value );
	}

	@Override
	public List<BoxMethodDeclarationModifier> getModifiers() {
		return List.of();
	}

}
