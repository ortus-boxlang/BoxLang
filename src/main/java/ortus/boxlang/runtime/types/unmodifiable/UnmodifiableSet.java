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
package ortus.boxlang.runtime.types.unmodifiable;

import java.util.Collection;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.BoxSet;
import ortus.boxlang.runtime.types.exceptions.UnmodifiableException;

/**
 * Represents an Unmodifiable {@link BoxSet}. All data must be provided at construction —
 * once instantiated, the set cannot be modified. Any mutation throws
 * {@link UnmodifiableException}.
 */
public class UnmodifiableSet extends BoxSet implements IUnmodifiable {

	private static final long serialVersionUID = 1L;

	/**
	 * Empty unmodifiable set (default variant).
	 */
	public UnmodifiableSet() {
		super( Type.DEFAULT, false );
	}

	/**
	 * Build an unmodifiable set of the given variant containing the elements of {@code source}.
	 *
	 * @param type   The backing variant
	 * @param source The source collection
	 */
	public UnmodifiableSet( Type type, Collection<?> source ) {
		super( type, false );
		// Bypass the mutator block below — populate the underlying wrapped set directly.
		if ( source != null ) {
			this.wrapped.addAll( source );
		}
	}

	/**
	 * Build an unmodifiable copy of an existing {@link BoxSet}, preserving its variant.
	 *
	 * @param source The source set
	 */
	public UnmodifiableSet( BoxSet source ) {
		this( source.getType(), source );
	}

	@Override
	public BoxSet toModifiable() {
		return new BoxSet( this.getType(), this );
	}

	/*
	 * ------------------------------------------------------------------ *
	 * Mutators — all throw UnmodifiableException.
	 * ------------------------------------------------------------------
	 */

	@Override
	public boolean add( Object e ) {
		throw new UnmodifiableException( "Cannot modify Unmodifiable Set" );
	}

	@Override
	public boolean remove( Object o ) {
		throw new UnmodifiableException( "Cannot modify Unmodifiable Set" );
	}

	@Override
	public boolean addAll( Collection<? extends Object> c ) {
		throw new UnmodifiableException( "Cannot modify Unmodifiable Set" );
	}

	@Override
	public boolean removeAll( Collection<?> c ) {
		throw new UnmodifiableException( "Cannot modify Unmodifiable Set" );
	}

	@Override
	public boolean retainAll( Collection<?> c ) {
		throw new UnmodifiableException( "Cannot modify Unmodifiable Set" );
	}

	@Override
	public void clear() {
		throw new UnmodifiableException( "Cannot modify Unmodifiable Set" );
	}

	@Override
	public Object assign( IBoxContext context, Key key, Object value ) {
		throw new UnmodifiableException( "Cannot modify Unmodifiable Set" );
	}

}
