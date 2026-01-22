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
package ortus.boxlang.runtime.types.meta;

import java.util.List;

import ortus.boxlang.compiler.ast.statement.BoxMethodDeclarationModifier;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Closure;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.Function.Access;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Lambda;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.unmodifiable.UnmodifiableArray;

/**
 * This class represents BoxLang metadata for a function
 * Future idea, implement IReferenceable to allow for metadata to be generated on the fly
 */
public class FunctionMeta extends BoxMeta<Function> {

	private Function	target;
	public Class<?>		$class;
	public IStruct		meta;

	/**
	 * Constructor
	 */
	public FunctionMeta( Function target ) {
		super();
		this.target	= target;
		this.$class	= target.getClass();
		this.meta	= generateMeta(
		    target.getClass(),
		    target.getDocumentation(),
		    target.getAnnotations(),
		    target.getName(),
		    target.getReturnType(),
		    target.getSourceType(),
		    target.getAccess(),
		    target.getArguments(),
		    target instanceof Closure,
		    target instanceof Lambda,
		    Function.canOutput( target.getAnnotations(), target.getSourceType(), target.getDefaultOutput() ),
		    target.getModifiers()
		);
	}

	/**
	 * Generate metadata for a function
	 * 
	 * @param functionClass The function class
	 * @param documentation The documentation struct
	 * @param annotations   The annotations struct
	 * @param name          The function name
	 * @param returnType    The return type
	 * @param sourceType    The source type
	 * @param access        The access level
	 * @param arguments     The function arguments
	 * @param isClosure     Whether the function is a closure
	 * @param isLambda      Whether the function is a lambda
	 * @param defaultOutput The default output value
	 * @param modifiers     The function modifiers
	 *
	 * @return The metadata as a struct
	 */
	public static IStruct generateMeta(
	    Class<? extends Function> functionClass,
	    IStruct documentation,
	    IStruct annotations,
	    Key name,
	    String returnType,
	    BoxSourceType sourceType,
	    Access access,
	    Argument[] arguments,
	    boolean isClosure,
	    boolean isLambda,
	    boolean defaultOutput,
	    List<BoxMethodDeclarationModifier> modifiers ) {

		// prepare args first
		Object[]	params	= new Object[ arguments.length ];
		int			i		= 0;
		for ( Argument argument : arguments ) {
			params[ i++ ] = Struct.of(
			    Key._NAME, argument.name().getName(),
			    Key.nameAsKey, argument.name(),
			    Key.required, argument.required(),
			    Key.type, argument.type(),
			    Key._DEFAULT, argument.defaultValue(),
			    Key.documentation, argument.documentation(),
			    Key.annotations, argument.annotations()
			);
		}
		// Assemble the metadata
		IStruct meta = Struct.of(
		    Key._NAME, name.getName(),
		    Key.nameAsKey, name,
		    Key.returnType, returnType,
		    Key.access, access.toString().toLowerCase(),
		    Key.documentation, documentation,
		    Key.annotations, annotations,
		    Key.parameters, new UnmodifiableArray( params ),
		    Key.closure, isClosure,
		    Key.lambda, isLambda,
		    Key.output, defaultOutput,
		    Key._STATIC, modifiers.contains( BoxMethodDeclarationModifier.STATIC )
		);

		return meta;
	}

	/**
	 * Get target object this metadata is for
	 */
	public Function getTarget() {
		return target;
	}

	/**
	 * Get the metadata
	 */
	public IStruct getMeta() {
		return meta;
	}

}
