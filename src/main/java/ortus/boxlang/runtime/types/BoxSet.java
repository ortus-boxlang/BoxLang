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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.BoxMemberExpose;
import ortus.boxlang.runtime.dynamic.casters.SetCaster;
import ortus.boxlang.runtime.operators.Compare;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.FunctionService;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.IChangeListener;
import ortus.boxlang.runtime.types.meta.IListenable;
import ortus.boxlang.runtime.types.meta.SetMeta;
import ortus.boxlang.runtime.types.unmodifiable.UnmodifiableSet;
import ortus.boxlang.runtime.types.util.TypeUtil;
import ortus.boxlang.runtime.util.RegexBuilder;

/**
 * The primary Set class in BoxLang. This class wraps a {@link java.util.Set} and provides
 * full BoxLang integration: member-function dispatch via {@link FunctionService}, change
 * listeners, metadata, and JSON serialization.
 *
 * <p>
 * BoxSet supports three backing variants chosen at construction:
 * <ul>
 * <li>{@link Type#DEFAULT} — {@link HashSet}, fastest, no order guarantee</li>
 * <li>{@link Type#LINKED} — {@link LinkedHashSet}, preserves insertion order</li>
 * <li>{@link Type#SORTED} — {@link TreeSet}, natural ordering (uses {@link Compare})</li>
 * </ul>
 *
 * <p>
 * This class is named {@code BoxSet} (rather than {@code Set}) to avoid collision with
 * {@link java.util.Set} throughout the BoxLang codebase.
 */
public class BoxSet implements Set<Object>, IType, IListenable<BoxSet>, Serializable {

	/**
	 * --------------------------------------------------------------------------
	 * Public properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * An empty, unmodifiable Set instance.
	 */
	public static final BoxSet EMPTY = new UnmodifiableSet();

	/**
	 * The backing storage variant for a {@link BoxSet}.
	 */
	public enum Type {
		/** Backed by {@link HashSet} — fastest, no order guarantee. */
		DEFAULT,
		/** Backed by {@link LinkedHashSet} — preserves insertion order. */
		LINKED,
		/** Backed by {@link TreeSet} — natural ordering. */
		SORTED
	}

	/**
	 * --------------------------------------------------------------------------
	 * Private properties
	 * --------------------------------------------------------------------------
	 */

	protected final Set<Object>							wrapped;

	private final Type									type;

	private final boolean								isSynchronized;
	private final boolean								useNormalization;
	private final boolean								caseSensitive;
	public transient BoxMeta<?>							$bx;

	private transient Map<Key, IChangeListener<BoxSet>>	listeners;

	private static FunctionService						functionService		= BoxRuntime.getInstance().getFunctionService();

	private static final long							serialVersionUID	= 1L;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Default constructor — creates a synchronized hash-backed set.
	 */
	public BoxSet() {
		this( Type.DEFAULT, true );
	}

	/**
	 * Construct a set of the given variant (synchronized).
	 *
	 * @param type The backing variant
	 */
	public BoxSet( Type type ) {
		this( type, true );
	}

	/**
	 * Construct a set with explicit variant and synchronization.
	 *
	 * @param type           The backing variant
	 * @param isSynchronized Whether the backing set should be thread-safe
	 */
	public BoxSet( Type type, boolean isSynchronized ) {
		this( type, isSynchronized, false );
	}

	/**
	 * Construct a set with explicit variant, synchronization, and case-sensitivity.
	 *
	 * @param type           The backing variant
	 * @param isSynchronized Whether the backing set should be thread-safe
	 * @param caseSensitive  Whether string comparisons are case-sensitive (default: false)
	 */
	public BoxSet( Type type, boolean isSynchronized, boolean caseSensitive ) {
		this.type				= type == null ? Type.DEFAULT : type;
		this.isSynchronized		= isSynchronized;
		this.useNormalization	= true;
		this.caseSensitive		= caseSensitive;
		Set<Object> raw = switch ( this.type ) {
			case LINKED -> new LinkedHashSet<>();
			case SORTED -> new TreeSet<>( ( a, b ) -> Compare.invoke( unwrap( a ), unwrap( b ) ) );
			default -> new HashSet<>();
		};
		this.wrapped = isSynchronized ? Collections.synchronizedSet( raw ) : raw;
	}

	/**
	 * Construct a set from an existing {@link Collection} — the contents are copied,
	 * the original is not wrapped.
	 *
	 * @param type   The backing variant for the new set
	 * @param source The collection whose elements should populate the new set
	 */
	public BoxSet( Type type, Collection<?> source ) {
		this( type, source, false );
	}

