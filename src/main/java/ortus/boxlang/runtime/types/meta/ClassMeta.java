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
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.immutable.ImmutableArray;
import ortus.boxlang.runtime.types.immutable.ImmutableStruct;

/**
 * This class represents generic BoxLang metadata for a an object which has no object-specifc properties
 */
public class ClassMeta extends BoxMeta {

	@SuppressWarnings( "unused" )
	private Object	target;
	public Class<?>	$class;
	public Struct	meta;

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
		for ( var entry : target.getThisScope().keySet() ) {
			var value = target.getThisScope().get( entry );
			if ( value instanceof Function fun ) {
				functions.add( ( ( FunctionMeta ) fun.getBoxMeta() ).meta );
			}
		}

		this.meta = ImmutableStruct.of(
		    "name", target.getName().getName(),
		    "documentation", target.getDocumentation(),
		    "annotations", target.getAnnotations(),
		    // TODO: add extends
		    "extends", Struct.EMPTY,
		    "functions", ImmutableArray.fromList( functions ),
		    "hashCode", target.hashCode(),
		    // TODO: add properties
		    "properties", Array.EMPTY,
		    "type", "Component",
		    "fullname", target.getName().getName(),
		    "path", target.getRunnablePath().toString()
		);

	}

	/**
	 * Get target object this metadata is for
	 */
	public Object getTarget() {
		return target;
	}

}
