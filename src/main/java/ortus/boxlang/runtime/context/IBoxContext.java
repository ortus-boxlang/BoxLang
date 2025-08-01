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

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import ortus.boxlang.compiler.ast.statement.BoxMethodDeclarationModifier;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.cache.providers.ICacheProvider;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.modules.ModuleRecord;
import ortus.boxlang.runtime.runnables.BoxInterface;
import ortus.boxlang.runtime.runnables.IBoxRunnable;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.CacheService;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.UDF;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ScopeNotFoundException;
import ortus.boxlang.runtime.util.DataNavigator.Navigator;
import ortus.boxlang.runtime.util.IBoxAttachable;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * This represents the interface for all box contexts.
 */
public interface IBoxContext extends IBoxAttachable, Serializable {

	/**
	 * This is mostly for the debugger. It returns all visible scopes from this
	 * context.
	 *
	 * @return A struct containing all contextual and lexically visible scopes
	 *
	 */
	public IStruct getVisibleScopes( IStruct scopes, boolean nearby, boolean shallow );

	/**
	 * This is mostly for the debugger. It returns all visible scopes from this
	 * context.
	 *
	 * @return A struct containing all contextual and lexically visible scopes
	 *
	 */
	public IStruct getVisibleScopes();

	/**
	 * Check if a key is visible in the current context as a scope name.
	 * This allows us to "reserve" known scope names to ensure arguments.foo
	 * will always look in the proper arguments scope and never in
	 * local.arguments.foo for example
	 * 
	 * @param key The key to check for visibility
	 * 
	 * @return True if the key is visible in the current context, else false
	 */
	public boolean isKeyVisibleScope( Key key );

	/**
	 * Check if a key is visible in the current context as a scope name.
	 * This allows us to "reserve" known scope names to ensure arguments.foo
	 * will always look in the proper arguments scope and never in
	 * local.arguments.foo for example
	 * 
	 * @param key     The key to check for visibility
	 * @param nearby  true, check only scopes that are nearby to the current execution context
	 * @param shallow true, do not delegate to parent or default scope if not found
	 * 
	 * @return True if the key is visible in the current context, else false
	 */
	public boolean isKeyVisibleScope( Key key, boolean nearby, boolean shallow );

	/**
	 * Get a scope from the context. If not found, the parent context is asked.
	 * Don't search for scopes which are nearby to an execution context
	 *
	 * @param name The name of the scope to get
	 *
	 * @return The requested scope
	 *
	 * @throws ScopeNotFoundException If the scope was not found in any context
	 */
	public IScope getScope( Key name ) throws ScopeNotFoundException;

	/**
	 * Get a scope from the context. If not found, the parent context is asked.
	 * Search all known scopes
	 *
	 * @param name The name of the scope to get
	 *
	 * @return The requested scope
	 *
	 * @throws ScopeNotFoundException If the scope was not found in any context
	 */
	public IScope getScopeNearby( Key name ) throws ScopeNotFoundException;

	/**
	 * Get a scope from the context. If not found, the parent context is asked.
	 * Search all known scopes
	 *
	 * @param name    The name of the scope to get
	 * @param shallow true, do not delegate to parent or default scope if not found
	 *
	 * @return The requested scope
	 *
	 * @throws ScopeNotFoundException If the scope was not found in any context
	 */
	public IScope getScopeNearby( Key name, boolean shallow ) throws ScopeNotFoundException;

	/**
	 * Try to get the requested key from an unknown scope
	 * Meaning it needs to search scopes in order according to it's context.
	 * Unlike scopeFindNearby(), this version only searches trancedent scopes like
	 * cgi or server which are never encapsulated like variables is inside a class.
	 *
	 * If defaultScope is null and the key can't be found, a KeyNotFoundException
	 * will be thrown
	 * If defaultScope is not null, it will return a record with the default scope
	 * and null value if the key is not found
	 *
	 * @param key          The key to search for
	 * @param defaultScope The default scope to return if the key is not found
	 * @param forAssign    true, this is for an assignment operation
	 *
	 * @return The value of the key if found
	 *
	 */
	public ScopeSearchResult scopeFind( Key key, IScope defaultScope, boolean forAssign );