	/**
	 * Construct a set from an existing {@link Collection} with explicit case-sensitivity.
	 *
	 * @param type          The backing variant for the new set
	 * @param source        The collection whose elements should populate the new set
	 * @param caseSensitive Whether string comparisons are case-sensitive (default: false)
	 */
	public BoxSet( Type type, Collection<?> source, boolean caseSensitive ) {
		this( type, true, caseSensitive );
		if ( source != null ) {
			for ( Object e : source ) {
				this.wrapped.add( wrap( e ) );
			}
		}
	}

	/**
	 * Wrap an existing {@link java.util.Set} so that mutations propagate to the underlying
	 * Java set. Used by {@link SetCaster} when handed a {@code java.util.Set} that is not
	 * already a {@link BoxSet} — preserves the contract that {@code mySet.add(x)} on a
	 * Java HashSet actually mutates the original set, just like {@code myList.add(x)}
	 * on a Java ArrayList does for {@link Array}.
	 *
	 * @param javaSet The Java Set to wrap (NOT copied — mutations propagate)
	 *
	 * @return A BoxSet backed directly by the given Java set without normalization
	 */
	@SuppressWarnings( "unchecked" )
	public static BoxSet wrapJavaSet( java.util.Set<Object> javaSet ) {
		Type t = javaSet instanceof java.util.SortedSet ? Type.SORTED
		    : javaSet instanceof java.util.LinkedHashSet ? Type.LINKED
		        : Type.DEFAULT;
		return new BoxSet( t, javaSet, false );
	}

