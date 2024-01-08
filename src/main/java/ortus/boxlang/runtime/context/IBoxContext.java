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

import java.util.List;
import java.util.Map;

import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.runnables.ITemplateRunnable;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.UDF;
import ortus.boxlang.runtime.types.exceptions.ScopeNotFoundException;

/**
 * This represents the most basic box context.
 */
public interface IBoxContext {

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
	 * cgi or server which are never encapsulated like variables is inside a CFC.
	 *
	 * If defaultScope is null and the key can't be found, a KeyNotFoundException will be thrown
	 * If defaultScope is not null, it will return a record with the default scope and null value if the key is not found
	 *
	 * @param key The key to search for
	 *
	 * @return The value of the key if found
	 *
	 */
	public ScopeSearchResult scopeFind( Key key, IScope defaultScope );

	/**
	 * Try to get the requested key from an unknown scope
	 * Meaning it needs to search scopes in order according to it's context.
	 * A nearby lookup is used for the closest context to the executing code
	 *
	 * If defaultScope is null and the key can't be found, a KeyNotFoundException will be thrown
	 * If defaultScope is not null, it will return a record with the default scope and null value if the key is not found
	 *
	 * @param key The key to search for
	 *
	 * @return The value of the key if found
	 *
	 */
	public ScopeSearchResult scopeFindNearby( Key key, IScope defaultScope );

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
	public ScopeSearchResult scopeFindNearby( Key key, IScope defaultScope, boolean shallow );

	/**
	 * Invoke a function call such as foo() using positional args. Will check for a registered BIF first, then search known scopes for a UDF.
	 *
	 * @return Return value of the function call
	 */
	public Object invokeFunction( Key name, Object[] positionalArguments );

	/**
	 * Invoke a function call such as foo() using named args. Will check for a registered BIF first, then search known scopes for a UDF.
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
	 * Invoke a template in the current context
	 *
	 * @param templatePath A relateive template path
	 */
	public void includeTemplate( String templatePath );

	/**
	 * Register a UDF with the local context.
	 *
	 * @param udf The UDF to register
	 */
	public void registerUDF( UDF udf );

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
	 * Finds the closest function call name
	 *
	 * @return The called name of the function if found, null if this code is not called from a function
	 */
	public Key findClosestFunctionName();

	/**
	 * Push a template to the stack
	 *
	 * @param template The template that this execution context is bound to
	 *
	 * @return IBoxContext
	 */
	public IBoxContext pushTemplate( ITemplateRunnable template );

	/**
	 * Pop a template from the stack
	 *
	 * @return The template that this execution context is bound to
	 */
	public ITemplateRunnable popTemplate();

	/**
	 * Has the execution context been bound to a template?
	 *
	 * @return True if bound, else false
	 */
	public boolean hasTemplates();

	/**
	 * Finds the closest template
	 *
	 * @return The template instance if found, null if this code is not called from a template
	 */
	public ITemplateRunnable findClosestTemplate();

	/**
	 * Finds the base (first) template in this request
	 *
	 * @return The template instance if found, null if this code is not called from a template
	 */
	public ITemplateRunnable findBaseTemplate();

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
	 * Represents the results of a successful scope hunting expedition.
	 *
	 * @param scope The scope which was found
	 * @param value The value of the key in the scope
	 */
	public record ScopeSearchResult( IScope scope, Object value ) {
		// The record automatically generates the constructor, getters, equals, hashCode, and toString methods.
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
	 */
	public void registerQueryLoop( Query query );

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
	 * Flush the buffer to the output stream. The default implementation simply flushes the buffer in this context
	 * to its parent context. Different "top level" buffers can decide what they want to do with the buffer.
	 * i.e. Scripting sends to the console, Web sends to HTTP response stream, etc.
	 * 
	 * @return This context
	 */
	public IBoxContext flushBuffer();

	/**
	 * Clear the buffer
	 * 
	 * @return This context
	 */
	public IBoxContext clearBuffer();

	/**
	 * Get the buffer
	 * 
	 * @return
	 */
	public StringBuffer getBuffer();

}
