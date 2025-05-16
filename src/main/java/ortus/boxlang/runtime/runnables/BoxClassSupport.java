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

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.MemberDescriptor;
import ortus.boxlang.runtime.context.BaseBoxContext;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.scopes.BaseScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.StaticScope;
import ortus.boxlang.runtime.scopes.ThisScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.AbstractFunction;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.UDF;
import ortus.boxlang.runtime.types.exceptions.AbstractClassException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.BoxValidationException;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.ClassMeta;
import ortus.boxlang.runtime.util.ArgumentUtil;
import ortus.boxlang.runtime.util.BoxFQN;

/**
 * The methods in this class are an extension of IClassRunnable. They are here for better readability
 * since IClassRunnables don't extend a base class, there are placeholders in the BoxClassTransformer that
 * delegate to these methods.
 */
public class BoxClassSupport {

	/**
	 * Call the pseudo constructor
	 *
	 * @param thisClass The class to call the pseudo constructor on
	 * @param context   The context to use
	 */
	public static void pseudoConstructor( IClassRunnable thisClass, IBoxContext context ) {
		context.pushTemplate( thisClass );
		try {
			// TODO: pre/post interceptor announcements here
			thisClass._pseudoConstructor( context );
		} finally {
			context.popTemplate();
		}
	}

	/**
	 * I handle creating default values for all properties defined
	 *
	 * @param thisClass The class to create default properties for
	 * @param context   The context to use
	 */
	public static void defaultProperties( IClassRunnable thisClass, IBoxContext context ) {
		// loop over properties and create variables.
		for ( var property : thisClass.getProperties().values() ) {
			Object existing = thisClass.getVariablesScope().get( property.name() );
			// Don't override existing values, probably from a super class
			// But UDFs of the same name? Yeah, nuke those suckers, lol. (╥﹏╥)
			if ( existing == null || existing instanceof Function ) {
				Object defaultValue = property.getDefaultValue( context );
				// If the compat module is making null behave like CF, then don't default null properties. ColdBox blows up otherwise, and I'm sure other code as well.
				if ( defaultValue != null || !BaseBoxContext.nullIsUndefined ) {
					thisClass.getVariablesScope().assign( context, property.name(), defaultValue );
				}
			}
			if ( hasAccessors( thisClass ) ) {
				// Don't override UDFs from a parent class which may already be defined
				if ( thisClass.getGetterLookup().containsKey( property.getterName() ) ) {
					context.registerUDF( property.generatedGetter(), false );
				}
				if ( thisClass.getSetterLookup().containsKey( property.setterName() ) ) {
					context.registerUDF( property.generatedSetter(), false );
				}
			}
		}
	}

	/**
	 * Get the class metadata
	 *
	 * @param thisClass The class to get the metadata for
	 *
	 * @return The metadata Box object
	 */
	public static BoxMeta getBoxMeta( IClassRunnable thisClass ) {
		if ( thisClass._getbx() == null ) {
			thisClass._setbx( new ClassMeta( thisClass ) );
		}
		return thisClass._getbx();
	}

	/**
	 * Represent as string, or throw exception if not possible
	 *
	 * @param thisClass The class to represent
	 *
	 * @return The string representation
	 */
	public static String asString( IClassRunnable thisClass ) {
		return "Class: " + thisClass.bxGetName().getName();
	}

	/**
	 * A helper to look at the "output" annotation, caching the result
	 *
	 * @param thisClass The class to check
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
			            // output defaults to true for Application.bx, but false for all others
			            // Strip just the class name from the FQN foo.com.bar.Application
			            new BoxFQN( thisClass.bxGetName().getName() ).getClassName().equalsIgnoreCase( "application" )
			        )
			) );
		}
		return thisClass.getCanOutput();
	}

	/**
	 * A helper to look at the "InvokeImplicitAccessor" annotation and application settings, caching the result
	 *
	 * @param context   The context to use
	 * @param thisClass The class to check
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
						// Box classes default to true, but CF classes default to false
						if ( thisClass.getSourceType().equals( BoxSourceType.BOXSCRIPT ) ) {
							thisClass.setCanInvokeImplicitAccessor( true );
						} else {
							thisClass.setCanInvokeImplicitAccessor( false );
						}
					}
				}
			}
		}
		return thisClass.getCanInvokeImplicitAccessor();
	}

	/**
	 * Set the super class.
	 * This method is called by the BoxClassTransformer
	 *
	 * @param thisClass The class to set the super class on
	 * @param _super    The super class
	 */
	public static void setSuper( IClassRunnable thisClass, IClassRunnable _super ) {
		thisClass._setSuper( _super );
		_super.setChild( thisClass );
		// This runs before the pseudo constructor and init, so the base class will override anything it declares
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

		// DO NOT merge annotations. They stay separate between parent/child classes and must be merged at runtime, if desired.
		// https://ortussolutions.atlassian.net/browse/BL-677

	}