	/**
	 * Internal constructor for wrapping a raw Java set without normalization.
	 */
	BoxSet( Type type, Set<Object> rawSet, boolean useNormalization ) {
		this.type				= type == null ? Type.DEFAULT : type;
		this.isSynchronized		= false;
		this.useNormalization	= useNormalization;
		this.caseSensitive		= false;
		this.wrapped			= rawSet;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Static convenience methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Build a new default (hash) set from a variadic list of values, deduplicating as it goes.
	 *
	 * @param values The values to add to the new set
	 *
	 * @return The new set
	 */
	public static BoxSet of( Object... values ) {
		return of( Type.DEFAULT, values );
	}

	/**
	 * Build a new set of the given variant from a variadic list of values.
	 *
	 * @param type   The backing variant
	 * @param values The values to add
	 *
	 * @return The new set
	 */
	public static BoxSet of( Type type, Object... values ) {
		BoxSet s = new BoxSet( type );
		if ( values != null ) {
			for ( Object v : values ) {
				s.add( v );
			}
		}
		return s;
	}

	/**
	 * Build a set from an existing {@link Collection}, deduplicating its elements.
	 * The variant defaults to {@link Type#DEFAULT}.
	 *
	 * @param source The source collection (e.g. an {@link Array} or {@link java.util.List})
	 *
	 * @return The new set
	 */
	public static BoxSet fromCollection( Collection<?> source ) {
		return new BoxSet( Type.DEFAULT, source );
	}

	/**
	 * Build a set of the given variant from a {@link Collection}.
	 *
	 * @param type   The backing variant
	 * @param source The source collection
	 *
	 * @return The new set
	 */
	public static BoxSet fromCollection( Type type, Collection<?> source ) {
		return new BoxSet( type, source );
	}

	/**
	 * Parse a variant name (case-insensitive) into a {@link Type}. Accepted values:
	 * {@code "default"} / {@code "hash"} → {@link Type#DEFAULT},
	 * {@code "linked"} / {@code "ordered"} → {@link Type#LINKED},
	 * {@code "sorted"} / {@code "tree"} → {@link Type#SORTED}.
	 *
	 * @param name The variant name (nullable — null returns {@link Type#DEFAULT})
	 *
	 * @return The matching {@link Type}
	 *
	 * @throws BoxRuntimeException if {@code name} is not a recognised variant
	 */
	public static Type parseType( String name ) {
		if ( name == null ) {
			return Type.DEFAULT;
		}
		return switch ( name.trim().toLowerCase() ) {
			case "", "default", "hash" -> Type.DEFAULT;
			case "linked", "ordered" -> Type.LINKED;
			case "sorted", "tree" -> Type.SORTED;
			default -> throw new BoxRuntimeException( "Unknown set type [" + name + "]. Must be one of: default, linked, sorted." );
		};
	}

	/**
	 * --------------------------------------------------------------------------
	 * Set / Collection interface
	 * --------------------------------------------------------------------------
	 */

	@Override
	public int size() {
		return wrapped.size();
	}

	@Override
	public boolean isEmpty() {
		return wrapped.isEmpty();
	}

	@Override
	public boolean contains( Object o ) {
		return wrapped.contains( wrap( o ) );
	}

	@Override
	public Iterator<Object> iterator() {
		Iterator<Object> inner = wrapped.iterator();
		return new Iterator<Object>() {

			@Override
			public boolean hasNext() {
				return inner.hasNext();
			}

			@Override
			public Object next() {
				return unwrap( inner.next() );
			}

			@Override
			public void remove() {
				inner.remove();
			}
		};
	}

	@Override
	public Object[] toArray() {
		return wrapped.stream().map( BoxSet::unwrap ).toArray();
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public <T> T[] toArray( T[] a ) {
		Object[] unwrapped = toArray();
		if ( a.length < unwrapped.length ) {
			return ( T[] ) java.util.Arrays.copyOf( unwrapped, unwrapped.length, a.getClass() );
		}
		System.arraycopy( unwrapped, 0, a, 0, unwrapped.length );
		if ( a.length > unwrapped.length ) {
			a[ unwrapped.length ] = null;
		}
		return a;
	}

	@Override
	public boolean add( Object e ) {
		synchronized ( wrapped ) {
			Object notified = notifyListeners( e, true );
			return wrapped.add( wrap( notified ) );
		}
	}

	@Override
	public boolean remove( Object o ) {
		synchronized ( wrapped ) {
			boolean changed = wrapped.remove( wrap( o ) );
			if ( changed ) {
				notifyListeners( o, false );
			}
			return changed;
		}
	}

	@Override
	public boolean containsAll( Collection<?> c ) {
		for ( Object e : c ) {
			if ( !contains( e ) ) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean addAll( Collection<? extends Object> c ) {
		synchronized ( wrapped ) {
			boolean changed = false;
			for ( Object e : c ) {
				if ( add( e ) ) {
					changed = true;
				}
			}
			return changed;
		}
	}

	@Override
	public boolean removeAll( Collection<?> c ) {
		synchronized ( wrapped ) {
			boolean changed = false;
			for ( Object e : c ) {
				if ( remove( e ) ) {
					changed = true;
				}
			}
			return changed;
		}
	}

	@Override
	public boolean retainAll( Collection<?> c ) {
		synchronized ( wrapped ) {
			BoxSet toKeep = new BoxSet( this.type, c );
			return wrapped.removeIf( nv -> !toKeep.wrapped.contains( nv ) );
		}
	}

	@Override
	public void clear() {
		synchronized ( wrapped ) {
			wrapped.clear();
		}
	}

	@Override
	@BoxMemberExpose
	public Stream<Object> stream() {
		return wrapped.stream().map( BoxSet::unwrap );
	}

	@Override
	@BoxMemberExpose
	public Stream<Object> parallelStream() {
		return wrapped.parallelStream().map( BoxSet::unwrap );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Public BoxLang-specific API
	 * --------------------------------------------------------------------------
	 */

	/**
	 * @return The backing variant of this set
	 */
	public Type getType() {
		return this.type;
	}

	/**
	 * @return true if the backing set is wrapped with {@link Collections#synchronizedSet}
	 */
	public boolean isSynchronized() {
		return this.isSynchronized;
	}

	/**
	 * @return true if string comparisons in this set are case-sensitive
	 */
	public boolean isCaseSensitive() {
		return this.caseSensitive;
	}

	/**
	 * Convert this set to an {@link Array}, preserving iteration order of the
	 * underlying variant (insertion order for LINKED, natural order for SORTED).
	 *
	 * @return A new {@link Array} containing the elements of this set
	 */
	public Array toArrayValue() {
		Array a = new Array();
		for ( Object val : wrapped ) {
			a.add( unwrap( val ) );
		}
		return a;
	}

	/**
	 * Compute the union of this set with another collection. Returns a new set —
	 * neither operand is modified.
	 *
	 * @param other The other collection (typically another {@link BoxSet} or {@link Array})
	 *
	 * @return A new set containing all elements of both
	 */
	public BoxSet union( Collection<?> other ) {
		BoxSet result = new BoxSet( this.type, this );
		if ( other != null ) {
			result.addAll( other );
		}
		return result;
	}

	/**
	 * Compute the intersection of this set with another collection.
	 *
	 * @param other The other collection
	 *
	 * @return A new set containing only elements present in both
	 */
	public BoxSet intersection( Collection<?> other ) {
		BoxSet result = new BoxSet( this.type );
		if ( other == null ) {
			return result;
		}
		// Wrap the other collection for efficient lookup
		BoxSet otherSet = ( other instanceof BoxSet bs ) ? bs : new BoxSet( Type.DEFAULT, other );
		for ( Object val : this.wrapped ) {
			if ( otherSet.wrapped.contains( val ) ) {
				result.wrapped.add( val );
			}
		}
		return result;
	}

	/**
	 * Compute the difference between this set and another collection (this − other).
	 *
	 * @param other The collection of elements to exclude
	 *
	 * @return A new set containing only elements of this set that are not in {@code other}
	 */
	public BoxSet difference( Collection<?> other ) {
		BoxSet result = new BoxSet( this.type, this );
		if ( other != null ) {
			result.removeAll( other );
		}
		return result;
	}

	/**
	 * Compute the symmetric difference between this set and another collection
	 * — elements present in exactly one of the two operands.
	 *
	 * @param other The other collection
	 *
	 * @return A new set containing the symmetric difference
	 */
	public BoxSet symmetricDifference( Collection<?> other ) {
		BoxSet result = new BoxSet( this.type, this );
		if ( other == null ) {
			return result;
		}
		for ( Object e : other ) {
			Object nv = wrap( e );
			if ( !result.wrapped.add( nv ) ) {
				result.wrapped.remove( nv );
			}
		}
		return result;
	}

	/**
	 * @param other The other collection
	 *
	 * @return true if every element of this set is also present in {@code other}
	 */
	public boolean isSubsetOf( Collection<?> other ) {
		if ( other == null ) {
			return this.wrapped.isEmpty();
		}
		BoxSet otherSet = ( other instanceof BoxSet bs ) ? bs : new BoxSet( Type.DEFAULT, other );
		return otherSet.wrapped.containsAll( this.wrapped );
	}

	/**
	 * @param other The other collection
	 *
	 * @return true if every element of {@code other} is present in this set
	 */
	public boolean isSupersetOf( Collection<?> other ) {
		if ( other == null ) {
			return true;
		}
		BoxSet otherSet = ( other instanceof BoxSet bs ) ? bs : new BoxSet( Type.DEFAULT, other );
		return this.wrapped.containsAll( otherSet.wrapped );
	}

	/**
	 * @param other The other collection
	 *
	 * @return true if this set shares no elements with {@code other}
	 */
	public boolean isDisjointFrom( Collection<?> other ) {
		if ( other == null || other.isEmpty() ) {
			return true;
		}
		for ( Object e : other ) {
			if ( this.wrapped.contains( wrap( e ) ) ) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Convert this set to an {@link UnmodifiableSet} that throws on any mutation.
	 *
	 * @return A new {@link UnmodifiableSet} wrapping the same elements
	 */
	public UnmodifiableSet toUnmodifiable() {
		return new UnmodifiableSet( this.type, this.wrapped );
	}

	/**
	 * --------------------------------------------------------------------------
	 * Object overrides
	 * --------------------------------------------------------------------------
	 */

	@Override
	@BoxMemberExpose
	public boolean equals( Object obj ) {
		if ( obj == this ) {
			return true;
		}
		if ( obj instanceof BoxSet other ) {
			return wrapped.equals( other.wrapped );
		}
		if ( obj instanceof Set<?> other ) {
			if ( wrapped.size() != other.size() ) {
				return false;
			}
			for ( Object e : other ) {
				if ( !contains( e ) ) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return computeHashCode( IType.createIdentitySetForType() );
	}

	@Override
	public int computeHashCode( Set<IType> visited ) {
		if ( visited.contains( this ) ) {
			return 0;
		}
		visited.add( this );
		int result = 1;
		for ( Object val : wrapped ) {
			result = result + val.hashCode();
		}
		return result;
	}

	@Override
	public String toString() {
		StringBuilder	sb		= new StringBuilder( "set{" );
		boolean			first	= true;
		for ( Object val : wrapped ) {
			if ( !first ) {
				sb.append( ", " );
			}
			sb.append( unwrap( val ) );
			first = false;
		}
		sb.append( "}" );
		return sb.toString();
	}

	/**
	 * --------------------------------------------------------------------------
	 * IType interface
	 * --------------------------------------------------------------------------
	 */

	@Override
	public String asString() {
		StringBuilder sb = new StringBuilder();
		sb.append( "{\n  " );
		sb.append(
		    wrapped.stream()
		        .map( BoxSet::unwrap )
		        .map( value -> {
			        if ( value == null )
				        return "[null]";
			        Class<?> clazz = value.getClass();
			        if ( clazz.isArray() ) {
				        return Array.copyOf( value );
			        }
			        return value;
		        } )
		        .map( val -> ( val instanceof IType t ? t.asString() : val.toString() ) )
		        .map( line -> RegexBuilder.of( line, RegexBuilder.MULTILINE_START_OF_LINE ).replaceAllAndGet( "  " ) )
		        .collect( java.util.stream.Collectors.joining( ",\n" ) )
		);
		sb.append( "\n}" );
		return sb.toString();
	}

	@Override
	public String getBoxTypeName() {
		return switch ( this.type ) {
			case LINKED -> "Set:Linked";
			case SORTED -> "Set:Sorted";
			default -> "Set";
		};
	}

	@Override
	public BoxMeta<?> getBoxMeta() {
		if ( this.$bx == null ) {
			this.$bx = new SetMeta( this );
		}
		return this.$bx;
	}

	/**
	 * --------------------------------------------------------------------------
	 * IListenable interface
	 * --------------------------------------------------------------------------
	 */

	@Override
	public BoxSet registerChangeListener( IChangeListener<BoxSet> listener ) {
		initListeners();
		listeners.put( IListenable.ALL_KEYS, listener );
		return this;
	}

	@Override
	public BoxSet registerChangeListener( Key key, IChangeListener<BoxSet> listener ) {
		initListeners();
		listeners.put( key, listener );
		return this;
	}

	@Override
	public BoxSet removeChangeListener( Key key ) {
		initListeners();
		listeners.remove( key );
		return this;
	}

	private Object notifyListeners( Object value, boolean isInsert ) {
		if ( listeners == null ) {
			return value;
		}
		// Sets are not keyed — we use a single ALL_KEYS listener.
		IChangeListener<BoxSet> listener = listeners.get( IListenable.ALL_KEYS );
		if ( listener == null ) {
			return value;
		}
		Object	newValue	= isInsert ? value : null;
		Object	oldValue	= isInsert ? null : value;
		return listener.notify( IListenable.ALL_KEYS, newValue, oldValue, this );
	}

	private void initListeners() {
		if ( listeners == null ) {
			listeners = new ConcurrentHashMap<>();
		}
	}

	/**
	 * Static helpers used by the runtime to drive {@code +}, {@code -}, {@code &}, {@code ^}
	 * operators without having to allocate intermediate {@link BoxSet} instances when the
	 * operands are not already sets.
	 */
	public static BoxSet union( BoxSet a, BoxSet b ) {
		return a.union( b );
	}

	public static BoxSet intersection( BoxSet a, BoxSet b ) {
		return a.intersection( b );
	}

	public static BoxSet difference( BoxSet a, BoxSet b ) {
		return a.difference( b );
	}

	public static BoxSet symmetricDifference( BoxSet a, BoxSet b ) {
		return a.symmetricDifference( b );
	}

	/**
	 * Utility — describe an arbitrary object for error messages.
	 */
	static String describe( Object o ) {
		return TypeUtil.getObjectName( o );
	}

	/**
	 * --------------------------------------------------------------------------
	 * NormalizedValue wrapping helpers
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Wrap a raw value into a {@link NormalizedValue} for internal storage.
	 * Short-circuits if normalization is disabled (raw Java set wrapping).
	 *
	 * @param value The raw value to wrap
	 *
	 * @return A NormalizedValue wrapping the given object, or the object cast directly if not normalizing
	 */
	@SuppressWarnings( "unchecked" )
	protected Object wrap( Object value ) {
		if ( !this.useNormalization ) {
			return value;
		}
		if ( value instanceof NormalizedValue nv ) {
			return nv;
		}
		return new NormalizedValue( value, this.caseSensitive );
	}

	/**
	 * Unwrap a value — if it's a {@link NormalizedValue}, return the original;
	 * otherwise return it as-is.
	 *
	 * @param value The value to unwrap
	 *
	 * @return The original value
	 */
	protected static Object unwrap( Object value ) {
		if ( value instanceof NormalizedValue nv ) {
			return nv.getOriginalValue();
		}
		return value;
	}

}
