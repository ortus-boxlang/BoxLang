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
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * I am a {@link Function} with some of its leading arguments already bound - the result of
 * calling {@code curry(args...)} on another Function/Closure/Lambda. Invoking me runs the
 * original target with the bound arguments first, followed by whatever arguments I'm actually
 * called with.
 * <p>
 * Modeled directly on {@link FunctionalMemberAccess} - a minimal, metadata-light {@code Function}
 * subclass with no compiled body of its own, just a Java-side {@code _invoke()} - the same
 * pattern used there for BoxLang's own native ".methodName" functional-member-access value.
 */
public class CurriedFunction extends Function {

	private static final Argument[]	EMPTY_ARGUMENTS	= new Argument[ 0 ];
	private static final IStruct	documentation	= Struct.of( "hint",
	    "I am a closure with some of its leading arguments already bound via curry()." );

	private final Function			target;
	private final Object[]			boundArgs;

	/**
	 * Constructor
	 *
	 * @param target    The original function/closure being curried
	 * @param boundArgs The leading arguments already bound to the call
	 */
	public CurriedFunction( Function target, Object[] boundArgs ) {
		this.target		= target;
		this.boundArgs	= boundArgs;
	}

	public Key getName() {
		return Key.of( "curried" );
	}

	/**
	 * We don't know the remaining arity ahead of time - same reasoning as
	 * {@link FunctionalMemberAccess#getArguments()}.
	 */
	public Argument[] getArguments() {
		return EMPTY_ARGUMENTS;
	}

	public String getReturnType() {
		return "any";
	}

	@Override
	public Key getReturnTypeKey() {
		return Key._ANY;
	}

	public IStruct getAnnotations() {
		return Struct.EMPTY;
	}

	public IStruct getDocumentation() {
		return documentation;
	}

	public Access getAccess() {
		return Access.PUBLIC;
	}

	/**
	 * Runs the original target with the bound arguments first, followed by whatever this curried
	 * function is actually called with.
	 */
	public Object _invoke( FunctionBoxContext context ) {
		Object[]	callArgs	= context.getArgumentsScope().asNativeArray();
		Object[]	combined	= new Object[ boundArgs.length + callArgs.length ];
		System.arraycopy( boundArgs, 0, combined, 0, boundArgs.length );
		System.arraycopy( callArgs, 0, combined, boundArgs.length, callArgs.length );
		return context.invokeFunction( target, combined );
	}

	// ITemplateRunnable implementation methods

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
	 * The target's own arity/strictness governs the combined call, not this wrapper.
	 */
	public boolean requiresStrictArguments() {
		return false;
	}

	public boolean canOutput( FunctionBoxContext context ) {
		return true;
	}

}
