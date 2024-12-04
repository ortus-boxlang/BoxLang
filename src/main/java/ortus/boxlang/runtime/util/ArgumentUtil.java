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
package ortus.boxlang.runtime.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.GenericCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.IntKey;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.NullValue;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Represents an argument to a function or BIF
 */
public class ArgumentUtil {

	/**
	 * Create an arguments scope from the positional arguments
	 *
	 * @param context             The context of the execution
	 * @param positionalArguments The positional arguments
	 * @param arguments           The declared arguments
	 * @param scope               The scope to add the arguments to
	 * @param functionName        The name of the function
	 *
	 * @return The arguments scope
	 */
	public static ArgumentsScope createArgumentsScope( IBoxContext context, Object[] positionalArguments, Argument[] arguments, ArgumentsScope scope,
	    Key functionName ) {
		scope.setPositional( true );
		// Add all incoming args to the scope, using the name if declared, otherwise using the position
		for ( int i = 0; i < positionalArguments.length; i++ ) {
			Key		name;
			Object	value	= positionalArguments[ i ];
			if ( arguments.length - 1 >= i ) {
				name	= arguments[ i ].name();
				value	= ensureArgumentType( context, name, value, arguments[ i ].type(), functionName );
			} else {
				name = Key.of( i + 1 );
			}
			if ( value == null && arguments.length - 1 >= i && arguments[ i ].hasDefaultValue() ) {
				value = ensureArgumentType( context, name, arguments[ i ].getDefaultValue( context ), arguments[ i ].type(), functionName );
			}
			scope.put( name, value );
		}

		// Fill in any remaining declared arguments with default value
		if ( arguments.length > scope.size() ) {
			for ( int i = scope.size(); i < arguments.length; i++ ) {
				if ( arguments[ i ].required() && !arguments[ i ].hasDefaultValue() ) {
					throw new BoxRuntimeException(
					    "Required argument [" + arguments[ i ].name().getName() + "] is missing for function [" + functionName.getName() + "]" );
				}
				scope.put( arguments[ i ].name(),
				    ensureArgumentType( context, arguments[ i ].name(), arguments[ i ].getDefaultValue( context ), arguments[ i ].type(), functionName )
				);
			}
		}
		return scope;
	}

	/**
	 * Create an arguments scope from the named arguments
	 *
	 * @param context        The context of the execution
	 * @param namedArguments The named arguments
	 * @param arguments      The declared arguments
	 * @param scope          The scope to add the arguments to
	 * @param functionName   The name of the function
	 *
	 * @return The arguments scope
	 */
	@SuppressWarnings( "unchecked" )
	public static ArgumentsScope createArgumentsScope( IBoxContext context, Map<Key, Object> namedArguments, Argument[] arguments, ArgumentsScope scope,
	    Key functionName ) {

		// If argumentCollection exists, add it
		if ( namedArguments.containsKey( Function.ARGUMENT_COLLECTION ) ) {
			// Create a clone of our named args so we don't modify upstream structs by reference
			Map<Key, Object> copyofNamedArguments = new LinkedHashMap<>();
			copyofNamedArguments.putAll( namedArguments );
			namedArguments = copyofNamedArguments;

			Object			argCollection	= namedArguments.get( Function.ARGUMENT_COLLECTION );
			List<Object>	listCollection	= null;
			if ( argCollection instanceof ArgumentsScope as ) {
				Map<Key, Object> copyofArgCol = new LinkedHashMap<>();
				copyofArgCol.putAll( as );

				// For all declared args, grab and add them first so they are in the order of the declared arguments
				int i = 0;
				for ( Argument argument : arguments ) {
					i++;
					IntKey intKey = Key.of( i );
					// If there is a top level key that matches the declared argument name, use it
					if ( namedArguments.containsKey( argument.name() ) ) {
						copyofArgCol.remove( argument.name() );
						// Otherwise, if there is a key in the argument collection that matches the declared argument name, use it
					} else if ( copyofArgCol.containsKey( argument.name() ) ) {
						namedArguments.put( argument.name(), copyofArgCol.get( argument.name() ) );
						copyofArgCol.remove( argument.name() );
						// Otherwise, if there is a key in the argument collection that matches the declared argument position, use it
					} else if ( copyofArgCol.containsKey( intKey ) ) {
						namedArguments.put( argument.name(), copyofArgCol.get( intKey ) );
						copyofArgCol.remove( intKey );
						// Otherwise, add a null
					} else {
						namedArguments.put( argument.name(), null );
					}
				}

				// Add remaining argument collection items in the order they appeared
				namedArguments.putAll( copyofArgCol );

				namedArguments.remove( Function.ARGUMENT_COLLECTION );
			} else if ( argCollection instanceof Map<?, ?> ) {
				Map<Key, Object> argumentCollection = ( Map<Key, Object> ) argCollection;
				scope.putAll( argumentCollection );
				namedArguments.remove( Function.ARGUMENT_COLLECTION );
			} else if ( argCollection instanceof List<?> ) {
				listCollection = ( List<Object> ) argCollection;
				for ( int i = 0; i < listCollection.size(); i++ ) {
					Key		name;
					Object	value	= listCollection.get( i );
					if ( arguments.length - 1 >= i ) {
						name = arguments[ i ].name();
					} else {
						name = Key.of( i + 1 );
					}
					scope.put( name, value );
				}
				namedArguments.remove( Function.ARGUMENT_COLLECTION );
			}

			// Lucee leaves non struct, non array argumentCollectionkeys as-is. Adobe removes them. We'll copy Lucee here, though it's an edge case.
		}

		// Put all remaining incoming args
		scope.putAll( namedArguments );
		// For all declared args
		for ( Argument argument : arguments ) {
			// If they aren't here, add their default value (if defined)
			if ( !scope.containsKey( argument.name() ) || scope.get( argument.name() ) == null ) {
				if ( argument.required() && !argument.hasDefaultValue() ) {
					throw new BoxRuntimeException( "Required argument " + argument.name().getName() + " is missing for function " + functionName.getName() );
				}
				// Make sure the default value is valid
				scope.put( argument.name(),
				    ensureArgumentType( context, argument.name(), argument.getDefaultValue( context ), argument.type(), functionName ) );
				// If they are here, confirm their types
			} else {
				scope.put( argument.name(),
				    ensureArgumentType( context, argument.name(), scope.get( argument.name() ), argument.type(), functionName ) );
			}
		}
		return scope;
	}