	/**
	 * Get the bottom class in the inheritance chain
	 *
	 * @param thisClass The class to start from
	 *
	 * @return The bottom class if any, otherwise the class itself
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
	 * @param thisClass The class to assign the value to
	 * @param context   The context to use
	 * @param key       The key to assign
	 * @param value     The value to assign
	 *
	 * @return The assigned value
	 */
	public static Object assign( IClassRunnable thisClass, IBoxContext context, Key key, Object value ) {

		// This would only matter if we called super.myField and we'd want the bottom class's this scope
		thisClass = thisClass.getBottomClass();

		// If invokeImplicitAccessor is enabled, and the key is a property, invoke the setter method.
		// This may call either a generated setter or a user-defined setter
		if ( thisClass.canInvokeImplicitAccessor( context ) && thisClass.getProperties().containsKey( key ) ) {
			// recursion avoidance
			if ( context instanceof FunctionBoxContext fbc && fbc.isInClass() && fbc.getThisClass() == thisClass ) {
				return thisClass.getThisScope().assign( context, key, value );
			}
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
	 * @param thisClass The class to dereference
	 * @param context   The context to use
	 * @param key       The key to dereference
	 * @param safe      Whether to throw an exception if the key is not found
	 *
	 * @return The requested object
	 */
	public static Object dereference( IClassRunnable thisClass, IBoxContext context, Key key, Boolean safe ) {

		// Special check for $bx
		if ( key.equals( BoxMeta.key ) ) {
			return thisClass.getBoxMeta();
		}

		// This would only matter if we called super.myField and we'd want the bottom class's this scope
		thisClass = thisClass.getBottomClass();

		// If invokeImplicitAccessor is enabled, and the key is a property, invoke the getter method.
		// This may call either a generated getter or a user-defined getter
		if ( thisClass.canInvokeImplicitAccessor( context ) && thisClass.getProperties().containsKey( key ) ) {
			// recursion avoidance
			if ( context instanceof FunctionBoxContext fbc && fbc.isInClass() && fbc.getThisClass() == thisClass ) {
				return thisClass.getThisScope().dereference( context, key, safe );
			}
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
	 * @param thisClass           The class to dereference
	 * @param context             The context to use
	 * @param name                The key to dereference
	 * @param positionalArguments The positional arguments to pass to the invokable
	 * @param safe                Whether to throw an exception if the key is not found
	 *
	 * @return The requested object
	 */
	public static Object dereferenceAndInvoke( IClassRunnable thisClass, IBoxContext context, Key name, Object[] positionalArguments, Boolean safe ) {
		// Where to look for the functions
		// This should always be the "bottom" class since "super" is the only way to get a direct reference to a parent class
		BaseScope scope = thisClass.getThisScope();
		// we are a super class, so we reached here via super.method()
		if ( thisClass.getChild() != null ) {
			// Don't use getBottomClass() as we want to get the actual UDFs at this level.
			// When the UDF runs, the scopes it "sees" will still be from the bottom (except super, of course)
			scope = thisClass.getVariablesScope();
		}

		// Look for function in this scope
		Object value = scope.get( name );
		if ( value instanceof Function function ) {
			FunctionBoxContext functionContext = Function.generateFunctionContext(
			    function,
			    // Function contexts' parent is the caller. The function will "know" about the class it's executing in
			    // because we've pushed the class onto the template stack in the function context.
			    context,
			    name,
			    positionalArguments,
			    thisClass,
			    null,
			    null
			);

			return function.invoke( functionContext );
		}

		// Look for function in the parent class if any
		if ( thisClass.getSuper() != null && thisClass.getSuper().getThisScope().get( name ) != null ) {
			return thisClass.getSuper().dereferenceAndInvoke( context, name, positionalArguments, safe );
		}

		// Look for function in static
		value = thisClass.getStaticScope().get( name );
		if ( value instanceof Function ) {
			return dereferenceAndInvokeStatic( DynamicObject.of( thisClass.getClass() ), thisClass.getStaticScope(), context, name, positionalArguments, safe );
		}

		// Not a function, throw an exception
		if ( value != null ) {
			throw new BoxRuntimeException(
			    "key '" + name.getName() + "' of type  '" + value.getClass().getName() + "'  is not a function " );
		}

		// Do we have a member function for classes?
		MemberDescriptor memberDescriptor = BoxRuntime.getInstance().getFunctionService().getMemberMethod( name, BoxLangType.CLASS );
		if ( memberDescriptor != null ) {
			return memberDescriptor.invoke( context, thisClass, positionalArguments );
		}

		// Do we have an onMissingMethod() method?
		if ( thisClass.getThisScope().get( Key.onMissingMethod ) != null ) {
			return thisClass.dereferenceAndInvoke(
			    context,
			    Key.onMissingMethod,
			    new Object[] { name.getName(), ArgumentUtil.createArgumentsScope( context, positionalArguments ) },
			    safe
			);
		}

		// Do we have a super java class? Only positional arguments are supported for Java classes
		if ( thisClass.isJavaExtends() ) {
			return DynamicObject.of( thisClass ).setTargetClass( thisClass.getClass().getSuperclass() ).dereferenceAndInvoke( context, name,
			    positionalArguments, safe );
		}

		// If not safe, throw an exception
		if ( !safe ) {
			throw new BoxRuntimeException( "Method '" + name.getName() + "' not found" );
		}

		return null;
	}

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method)
	 *
	 * @param thisClass      The class to dereference
	 * @param context        The context to use
	 * @param name           The name of the key to dereference, which becomes the method name
	 * @param namedArguments The arguments to pass to the invokable
	 * @param safe           If true, return null if the method is not found, otherwise throw an exception
	 *
	 * @return The requested return value or null
	 */
	public static Object dereferenceAndInvoke( IClassRunnable thisClass, IBoxContext context, Key name, Map<Key, Object> namedArguments, Boolean safe ) {
		// Where to look for the functions
		// This should always be the "bottom" class since "super" is the only way to get a direct reference to a parent class
		BaseScope scope = thisClass.getThisScope();
		// we are a super class, so we reached here via super.method()
		if ( thisClass.getChild() != null ) {
			// Don't use getBottomClass() as we want to get the actual UDFs at this level.
			// When the UDF runs, the scopes it "sees" will still be from the bottom (except super, of course)
			scope = thisClass.getVariablesScope();
		}

		// Look for function in this scope
		Object value = scope.get( name );
		if ( value instanceof Function function ) {
			FunctionBoxContext functionContext = Function.generateFunctionContext(
			    function,
			    // Function contexts' parent is the caller. The function will "know" about the class it's executing in
			    // because we've pushed the class onto the template stack in the function context.
			    context,
			    name,
			    namedArguments,
			    thisClass,
			    null,
			    null
			);

			return function.invoke( functionContext );
		}

		// Look for function in the parent class if any
		if ( thisClass.getSuper() != null && thisClass.getSuper().getThisScope().get( name ) != null ) {
			return thisClass.getSuper().dereferenceAndInvoke( context, name, namedArguments, safe );
		}

		// Look for function in static
		value = thisClass.getStaticScope().get( name );
		if ( value instanceof Function ) {
			return dereferenceAndInvokeStatic( DynamicObject.of( thisClass.getClass() ), thisClass.getStaticScope(), context, name, namedArguments, safe );
		}

		// Not a function, throw an exception
		if ( value != null ) {
			throw new BoxRuntimeException(
			    "key '" + name.getName() + "' of type  '" + value.getClass().getName() + "'  is not a function " );
		}

		// Do we have a member function for classes?
		MemberDescriptor memberDescriptor = BoxRuntime.getInstance().getFunctionService().getMemberMethod( name, BoxLangType.CLASS );
		if ( memberDescriptor != null ) {
			return memberDescriptor.invoke( context, thisClass, namedArguments );
		}

		// Do we have an onMissingMethod() method?
		if ( thisClass.getThisScope().get( Key.onMissingMethod ) != null ) {
			Map<Key, Object> args = new HashMap<>();
			args.put( Key.missingMethodName, name.getName() );
			args.put( Key.missingMethodArguments, ArgumentUtil.createArgumentsScope( context, namedArguments ) );
			return thisClass.dereferenceAndInvoke( context, Key.onMissingMethod, args, safe );
		}

		// If not safe, throw an exception
		if ( !safe ) {
			throw new BoxRuntimeException( "Method '" + name.getName() + "' not found" );
		}

		return null;
	}

	/**
	 * Get the combined metadata for this function and all it's parameters
	 * This follows the format of Lucee and Adobe's "combined" metadata
	 * This is to keep compatibility for CFML engines
	 *
	 * @param thisClass The class to get the metadata for
	 *
	 * @return The metadata as a struct
	 */
	public static IStruct getMetaData( IClassRunnable thisClass ) {
		IStruct meta = new Struct( IStruct.TYPES.SORTED );
		meta.putIfAbsent( "hint", "" );
		meta.putIfAbsent( "output", thisClass.canOutput() );
		meta.putIfAbsent( "invokeImplicitAccessor", thisClass.getCanInvokeImplicitAccessor() );

		// Assemble the metadata
		var	functions				= new ArrayList<Object>();
		var	compileTimeMethodNames	= thisClass.getCompileTimeMethodNames();
		// loop over target's variables scope and add metadata for each function
		for ( var entry : compileTimeMethodNames ) {
			// The only reason it wouldn't be a functon is if someone overwrote/removed it in the variables scope
			if ( thisClass.getVariablesScope().get( entry ) instanceof Function fun ) {
				functions.add( fun.getMetaData() );
			}
		}
		meta.put( Key._NAME, thisClass.bxGetName().getName() );
		meta.put( Key.accessors, hasAccessors( thisClass ) );
		meta.put( Key.functions, Array.fromList( functions ) );

		// meta.put( "hashCode", hashCode() );
		var properties = new Array();
		// loop over properties list and add struct for each property
		for ( var entry : thisClass.getProperties().entrySet() ) {
			var property = entry.getValue();
			// Only include properties declared here, not in a parent/extends
			if ( property.declaringClass() != thisClass.getClass() ) {
				continue;
			}
			var propertyStruct = new Struct( IStruct.TYPES.LINKED );
			propertyStruct.put( Key._NAME, property.name().getName() );
			propertyStruct.put( Key.type, property.type() );
			if ( property.hasDefaultValue() ) {
				propertyStruct.put( Key._DEFAULT, property.getDefaultValueForMeta() );
			}
			if ( property.documentation() != null ) {
				propertyStruct.putAll( property.documentation() );
			}
			if ( property.annotations() != null ) {
				if ( property.annotations() != null ) {
					for ( var annotation : property.annotations().entrySet() ) {
						if ( !annotation.getKey().equals( Key._DEFAULT ) ) {
							propertyStruct.put( annotation.getKey(), annotation.getValue() );
						}
					}
				}
			}
			properties.add( propertyStruct );
		}
		meta.put( Key.properties, properties );
		meta.put( Key.type, "Component" );
		meta.put( Key._NAME, thisClass.bxGetName().getName() );
		meta.put( Key.fullname, thisClass.bxGetName().getName() );
		meta.put( Key.path, thisClass.getRunnablePath().absolutePath().toString() );
		meta.put( Key.persisent, false );

		if ( thisClass.getDocumentation() != null ) {
			meta.putAll( thisClass.getDocumentation() );
		}
		if ( thisClass.getAnnotations() != null ) {
			meta.putAll( thisClass.getAnnotations() );
		}
		if ( thisClass.getSuper() != null ) {
			meta.put( Key._EXTENDS, thisClass.getSuper().getMetaData() );
		}
		return meta;
	}

	/**
	 * Register an interface with a class
	 *
	 * @param thisClass  The class to register the interface with
	 * @param _interface The interface to register
	 */
	public static void registerInterface( IClassRunnable thisClass, BoxInterface _interface ) {
		_interface.validateClass( thisClass );
		VariablesScope	variablesScope	= thisClass.getVariablesScope();
		ThisScope		thisScope		= thisClass.getThisScope();
		thisClass.getInterfaces().add( _interface );
		// Add in default methods to the this and variables scopes
		// The get "ALL" default methods includes super interfaces
		for ( Map.Entry<Key, Function> entry : _interface.getAllDefaultMethods().entrySet() ) {
			if ( !variablesScope.containsKey( entry.getKey() ) ) {
				variablesScope.put( entry.getKey(), entry.getValue() );
			}
			if ( !thisScope.containsKey( entry.getKey() ) && entry.getValue().getAccess() == Function.Access.PUBLIC ) {
				thisScope.put( entry.getKey(), entry.getValue() );
			}
		}
	}

	public static Object dereferenceAndInvokeStatic( DynamicObject targetClass, IBoxContext context, Key name, Map<Key, Object> namedArguments, Boolean safe ) {
		StaticScope staticScope = getStaticScope( context, targetClass );
		return dereferenceAndInvokeStatic( targetClass, staticScope, context, name, namedArguments, safe );
	}

	public static Object dereferenceAndInvokeStatic( DynamicObject targetClass, IBoxContext context, Key name, Object[] positionalArguments, Boolean safe ) {
		StaticScope staticScope = getStaticScope( context, targetClass );
		return dereferenceAndInvokeStatic( targetClass, staticScope, context, name, positionalArguments, safe );
	}

	public static Object assignStatic( DynamicObject targetClass, IBoxContext context, Key name, Object value ) {
		StaticScope staticScope = getStaticScope( context, targetClass );
		return assignStatic( staticScope, context, name, value );
	}

	public static Object dereferenceStatic( DynamicObject targetClass, IBoxContext context, Key name, Boolean safe ) {
		StaticScope staticScope = getStaticScope( context, targetClass );
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
			    null,
			    targetClass,
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
			    null,
			    targetClass,
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

	/**
	 * Assign a value to a key in the static scope
	 *
	 * @param staticScope The static scope to assign the value to
	 * @param context     The context to use
	 * @param name        The key to assign
	 * @param value       The value to assign
	 *
	 * @return The assigned value
	 */
	public static Object assignStatic( StaticScope staticScope, IBoxContext context, Key name, Object value ) {
		// If there is no this key of this name, but there is a static var, then set it
		staticScope.put( name, value );
		return value;
	}

	/**
	 * Dereference this object by a key and return the value, or throw exception
	 * This is a static version of the dereference method
	 *
	 * @param staticScope The static scope to dereference
	 * @param context     The context to use
	 * @param name        The key to dereference
	 * @param safe        Whether to throw an exception if the key is not found
	 *
	 * @return The requested object
	 */
	public static Object dereferenceStatic( StaticScope staticScope, IBoxContext context, Key name, Boolean safe ) {
		return staticScope.dereference( context, name, safe );
	}

	/**
	 * Get the static scope from a static context
	 *
	 * @param context     The context to use
	 * @param targetClass The class to get the static scope from
	 *
	 * @return The static scope
	 */
	public static StaticScope getStaticScope( IBoxContext context, DynamicObject targetClass ) {
		return ( StaticScope ) targetClass.invokeStatic( context, "getStaticScopeStatic" );
	}

	/**
	 * Get the annotations from a static context
	 *
	 * @param context     The context to use
	 * @param targetClass The class to get the annotations from
	 *
	 * @return The annotations
	 */
	public static IStruct getAnnotations( IBoxContext context, DynamicObject targetClass ) {
		return ( IStruct ) targetClass.invokeStatic( context, "getAnnotationsStatic" );
	}

	/**
	 * A helper to look at the "output" annotation from a static context
	 * By default in BoxLang this is false
	 *
	 * @param context     The context to use
	 * @param targetClass The class to check
	 *
	 * @return Whether the function can output
	 */
	public static Boolean canOutput( IBoxContext context, DynamicObject targetClass ) {
		return BooleanCaster.cast( getAnnotations( context, targetClass )
		    .getOrDefault(
		        Key.output,
		        false
		    ) );
	}

	/**
	 * A helper to look at the "accessors" annotation
	 * By default in BoxLang this is true
	 *
	 * @param targetClass The class to check
	 *
	 * @return Whether the class has accessors
	 */
	public static Boolean hasAccessors( IClassRunnable targetClass ) {
		return BooleanCaster.cast(
		    targetClass
		        .getAnnotations()
		        .getOrDefault(
		            Key.accessors,
		            true
		        )
		);
	}

	/**
	 * Take an object and check if it is a dynamic object already or a string, in which case, load the class.
	 *
	 * @param context The context to use
	 * @param obj     The object to check
	 * @param imports The imports to use
	 *
	 * @return The dynamic object of the class
	 */
	public static DynamicObject ensureClass( IBoxContext context, Object obj, List<ImportDefinition> imports ) {
		if ( obj instanceof DynamicObject dynO ) {
			return dynO;
		}
		if ( obj instanceof String str ) {
			// This could be a variable reference, or a class name. Let's find out.
			Object variableRef = ExpressionInterpreter.getVariable( context, str, true );
			// We found a variable of this name. What is it?
			if ( variableRef != null ) {
				if ( variableRef instanceof DynamicObject dynO ) {
					return dynO;
				}
				if ( variableRef instanceof Class c ) {
					return DynamicObject.of( c );
				}
				// If there is a var, but it's not a static class refernce, then we'll just ignore it now and move on to our class loading attempt
			}
			return BoxRuntime.getInstance().getClassLocator().load( context, str, imports );
		}
		throw new BoxRuntimeException( "Cannot load class for static access.  Did you try to statically dereference an instance on accident?  Type provided: "
		    + obj.getClass().getName() );

	}

	/**
	 * Vailidate if a given class instance satisfies the interface.
	 * Throws a BoxValidationException if not.
	 *
	 * @param thisClass       The class to validate
	 * @param abstractMethods The abstract methods for which to check the class for existence
	 *
	 * @throws BoxValidationException If the class does not satisfy the interface
	 */
	public static void validateAbstractMethods( IClassRunnable thisClass, Map<Key, AbstractFunction> abstractMethods ) {
		String className = thisClass.bxGetName().getName();

		// Having an onMissingMethod() UDF is the golden ticket to implementing any interface
		if ( thisClass.getThisScope().get( Key.onMissingMethod ) instanceof Function ) {
			return;
		}

		for ( Map.Entry<Key, AbstractFunction> abstractMethod : abstractMethods.entrySet() ) {
			if ( thisClass.getThisScope().containsKey( abstractMethod.getKey() )
			    && thisClass.getThisScope().get( abstractMethod.getKey() ) instanceof Function classMethod ) {
				if ( !classMethod.implementsSignature( abstractMethod.getValue() ) ) {
					throw new AbstractClassException(
					    "Class [" + className + "] has method [" + classMethod.signatureAsString() + "] but the signature doesn't match the signature of ["
					        + abstractMethod.getValue().signatureAsString() + "] in " + abstractMethod.getValue().getSourceObjectType() + " ["
					        + abstractMethod.getValue().getSourceObjectName() + "]." );
				}
			} else {
				throw new AbstractClassException(
				    "Class [" + className + "] does not implement method [" + abstractMethod.getValue().signatureAsString() + "] from "
				        + abstractMethod.getValue().getSourceObjectType() + " [" + abstractMethod.getValue().getSourceObjectName() + "]." );
			}
		}
	}

	/**
	 * Given a UDF instance, resolve the actual class it was declared in. A method may be inherited by a child class, and copied down into the child class's
	 * variables scope, but at runtime, the class it uses as `super` needs to be relative to the current location.
	 */
	public static IClassRunnable resolveClassForUDF( IClassRunnable thisClass, Function udf ) {
		// If null, then skip all our logic
		if ( thisClass == null ) {
			return null;
		}

		// This logic is only for UDFs, not closure, lambdas, etc
		if ( ! ( udf instanceof UDF ) ) {
			return thisClass;
		}

		// Where was this function origionally defined
		Class<?> enclosingClass = udf.getClass().getEnclosingClass();

		// Start at the bottom class
		thisClass = thisClass.getBottomClass();

		// If the enclosing class is the same as the current class, then we're good
		if ( enclosingClass == thisClass.getClass() ) {
			return thisClass;
		}

		// Otherwise, let's climb the supers (if they even exist) and see if one of them declared it
		IClassRunnable thisSuper = thisClass.getSuper();
		while ( thisSuper != null ) {
			if ( enclosingClass == thisSuper.getClass() ) {
				return thisSuper;
			}
			thisSuper = thisSuper.getSuper();
		}
		// If the original class and no supers were the enclosing class, then this is prolly a mixin. Just return the original value.
		return thisClass;
	}

}
