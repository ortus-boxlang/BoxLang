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

import ortus.boxlang.runtime.runnables.IBoxRunnable;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.util.ResolvedFilePath;

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
	@Override
	public IStruct getVisibleScopes( IStruct scopes, boolean nearby, boolean shallow ) {
		return getParent().getVisibleScopes( scopes, nearby, shallow );
	}

	@Override
	public boolean isKeyVisibleScope( Key key, boolean nearby, boolean shallow ) {
		return getParent().isKeyVisibleScope( key, nearby, shallow );
	}

	@Override
	public ScopeSearchResult scopeFindNearby( Key key, IScope defaultScope, boolean shallow, boolean forAssign ) {
		return getParent().scopeFindNearby( key, defaultScope, shallow, forAssign );
	}

	@Override
	public ScopeSearchResult scopeFind( Key key, IScope defaultScope, boolean forAssign ) {
		return getParent().scopeFind( key, defaultScope, forAssign );
	}

	@Override
	public IScope getScope( Key name ) {
		return getParent().getScope( name );
	}

	@Override
	public IScope getScopeNearby( Key name, boolean shallow ) {
		return getParent().getScopeNearby( name, shallow );
	}

	@Override
	public IScope getDefaultAssignmentScope() {
		return getParent().getDefaultAssignmentScope();
	}

	@Override
	public IBoxContext writeToBuffer( Object o ) {
		if ( o == null ) {
			return this;
		}
		getParent().writeToBuffer( o );
		return this;
	}

	@Override
	public IBoxContext writeToBuffer( Object o, boolean force ) {
		if ( o == null ) {
			return this;
		}
		getParent().writeToBuffer( o, force );
		return this;
	}

	@Override
	public Boolean canOutput() {
		return getParent().canOutput();
	}

	@Override
	public IBoxContext flushBuffer( boolean force ) {
		getParent().flushBuffer( force );
		return this;
	}

	@Override
	public IBoxContext clearBuffer() {
		getParent().clearBuffer();
		return this;
	}

	@Override
	public StringBuffer getBuffer() {
		return getParent().getBuffer();
	}

	@Override
	public IBoxContext pushBuffer( StringBuffer buffer ) {
		getParent().pushBuffer( buffer );
		return this;
	}

	@Override
	public IBoxContext popBuffer() {
		getParent().popBuffer();
		return this;
	}

	@Override
	public Object invokeFunction( Key name, Object[] positionalArguments ) {
		return getParent().invokeFunction( name, positionalArguments );
	}

	@Override
	public Object invokeFunction( Key name, Map<Key, Object> namedArguments ) {
		return getParent().invokeFunction( name, namedArguments );
	}

	@Override
	public Object invokeFunction( Key name ) {
		return getParent().invokeFunction( name );
	}

	@Override
	public Object invokeFunction( Object function, Object[] positionalArguments ) {
		return getParent().invokeFunction( function, positionalArguments );
	}

	@Override
	public Object invokeFunction( Object function, Map<Key, Object> namedArguments ) {
		return getParent().invokeFunction( function, namedArguments );
	}

	@Override
	public Object invokeFunction( Object function ) {
		return getParent().invokeFunction( function );
	}

	@Override
	public IBoxContext pushComponent( IStruct executionState ) {
		return getParent().pushComponent( executionState );
	}

	@Override
	public IBoxContext popComponent() {
		return getParent().popComponent();
	}

	@Override
	public IStruct[] getComponents() {
		return getParent().getComponents();
	}

	@Override
	public IStruct findClosestComponent( Key name ) {
		return getParent().findClosestComponent( name );
	}

	@Override
	public IStruct findClosestComponent( Key name, Predicate<IStruct> predicate ) {
		return getParent().findClosestComponent( name, predicate );
	}

	@Override
	public boolean hasTemplates() {
		return getParent().hasTemplates();
	}

	@Override
	public ResolvedFilePath findClosestTemplate() {
		return getParent().findClosestTemplate();
	}

	@Override
	public ResolvedFilePath findBaseTemplate() {
		return getParent().findBaseTemplate();
	}

	@Override
	public IBoxContext pushTemplate( IBoxRunnable template ) {
		return getParent().pushTemplate( template );
	}

	@Override
	public IBoxContext pushTemplate( ResolvedFilePath template ) {
		return getParent().pushTemplate( template );
	}

	@Override
	public ResolvedFilePath popTemplate() {
		return getParent().popTemplate();
	}

	@Override
	public ResolvedFilePath[] getTemplates() {
		return getParent().getTemplates();
	}

}
