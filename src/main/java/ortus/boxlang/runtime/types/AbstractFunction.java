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

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * A BoxLang abstract Function.
 * I am a concrete class and will not be extended by a compiled runnable
 */
public class AbstractFunction extends Function {

	private final Key			name;
	private final Argument[]	arguments;
	private final String		returnType;
	private final Access		access;
	private final IStruct		annotations;
	private final IStruct		documentation;

	/**
	 * Constructor
	 * Create a new abstract function. There is no body to execute, just the metadata
	 */
	public AbstractFunction( Key name, Argument[] arguments, String returnType, Access access, IStruct annotations, IStruct documentation ) {
		this.name			= name;
		this.arguments		= arguments;
		this.returnType		= returnType;
		this.access			= access;
		this.annotations	= annotations;
		this.documentation	= documentation;
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
		return this.arguments;
	}

	/**
	 * Get the return type of the function.
	 *
	 * @return return type
	 */
	public String getReturnType() {
		return this.returnType;
	}

	/**
	 * Get any annotations declared for this function, both the @annotation syntax and inline.
	 *
	 * @return function metadata
	 */
	public IStruct getAnnotations() {
		return this.annotations;
	}

	/**
	 * Get the contents of the documentation comment for this function.
	 *
	 * @return function metadata
	 */
	public IStruct getDocumentation() {
		return this.documentation;
	}

	/**
	 * Get access modifier of the function
	 *
	 * @return function access modifier
	 */
	public Access getAccess() {
		return this.access;
	}

	/**
	 * Implement this method to invoke the actual function logic
	 *
	 * @param context
	 *
	 * @return
	 */
	public Object _invoke( FunctionBoxContext context ) {
		throw new BoxRuntimeException( "Cannot invoke abstract method." );
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
	public Path getRunnablePath() {
		return Path.of( "unknown" );
	}

	@Override
	public BoxSourceType getSourceType() {
		return BoxSourceType.BOXSCRIPT;
	}

}