	/**
	 * Create a generic arguments scope from the positional arguments
	 *
	 * @param context             The context of the execution
	 * @param positionalArguments The positional arguments
	 *
	 * @return The arguments scope
	 */
	public static ArgumentsScope createArgumentsScope( IBoxContext context, Object[] positionalArguments ) {
		ArgumentsScope scope = new ArgumentsScope().setPositional( true );
		for ( int i = 0; i < positionalArguments.length; i++ ) {
			scope.put( Key.of( i + 1 ), positionalArguments[ i ] );
		}
		return scope;
	}

	/**
	 * Create a generic arguments scope from the named arguments
	 *
	 * @param context        The context of the execution
	 * @param namedArguments The named arguments
	 *
	 * @return The arguments scope
	 */
	public static ArgumentsScope createArgumentsScope( IBoxContext context, Map<Key, Object> namedArguments ) {
		ArgumentsScope scope = new ArgumentsScope();
		// handle argumentCollection
		if ( namedArguments.get( Key.argumentCollection ) instanceof IStruct argCol ) {
			scope.putAll( argCol );
			namedArguments.remove( Key.argumentCollection );
		} else if ( namedArguments.get( Key.argumentCollection ) instanceof Array argArray ) {
			for ( int i = 0; i < argArray.size(); i++ ) {
				scope.put( Key.of( i + 1 ), argArray.get( i ) );
			}
			namedArguments.remove( Key.argumentCollection );
		}
		scope.putAll( namedArguments );
		return scope;
	}

	/**
	 * Create an arguments scope from no arguments
	 *
	 * @param context      The context of the execution
	 * @param arguments    The declared arguments
	 * @param scope        The scope to add the arguments to
	 * @param functionName The name of the function
	 *
	 * @return The arguments scope
	 */
	public static ArgumentsScope createArgumentsScope( IBoxContext context, Argument[] arguments, ArgumentsScope scope, Key functionName ) {
		return createArgumentsScope( context, new Object[] {}, arguments, scope, functionName );
	}

	/**
	 * Ensure the argument is the correct type
	 *
	 * @param context      The context of the execution
	 * @param name         The name of the argument
	 * @param value        The value of the argument
	 * @param type         The type of the argument
	 * @param functionName The name of the function
	 *
	 * @return The value of the argument
	 *
	 */
	public static Object ensureArgumentType( IBoxContext context, Key name, Object value, String type, Key functionName ) {
		if ( value == null ) {
			return null;
		}
		CastAttempt<Object> typeCheck = GenericCaster.attempt( context, value, type, true );
		if ( !typeCheck.wasSuccessful() ) {
			throw new BoxRuntimeException(
			    String.format( "In function [%s], argument [%s] with a type of [%s] does not match the declared type of [%s]",
			        functionName.getName(), name.getName(), DynamicObject.unWrap( value ).getClass().getName(),
			        type )
			);
		}
		// Should we actually return the casted value??? Not CFML Compat! If so, return typeCheck.get() with check for NullValue instances.
		Object result = typeCheck.get();
		if ( result instanceof NullValue ) {
			return null;
		}
		return result;
	}

	/**
	 * Convert positional arguments to a map of key/value pairs
	 *
	 * @param args The positional arguments
	 *
	 * @return The map of key/value pairs
	 */
	public static Map<Key, Object> positionalToMap( Object... args ) {
		Map<Key, Object> map = new LinkedHashMap<>();
		for ( int i = 0; i < args.length; i++ ) {
			map.put( Key.of( i + 1 ), args[ i ] );
		}
		return map;
	}

	/**
	 * Map the arguments to the declared arguments by position
	 *
	 * @param args              The arguments to map. Each key is the position of the argument
	 * @param declaredArguments The declared arguments
	 *
	 * @return The mapped arguments, if any.
	 */
	public static Map<Key, Object> mapArgumentsToDeclaredArguments( Map<Key, Object> args, Argument[] declaredArguments ) {
		// Iterate over the declared arguments and change the key of the args in that position to the declared argument name
		for ( int i = 0; i < declaredArguments.length; i++ ) {
			Argument declaredArgument = declaredArguments[ i ];
			if ( args.containsKey( Key.of( i + 1 ) ) ) {
				args.put( declaredArgument.name(), args.get( Key.of( i + 1 ) ) );
				args.remove( Key.of( i + 1 ) );
			}
		}

		return args;
	}

}
