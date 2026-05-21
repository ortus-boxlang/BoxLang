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
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.interop.DynamicInteropService;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.GenericMeta;

/**
 * Lazy open-ended integer range.
 */
public class LazyRange implements IType, IReferenceable, Iterable<Integer>, Serializable {

	private static final long		serialVersionUID	= 1L;

	private final Integer			start;
	private final Integer			end;
	private final int				step;

	private transient BoxMeta<?>	$bx;

	private LazyRange( Integer start, Integer end, int step ) {
		this.start	= start;
		this.end	= end;
		this.step	= step;
	}

	public static LazyRange startingAt( Integer start ) {
		Objects.requireNonNull( start, "LazyRange start cannot be null" );
		return new LazyRange( start, null, 1 );
	}

	public static LazyRange endingAt( Integer end ) {
		Objects.requireNonNull( end, "LazyRange end cannot be null" );
		return new LazyRange( null, end, -1 );
	}

	public boolean hasStart() {
		return this.start != null;
	}

	public boolean hasEnd() {
		return this.end != null;
	}

	public Integer getStart() {
		return this.start;
	}

	public Integer getEnd() {
		return this.end;
	}

	public boolean contains( Object value ) {
		Integer candidate = IntegerCaster.cast( value, false );
		if ( candidate == null ) {
			return false;
		}

		if ( hasStart() ) {
			return candidate >= this.start;
		}

		return candidate <= this.end;
	}

	@Override
	public Iterator<Integer> iterator() {
		return new Iterator<>() {

			private int current = hasStart() ? LazyRange.this.start : LazyRange.this.end;

			@Override
			public boolean hasNext() {
				return true;
			}

			@Override
			public Integer next() {
				int result = this.current;
				this.current += LazyRange.this.step;
				return result;
			}
		};
	}

	public Stream<Integer> stream() {
		Spliterator<Integer> spliterator = Spliterators.spliteratorUnknownSize( iterator(), Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL );
		return StreamSupport.stream( spliterator, false );
	}

	public Stream<Integer> parallelStream() {
		Spliterator<Integer> spliterator = Spliterators.spliteratorUnknownSize( iterator(), Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL );
		return StreamSupport.stream( spliterator, true );
	}

	@Override
	public String asString() {
		if ( hasStart() ) {
			return this.start + "..";
		}

		return ".." + this.end;
	}

	@Override
	public String getBoxTypeName() {
		return "LazyRange";
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
		throw new BoxRuntimeException( "LazyRange values are immutable." );
	}

	@Override
	public boolean equals( Object obj ) {
		if ( this == obj ) {
			return true;
		}

		if ( ! ( obj instanceof LazyRange other ) ) {
			return false;
		}

		return Objects.equals( this.start, other.start ) && Objects.equals( this.end, other.end ) && this.step == other.step;
	}

	@Override
	public int hashCode() {
		return Objects.hash( this.start, this.end, this.step );
	}

	@Override
	public String toString() {
		return asString();
	}
}
