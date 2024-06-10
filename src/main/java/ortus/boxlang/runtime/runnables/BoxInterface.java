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
package ortus.boxlang.runtime.runnables;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.StaticScope;
import ortus.boxlang.runtime.types.AbstractFunction;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.IType;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.InterfaceMeta;

public abstract class BoxInterface implements ITemplateRunnable, IReferenceable, IType {

	/**
	 * Metadata object
	 */
	public BoxMeta	$bx;

	/**
	 * Cached lookup of the output annotation
	 */
	private Boolean	canOutput	= null;

	protected void resolveSupers( IBoxContext context ) {
		// First, we load an super interface
		Object superInterfaceObject = getAnnotations().get( Key._EXTENDS );
		if ( superInterfaceObject != null ) {
			String superInterfaceName = StringCaster.cast( superInterfaceObject );
			if ( superInterfaceName != null && superInterfaceName.length() > 0 ) {
				if ( superInterfaceName.toLowerCase().startsWith( "java:" ) ) {
					throw new BoxRuntimeException( "BoxLang Interaces cannot extend Java interfaces" );
				}
				// Recursivley load the super interface
				BoxInterface _super = ( BoxInterface ) ClassLocator.getInstance().load( context,
				    superInterfaceName,
				    context.getCurrentImports()
				)
				    .unWrapBoxLangClass();

				// Set in our super interface
				setSuper( _super );
			}
		}
	}