	/**
	 * Try to get the requested key from an unknown scope
	 * Meaning it needs to search scopes in order according to it's context.
	 * A nearby lookup is used for the closest context to the executing code
	 *
	 * If defaultScope is null and the key can't be found, a KeyNotFoundException
	 * will be thrown
	 * If defaultScope is not null, it will return a record with the default scope
	 * and null value if the key is not found
	 *
	 * @param key          The key to search for
	 * @param defaultScope The default scope to return if the key is not found
	 * @param forAssign    true, this is for an assignment operation
	 *
	 * @return The value of the key if found
	 *
	 */
	public ScopeSearchResult scopeFindNearby( Key key, IScope defaultScope, boolean forAssign );

	/**
	 * Try to get the requested key from an unkonwn scope but not delegating to
	 * parent or default missing keys
	 *
	 * @param key          The key to search for
	 * @param defaultScope The default scope to return if the key is not found
	 * @param shallow      true, do not delegate to parent or default scope if not
	 *                     found
	 * @param forAssign    true, this is for an assignment operation
	 *
	 * @return The result of the search. Null if performing a shallow search and
	 *         nothing was fond
	 *
	 */
	public ScopeSearchResult scopeFindNearby( Key key, IScope defaultScope, boolean shallow, boolean forAssign );

	/**
	 * Invoke a function call such as foo() using positional args. Will check for a
	 * registered BIF first, then search known scopes for a UDF.
	 *
	 * @return Return value of the function call
	 */
	public Object invokeFunction( Key name, Object[] positionalArguments );

	/**
	 * Invoke a function call such as foo() using named args. Will check for a
	 * registered BIF first, then search known scopes for a UDF.
	 *
	 * @return Return value of the function call
	 */
	public Object invokeFunction( Key name, Map<Key, Object> namedArguments );

	/**
	 * Invoke a function call such as foo() using no args.
	 *
	 * @return Return value of the function call
	 */
	public Object invokeFunction( Key name );

	/**
	 * Invoke a function expression such as (()=>{})() using positional args.
	 * This method will validate the incoming object is a function type.
	 *
	 * @return Return value of the function call
	 */
	public Object invokeFunction( Object function, Object[] positionalArguments );

	/**
	 * Invoke a function expression such as (()=>{})() using named args.
	 * This method will validate the incoming object is a function type.
	 *
	 * @return Return value of the function call
	 */
	public Object invokeFunction( Object function, Map<Key, Object> namedArguments );

	/**
	 * Invoke a function expression such as (()=>{})() using no args.
	 * This method will validate the incoming object is a function type.
	 *
	 * @return Return value of the function call
	 */
	public Object invokeFunction( Object function );

	/**
	 * Invoke a component call. If the optional that comes back has a value, it
	 * means the body of the compnent early-returned and did not finish.
	 * The calling code should check, and if the value is present, it should return
	 * that value
	 *
	 * @param name          The name of the component to invoke
	 * @param attributes    The attributes to pass to the component
	 * @param componentBody The body of the component
	 *
	 */
	public Component.BodyResult invokeComponent( Key name, IStruct attributes, Component.ComponentBody componentBody );

	/**
	 * Invoke a template in the current context
	 *
	 * @param templatePath A relateive template path
	 */
	public default void includeTemplate( String templatePath ) {
		includeTemplate( templatePath, false );
	}

	/**
	 * Invoke a template in the current context
	 *
	 * @param templatePath A relateive template path
	 */
	public void includeTemplate( String templatePath, boolean externalOnly );

	/**
	 * Register a UDF with the local context.
	 *
	 * @param udf The UDF to register
	 */
	public void registerUDF( UDF udf );

	/**
	 * Register a UDF with the local context choosing to override.
	 *
	 * @param udf      The UDF to register
	 * @param override true, override any existing UDF with the same name
	 */
	public void registerUDF( UDF udf, boolean override );

	/**
	 * Verifies if a parent context is attached to this context
	 *
	 * @return True if a parent context is attached to this context, else false
	 */
	public Boolean hasParent();

	/**
	 * Returns the parent box context. Null if none.
	 *
	 * @return The parent box context. Null if none.
	 */
	public IBoxContext getParent();

	/**
	 * Sets the parent box context.
	 *
	 * @param parentContext The parent context to set
	 *
	 * @return This context
	 */
	public IBoxContext setParent( IBoxContext parentContext );

	/**
	 * Inject a parent context, moving the current parent to the grandparent
	 * Any existing parent in the passed context will be overwritten with the
	 * current parent
	 *
	 * @param parentContext The parent context to inject
	 *
	 * @return This context
	 */
	public IBoxContext injectParentContext( IBoxContext parentContext );

	/**
	 * Inject a top parent context above the request-type context, moving the
	 * request context's current parent to its grandparent
	 *
	 * @param parentContext The parent context to inject
	 *
	 * @return This context
	 */
	public IBoxContext injectTopParentContext( IBoxContext parentContext );

