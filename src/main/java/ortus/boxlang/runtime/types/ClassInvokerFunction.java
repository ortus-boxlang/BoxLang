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
import java.util.List;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.interop.DynamicInteropService;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * A thin Function wrapper around a Class reference that allows calling init() on it.
 * When invoked, this function creates a new instance of the wrapped class using the
 * same plumbing as the {@code new} keyword. This enables treating class references as
 * callable constructors: {@code myClass("arg1", "arg2")} or {@code (getClass())("arg")}.
 */
public class ClassInvokerFunction extends Function {

	private static final Argument[]	EMPTY_ARGUMENTS	= new Argument[ 0 ];
	private static final IStruct	DOCUMENTATION	= Struct.of( "hint",
	    "I am a class reference that can be invoked to create instances." );

	private static final String		RETURN_TYPE		= "any";
	private static final Key		RETURN_TYPE_KEY	= Key._ANY;
	private final Class<?>			targetClass;

	/**
	 * Constructor
	 *
	 * @param targetClass The class to wrap as a callable constructor
	 */
	public ClassInvokerFunction( Class<?> targetClass ) {
		this.targetClass = targetClass;
	}

	/**
	 * Get the wrapped class
	 *
	 * @return The target class
	 */
	public Class<?> getTargetClass() {
		return this.targetClass;
	}

	@Override
	public Key getName() {
		return Key.init;
	}

	@Override
	public Argument[] getArguments() {
		return EMPTY_ARGUMENTS;
	}

	@Override
	public String getReturnType() {
		return RETURN_TYPE;
	}

	@Override
	public Key getReturnTypeKey() {
		return RETURN_TYPE_KEY;
	}

	@Override
	public IStruct getAnnotations() {
		return Struct.EMPTY;
	}

	@Override
	public IStruct getDocumentation() {
		return DOCUMENTATION;
	}

	@Override
	public Access getAccess() {
		return Access.PUBLIC;
	}

	@Override
	public Object _invoke( FunctionBoxContext context ) {
		Object result;
		if ( context.getArgumentsScope().isPositional() ) {
			result = DynamicInteropService.invokeConstructor( context, this.targetClass, context.getArgumentsScope().asNativeArray() );
		} else {
			result = DynamicInteropService.invokeConstructor( context, this.targetClass, context.getArgumentsScope() );
		}
		// Unwrap DynamicObject if it contains an IClassRunnable instance
		if ( result instanceof DynamicObject dob ) {
			return dob.unWrapBoxLangClass();
		}
		return result;
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

	@Override
	public boolean requiresStrictArguments() {
		return true;
	}

}
