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

import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.LocalScope;
import ortus.boxlang.runtime.types.Closure;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ScopeNotFoundException;

/**
 * This context represents the execution of a closure. Closures are a simpler form of a Function which,
 * unlike UDFs, do not track things like return type, output, etc. Closures also retain a reference to
 * context in which they were created, which allows for lexical scoping.
 */
public class ClosureBoxContext extends FunctionBoxContext {

	/**
	 * Creates a new execution context with a bounded function instance and parent context
	 *
	 * @param parent   The parent context
	 * @param function The Closure being invoked with this context
	 */
	public ClosureBoxContext( IBoxContext parent, Closure function ) {
		this( parent, function, new ArgumentsScope() );
	}

	/**
	 * Creates a new execution context with a bounded function instance and parent context and arguments scope
	 *
	 * @param parent         The parent context
	 * @param function       The Closure being invoked with this context
	 * @param argumentsScope The arguments scope for this context
	 */
	public ClosureBoxContext( IBoxContext parent, Closure function, ArgumentsScope argumentsScope ) {
		this( parent, function, function.getName(), argumentsScope );
	}

	/**
	 * Creates a new execution context with a bounded function instance and parent context and arguments scope
	 *
	 * @param parent             The parent context
	 * @param function           The Closure being invoked with this context
	 * @param functionCalledName The name of the function being invoked
	 * @param argumentsScope     The arguments scope for this context
	 */
	public ClosureBoxContext( IBoxContext parent, Closure function, Key functionCalledName, ArgumentsScope argumentsScope ) {
		super( parent, function, functionCalledName, argumentsScope );
		if ( parent == null ) {
			throw new BoxRuntimeException( "Parent context cannot be null for ClosureBoxContext" );
		}
	}