	/**
	 * Remove ancestor contexts of this type
	 *
	 * @param type The type of context to remove
	 *
	 * @return This context
	 */
	public IBoxContext removeParentContext( Class<? extends IBoxContext> type );

	/**
	 * Finds the closest function call name
	 *
	 * @return The called name of the function if found, null if this code is not
	 *         called from a function
	 */
	public Key findClosestFunctionName();

	/**
	 * Push a template and imports to the stack
	 *
	 * @param template The template that this execution context is bound to
	 *
	 * @return IBoxContext
	 */
	public IBoxContext pushTemplate( IBoxRunnable template );

	/**
	 * Push a template to the stack
	 *
	 * @param template The template that this execution context is bound to
	 *
	 * @return IBoxContext
	 */
	public IBoxContext pushTemplate( ResolvedFilePath template );

	/**
	 * Pop a template from the stack
	 *
	 * @return The template that this execution context is bound to
	 */
	public ResolvedFilePath popTemplate();

	/**
	 * Push a Component to the stack
	 *
	 * @param executionState The state for this component execution
	 *
	 * @return This context
	 */
	public IBoxContext pushComponent( IStruct executionState );

	/**
	 * Pop a template from the stack
	 *
	 * @return This context
	 */
	public IBoxContext popComponent();

	/**
	 * Get the stack of components as an array
	 *
	 * @return This context
	 */
	public IStruct[] getComponents();

	/**
	 * Gets the execution state for the closest component.
	 *
	 * @return The execution state for the closest component, null if none was found
	 */
	public IStruct findClosestComponent( Key name );

	/**
	 * Gets the execution state for the closest component at an offset.
	 *
	 * @return The execution state for the closest component, null if none was found
	 */
	public IStruct findClosestComponent( Key name, int offset );

	/**
	 * Gets the execution state for the closest component with a predicate to
	 * filter.
	 *
	 * @return The execution state for the closest component, null if none was found
	 */
	public IStruct findClosestComponent( Key name, Predicate<IStruct> predicate );

	/**
	 * Gets the execution state for the closest component with a predicate to
	 * filter at an offset.
	 *
	 * @return The execution state for the closest component, null if none was found
	 */
	public IStruct findClosestComponent( Key name, int offset, Predicate<IStruct> predicate );

	/**
	 * Is there at least one output component on the stack
	 * 
	 * @return True if there is at least one output component, else false
	 */
	public boolean isInOutputComponent();

	/**
	 * Has the execution context been bound to a template?
	 *
	 * @return True if bound, else false
	 */
	public boolean hasTemplates();

	/**
	 * Get templates
	 *
	 * @return The templates
	 */
	public ResolvedFilePath[] getTemplates();

	/**
	 * Finds the closest template
	 *
	 * @return The template instance if found, null if this code is not called from
	 *         a template
	 */
	public ResolvedFilePath findClosestTemplate();

	/**
	 * Finds the base (first) template in this request
	 *
	 * @return The template instance if found, null if this code is not called from
	 *         a template
	 */
	public ResolvedFilePath findBaseTemplate();

	/**
	 * Get the default variable assignment scope for this context
	 *
	 * @return The scope reference to use
	 */
	public IScope getDefaultAssignmentScope();

	/**
	 * Get parent context for a function execution happening in this context
	 *
	 * @return The context to use
	 */
	public IBoxContext getFunctionParentContext();

	/**
	 * Get the class, if any, for a function invocation
	 *
	 * @return The class to use, or null if none
	 */
	public IClassRunnable getFunctionClass();

	/**
	 * Get the class, if any, for a function invocation
	 *
	 * @return The class to use, or null if none
	 */
	public DynamicObject getFunctionStaticClass();

	/**
	 * Get the interface, if any, for a function invocation
	 *
	 * @return The interface to use, or null if none
	 */
	public BoxInterface getFunctionInterface();

	/**
	 * Represents the results of a successful scope hunting expedition.
	 *
	 * @param scope The scope which was found
	 * @param value The value of the key in the scope
	 */
	public record ScopeSearchResult( IReferenceable scope, Object value, Key key, boolean isScope ) {

		public ScopeSearchResult( IReferenceable scope, Object value, Key key ) {
			this( scope, value, key, false );
		}

