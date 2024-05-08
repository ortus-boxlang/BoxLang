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

import java.util.List;
import java.util.Map;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.GenericCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.NullValue;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Represents an argument to a function or BIF
 */
public class ArgumentUtil {

	/**
	 * Create an arguments scope from the positional arguments
	 *
	 * @param positionalArguments The positional arguments
	 * @param arguments           The declared arguments
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
				value = arguments[ i ].getDefaultValue( context );
			}
			scope.put( name, value );
		}

		// Fill in any remaining declared arguments with default value
		if ( arguments.length > scope.size() ) {
			for ( int i = scope.size(); i < arguments.length; i++ ) {
				if ( arguments[ i ].required() && !arguments[ i ].hasDefaultValue() ) {
					throw new BoxRuntimeException( "Required argument " + arguments[ i ].name().getName() + " is missing" );
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
	 * @param namedArguments The named arguments
	 * @param arguments      The declared arguments
	 *
	 * @return The arguments scope
	 */
	public static ArgumentsScope createArgumentsScope( IBoxContext context, Map<Key, Object> namedArguments, Argument[] arguments, ArgumentsScope scope,
	    Key functionName ) {
		// If argumentCollection exists, add it
		if ( namedArguments.containsKey( Function.ARGUMENT_COLLECTION ) ) {
			Object argCollection = namedArguments.get( Function.ARGUMENT_COLLECTION );
			if ( argCollection instanceof Map<?, ?> ) {
				@SuppressWarnings( "unchecked" )
				Map<Key, Object> argumentCollection = ( Map<Key, Object> ) argCollection;
				scope.putAll( argumentCollection );
				namedArguments.remove( Function.ARGUMENT_COLLECTION );
			}
			if ( argCollection instanceof List<?> ) {
				@SuppressWarnings( "unchecked" )
				List<Object> argumentCollection = ( List<Object> ) argCollection;

				for ( int i = 0; i < argumentCollection.size(); i++ ) {
					Key		name;
					Object	value	= argumentCollection.get( i );
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
					throw new BoxRuntimeException( "Required argument " + argument.name().getName() + " is missing" );
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
	 * @param namedArguments The named arguments
	 *
	 * @return The arguments scope
	 */
	public static ArgumentsScope createArgumentsScope( IBoxContext context, Map<Key, Object> namedArguments ) {
		ArgumentsScope scope = new ArgumentsScope();
		scope.putAll( namedArguments );
		return scope;
	}

	/**
	 * Create an arguments scope from no arguments
	 *
	 * @param arguments The declared arguments
	 *
	 * @return The arguments scope
	 */
	public static ArgumentsScope createArgumentsScope( IBoxContext context, Argument[] arguments, ArgumentsScope scope, Key functionName ) {
		return createArgumentsScope( context, new Object[] {}, arguments, scope, functionName );
	}

	/**
	 * Ensure the argument is the correct type
	 *
	 * @param name  The name of the argument
	 * @param value The value of the argument
	 * @param type  The type of the argument
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

}
