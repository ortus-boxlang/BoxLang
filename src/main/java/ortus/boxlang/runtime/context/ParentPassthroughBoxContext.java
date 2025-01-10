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
import ortus.boxlang.runtime.types.IStruct;

/**
 * This context provides a base class for contexts which add some functionality, but mostly pass through all of their methods
 * to their parent context.
 */
public abstract class ParentPassthroughBoxContext extends BaseBoxContext {

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Creates a new execution context with a bounded execution template and parent context
	 *
	 * @param parent The parent context
	 */
	public ParentPassthroughBoxContext( IBoxContext parent ) {
		super( parent );
	}

	/**
	 * All these methods just delegate to the parent context so the config override context is mostly invisible.
	 */

	public IStruct getVisibleScopes( IStruct scopes, boolean nearby, boolean shallow ) {
		return getParent().getVisibleScopes( scopes, nearby, shallow );
	}

	public ScopeSearchResult scopeFindNearby( Key key, IScope defaultScope, boolean shallow, boolean forAssign ) {
		return getParent().scopeFindNearby( key, defaultScope, shallow, forAssign );
	}

	public ScopeSearchResult scopeFind( Key key, IScope defaultScope, boolean forAssign ) {
		return getParent().scopeFind( key, defaultScope, forAssign );
	}

	public IScope getScope( Key name ) {
		return getParent().getScope( name );
	}

	public IScope getScopeNearby( Key name, boolean shallow ) {
		return getParent().getScopeNearby( name, shallow );
	}

	public IScope getDefaultAssignmentScope() {
		return getParent().getDefaultAssignmentScope();
	}

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