	/**
	 * Set the super interface.
	 *
	 * @param _super The super class
	 */
	public void setSuper( BoxInterface _super ) {
		// Set the actual super referene
		_setSuper( _super );

		// merge annotations
		for ( var entry : _super.getAnnotations().entrySet() ) {
			Key key = entry.getKey();
			if ( !getAnnotations().containsKey( key ) && !key.equals( Key._EXTENDS ) ) {
				getAnnotations().put( key, entry.getValue() );
			}
		}

	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Get the name
	 */
	public abstract Key getName();

	/**
	 * Get annotations
	 */
	public abstract IStruct getAnnotations();

	/**
	 * Get documentation
	 */
	public abstract IStruct getDocumentation();

	/**
	 * Get interface abstract methods
	 */
	public abstract Map<Key, AbstractFunction> getAbstractMethods();

	/**
	 * Get interface default methods
	 */
	public abstract Map<Key, Function> getDefaultMethods();

	/**
	 * Get the static scope
	 */
	public abstract StaticScope getStaticScope();

	/**
	 * Set the actual static super var
	 * 
	 * @param _super The super interface
	 */
	public abstract void _setSuper( BoxInterface _super );

	/**
	 * Get the super interface. Null if not exists
	 * 
	 * @return The super interface
	 */
	public abstract BoxInterface getSuper();

	/**
	 * Represent as string, or throw exception if not possible
	 *
	 * @return The string representation
	 */
	public String asString() {
		return "Interface: " + getName().getName();
	}

	/**
	 * Get the BoxMeta object for the interface
	 *
	 * @return The metadata object
	 */
	public BoxMeta getBoxMeta() {
		if ( this.$bx == null ) {
			this.$bx = new InterfaceMeta( this );
		}
		return this.$bx;
	}

	/**
	 * A helper to look at the "output" annotation, caching the result
	 *
	 * @return Whether the function can output
	 */
	public boolean canOutput() {
		// Initialize if neccessary
		if ( this.canOutput == null ) {
			this.canOutput = BooleanCaster.cast(
			    getAnnotations()
			        .getOrDefault(
			            Key.output,
			            false
			        )
			);
		}
		return this.canOutput;
	}

	/**
	 * --------------------------------------------------------------------------
	 * IReferenceable Interface Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Assign a value to a key
	 *
	 * @param key   The key to assign
	 * @param value The value to assign
	 */
	public Object assign( IBoxContext context, Key key, Object value ) {
		return getStaticScope().assign( context, key, value );
	}

	/**
	 * Dereference this object by a key and return the value, or throw exception
	 *
	 * @param key  The key to dereference
	 * @param safe Whether to throw an exception if the key is not found
	 *
	 * @return The requested object
	 */
	public Object dereference( IBoxContext context, Key key, Boolean safe ) {

		// Special check for $bx
		if ( key.equals( BoxMeta.key ) ) {
			return getBoxMeta();
		}

		return getStaticScope().dereference( context, key, safe );
	}

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method) using positional arguments
	 *
	 * @param name                The key to dereference
	 * @param positionalArguments The positional arguments to pass to the invokable
	 * @param safe                Whether to throw an exception if the key is not found
	 *
	 * @return The requested object
	 */
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Object[] positionalArguments, Boolean safe ) {
		Object func = getStaticScope().get( name );
		if ( func instanceof Function function ) {
			FunctionBoxContext functionContext = Function.generateFunctionContext(
			    function,
			    // Function contexts' parent is the caller. The function will "know" about the class it's executing in
			    // because we've pushed the class onto the template stack in the function context.
			    context,
			    name,
			    positionalArguments,
			    null,
			    this
			);
			return function.invoke( functionContext );
		} else if ( func != null ) {
			throw new BoxRuntimeException( "Key [" + name.getName() + "] in the static scope is not a method." );
		} else {
			throw new KeyNotFoundException(
			    // TODO: Limit the number of keys. There could be thousands!
			    String.format( "The key [%s] was not found in the struct. Valid keys are (%s)", name.getName(), getStaticScope().getKeysAsStrings() )
			);
		}
	}

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method)
	 *
	 * @param name           The name of the key to dereference, which becomes the method name
	 * @param namedArguments The arguments to pass to the invokable
	 * @param safe           If true, return null if the method is not found, otherwise throw an exception
	 *
	 * @return The requested return value or null
	 */
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Map<Key, Object> namedArguments, Boolean safe ) {
		Object func = getStaticScope().get( name );
		if ( func instanceof Function function ) {
			FunctionBoxContext functionContext = Function.generateFunctionContext(
			    function,
			    // Function contexts' parent is the caller. The function will "know" about the class it's executing in
			    // because we've pushed the class onto the template stack in the function context.
			    context,
			    name,
			    namedArguments,
			    null,
			    this
			);
			return function.invoke( functionContext );
		} else if ( func != null ) {
			throw new BoxRuntimeException( "Key [" + name.getName() + "] in the static scope is not a method." );
		} else {
			throw new KeyNotFoundException(
			    // TODO: Limit the number of keys. There could be thousands!
			    String.format( "The key [%s] was not found in the struct. Valid keys are (%s)", name.getName(), getStaticScope().getKeysAsStrings() )
			);
		}
	}

	/**
	 * Get the combined metadata for this function and all it's parameters
	 * This follows the format of Lucee and Adobe's "combined" metadata
	 * This is to keep compatibility for CFML engines
	 *
	 * @return The metadata as a struct
	 */
	public IStruct getMetaData() {
		IStruct meta = new Struct( IStruct.TYPES.SORTED );
		meta.putIfAbsent( "hint", "" );
		meta.putIfAbsent( "output", canOutput() );

		// Assemble the metadata
		var functions = new ArrayList<Object>();
		for ( var entry : getAbstractMethods().keySet() ) {
			var value = getAbstractMethods().get( entry );
			functions.add( value.getMetaData() );
		}
		for ( var entry : getDefaultMethods().keySet() ) {
			var value = getDefaultMethods().get( entry );
			functions.add( value.getMetaData() );
		}
		meta.put( "name", getName().getName() );
		meta.put( "accessors", false );
		meta.put( "functions", Array.fromList( functions ) );
		// meta.put( "hashCode", hashCode() );
		meta.put( "type", "Interface" );
		meta.put( "fullname", getName().getName() );
		meta.put( "path", getRunnablePath().absolutePath().toString() );

		if ( getDocumentation() != null ) {
			meta.putAll( getDocumentation() );
		}
		if ( getAnnotations() != null ) {
			meta.putAll( getAnnotations() );
		}
		if ( getSuper() != null ) {
			meta.put( "extends", getSuper().getMetaData() );
		}
		return meta;
	}

	/**
	 * Vailidate if a given class instance satisfies the interface.
	 * Throws a BoxValidationException if not.
	 *
	 * @param boxClass The class to validate
	 *
	 * @throws BoxValidationException If the class does not satisfy the interface
	 */
	void validateClass( IClassRunnable boxClass ) {
		BoxClassSupport.validateAbstractMethods( boxClass, getAllAbstractMethods() );
	}

	/**
	 * Get interface abstract methods including super interfaces
	 */
	public Map<Key, AbstractFunction> getAllAbstractMethods() {
		Map<Key, AbstractFunction> methods = new LinkedHashMap<>();
		if ( getSuper() != null ) {
			methods.putAll( getSuper().getAllAbstractMethods() );
		}
		// I override my super interface
		methods.putAll( getAbstractMethods() );
		return methods;
	}

	public Map<Key, Function> getAllDefaultMethods() {
		Map<Key, Function> methods = new LinkedHashMap<>();
		if ( getSuper() != null ) {
			methods.putAll( getSuper().getAllDefaultMethods() );
		}
		// I override my super interface
		methods.putAll( getDefaultMethods() );
		return methods;
	}

}
