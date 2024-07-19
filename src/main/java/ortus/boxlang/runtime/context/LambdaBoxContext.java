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
package ortus.boxlang.runtime.context;

import java.util.Map;

import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.LocalScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Lambda;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;
import ortus.boxlang.runtime.types.exceptions.ScopeNotFoundException;

/**
 * This context represents the execution of a Lambda. Lambdas are a simpler form of a Function which,
 * unlike UDFs, do not track things like return type, output, etc. Lambdas retain NO reference to the
 * context in which they were created, and do not search scopes outside their local and arguments scope.
 */
public class LambdaBoxContext extends FunctionBoxContext {

	/**
	 * Creates a new execution context with a bounded function instance and parent context
	 *
	 * @param parent   The parent context
	 * @param function The Lambda being invoked with this context
	 */
	public LambdaBoxContext( IBoxContext parent, Lambda function ) {
		this( parent, function, new ArgumentsScope() );
	}

	/**
	 * Creates a new execution context with a bounded function instance and parent context and arguments scope
	 *
	 * @param parent         The parent context
	 * @param function       The Lambda being invoked with this context
	 * @param argumentsScope The arguments scope for this context
	 */
	public LambdaBoxContext( IBoxContext parent, Lambda function, ArgumentsScope argumentsScope ) {
		this( parent, function, function.getName(), argumentsScope );
	}

	/**
	 * Creates a new execution context with a bounded function instance and parent context and arguments scope
	 *
	 * @param parent              The parent context
	 * @param function            The Closure being invoked with this context
	 * @param functionCalledName  The name of the function being invoked
	 * @param positionalArguments The arguments scope for this context
	 */
	public LambdaBoxContext( IBoxContext parent, Lambda function, Key functionCalledName, Object[] positionalArguments ) {
		super( parent, function, functionCalledName, positionalArguments, null );
		if ( parent == null ) {
			throw new BoxRuntimeException( "Parent context cannot be null for LambdaBoxContext" );
		}
	}

	/**
	 * Creates a new execution context with a bounded function instance and parent context and arguments scope
	 *
	 * @param parent             The parent context
	 * @param function           The Closure being invoked with this context
	 * @param functionCalledName The name of the function being invoked
	 * @param namedArguments     The arguments scope for this context
	 */
	public LambdaBoxContext( IBoxContext parent, Lambda function, Key functionCalledName, Map<Key, Object> namedArguments ) {
		super( parent, function, functionCalledName, namedArguments, null );
		if ( parent == null ) {
			throw new BoxRuntimeException( "Parent context cannot be null for LambdaBoxContext" );
		}
	}

	/**
	 * Creates a new execution context with a bounded function instance and parent context and arguments scope
	 *
	 * @param parent             The parent context
	 * @param function           The Closure being invoked with this context
	 * @param functionCalledName The name of the function being invoked
	 * @param argumentsScope     The arguments scope for this context
	 */
	public LambdaBoxContext( IBoxContext parent, Lambda function, Key functionCalledName, ArgumentsScope argumentsScope ) {
		super( parent, function, functionCalledName, argumentsScope );
		if ( parent == null ) {
			throw new BoxRuntimeException( "Parent context cannot be null for LambdaBoxContext" );
		}
	}

	public IStruct getVisibleScopes( IStruct scopes, boolean nearby, boolean shallow ) {
		if ( !nearby ) {
			getParent().getVisibleScopes( scopes, false, false );
		}
		if ( nearby ) {
			scopes.getAsStruct( Key.contextual ).put( ArgumentsScope.name, argumentsScope );
			scopes.getAsStruct( Key.contextual ).put( LocalScope.name, localScope );
		}
		return scopes;
	}

	/**
	 * Search for a variable in "nearby" scopes
	 */
	public ScopeSearchResult scopeFindNearby( Key key, IScope defaultScope, boolean shallow ) {

		if ( key.equals( localScope.getName() ) ) {
			return new ScopeSearchResult( localScope, localScope, key, true );
		}

		if ( key.equals( argumentsScope.getName() ) ) {
			return new ScopeSearchResult( argumentsScope, argumentsScope, key, true );
		}

		Object result = localScope.getRaw( key );
		// Null means not found
		if ( result != null ) {
			// Unwrap the value now in case it was really actually null for real
			return new ScopeSearchResult( localScope, Struct.unWrapNull( result ), key );
		}

		result = argumentsScope.getRaw( key );
		// Null means not found
		if ( result != null ) {
			// Unwrap the value now in case it was really actually null for real
			return new ScopeSearchResult( argumentsScope, Struct.unWrapNull( result ), key );
		}

		// In query loop?
		var querySearch = queryFindNearby( key );
		if ( querySearch != null ) {
			return querySearch;
		}

		if ( shallow ) {
			return null;
		}

		// Lambdas don't look anywhere else!
		throw new KeyNotFoundException(
		    String.format( "The requested key [%s] was not located in any scope or it's undefined", key.getName() )
		);

	}

	/**
	 * Look for a scope by name
	 */
	public IScope getScope( Key name ) throws ScopeNotFoundException {
		// Lambda's are a "dead end". They do not "see" anything outside of their local and arguments scopes
		throw new ScopeNotFoundException(
		    String.format( "The requested scope name [%s] was not located in any context", name.getName() )
		);
	}

	/**
	 * Look for a "nearby" scope by name
	 */
	public IScope getScopeNearby( Key name, boolean shallow ) throws ScopeNotFoundException {
		// Check the scopes I know about
		if ( name.equals( localScope.getName() ) ) {
			return localScope;
		}
		if ( name.equals( argumentsScope.getName() ) ) {
			return argumentsScope;
		}

		if ( shallow ) {
			return null;
		}

		// Lambdas don't look anywhere else!
		throw new ScopeNotFoundException(
		    String.format( "The requested scope name [%s] was not located in any context", name.getName() )
		);

	}

	@Override
	public ScopeSearchResult scopeFind( Key key, IScope defaultScope ) {
		return parent.scopeFind( key, defaultScope );
	}

	/**
	 * Returns the function being invoked with this context, cast as a Lambda
	 */
	@Override
	public Lambda getFunction() {
		return ( Lambda ) function;
	}

	/**
	 * Get the default variable assignment scope for this context
	 *
	 * @return The scope reference to use
	 */
	@Override
	public IScope getDefaultAssignmentScope() {
		return localScope;
	}

}
