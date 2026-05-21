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
package ortus.boxlang.runtime.types;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.interop.DynamicInteropService;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.GenericMeta;

/**
 * Immutable inclusive integer range value.
 *
 * Phase 1 ranges are finite integer progressions with an implicit unit step.
 * The direction determines the step: ascending ranges advance by {@code +1}
 * and descending ranges advance by {@code -1}. Materialize the values with
 * {@link #toBoxArray()} when a mutable BoxLang array is required.
 */
public class Range extends AbstractCollection<Integer> implements IType, IReferenceable, Serializable {

	private static final long		serialVersionUID	= 1L;

	private final int				from;
	private final int				to;
	private final int				step;
	private final int				size;
	private final int				hashCode;

	private transient BoxMeta<?>	$bx;

	public Range( Integer from, Integer to ) {
		Objects.requireNonNull( from, "Range start cannot be null" );
		Objects.requireNonNull( to, "Range end cannot be null" );

		this.from		= from.intValue();
		this.to			= to.intValue();
		this.step		= this.from <= this.to ? 1 : -1;
		this.size		= Math.abs( this.to - this.from ) + 1;
		this.hashCode	= computeHashCode();
	}

	public int getFrom() {
		return this.from;
	}

	public int getTo() {
		return this.to;
	}

	public Array toBoxArray() {
		Array result = new Array( this.size );
		for ( Integer value : this ) {
			result.add( value );
		}
		return result;
	}

	@Override
	public Iterator<Integer> iterator() {
		return new Iterator<>() {

			private int	remaining	= Range.this.size;
			private int	current		= Range.this.from;

			@Override
			public boolean hasNext() {
				return this.remaining > 0;
			}

			@Override
			public Integer next() {
				if ( !hasNext() ) {
					throw new NoSuchElementException();
				}

				int result = this.current;
				this.current += Range.this.step;
				this.remaining--;
				return result;
			}
		};
	}

	@Override
	public int size() {
		return this.size;
	}

	@Override
	public boolean contains( Object value ) {
		Integer candidate = IntegerCaster.cast( value, false );
		if ( candidate == null ) {
			return false;
		}

		if ( this.step > 0 && ( candidate < this.from || candidate > this.to ) ) {
			return false;
		}

		if ( this.step < 0 && ( candidate > this.from || candidate < this.to ) ) {
			return false;
		}

		// Phase 1 ranges always use a unit step, so this modulo check remains the
		// shared membership rule for both ascending and descending ranges.
		return ( candidate - this.from ) % this.step == 0;
	}

	@Override
	public String asString() {
		return this.from + ".." + this.to;
	}

	@Override
	public String getBoxTypeName() {
		return "Range";
	}

	@Override
	public BoxMeta<?> getBoxMeta() {
		if ( this.$bx == null ) {
			this.$bx = new GenericMeta( this );
		}
		return this.$bx;
	}

	@Override
	public Object dereference( IBoxContext context, Key name, Boolean safe ) {
		if ( name.equals( BoxMeta.key ) ) {
			return getBoxMeta();
		}

		return DynamicInteropService.dereference( context, this.getClass(), this, name, safe );
	}

	@Override
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Object[] positionalArguments, Boolean safe ) {
		return DynamicInteropService.invoke( context, this, name.getName(), safe, positionalArguments );
	}

	@Override
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Map<Key, Object> namedArguments, Boolean safe ) {
		return DynamicInteropService.invoke( context, this, name.getName(), safe, namedArguments );
	}

	@Override
	public Object assign( IBoxContext context, Key name, Object value ) {
		throw new BoxRuntimeException( "Range values are immutable." );
	}

	@Override
	public boolean add( Integer value ) {
		throw immutableCollectionException();
	}

	@Override
	public boolean remove( Object value ) {
		throw immutableCollectionException();
	}

	@Override
	public boolean addAll( Collection<? extends Integer> collection ) {
		throw immutableCollectionException();
	}

	@Override
	public boolean removeAll( Collection<?> collection ) {
		throw immutableCollectionException();
	}

	@Override
	public boolean retainAll( Collection<?> collection ) {
		throw immutableCollectionException();
	}

	@Override
	public void clear() {
		throw immutableCollectionException();
	}

	@Override
	public boolean equals( Object obj ) {
		if ( this == obj ) {
			return true;
		}

		if ( ! ( obj instanceof Range other ) ) {
			return false;
		}

		return this.from == other.from && this.to == other.to;
	}

	@Override
	public int hashCode() {
		return this.hashCode;
	}

	@Override
	public String toString() {
		return asString();
	}

	private int computeHashCode() {
		int result = Integer.hashCode( this.from );
		result = 31 * result + Integer.hashCode( this.to );
		return result;
	}

	private UnsupportedOperationException immutableCollectionException() {
		return new UnsupportedOperationException( "Range is immutable. Call toBoxArray() to materialize a mutable array." );
	}
}