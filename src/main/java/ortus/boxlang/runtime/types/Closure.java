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
import java.util.Objects;

import ortus.boxlang.compiler.ast.statement.BoxMethodDeclarationModifier;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * Represents a closure, which is a function, but has less data than a UDF and also retains a reference to the declaring context.
 */
public class Closure extends CompiledFunction {

	public static final Key		defaultName	= Key.of( "Closure" );

	/**
	 * The context in which this closure was declared.
	 */
	private IBoxContext			declaringContext;

	/**
	 * The original closure definition
	 * 
	 * @param closureDefinition The original closure definition
	 */
	public ClosureDefinition	closureDefinition;

	/**
	 * Simple constructor for subclasses that override all getters.
	 * Only sets the declaring context.
	 *
	 * @param declaringContext The context in which this closure was declared
	 */
	public Closure( IBoxContext declaringContext ) {
		super();
		Objects.requireNonNull( declaringContext, "A Closure's declaring context cannot be null." );
		this.declaringContext	= declaringContext;
		this.closureDefinition	= null;
	}

	/**
	 * Simple constructor for subclasses that override all getters.
	 * Only sets the declaring context.
	 *
	 * @param declaringContext The context in which this closure was declared
	 * @param originalClosure  The original closure definition that this closure is based on, which will be used for delegation of all metadata getters and invocation
	 */
	public Closure( IBoxContext declaringContext, ClosureDefinition originalClosure ) {
		super();
		Objects.requireNonNull( declaringContext, "A Closure's declaring context cannot be null." );
		this.declaringContext	= declaringContext;
		this.closureDefinition	= originalClosure;
	}

	/**
	 * Get the context in which this closure was declared.
	 *
	 * @return the context.
	 */
	public IBoxContext getDeclaringContext() {
		return this.declaringContext;
	}

	/**
	 * Detects of this Function is executing in the context of a class
	 *
	 * @return true if there is an IClassRunnable at the top of the template stack
	 */
	public boolean isInClass() {
		return false;
	}

	/**
	 * Detects of this Function is executing in the context of a class
	 *
	 * @return the IClassRunnable this context is executing in, or null if not in a class
	 */
	public IClassRunnable getThisClass() {
		return null;
	}

	/**
	 * Get the BoxLang type name for this type
	 * 
	 * @return The BoxLang type name
	 */
	@Override
	public String getBoxTypeName() {
		return "Closure";
	}

	// --------------------------------------------------------------------------
	// Delegation methods to closureDefinition
	// --------------------------------------------------------------------------

	/**
	 * Get the name of the function.
	 *
	 * @return function name
	 */
	@Override
	public Key getName() {
		return this.closureDefinition.getName();
	}

	/**
	 * Get the arguments of the function.
	 *
	 * @return array of arguments
	 */
	@Override
	public Argument[] getArguments() {
		return this.closureDefinition.getArguments();
	}

	/**
	 * Get the return type of the function.
	 *
	 * @return return type
	 */
	@Override
	public String getReturnType() {
		return this.closureDefinition.getReturnType();
	}

	@Override
	public Key getReturnTypeKey() {
		return this.closureDefinition.getReturnTypeKey();
	}

	/**
	 * Get any annotations declared for this function, both the @annotation syntax and inline.
	 *
	 * @return function metadata
	 */
	@Override
	public IStruct getAnnotations() {
		return this.closureDefinition.getAnnotations();
	}

	/**
	 * Get the contents of the documentation comment for this function.
	 *
	 * @return function metadata
	 */
	@Override
	public IStruct getDocumentation() {
		return this.closureDefinition.getDocumentation();
	}

	/**
	 * Get access modifier of the function
	 *
	 * @return function access modifier
	 */
	@Override
	public Access getAccess() {
		return this.closureDefinition.getAccess();
	}

	/**
	 * Get the imports for this function.
	 *
	 * @return list of import definitions
	 */
	@Override
	public List<ImportDefinition> getImports() {
		return this.closureDefinition.getImports();
	}

	/**
	 * Get the source type of the function.
	 *
	 * @return the source type
	 */
	@Override
	public BoxSourceType getSourceType() {
		return this.closureDefinition.getSourceType();
	}

	/**
	 * Get the path to the runnable.
	 *
	 * @return the resolved file path
	 */
	@Override
	public ResolvedFilePath getRunnablePath() {
		return this.closureDefinition.getRunnablePath();
	}

	/**
	 * Get modifier of the function
	 *
	 * @return function modifiers
	 */
	@Override
	public List<BoxMethodDeclarationModifier> getModifiers() {
		return this.closureDefinition.getModifiers();
	}

	/**
	 * Invoke the actual function logic
	 *
	 * @param context The function box context
	 *
	 * @return The result of the function invocation
	 */
	@Override
	public Object _invoke( FunctionBoxContext context ) {
		return this.closureDefinition._invoke( context );
	}

	/**
	 * Get the enclosing class of the function, if any.
	 * Lazy loads it
	 */
	public Class<?> getEnclosingClass() {
		return this.closureDefinition.getEnclosingClass();
	}

}
