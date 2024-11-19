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
import java.util.Map;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.BoxInterface;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.AbstractFunction;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.unmodifiable.UnmodifiableArray;
import ortus.boxlang.runtime.types.unmodifiable.UnmodifiableStruct;

/**
 * This class represents generic BoxLang metadata for a an object which has no object-specifc properties
 */
public class InterfaceMeta extends BoxMeta {

	@SuppressWarnings( "unused" )
	private BoxInterface	target;
	public Class<?>			$class;
	public IStruct			meta;

	/**
	 * Constructor
	 *
	 * @param target The target object this metadata is for
	 */
	public InterfaceMeta( BoxInterface target ) {
		super();
		this.target	= target;
		this.$class	= target.getClass();

		// Assemble the metadata
		var functions = new ArrayList<Object>();
		target.getAbstractMethods().forEach( ( key, function ) -> functions.add( ( ( FunctionMeta ) function.getBoxMeta() ).meta ) );

		var defaultFunctions = new ArrayList<Object>();
		target.getDefaultMethods().forEach( ( key, function ) -> defaultFunctions.add( ( ( FunctionMeta ) function.getBoxMeta() ).meta ) );

		IStruct supersMeta = new Struct( IStruct.TYPES.LINKED );
		for ( BoxInterface _super : target.getSupers() ) {
			supersMeta.put( _super.getName().getName(), _super.getBoxMeta().getMeta() );
		}

		this.meta = UnmodifiableStruct.of(
		    Key._NAME, target.getName().getName(),
		    Key.nameAsKey, target.getName(),
		    Key.documentation, UnmodifiableStruct.fromStruct( target.getDocumentation() ),
		    Key.annotations, UnmodifiableStruct.fromStruct( target.getAnnotations() ),
		    Key._EXTENDS, supersMeta,
		    Key.functions, UnmodifiableArray.fromList( functions ),
		    Key.defaultFunctions, UnmodifiableArray.fromList( defaultFunctions ),
		    Key._HASHCODE, target.hashCode(),
		    Key.type, "Interface",
		    Key.fullname, target.getName().getName(),
		    Key.path, target.getRunnablePath().absolutePath().toString()
		);

	}

	/**
	 * So we can get a pretty print of the metadata
	 */
	public String toString() {
		return Struct.of(
		    "meta", this.meta.asString(),
		    "$class", this.$class.getName()
		).asString();
	}

	/**
	 * Get target object this metadata is for
	 */
	public BoxInterface getTarget() {
		return this.target;
	}

	/**
	 * Direct invoke a Java method on the target bypassing the referencable methods
	 */
	public Object invokeTargetMethod( IBoxContext context, String methodName, Object[] args ) {
		return DynamicObject.of( target ).invoke( context, methodName, args );
	}

	/**
	 * Direct invoke a static Java method on the target's class bypassing the referencable methods
	 */
	public Object invokeTargetMethodStatic( IBoxContext context, String methodName, Object[] args ) {
		return DynamicObject.of( target ).invokeStatic( context, methodName, args );
	}

	/**
	 * Get the metadata
	 */
	public IStruct getMeta() {
		return this.meta;
	}

	/**
	 * Get interface abstract methods
	 */
	public Map<Key, AbstractFunction> getAbstractMethods() {
		return this.target.getAbstractMethods();
	}

	/**
	 * Get interface default methods
	 */
	public Map<Key, Function> getDefaultMethods() {
		return this.target.getDefaultMethods();
	}

}
