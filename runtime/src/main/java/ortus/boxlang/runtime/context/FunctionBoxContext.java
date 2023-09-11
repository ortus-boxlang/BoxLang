package ortus.boxlang.runtime.context;

import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.LocalScope;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;
import ortus.boxlang.runtime.types.exceptions.ScopeNotFoundException;

public class FunctionBoxContext extends BaseBoxContext {

	/**
	 * The arguments scope
	 */
	private IScope		argumentsScope;

	/**
	 * The local scope
	 */
	private IScope		localScope;

	/**
	 * The Function being invoked with this context
	 */
	private Function	function;

	/**
	 * Creates a new execution context with a bounded function instance and parent context
	 *
	 * @param parent   The parent context
	 * @param function The function being invoked with this context
	 */
	public FunctionBoxContext( IBoxContext parent, Function function ) {
		this( parent, function, new ArgumentsScope() );
	}

	public FunctionBoxContext( IBoxContext parent, Function function, ArgumentsScope argumentsScope ) {
		super( parent );
		if ( parent == null ) {
			throw new IllegalArgumentException( "Parent context cannot be null for FunctionBoxContext" );
		}
		this.localScope		= new LocalScope();
		this.argumentsScope	= argumentsScope;
		this.function		= function;
	}

	/**
	 * Returns the function being invoked with this context
	 */
	public Function getFunction() {
		return function;
	}

	public ScopeSearchResult scopeFindNearby( Key key, IScope defaultScope ) {

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

		if ( parent != null ) {
			// A UDF is "transparent" and can see everything in the parent scope as a "local" observer
			return parent.scopeFindNearby( key, defaultScope );
		}

		// Default scope requested for missing keys
		if ( defaultScope != null ) {
			return new ScopeSearchResult( defaultScope, null );
		}
		// Not found anywhere
		throw new KeyNotFoundException(
		    String.format( "The requested key [%s] was not located in any scope or it's undefined", key.getName() )
		);
	}

	public ScopeSearchResult scopeFind( Key key, IScope defaultScope ) {

		// The FunctionBoxContext has no "global" scopes, so just defer to parent

		if ( parent != null ) {
			// A UDF is "transparent" and can see everything in the parent scope as a "local" observer
			return parent.scopeFind( key, defaultScope );
		}

		// Default scope requested for missing keys
		if ( defaultScope != null ) {
			return new ScopeSearchResult( defaultScope, null );
		}
		// Not found anywhere
		throw new KeyNotFoundException(
		    String.format( "The requested key [%s] was not located in any scope or it's undefined", key.getName() )
		);
	}

	public IScope getScope( Key name ) throws ScopeNotFoundException {

		// The FunctionBoxContext has no "global" scopes, so just defer to parent
		if ( parent != null ) {
			return parent.getScope( name );
		}

		// Not found anywhere
		throw new ScopeNotFoundException(
		    String.format( "The requested scope name [%s] was not located in any context", name.getName() )
		);

	}

	public IScope getScopeNearby( Key name ) throws ScopeNotFoundException {
		// Check the scopes I know about
		if ( name.equals( localScope.getName() ) ) {
			return localScope;
		}
		if ( name.equals( argumentsScope.getName() ) ) {
			return argumentsScope;
		}

		// The FunctionBoxContext has no "global" scopes, so just defer to parent
		if ( parent != null ) {
			return parent.getScopeNearby( name );
		}

		// Not found anywhere
		throw new ScopeNotFoundException(
		    String.format( "The requested scope name [%s] was not located in any context", name.getName() )
		);

	}

	/**
	 * Finds the closest function call
	 *
	 * @return The Function instance
	 */
	public Function findClosestFunction() {
		return function;
	}

	/**
	 * Get the default variable assignment scope for this context
	 *
	 * @return The scope reference to use
	 */
	public IScope getDefaultAssignmentScope() {
		// DIFFERENT FROM CFML ENGINES! Same as Lucee's "local mode"
		return localScope;
	}

}