		/**
		 * This allow abstraction of the use cases where
		 * local.foo = 1
		 * should really be
		 * variables.local.foo = 1
		 */
		public Key[] getAssignmentKeys( Key... keys ) {
			if ( isScope ) {
				return keys;
			}
			Key[] result = new Key[ keys.length + 1 ];
			result[ 0 ] = key;
			System.arraycopy( keys, 0, result, 1, keys.length );
			return result;
		}

	}

	/**
	 * rethrows the closest exception
	 */
	public void rethrow();

	/**
	 * Retrieve all known imports for the current template
	 *
	 * @return List of import definitions
	 */
	public List<ImportDefinition> getCurrentImports();

	/**
	 * If input is a QueryColumn, unwrap it to the underlying value
	 * If input is not a QueryColumn, return it as-is
	 *
	 * @param value The value to unwrap
	 *
	 * @return The unwrapped value
	 */
	public Object unwrapQueryColumn( Object value );

	/**
	 * Get the current query row, providing a default value if the query is not registered
	 *
	 * @param query      The query to get the row from
	 * @param defaultRow The default value to return if the query is not registered
	 *
	 * @return The current row
	 */
	public int getQueryRow( Query query, int defaultRow );

	/**
	 * Get the current query row
	 *
	 * @param query The query to get the row from
	 *
	 * @return The current row
	 */
	public int getQueryRow( Query query );

	/**
	 * Register a query loop
	 *
	 * @param query The query to register
	 * @param row   The row to start at
	 */
	public void registerQueryLoop( Query query, int row );

	/**
	 * Unregister a query loop
	 *
	 * @param query The query to unregister
	 */
	public void unregisterQueryLoop( Query query );

	/**
	 * Increment the query loop
	 *
	 * @param query The query to increment
	 */
	public void incrementQueryLoop( Query query );

	/**
	 * Write output to this buffer. Any input object will be converted to a string
	 *
	 * @param o The object to write
	 *
	 * @return This context
	 */
	public IBoxContext writeToBuffer( Object o );

	/**
	 * Write output to this buffer. Any input object will be converted to a string
	 * If force is true, write even if the setting component has been used with enableOutputOnly=true
	 *
	 * @param o     The object to write
	 * @param force true, write even if output is disabled
	 *
	 * @return This context
	 */
	public IBoxContext writeToBuffer( Object o, boolean force );

	/**
	 * Can the current context output to the response stream?
	 * Contexts tied to a specific object like a function or class may override this
	 * to return false based on their own logic.
	 */
	public Boolean canOutput();

	/**
	 * Flush the buffer to the output stream. The default implementation simply
	 * flushes the buffer in this context
	 * to its parent context. Different "top level" buffers can decide what they
	 * want to do with the buffer.
	 * i.e. Scripting sends to the console, Web sends to HTTP response stream, etc.
	 *
	 * @param force true, flush even if output is disabled
	 *
	 * @return This context
	 */
	public IBoxContext flushBuffer( boolean force );

	/**
	 * Clear the buffer
	 *
	 * @return This context
	 */
	public IBoxContext clearBuffer();

	/**
	 * Get the buffer
	 *
	 * @return The buffer
	 */
	public StringBuffer getBuffer();

	/**
	 * Push a buffer onto the stack. This is mostly so components can capture any
	 * output generated in their body
	 *
	 * @param buffer The buffer to push
	 *
	 * @return This context
	 */
	public IBoxContext pushBuffer( StringBuffer buffer );

	/**
	 * Pop a buffer from the stack
	 *
	 * @return This context
	 */
	public IBoxContext popBuffer();

	/**
	 * Get the contexual config struct. Each context has a chance to add in config
	 * of their
	 * own to the struct, or override existing config with a new struct of their own
	 * design.
	 * It depends on whether the context wants its changes to exist for the rest of
	 * the entire
	 * request or only for code that executes in the current context and below.
	 *
	 * @return A struct of configuration
	 */
	public IStruct getConfig();

	/**
	 * Get the contexual config as a DataNavigator. Call it with a {@code path} and
	 * seed the navigation path.
	 *
	 * @param path The path to navigate the struct with
	 *
	 * @return A Data navigator of the config
	 */
	public Navigator navigateConfig( String... path );

	/**
	 * Contexts can optionallky cache their config. If so, they must override this method
	 * to clear the cache when requested, and propagate the request to their parent context
	 */
	public void clearConfigCache();

	/**
	 * Serach for an ancestor context of the given type
	 *
	 * @param <T> The type of context to search for
	 *
	 * @return The matching parent context, or null if one is not found of this
	 *         type.
	 */
	public <T> T getParentOfType( Class<T> type );

