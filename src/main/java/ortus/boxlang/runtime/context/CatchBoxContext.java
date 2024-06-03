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
import java.util.function.Predicate;

import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.ScopeWrapper;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;
import ortus.boxlang.runtime.types.exceptions.ScopeNotFoundException;

/**
 * This context represents the context of a template execution in BoxLang
 */
public class CatchBoxContext extends BaseBoxContext {

	/**
	 * The variables scope
	 */
	private IScope		variablesScope;
	private Throwable	exception;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Creates a new execution context with a bounded execution template and parent context
	 *
	 * @param parent       The parent context
	 * @param exceptionKey The key to use for the exception
	 * @param exception    The exception to store
	 */
	public CatchBoxContext( IBoxContext parent, Key exceptionKey, Throwable exception ) {
		super( parent );
		if ( parent == null ) {
			throw new BoxRuntimeException( "Parent context cannot be null for CatchBoxContext" );
		}
		this.exception		= exception;
		this.variablesScope	= new ScopeWrapper(
		    parent.getScopeNearby( VariablesScope.name ),
		    Map.of( exceptionKey, exception )
		);
	}

	/**
	 * --------------------------------------------------------------------------
	 * Getters + Setters
	 * --------------------------------------------------------------------------
	 */

	public IStruct getVisibleScopes( IStruct scopes, boolean nearby, boolean shallow ) {
		if ( hasParent() && !shallow ) {
			getParent().getVisibleScopes( scopes, false, false );
		}
		if ( nearby ) {
			scopes.getAsStruct( Key.contextual ).put( VariablesScope.name, variablesScope );
		}
		return scopes;
	}

	/**
	 * Try to get the requested key from the unscoped scope
	 * Meaning it needs to search scopes in order according to it's context.
	 * A nearby lookup is used for the closest context to the executing code
	 *
	 * @param key          The key to search for
	 * @param defaultScope The default scope to use if the key is not found
	 * @param shallow      true, do not delegate to parent or default scope if not found
	 *
	 * @return The value of the key if found
	 *
	 */
	public ScopeSearchResult scopeFindNearby( Key key, IScope defaultScope, boolean shallow ) {

		// In query loop?
		var querySearch = queryFindNearby( key );
		if ( querySearch != null ) {
			return querySearch;
		}

		// In Variables scope? (thread-safe lookup and get)
		Object result = variablesScope.getRaw( key );
		// Null means not found
		if ( result != null ) {
			// Unwrap the value now in case it was really actually null for real
			return new ScopeSearchResult( variablesScope, Struct.unWrapNull( result ), key );
		}

		if ( shallow ) {
			return null;
		}

		if ( parent != null ) {
			return parent.scopeFindNearby( key, defaultScope );
		}

		return scopeFind( key, defaultScope );
	}

	/**
	 * Try to get the requested key from the unscoped scope
	 * Meaning it needs to search scopes in order according to it's context.
	 * Unlike scopeFindNearby(), this version only searches trancedent scopes like
	 * cgi or server which are never encapsulated like variables is inside a class.
	 *
	 * @param key          The key to search for
	 * @param defaultScope The default scope to use if the key is not found
	 *
	 * @return The value of the key if found
	 *
	 */
	public ScopeSearchResult scopeFind( Key key, IScope defaultScope ) {

		if ( parent != null ) {
			return parent.scopeFind( key, defaultScope );
		}

		// Default scope requested for missing keys
		if ( defaultScope != null ) {
			return new ScopeSearchResult( defaultScope, null, key );
		}

		// Not found anywhere
		throw new KeyNotFoundException(
		    String.format( "The requested key [%s] was not located in any scope or it's undefined", key.getName() )
		);
	}

	/**
	 * Get a scope from the context. If not found, the parent context is asked.
	 * Don't search for scopes which are nearby to an execution context
	 *
	 * @param name The name of the scope to get
	 *
	 * @return The requested scope
	 */
	public IScope getScope( Key name ) {

		if ( parent != null ) {
			return parent.getScopeNearby( name );
		}

		// Not found anywhere
		throw new ScopeNotFoundException(
		    String.format( "The requested scope name [%s] was not located in any context", name.getName() )
		);

	}

	/**
	 * Get a scope from the context. If not found, the parent context is asked.
	 * Search all konwn scopes
	 *
	 * @param name    The name of the scope to get
	 * @param shallow true, do not delegate to parent or default scope if not found
	 *
	 * @return The requested scope
	 */
	public IScope getScopeNearby( Key name, boolean shallow ) {
		// Check the scopes I know about
		if ( name.equals( VariablesScope.name ) ) {
			return variablesScope;
		}

		if ( shallow ) {
			return null;
		}

		return getScope( name );

	}

	/**
	 * Get the default variable assignment scope for this context
	 *
	 * @return The scope reference to use
	 */
	public IScope getDefaultAssignmentScope() {
		// parent is never null
		return getParent().getDefaultAssignmentScope();
	}

	/**
	 * rethrows the closest exception
	 */
	public void rethrow() {
		ExceptionUtil.throwException( exception );
	}

	/**
	 * Most of these methods just delegate to the parent context so the catch context is mostly invisible.
	 */

	public IBoxContext writeToBuffer( Object o ) {
		if ( o == null ) {
			return this;
		}
		getParent().writeToBuffer( o );
		return this;
	}

	public IBoxContext writeToBuffer( Object o, boolean force ) {
		if ( o == null ) {
			return this;
		}
		getParent().writeToBuffer( o, force );
		return this;
	}

	public Boolean canOutput() {
		return getParent().canOutput();
	}

	public IBoxContext flushBuffer( boolean force ) {
		getParent().flushBuffer( force );
		return this;
	}

	public IBoxContext clearBuffer() {
		getParent().clearBuffer();
		return this;
	}

	public StringBuffer getBuffer() {
		return getParent().getBuffer();
	}

	public IBoxContext pushBuffer( StringBuffer buffer ) {
		getParent().pushBuffer( buffer );
		return this;
	}

	public IBoxContext popBuffer() {
		getParent().popBuffer();
		return this;
	}

	public Object invokeFunction( Key name, Object[] positionalArguments ) {
		return getParent().invokeFunction( name, positionalArguments );
	}

	public Object invokeFunction( Key name, Map<Key, Object> namedArguments ) {
		return getParent().invokeFunction( name, namedArguments );
	}

	public Object invokeFunction( Key name ) {
		return getParent().invokeFunction( name );
	}

	public Object invokeFunction( Object function, Object[] positionalArguments ) {
		return getParent().invokeFunction( function, positionalArguments );
	}

	public Object invokeFunction( Object function, Map<Key, Object> namedArguments ) {
		return getParent().invokeFunction( function, namedArguments );
	}

	public Object invokeFunction( Object function ) {
		return getParent().invokeFunction( function );
	}

	public Component.BodyResult invokeComponent( Key name, IStruct attributes, Component.ComponentBody componentBody ) {
		return getParent().invokeComponent( name, attributes, componentBody );
	}

	public IBoxContext pushComponent( IStruct executionState ) {
		return getParent().pushComponent( executionState );
	}

	public IBoxContext popComponent() {
		return getParent().popComponent();
	}

	public IStruct[] getComponents() {
		return getParent().getComponents();
	}

	public IStruct findClosestComponent( Key name ) {
		return getParent().findClosestComponent( name );
	}

	public IStruct findClosestComponent( Key name, Predicate<IStruct> predicate ) {
		return getParent().findClosestComponent( name, predicate );
	}

}
