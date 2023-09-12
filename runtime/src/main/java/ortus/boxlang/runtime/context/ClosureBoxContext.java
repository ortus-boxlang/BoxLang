package ortus.boxlang.runtime.context;

import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Closure;

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
		super( parent, function, argumentsScope );
		if ( parent == null ) {
			throw new IllegalArgumentException( "Parent context cannot be null for ClosureBoxContext" );
		}
	}

	/**
	 * Search for a variable in "nearby" scopes
	 */
	public ScopeSearchResult scopeFindNearby( Key key, IScope defaultScope ) {
		// TODO: search getFunction().getDeclaringContext() first-- need to limit this context to ONLY the "nearby" contexts. i.e., we want to find local,
		// variables, this, but not cgi, server, application, etc

		return super.scopeFindNearby( key, defaultScope );
	}

	// scopeFind(), getScope() and getScopeNearby() do not need to be overridden, as the closure's declaring context does not affect specific scope
	// lookups

	/**
	 * Returns the function being invoked with this context, cast as a Closure
	 */
	@Override
	public Closure getFunction() {
		return ( Closure ) function;
	}

}
