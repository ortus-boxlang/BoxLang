package ortus.boxlang.runtime.context;

import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.LocalScope;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.ApplicationException;
import ortus.boxlang.runtime.types.exceptions.ScopeNotFoundException;

/**
 * This context represents the context of any function execution in BoxLang
 * It encapsulates the arguments scope and local scope and has a reference to the function being invoked.
 * This context is extended for use with both UDFs and Closures as well
 */
public class FunctionBoxContext extends BaseBoxContext {

	/**
	 * The arguments scope
	 */
	protected IScope	argumentsScope;

	/**
	 * The local scope
	 */
	protected IScope	localScope;

	/**
	 * The Function being invoked with this context
	 */
	protected Function	function;

	/**
	 * The Function name being invoked with this context. Note this may or may not be the name the function was declared as.
	 */
	protected Key		functionCalledName;

	/**
	 * Creates a new execution context with a bounded function instance and parent context
	 *
	 * @param parent   The parent context
	 * @param function The function being invoked with this context
	 */
	public FunctionBoxContext( IBoxContext parent, Function function ) {
		this( parent, function, function.getName() );
	}

	/**
	 * Creates a new execution context with a bounded function instance and parent context
	 *
	 * @param parent             The parent context
	 * @param function           The function being invoked with this context
	 * @param functionCalledName The name of the function being invoked
	 *
	 */
	public FunctionBoxContext( IBoxContext parent, Function function, Key functionCalledName ) {
		this( parent, function, functionCalledName, new ArgumentsScope() );
	}

	/**
	 * Creates a new execution context with a bounded function instance and parent context and arguments scope
	 *
	 * @param parent         The parent context
	 * @param function       The function being invoked with this context
	 * @param argumentsScope The arguments scope
	 */
	public FunctionBoxContext( IBoxContext parent, Function function, ArgumentsScope argumentsScope ) {
		this( parent, function, function.getName(), argumentsScope );
	}

	/**
	 * Creates a new execution context with a bounded function instance and parent context and arguments scope
	 *
	 * @param parent             The parent context
	 * @param function           The function being invoked with this context
	 * @param functionCalledName The name of the function being invoked
	 * @param argumentsScope     The arguments scope
	 */
	public FunctionBoxContext( IBoxContext parent, Function function, Key functionCalledName, ArgumentsScope argumentsScope ) {
		super( parent );
		if ( parent == null ) {
			throw new ApplicationException( "Parent context cannot be null for FunctionBoxContext" );
		}
		if ( function == null ) {
			throw new ApplicationException( "function cannot be null for FunctionBoxContext" );
		}
		this.localScope			= new LocalScope();
		this.argumentsScope		= argumentsScope;
		this.function			= function;
		this.functionCalledName	= functionCalledName;
	}

	/**
	 * Returns the function being invoked with this context
	 */
	public Function getFunction() {
		return function;
	}

	/**
	 * Search for a variable in "nearby" scopes
	 *
	 * @param key          The key to search for
	 * @param defaultScope The default scope to use if the key is not found
	 * @param shallow      Whether to search only the "nearby" scopes or all scopes
	 *
	 * @return The search result
	 */
	@Override
	public ScopeSearchResult scopeFindNearby( Key key, IScope defaultScope, boolean shallow ) {

		Object result = localScope.getRaw( key );
		// Null means not found
		if ( result != null ) {
			// Unwrap the value now in case it was really actually null for real
			return new ScopeSearchResult( localScope, Struct.unWrapNull( result ) );
		}

		result = argumentsScope.getRaw( key );
		// Null means not found
		if ( result != null ) {
			// Unwrap the value now in case it was really actually null for real
			return new ScopeSearchResult( argumentsScope, Struct.unWrapNull( result ) );
		}

		if ( shallow ) {
			return null;
		}

		// A UDF is "transparent" and can see everything in the parent scope as a "local" observer
		return parent.scopeFindNearby( key, defaultScope );

	}

	/**
	 * Search for a variable in scopes
	 *
	 * @param key          The key to search for
	 * @param defaultScope The default scope to use if the key is not found
	 *
	 * @return The search result
	 */
	@Override
	public ScopeSearchResult scopeFind( Key key, IScope defaultScope ) {
		// The FunctionBoxContext has no "global" scopes, so just defer to parent
		return parent.scopeFind( key, defaultScope );
	}

	/**
	 * Look for a scope by name
	 *
	 * @param name The name of the scope to look for
	 *
	 * @return The scope reference to use
	 */
	@Override
	public IScope getScope( Key name ) throws ScopeNotFoundException {
		// The FunctionBoxContext has no "global" scopes, so just defer to parent
		return parent.getScope( name );
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
		if ( name.equals( localScope.getName() ) ) {
			return localScope;
		}
		if ( name.equals( argumentsScope.getName() ) ) {
			return argumentsScope;
		}

		if ( shallow ) {
			return null;
		}

		// The FunctionBoxContext has no "global" scopes, so just defer to parent
		return parent.getScopeNearby( name );
	}

	/**
	 * Finds the closest function call name
	 *
	 * @return The called name of the function if found, null if this code is not called from a function
	 */
	@Override
	public Key findClosestFunctionName() {
		return functionCalledName;
	};

	/**
	 * Get the default variable assignment scope for this context
	 *
	 * @return The scope reference to use
	 */
	@Override
	public IScope getDefaultAssignmentScope() {
		// DIFFERENT FROM CFML ENGINES! Same as Lucee's "local mode"
		return localScope;
	}

	/**
	 * Get parent context for a function execution happening in this context
	 *
	 * @return The context to use
	 */
	@Override
	public IBoxContext getFunctionParentContext() {
		// If a function is executed inside another function, it uses the parent since there is nothing a function can "see" from inside it's calling function
		return getParent();
	}

}
