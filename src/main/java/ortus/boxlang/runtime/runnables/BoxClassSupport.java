/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.runnables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.GenericCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.scopes.BaseScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.StaticScope;
import ortus.boxlang.runtime.scopes.ThisScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.NullValue;
import ortus.boxlang.runtime.types.Property;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.ClassMeta;
import ortus.boxlang.runtime.util.ArgumentUtil;

/**
 * The methods in this class are an extension of IClassRunnable. They are here for better readability
 * since IClassRunnables don't extend a base class, there are placeholders in the BoxClassTransformer that
 * delegate to these methods.
 */
public class BoxClassSupport {

	public static void pseudoConstructor( IClassRunnable thisClass, IBoxContext context ) {
		context.pushTemplate( thisClass );
		try {
			// loop over properties and create variables.
			for ( var property : thisClass.getProperties().values() ) {
				if ( thisClass.getVariablesScope().get( property.name() ) == null ) {
					thisClass.getVariablesScope().assign( context, property.name(), property.defaultValue() );
				}
			}
			// TODO: pre/post interceptor announcements here
			thisClass._pseudoConstructor( context );
		} finally {
			context.popTemplate();
		}
	}

	public static BoxMeta getBoxMeta( IClassRunnable thisClass ) {
		if ( thisClass._getbx() == null ) {
			thisClass._setbx( new ClassMeta( thisClass ) );
		}
		return thisClass._getbx();
	}

	/**
	 * Represent as string, or throw exception if not possible
	 *
	 * @return The string representation
	 */
	public static String asString( IClassRunnable thisClass ) {
		return "Class: " + thisClass.getName().getName();
	}

	/**
	 * A helper to look at the "output" annotation, caching the result
	 *
	 * @return Whether the function can output
	 */
	public static Boolean canOutput( IClassRunnable thisClass ) {
		// Initialize if neccessary
		if ( thisClass.getCanOutput() == null ) {
			thisClass.setCanOutput( BooleanCaster.cast(
			    thisClass.getAnnotations()
			        .getOrDefault(
			            Key.output,
			            false
			        )
			) );
		}
		return thisClass.getCanOutput();
	}

	/**
	 * A helper to look at the "InvokeImplicitAccessor" annotation and application settings, caching the result
	 *
	 * @return Whether the function can invoke implicit accessors
	 */
	public static Boolean canInvokeImplicitAccessor( IClassRunnable thisClass, IBoxContext context ) {
		// Initialize if neccessary
		if ( thisClass.getCanInvokeImplicitAccessor() == null ) {
			synchronized ( thisClass ) {
				if ( thisClass.getCanInvokeImplicitAccessor() == null ) {
					Object setting = thisClass.getAnnotations().get( Key.invokeImplicitAccessor );
					if ( setting == null ) {
						setting = context.getConfigItems( Key.applicationSettings, Key.invokeImplicitAccessor );
					}
					if ( setting != null ) {
						thisClass.setCanInvokeImplicitAccessor( BooleanCaster.cast( setting ) );
					} else {
						thisClass.setCanInvokeImplicitAccessor( false );
					}
				}
			}
		}
		return thisClass.getCanInvokeImplicitAccessor();
	}

	/**
	 * Set the super class.
	 */
	public static void setSuper( IClassRunnable thisClass, IClassRunnable _super ) {
		thisClass._setSuper( _super );
		_super.setChild( thisClass );
		// This runs before the psedu constructor and init, so the base class will override anything it declares
		thisClass.getVariablesScope().addAll( _super.getVariablesScope().getWrapped() );
		thisClass.getThisScope().addAll( _super.getThisScope().getWrapped() );

		// merge properties that don't already exist
		for ( var entry : _super.getProperties().entrySet() ) {
			if ( !thisClass.getProperties().containsKey( entry.getKey() ) ) {
				thisClass.getProperties().put( entry.getKey(), entry.getValue() );
			}
		}
		// merge getterLookup and setterLookup
		thisClass.getGetterLookup().putAll( _super.getGetterLookup() );
		thisClass.getSetterLookup().putAll( _super.getSetterLookup() );

		// merge annotations
		for ( var entry : _super.getAnnotations().entrySet() ) {
			Key key = entry.getKey();
			if ( !thisClass.getAnnotations().containsKey( key ) && !key.equals( Key._EXTENDS ) && !key.equals( Key._IMPLEMENTS ) ) {
				thisClass.getAnnotations().put( key, entry.getValue() );
			}
		}

	}

