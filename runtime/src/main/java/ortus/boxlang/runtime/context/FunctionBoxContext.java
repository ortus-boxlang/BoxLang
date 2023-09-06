package ortus.boxlang.runtime.context;

import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.LocalScope;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;
import ortus.boxlang.runtime.types.exceptions.ScopeNotFoundException;

public class FunctionBoxContext extends BaseBoxContext {

	/**
	 * The arguments scope
	 */
	private IScope	argumentsScope;

	/**
	 * The local scope
	 */
	private IScope	localScope;

	public FunctionBoxContext( IBoxContext parent ) {
		this( parent, new ArgumentsScope() );
	}

	public FunctionBoxContext( IBoxContext parent, ArgumentsScope argumentsScope ) {
		super( parent );
		if ( parent == null ) {
			throw new IllegalArgumentException( "Parent context cannot be null for FunctionBoxContext" );
		}
		this.localScope		= new LocalScope();
		this.argumentsScope	= argumentsScope;
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

		return scopeFind( key, defaultScope );
	}

	public ScopeSearchResult scopeFind( Key key, IScope defaultScope ) {

		// The FunctionBoxContext has no "global" scopes, so just defer to parent

		if ( parent != null ) {
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

		return getScope( name );

	}

}
