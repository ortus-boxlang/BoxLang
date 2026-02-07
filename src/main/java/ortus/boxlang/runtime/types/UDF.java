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

import java.util.List;

import ortus.boxlang.compiler.ast.statement.BoxMethodDeclarationModifier;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * Represents a UDF. A UDF is specifically a function that is defined with the "function name()" syntax.
 * UDFs have names, access, hints, etc which closures do not have.
 */
public class UDF extends CompiledFunction {

	/**
	 * Simple constructor for subclasses that override all getters.
	 * Sets all fields to null/empty defaults.
	 */
	protected UDF() {
		super();
	}

	/**
	 * Constructor
	 *
	 * @param name          The name of the function
	 * @param arguments     The arguments of the function
	 * @param returnType    The return type of the function
	 * @param access        The access modifier of the function
	 * @param annotations   The annotations of the function
	 * @param documentation The documentation of the function
	 * @param modifiers     The modifiers of the function
	 * @param defaultOutput Whether the function should output by default
	 * @param imports       The imports for this function
	 * @param sourceType    The source type of the function
	 * @param runnablePath  The path to the runnable
	 * @param invoker       The functional interface to invoke the function logic
	 */
	public UDF(
	    Key name,
	    Argument[] arguments,
	    String returnType,
	    Function.Access access,
	    IStruct annotations,
	    IStruct documentation,
	    List<BoxMethodDeclarationModifier> modifiers,
	    boolean defaultOutput,
	    List<ImportDefinition> imports,
	    BoxSourceType sourceType,
	    ResolvedFilePath runnablePath,
	    java.util.function.Function<FunctionBoxContext, Object> invoker ) {

		super(
		    name,
		    arguments,
		    returnType,
		    access,
		    annotations,
		    documentation,
		    modifiers,
		    defaultOutput,
		    imports,
		    sourceType,
		    runnablePath,
		    invoker
		);
	}

}