	/**
	 * Serach for an ancestor context of RequestBoxContext
	 * This is a convenience method for getParentOfType( RequestBoxContext.class )
	 * since it is so common
	 *
	 * @return The matching parent RequestBoxContext, or null if one is not found of this
	 *         type.
	 */
	public RequestBoxContext getRequestContext();

	/**
	 * Serach for an ancestor context of ApplicationBoxContext
	 * This is a convenience method for getParentOfType( ApplicationBoxContext.class )
	 * since it is so common
	 *
	 * @return The matching parent ApplicationBoxContext, or null if one is not found of this
	 *         type.
	 */
	public ApplicationBoxContext getApplicationContext();

	/**
	 * This is a convenience method to get the running application's name from the context if any.
	 * This will give you the name in a case-insensitive manner.
	 * If there is no application context, it will return an empty string
	 *
	 * @return The application name or an empty string if not running in an application context
	 */
	public default String getApplicationName() {
		ApplicationBoxContext app = getApplicationContext();
		return app != null ? app.getApplication().getName().getNameNoCase() : "";
	}

	/**
	 * This is a convenience method to get an application cache using the app name prefix
	 * or if not found, it tries to find it by the name as a global cache
	 *
	 * @param cacheName The name of the cache to retrieve
	 *
	 * @return The app or global cache provider
	 *
	 * @throws BoxRuntimeException If the cache was not found
	 */
	public default ICacheProvider getApplicationCache( String cacheName ) {
		ApplicationBoxContext	appContext		= getApplicationContext();
		CacheService			cacheService	= getRuntime().getCacheService();

		if ( appContext != null ) {
			Key appCacheName = appContext.getApplication().buildAppCacheKey( Key.of( cacheName ) );
			if ( cacheService.hasCache( appCacheName ) ) {
				return cacheService.getCache( appCacheName );
			}
		}

		return cacheService.getCache( Key.of( cacheName ) );
	}

	/**
	 * Convenience method to retrieve a single config item
	 *
	 * @param itemKey the object key to retrieve
	 *
	 * @return The object value of the key or null if not found
	 */
	public Object getConfigItem( Key itemKey );

	/**
	 * Convenience method to retrieve a config item(s). You can pass in multiple keys
	 * separated by commas. It will traverse the keys in order and return the last key requested.
	 *
	 * @param itemKey the object key(s)
	 *
	 * @return The object value of the key or null if not found
	 */
	public Object getConfigItems( Key... itemKey );

	/**
	 * Convenience method to retrieve a config item with with an optional default
	 *
	 * @param itemKey      the object key
	 * @param defaultValue a default value to return
	 *
	 * @return The object value of the key or the default value if not found
	 */
	public Object getConfigItem( Key itemKey, Object defaultValue );

	/**
	 * Get the BoxLang runtime
	 *
	 * @return The runtime
	 */
	public BoxRuntime getRuntime();

	/**
	 * Get a struct of module settings
	 *
	 * @param name The name of the module
	 *
	 * @throws BoxRuntimeException If the module was not found
	 *
	 * @return The module settings struct
	 */
	public IStruct getModuleSettings( Key name );

	/**
	 * Get a module record
	 *
	 * @param name The name of the module
	 *
	 * @throws BoxRuntimeException If the module was not found
	 *
	 * @return The module record
	 */
	public ModuleRecord getModuleRecord( Key name );

	/**
	 * This is an optional method on contexts which require shutdown outside of its constructor
	 */
	public default void shutdown() {
		// Default is nothing
	}

	/**
	 * This is an optional method on contexts which require startup outside of its constructor
	 */
	public default void startup() {
		// Default is nothing
	}

	/**
	 * Register a UDF with a specific scope
	 *
	 * @param scope    The scope to assign to
	 * @param udf      The UDF to register
	 * @param override true, override any existing UDF with the same name
	 */
	default void registerUDF( IScope scope, UDF udf, boolean override ) {
		if ( override || !scope.containsKey( udf.getName() ) ) {
			boolean isFinal = udf.hasModifier( BoxMethodDeclarationModifier.FINAL );
			if ( isFinal ) {
				scope.assignFinal( this, udf.getName(), udf );
			} else {
				scope.put( udf.getName(), udf );
			}
		}
	}

	/**
	 * This implements a check if a value is defined, which allows the compat module to override for CF behavior
	 *
	 * @param value The value to check
	 *
	 * @return True if the value is defined, else false
	 */
	public boolean isDefined( Object value, boolean forAssign );

}
