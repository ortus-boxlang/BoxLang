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

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Closure;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Lambda;
import ortus.boxlang.runtime.types.immutable.ImmutableArray;
import ortus.boxlang.runtime.types.immutable.ImmutableStruct;

/**
 * This class represents BoxLang metadata for a function
 * Future idea, implement IReferenceable to allow for metadata to be generated on the fly
 */
public class FunctionMeta extends BoxMeta {

	@SuppressWarnings( "unused" )
	private Function	target;
	public Object		AST;
	public Class<?>		$class;
	// TODO: Make this into an actual class to allow modification on the fly of metadata
	public IStruct		meta;

	/**
	 * Constructor
	 */
	public FunctionMeta( Function target ) {
		super();
		this.target	= target;
		this.$class	= target.getClass();
		this.AST	= target.getRunnableAST();

		// prepare args first
		Object[]	params	= new Object[ target.getArguments().length ];
		int			i		= 0;
		for ( Argument argument : target.getArguments() ) {
			params[ i++ ] = ImmutableStruct.of(
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
		this.meta = ImmutableStruct.of(
		    Key._NAME, target.getName().getName(),
		    Key.nameAsKey, target.getName(),
		    Key.returnType, target.getReturnType(),
		    Key.access, target.getAccess().toString().toLowerCase(),
		    Key.documentation, target.getDocumentation(),
		    Key.annotations, target.getAnnotations(),
		    Key.parameters, new ImmutableArray( params ),
		    Key.closure, target instanceof Closure,
		    Key.lambda, target instanceof Lambda
		);
	}

	/**
	 * Get target object this metadata is for
	 */
	public Object getTarget() {
		return target;
	}

	/**
	 * Get the metadata
	 */
	public IStruct getMeta() {
		return meta;
	}

}
