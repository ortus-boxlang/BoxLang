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

import java.util.ArrayList;

import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.immutable.ImmutableArray;
import ortus.boxlang.runtime.types.immutable.ImmutableStruct;

/**
 * This class represents generic BoxLang metadata for a an object which has no object-specifc properties
 */
public class ClassMeta extends BoxMeta {

	@SuppressWarnings( "unused" )
	private IClassRunnable	target;
	public Class<?>			$class;
	public IStruct			meta;

	/**
	 * Constructor
	 */
	public ClassMeta( IClassRunnable target ) {
		super();
		this.target	= target;
		this.$class	= target.getClass();
		// Assemble the metadata
		var functions = new ArrayList<Object>();
		// loop over target's variables scope and add metadata for each function
		// TODO: If more than 20 do parallel
		for ( var entry : target.getThisScope().entrySet() ) {
			if ( entry.getValue() instanceof Function fun ) {
				functions.add( ( ( FunctionMeta ) fun.getBoxMeta() ).meta );
			}
		}

		this.meta = ImmutableStruct.of(
		    Key._NAME, target.getName().getName(),
		    Key.nameAsKey, target.getName(),
		    Key.documentation, ImmutableStruct.fromStruct( target.getDocumentation() ),
		    Key.annotations, ImmutableStruct.fromStruct( target.getAnnotations() ),
		    Key._EXTENDS, target.getSuper() != null ? target.getSuper().getBoxMeta().getMeta() : Struct.EMPTY,
		    Key.functions, ImmutableArray.fromList( functions ),
		    Key._HASHCODE, target.hashCode(),
		    Key.properties, ImmutableArray.of( target.getProperties().entrySet().stream().map( entry -> ImmutableStruct.of(
		        Key._NAME, entry.getKey().getName(),
		        Key.nameAsKey, entry.getKey(),
		        Key.type, entry.getValue().type(),
		        Key.defaultValue, entry.getValue().defaultValue(),
		        Key.annotations, ImmutableStruct.fromStruct( entry.getValue().annotations() ),
		        Key.documentation, ImmutableStruct.fromStruct( entry.getValue().documentation() )
		    ) ).toArray() ),
		    Key.type, "Component",
		    Key.fullname, target.getName().getName(),
		    Key.path, target.getRunnablePath().toString()
		);

	}

	/**
	 * Get target object this metadata is for
	 */
	public IClassRunnable getTarget() {
		return target;
	}

	/**
	 * Get the metadata
	 */
	public IStruct getMeta() {
		return meta;
	}

	/**
	 * Get the variables scope directly
	 * 
	 * @return The variables scope
	 */
	public IScope getVariablesScope() {
		return target.getVariablesScope();
	}

	/**
	 * Get the this scope directly
	 * 
	 * @return The this scope
	 */
	public IScope getThisScope() {
		return target.getThisScope();
	}

}