	/**
	 * Get the bottom class in the inheritance chain
	 */
	public static IClassRunnable getBottomClass( IClassRunnable thisClass ) {
		if ( thisClass.getChild() != null ) {
			return thisClass.getChild().getBottomClass();
		}
		return thisClass;
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
	public static Object assign( IClassRunnable thisClass, IBoxContext context, Key key, Object value ) {
		// If invokeImplicitAccessor is enabled, and the key is a property, invoke the setter method.
		// This may call either a generated setter or a user-defined setter
		if ( thisClass.canInvokeImplicitAccessor( context ) && thisClass.getProperties().containsKey( key ) ) {
			return BoxClassSupport.dereferenceAndInvoke( thisClass, context, thisClass.getProperties().get( key ).setterName(), new Object[] { value }, false );
		}
		// If there is no this key of this name, but there is a static var, then set it
		if ( !thisClass.getThisScope().containsKey( key ) && thisClass.getStaticScope().containsKey( key ) ) {
			return assignStatic( thisClass.getStaticScope(), context, key, value );
		}
		thisClass.getThisScope().assign( context, key, value );
		return value;
	}

	/**
	 * Dereference this object by a key and return the value, or throw exception
	 *
	 * @param key  The key to dereference
	 * @param safe Whether to throw an exception if the key is not found
	 *
	 * @return The requested object
	 */
	public static Object dereference( IClassRunnable thisClass, IBoxContext context, Key key, Boolean safe ) {

		// Special check for $bx
		if ( key.equals( BoxMeta.key ) ) {
			return thisClass.getBoxMeta();
		}

		// If invokeImplicitAccessor is enabled, and the key is a property, invoke the getter method.
		// This may call either a generated getter or a user-defined getter
		if ( thisClass.canInvokeImplicitAccessor( context ) && thisClass.getProperties().containsKey( key ) ) {
			return BoxClassSupport.dereferenceAndInvoke( thisClass, context, thisClass.getProperties().get( key ).getterName(), new Object[] {}, false );
		}

		if ( thisClass.getThisScope().containsKey( key ) ) {
			return thisClass.getThisScope().dereference( context, key, safe );
		}

		if ( thisClass.getStaticScope().containsKey( key ) ) {
			return thisClass.getStaticScope().dereference( context, key, safe );
		}

		if ( thisClass.isJavaExtends() ) {
			return DynamicObject.of( thisClass ).setTargetClass( thisClass.getClass().getSuperclass() ).dereference( context, key, safe );
		}
		if ( safe ) {
			return null;
		} else {
			throw new KeyNotFoundException(
			    // TODO: Limit the number of keys. There could be thousands!
			    String.format( "The key [%s] was not found in the struct. Valid keys are (%s)", key.getName(), thisClass.getThisScope().getKeysAsStrings() )
			);
		}
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
	public static Object dereferenceAndInvoke( IClassRunnable thisClass, IBoxContext context, Key name, Object[] positionalArguments, Boolean safe ) {
		// TODO: component member methods?

		BaseScope scope = thisClass.getThisScope();
		// we are a super class, so we reached here via super.method()
		if ( thisClass.getChild() != null ) {
			scope = thisClass.getVariablesScope();
		}

		// Look for function in this
		Object value = scope.get( name );
		if ( value instanceof Function function ) {
			FunctionBoxContext functionContext = Function.generateFunctionContext(
			    function,
			    // Function contexts' parent is the caller. The function will "know" about the class it's executing in
			    // because we've pushed the class onto the template stack in the function context.
			    context,
			    name,
			    positionalArguments,
			    thisClass
			);

			functionContext.setThisClass( thisClass );
			return function.invoke( functionContext );
		}

		if ( value != null ) {
			throw new BoxRuntimeException(
			    "key '" + name.getName() + "' of type  '" + value.getClass().getName() + "'  is not a function " );
		}

		// Look for function in static
		value = thisClass.getStaticScope().get( name );
		if ( value instanceof Function ) {
			return dereferenceAndInvokeStatic( DynamicObject.of( thisClass.getClass() ), thisClass.getStaticScope(), context, name, positionalArguments, safe );
		}

		// Check for generated accessors
		Object hasAccessors = thisClass.getAnnotations().get( Key.accessors );

		if ( hasAccessors != null && BooleanCaster.cast( hasAccessors ) ) {
			Property getterProperty = thisClass.getGetterLookup().get( name );
			if ( getterProperty != null ) {
				return thisClass.getBottomClass().getVariablesScope().dereference( context, thisClass.getGetterLookup().get( name ).name(), safe );
			}
			Property setterProperty = thisClass.getSetterLookup().get( name );
			if ( setterProperty != null ) {
				Key thisName = setterProperty.name();
				if ( positionalArguments.length == 0 ) {
					throw new BoxRuntimeException( "Missing argument for setter '" + name.getName() + "'" );
				}
				Object valueToSet = positionalArguments[ 0 ];
				// If there is a type on the property, enforce it on the incoming arg
				if ( setterProperty.type() != null && !setterProperty.type().equalsIgnoreCase( "any" ) ) {
					CastAttempt<Object> typeCheck = GenericCaster.attempt( context, valueToSet, setterProperty.type(), true );
					if ( !typeCheck.wasSuccessful() ) {
						String actualType;
						if ( valueToSet == null ) {
							actualType = "null";
						} else {
							actualType = valueToSet.getClass().getName();
						}
						throw new BoxValidationException(
						    String.format( "The provided value to the function [%s()] is of type [%s] does not match the declared property type of [%s]",
						        name.getName(), actualType, setterProperty.type() )
						);
					}
					if ( typeCheck.get() instanceof NullValue ) {
						valueToSet = null;
					}
					valueToSet = typeCheck.get();
				}
				thisClass.getBottomClass().getVariablesScope().assign( context, thisName, valueToSet );
				return thisClass;
			}
		}

		if ( thisClass.getThisScope().get( Key.onMissingMethod ) != null ) {
			return thisClass.dereferenceAndInvoke( context, Key.onMissingMethod,
			    new Object[] { name.getName(), ArgumentUtil.createArgumentsScope( context, positionalArguments ) }, safe );
		}

		if ( thisClass.isJavaExtends() ) {
			return DynamicObject.of( thisClass ).setTargetClass( thisClass.getClass().getSuperclass() ).dereferenceAndInvoke( context, name,
			    positionalArguments, safe );
		}

		if ( !safe ) {
			throw new BoxRuntimeException( "Method '" + name.getName() + "' not found" );
		}
		return null;
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
	public static Object dereferenceAndInvoke( IClassRunnable thisClass, IBoxContext context, Key name, Map<Key, Object> namedArguments, Boolean safe ) {

		BaseScope scope = thisClass.getThisScope();
		// we are a super class, so we reached here via super.method()
		if ( thisClass.getChild() != null ) {
			scope = thisClass.getVariablesScope();
		}

		Object value = scope.get( name );
		if ( value instanceof Function function ) {
			FunctionBoxContext functionContext = Function.generateFunctionContext(
			    function,
			    // Function contexts' parent is the caller. The function will "know" about the class it's executing in
			    // because we've pushed the class onto the template stack in the function context.
			    context,
			    name,
			    namedArguments,
			    thisClass
			);

			functionContext.setThisClass( thisClass );
			return function.invoke( functionContext );
		}

		if ( thisClass.getSuper() != null && thisClass.getSuper().getThisScope().get( name ) != null ) {
			return thisClass.getSuper().dereferenceAndInvoke( context, name, namedArguments, safe );
		}

		// Look for function in static
		value = thisClass.getStaticScope().get( name );
		if ( value instanceof Function ) {
			return dereferenceAndInvokeStatic( DynamicObject.of( thisClass.getClass() ), thisClass.getStaticScope(), context, name, namedArguments, safe );
		}

		if ( value != null ) {
			throw new BoxRuntimeException(
			    "key '" + name.getName() + "' of type  '" + value.getClass().getName() + "'  is not a function " );
		}

		// Check for generated accessors
		Object hasAccessors = thisClass.getAnnotations().get( Key.accessors );
		if ( hasAccessors != null && BooleanCaster.cast( hasAccessors ) ) {

			// Getter Call and Return
			Property getterProperty = thisClass.getGetterLookup().get( name );
			if ( getterProperty != null ) {
				return thisClass.getBottomClass().getVariablesScope().dereference( context, getterProperty.name(), safe );
			}

			// Setter Call and Return
			Property setterProperty = thisClass.getSetterLookup().get( name );
			if ( setterProperty != null ) {
				Key		thisName	= setterProperty.name();
				Object	thisValue	= namedArguments.containsKey( thisName ) ? namedArguments.get( thisName ) : null;

				// If we are still null, check an argument collection
				if ( thisValue == null && namedArguments.containsKey( Function.ARGUMENT_COLLECTION ) ) {
					Object argCollection = namedArguments.get( Function.ARGUMENT_COLLECTION );
					if ( argCollection instanceof IStruct castedArgCollection ) {
						thisValue = castedArgCollection.getOrDefault( thisName, null );
					} else if ( argCollection instanceof List castedArgCollection && !castedArgCollection.isEmpty() ) {
						thisValue = castedArgCollection.get( 0 );
					}
				}

				if ( thisValue == null ) {
					throw new BoxRuntimeException(
					    "Missing argument value for setter '" + name.getName() + "'. The passed arguments are [" + namedArguments.toString() + "]" );
				}

				thisClass.getBottomClass().getVariablesScope().assign( context, thisName, thisValue );
				return thisClass;
			}
		}

		if ( thisClass.getThisScope().get( Key.onMissingMethod ) != null ) {
			Map<Key, Object> args = new HashMap<Key, Object>();
			args.put( Key.missingMethodName, name.getName() );
			args.put( Key.missingMethodArguments, ArgumentUtil.createArgumentsScope( context, namedArguments ) );
			return thisClass.dereferenceAndInvoke( context, Key.onMissingMethod, args, safe );
		}

		if ( !safe ) {
			throw new BoxRuntimeException( "Method '" + name.getName() + "' not found" );
		}
		return null;
	}

	/**
	 * Get the combined metadata for this function and all it's parameters
	 * This follows the format of Lucee and Adobe's "combined" metadata
	 * TODO: Move this to compat module
	 *
	 * @return The metadata as a struct
	 */
	public static IStruct getMetaData( IClassRunnable thisClass ) {
		IStruct meta = new Struct( IStruct.TYPES.SORTED );
		meta.putIfAbsent( "hint", "" );
		meta.putIfAbsent( "output", thisClass.canOutput() );
		meta.putIfAbsent( "invokeImplicitAccessor", thisClass.getCanInvokeImplicitAccessor() );

		// Assemble the metadata
		var functions = new ArrayList<Object>();
		// loop over target's variables scope and add metadata for each function
		for ( var entry : thisClass.getThisScope().keySet() ) {
			var value = thisClass.getThisScope().get( entry );
			if ( value instanceof Function fun ) {
				functions.add( fun.getMetaData() );
			}
		}
		meta.put( "name", thisClass.getName().getName() );
		meta.put( "accessors", thisClass.getAnnotations().getOrDefault( Key.accessors, false ) );
		meta.put( "functions", Array.fromList( functions ) );

		// meta.put( "hashCode", hashCode() );
		var properties = new Array();
		// loop over properties list and add struct for each property
		for ( var entry : thisClass.getProperties().entrySet() ) {
			var	property		= entry.getValue();
			var	propertyStruct	= new Struct( IStruct.TYPES.LINKED );
			propertyStruct.put( "name", property.name().getName() );
			propertyStruct.put( "type", property.type() );
			propertyStruct.put( "default", property.defaultValue() );
			if ( property.documentation() != null ) {
				propertyStruct.putAll( property.documentation() );
			}
			if ( property.annotations() != null ) {
				propertyStruct.putAll( property.annotations() );
			}
			properties.add( propertyStruct );
		}
		meta.put( "properties", properties );
		meta.put( "type", "Component" );
		meta.put( "name", thisClass.getName().getName() );
		meta.put( "fullname", thisClass.getName().getName() );
		meta.put( "path", thisClass.getRunnablePath().absolutePath().toString() );
		meta.put( "persisent", false );

		if ( thisClass.getDocumentation() != null ) {
			meta.putAll( thisClass.getDocumentation() );
		}
		if ( thisClass.getAnnotations() != null ) {
			meta.putAll( thisClass.getAnnotations() );
		}
		if ( thisClass.getSuper() != null ) {
			meta.put( "extends", thisClass.getSuper().getMetaData() );
		}
		return meta;
	}

	public static void registerInterface( IClassRunnable thisClass, BoxInterface _interface ) {
		_interface.validateClass( thisClass );
		VariablesScope	variablesScope	= thisClass.getVariablesScope();
		ThisScope		thisScope		= thisClass.getThisScope();
		thisClass.getInterfaces().add( _interface );
		// Add in default methods to the this and variables scopes
		for ( Map.Entry<Key, Function> entry : _interface.getDefaultMethods().entrySet() ) {
			if ( !variablesScope.containsKey( entry.getKey() ) ) {
				variablesScope.put( entry.getKey(), entry.getValue() );
			}
			if ( !thisScope.containsKey( entry.getKey() ) && entry.getValue().getAccess() == Function.Access.PUBLIC ) {
				thisScope.put( entry.getKey(), entry.getValue() );
			}
		}
	}

	public static Object dereferenceAndInvokeStatic( DynamicObject targetClass, IBoxContext context, Key name, Map<Key, Object> namedArguments, Boolean safe ) {
		StaticScope staticScope = getStaticScope( targetClass );
		return dereferenceAndInvokeStatic( targetClass, staticScope, context, name, namedArguments, safe );
	}

	public static Object dereferenceAndInvokeStatic( DynamicObject targetClass, IBoxContext context, Key name, Object[] positionalArguments, Boolean safe ) {
		StaticScope staticScope = getStaticScope( targetClass );
		return dereferenceAndInvokeStatic( targetClass, staticScope, context, name, positionalArguments, safe );
	}

	public static Object assignStatic( DynamicObject targetClass, IBoxContext context, Key name, Object value ) {
		StaticScope staticScope = getStaticScope( targetClass );
		return assignStatic( staticScope, context, name, value );
	}

	public static Object dereferenceStatic( DynamicObject targetClass, IBoxContext context, Key name, Boolean safe ) {
		StaticScope staticScope = getStaticScope( targetClass );
		return dereferenceStatic( staticScope, context, name, safe );
	}

	public static Object dereferenceAndInvokeStatic( DynamicObject targetClass, StaticScope staticScope, IBoxContext context, Key name,
	    Map<Key, Object> namedArguments, Boolean safe ) {
		Object func = staticScope.get( name );
		if ( func instanceof Function function ) {
			FunctionBoxContext functionContext = Function.generateFunctionContext(
			    function,
			    // Function contexts' parent is the caller. The function will "know" about the class it's executing in
			    // because we've pushed the class onto the template stack in the function context.
			    context,
			    name,
			    namedArguments,
			    null
			);

			functionContext.setThisStaticClass( targetClass );
			return function.invoke( functionContext );
		} else if ( func != null ) {
			throw new BoxRuntimeException( "Key [" + name.getName() + "] in the static scope is not a method." );
		} else {
			throw new KeyNotFoundException(
			    // TODO: Limit the number of keys. There could be thousands!
			    String.format( "The key [%s] was not found in the struct. Valid keys are (%s)", name.getName(), staticScope.getKeysAsStrings() )
			);
		}
	}

	public static Object dereferenceAndInvokeStatic( DynamicObject targetClass, StaticScope staticScope, IBoxContext context, Key name,
	    Object[] positionalArguments, Boolean safe ) {
		Object func = staticScope.get( name );
		if ( func instanceof Function function ) {
			FunctionBoxContext functionContext = Function.generateFunctionContext(
			    function,
			    // Function contexts' parent is the caller. The function will "know" about the class it's executing in
			    // because we've pushed the class onto the template stack in the function context.
			    context,
			    name,
			    positionalArguments,
			    null
			);

			functionContext.setThisStaticClass( targetClass );
			return function.invoke( functionContext );
		} else if ( func != null ) {
			throw new BoxRuntimeException( "Key [" + name.getName() + "] in the static scope is not a method." );
		} else {
			throw new KeyNotFoundException(
			    // TODO: Limit the number of keys. There could be thousands!
			    String.format( "The key [%s] was not found in the struct. Valid keys are (%s)", name.getName(), staticScope.getKeysAsStrings() )
			);
		}
	}

	public static Object assignStatic( StaticScope staticScope, IBoxContext context, Key name, Object value ) {
		// If there is no this key of this name, but there is a static var, then set it
		staticScope.put( name, value );
		return value;
	}

	public static Object dereferenceStatic( StaticScope staticScope, IBoxContext context, Key name, Boolean safe ) {
		return staticScope.dereference( context, name, safe );
	}

	public static StaticScope getStaticScope( DynamicObject targetClass ) {
		return ( StaticScope ) targetClass.invokeStatic( "getStaticScopeStatic" );
	}

	public static IStruct getAnnotations( DynamicObject targetClass ) {
		return ( IStruct ) targetClass.invokeStatic( "getAnnotationsStatic" );
	}

	/**
	 * A helper to look at the "output" annotation from a static context
	 *
	 * @return Whether the function can output
	 */
	public static Boolean canOutput( DynamicObject targetClass ) {
		return BooleanCaster.cast( getAnnotations( targetClass )
		    .getOrDefault(
		        Key.output,
		        false
		    ) );
	}

	/**
	 * Take an object and check if it is a dynamic object already or a string, in which case, load the class.
	 */
	public static DynamicObject ensureClass( IBoxContext context, Object obj, List<ImportDefinition> imports ) {
		if ( obj instanceof DynamicObject dynO ) {
			return dynO;
		}
		if ( obj instanceof String str ) {
			return ClassLocator.getInstance().load( context, str, imports );
		}
		throw new BoxRuntimeException( "Cannot load class for static access.  Type provided: " + obj.getClass().getName() );

	}

}
