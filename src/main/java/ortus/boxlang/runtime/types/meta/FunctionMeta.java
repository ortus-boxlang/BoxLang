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

import ortus.boxlang.runtime.types.Closure;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.Function.Argument;
import ortus.boxlang.runtime.types.Lambda;
import ortus.boxlang.runtime.types.Struct;
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
	public Struct		meta;

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
			    "name", argument.name().getName(),
			    "required", argument.required(),
			    "type", argument.type(),
			    "default", argument.defaultValue(),
			    "documentation", argument.documentation(),
			    "annotations", argument.annotations()
			);
		}
		// Assemble the metadata
		this.meta = ImmutableStruct.of(
		    "name", target.getName().getName(),
		    "returnType", target.getReturnType(),
		    "access", target.getAccess().toString().toLowerCase(),
		    "documentation", target.getDocumentation(),
		    "annotations", target.getAnnotations(),
		    "parameters", new ImmutableArray( params ),
		    "closure", target instanceof Closure,
		    "lambda", target instanceof Lambda
		);

	}

	/**
	 * Get target object this metadata is for
	 */
	public Object getTarget() {
		return target;
	}

}
