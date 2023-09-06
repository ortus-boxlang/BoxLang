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
package ortus.boxlang.runtime.types;

import java.util.Map;

import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;

/**
 * A struct is a collection of key-value pairs, where the key is unique and case insensitive
 */
public abstract class Function implements IType {

	public enum Access {
		PRIVATE,
		PUBLIC,
		PROTECTED,
		REMOTE
	}

	public static Key	ARGUMENT_COLLECTION	= Key.of( "argumentCollection" );

	/**
	 * The arguments of the function
	 */
	private Argument[]	arguments;

	/**
	 * Constructor
	 */
	public Function( Argument[] arguments ) {
		this.arguments = arguments;
	}

	public Argument[] getArguments() {
		return arguments;
	}

	public abstract Object invoke( FunctionBoxContext context );

	public ArgumentsScope createArgumentsScope( Object[] positionalArguments ) {
		ArgumentsScope scope = new ArgumentsScope();
		// Add all incoming args to the scope, using the name if declared, otherwise using the position
		for ( int i = 0; i < positionalArguments.length; i++ ) {
			Key name;
			if ( arguments.length - 1 >= i ) {
				// TODO: Check types of declared args
				name = arguments[ i ].name();
			} else {
				name = Key.of( Integer.toString( i + 1 ) );
			}
			scope.put( name, positionalArguments[ i ] );
		}

		// Fill in any remaining declared arguments with default value
		if ( arguments.length > scope.size() ) {
			for ( int i = scope.size(); i < arguments.length; i++ ) {
				if ( arguments[ i ].required() && arguments[ i ].defaultValue() == null ) {
					throw new RuntimeException( "Required argument " + arguments[ i ].name() + " is missing" );
				}
				scope.put( arguments[ i ].name(), arguments[ i ].defaultValue() );
			}
		}
		return scope;
	}

	// TODO handle required args
	public ArgumentsScope createArgumentsScope( Map<Key, Object> namedArguments ) {
		ArgumentsScope scope = new ArgumentsScope();

		// If argumentCollection exists, add it
		if ( namedArguments.containsKey( ARGUMENT_COLLECTION ) && namedArguments.get( ARGUMENT_COLLECTION ) instanceof Map ) {
			// TODO: Check types of declared args
			scope.addAll( ( Map<Object, Object> ) namedArguments.get( ARGUMENT_COLLECTION ) );
			namedArguments.remove( ARGUMENT_COLLECTION );
		}

		// Put all remaining incoming args
		// TODO: Check types of declared args
		scope.putAll( namedArguments );

		for ( Argument argument : arguments ) {
			if ( !scope.containsKey( argument.name() ) ) {
				if ( argument.required() && argument.defaultValue() == null ) {
					throw new RuntimeException( "Required argument " + argument.name() + " is missing" );
				}
				scope.put( argument.name(), argument.defaultValue() );
			}
		}
		return scope;
	}

	public ArgumentsScope createArgumentsScope() {
		ArgumentsScope scope = new ArgumentsScope();
		for ( int i = 0; i < arguments.length; i++ ) {
			if ( arguments[ i ].required() && arguments[ i ].defaultValue() == null ) {
				throw new RuntimeException( "Required argument " + arguments[ i ].name() + " is missing" );
			}
			scope.put( arguments[ i ].name(), arguments[ i ].defaultValue() );
		}
		return scope;
	}

	public String asString() {
		return toString();
	}

	/**
	 * Represents an argument to a function
	 *
	 * @param required     Whether the argument is required
	 * @param type         The type of the argument
	 * @param name         The name of the argument
	 * @param defaultValue The default value of the argument
	 *
	 */
	public record Argument( boolean required, String type, Key name, Object defaultValue, String hint ) {
		// The record automatically generates the constructor, getters, equals, hashCode, and toString methods.
	}
}
