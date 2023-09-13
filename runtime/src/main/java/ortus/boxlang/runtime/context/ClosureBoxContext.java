package ortus.boxlang.runtime.context;

import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Closure;
import ortus.boxlang.runtime.types.Struct;

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
		// A UDF is "transparent" and can see everything in the parent scope as a "local" observer, so we call scopeFindNearby() on the parent
		return parent.scopeFindNearby( key, defaultScope );

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