	/**
	 * Creates a new execution context with a bounded function instance and parent context and arguments scope
	 *
	 * @param parent              The parent context
	 * @param function            The Closure being invoked with this context
	 * @param functionCalledName  The name of the function being invoked
	 * @param positionalArguments The arguments scope for this context
	 */
	public ClosureBoxContext( IBoxContext parent, Closure function, Key functionCalledName, Object[] positionalArguments ) {
		super( parent, function, functionCalledName, positionalArguments, ( IClassRunnable ) null );
		if ( parent == null ) {
			throw new BoxRuntimeException( "Parent context cannot be null for ClosureBoxContext" );
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
	public ClosureBoxContext( IBoxContext parent, Closure function, Key functionCalledName, Map<Key, Object> namedArguments ) {
		super( parent, function, functionCalledName, namedArguments, null );
		if ( parent == null ) {
			throw new BoxRuntimeException( "Parent context cannot be null for ClosureBoxContext" );
		}
	}

	public IStruct getVisibleScopes( IStruct scopes, boolean nearby, boolean shallow ) {
		if ( hasParent() && !shallow ) {
			getParent().getVisibleScopes( scopes, false, false );
		}
		if ( nearby ) {
			scopes.getAsStruct( Key.contextual ).put( ArgumentsScope.name, argumentsScope );
			scopes.getAsStruct( Key.contextual ).put( LocalScope.name, localScope );
		}
		IStruct lexicalScopes = getFunction().getDeclaringContext().getVisibleScopes(
		    Struct.linkedOf(
		        Key.contextual,
		        Struct.linkedOf(),
		        Key.lexical,
		        Struct.linkedOf()
		    ),
		    true,
		    true
		);
		// If we're in more than one closure call with the same name, this will overwrite. We can do something to force it dynamic like adding a counter to
		// the name
		scopes.getAsStruct( Key.lexical ).putAll( lexicalScopes.getAsStruct( Key.lexical ) );
		scopes.getAsStruct( Key.lexical ).put( findClosestFunctionName(), lexicalScopes.getAsStruct( Key.contextual ) );
		return scopes;
	}

	/**
	 * Try to get the requested key from an unkonwn scope but not delegating to parent or default missing keys
	 *
	 * @param key          The key to search for
	 * @param defaultScope The default scope to return if the key is not found
	 * @param shallow      true, do not delegate to parent or default scope if not found
	 *
	 * @return The result of the search. Null if performing a shallow search and nothing was fond
	 *
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
		if ( isDefined( result ) ) {
			// Unwrap the value now in case it was really actually null for real
			return new ScopeSearchResult( localScope, Struct.unWrapNull( result ), key );
		}

		result = argumentsScope.getRaw( key );
		// Null means not found
		if ( isDefined( result ) ) {
			// Unwrap the value now in case it was really actually null for real
			return new ScopeSearchResult( argumentsScope, Struct.unWrapNull( result ), key );
		}

		// In query loop?
		var querySearch = queryFindNearby( key );
		if ( querySearch != null ) {
			return querySearch;
		}

		// After a closure has checked local and arguments, it stops to do a shallow lookup in the declaring scope. If the declaring scope
		// is also a CLosureBoxContext, it will do the same thing, and so on until it finds a non-ClosureBoxContext.
		ScopeSearchResult declaringContextResult = getFunction().getDeclaringContext().scopeFindNearby( key, defaultScope, true );
		if ( declaringContextResult != null ) {
			return declaringContextResult;
		}

		// Shallow lookups don't defer to the parent
		if ( shallow ) {
			return null;
		}

		// Now we pick up where we left off at the original closure context's parent.
		// Closures don't care about the "nearby" scopes of their parent execution contexts! We only climb the parent chain to find global scopes like CGI or
		// server
		return parent.scopeFind( key, defaultScope );

	}

	/**
	 * Look for a "nearby" scope by name
	 *
	 * @param name The name of the scope to look for
	 *
	 * @return The scope reference to use
	 */
	@Override
	public IScope getScopeNearby( Key name, boolean shallow ) throws ScopeNotFoundException {
		// Check the scopes I know about
		if ( name.equals( LocalScope.name ) ) {
			return localScope;
		}
		if ( name.equals( ArgumentsScope.name ) ) {
			return argumentsScope;
		}

		// After a closure has checked local and arguments, it stops to do a shallow lookup in the declaring scope. If the declaring scope
		// is also a CLosureBoxContext, it will do the same thing, and so on until it finds a non-ClosureBoxContext.
		IScope declaringContextResult = getFunction().getDeclaringContext().getScopeNearby( name, true );
		if ( declaringContextResult != null ) {
			return declaringContextResult;
		}

		if ( shallow ) {
			return null;
		}

		// The FunctionBoxContext has no "global" scopes, so just defer to parent
		return parent.getScope( name );

	}

	/**
	 * Returns the function being invoked with this context, cast as a Closure
	 */
	@Override
	public Closure getFunction() {
		return ( Closure ) function;
	}

	/**
	 * Invoke a function expression such as (()=>{})() using named args.
	 *
	 * @return Return value of the function call
	 */
	public Object invokeFunction( Function function, Key calledName, Object[] positionalArguments ) {
		FunctionBoxContext functionContext = Function.generateFunctionContext( function, getFunctionParentContext(), calledName, positionalArguments,
		    ( getFunction().getDeclaringContext() instanceof FunctionBoxContext fbc && fbc.isInClass() ) ? fbc.getThisClass() : null, getFunctionInterface() );
		return function.invoke( functionContext );
	}

	/**
	 * Invoke a function expression such as (()=>{})() using named args.
	 *
	 * @return Return value of the function call
	 */
	public Object invokeFunction( Function function, Key calledName, Map<Key, Object> namedArguments ) {
		FunctionBoxContext functionContext = Function.generateFunctionContext( function, getFunctionParentContext(), calledName, namedArguments,
		    ( getFunction().getDeclaringContext() instanceof FunctionBoxContext fbc && fbc.isInClass() ) ? fbc.getThisClass() : null, getFunctionInterface() );
		return function.invoke( functionContext );
	}

	/**
	 * Detects of this Function is executing in the context of a class
	 * 
	 * @return true if there is an IClassRunnable at the top of the template stack
	 */
	public boolean isInClass() {
		return getFunction().getDeclaringContext() instanceof FunctionBoxContext fbc && fbc.isInClass();
	}

	/**
	 * Detects of this Function is executing in the context of a class
	 * 
	 * @return true if there is an IClassRunnable at the top of the template stack
	 */
	public IClassRunnable getThisClass() {
		if ( getFunction().getDeclaringContext() instanceof FunctionBoxContext fbc ) {
			return fbc.getThisClass();
		}
		return null;
	}

}
