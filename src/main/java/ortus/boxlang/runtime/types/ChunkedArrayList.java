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
import java.util.AbstractList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A chunked array list that grows without ever copying or moving existing data.
 *
 * Structure:
 * - Top level: Object[chunkSize] holding references to chunks
 * - Each chunk: Object[chunkSize + 1] holding chunkSize data slots + 1 link slot
 * - The link slot (last element) chains to the next chunk for overflow
 *
 * With default chunkSize=1024:
 * - Indices 0–1,048,575 are 2 array lookups (top chunk + data)
 * - Indices 1,048,576–2,097,151 are 3 lookups (top chunk + link + data)
 * - And so on for deeper levels
 *
 * Chunk sizes are always rounded up to the nearest power of 2 so that all
 * index arithmetic uses bitwise shift/mask instead of integer division.
 *
 * Thread safety:
 * - {@link #add(Object)} is safe for concurrent use from multiple threads.
 * Each caller gets a unique index via atomic increment, chunk creation is
 * internally synchronized, and the data write is a plain array store.
 * - {@link #get(int)} and {@link #set(int, Object)} are lock-free array lookups.
 * - All other mutations (remove, insert, clear) require external synchronization.
 * - After concurrent adds, a synchronization point (e.g. stream completion,
 * Thread.join) is needed before reading the added data from another thread.
 */
public class ChunkedArrayList<E> extends AbstractList<E> implements Serializable {

	private static final long	serialVersionUID	= 1L;

	/**
	 * Default number of data slots per chunk (must be a power of 2)
	 */
	public static final int		DEFAULT_CHUNK_SIZE	= 1024;

	/**
	 * Number of data slots per chunk (always a power of 2)
	 */
	private final int			chunkSize;

	/**
	 * Bit shift for chunkSize (log2 of chunkSize)
	 */
	private final int			chunkShift;

	/**
	 * Bit mask for chunkSize (chunkSize - 1)
	 */
	private final int			chunkMask;

	/**
	 * Bit shift for layerSize (2 * chunkShift)
	 */
	private final int			layerShift;

	/**
	 * Bit mask for layerSize (layerSize - 1)
	 */
	private final int			layerMask;

	/**
	 * Top-level array of chunk references
	 */
	private final Object[]		chunks;

	/**
	 * Logical size of the list
	 */
	private final AtomicInteger	size				= new AtomicInteger( 0 );

	/**
	 * Create a ChunkedArrayList with the default chunk size (1024)
	 */
	public ChunkedArrayList() {
		this( DEFAULT_CHUNK_SIZE );
	}

	/**
	 * Create a ChunkedArrayList with the specified chunk size.
	 * If the chunk size is not a power of 2, it will be rounded up to the
	 * nearest power of 2 automatically. Zero or negative values use the default.
	 *
	 * @param chunkSize number of data slots per chunk (rounded up to next power of 2)
	 */
	public ChunkedArrayList( int chunkSize ) {
		this( chunkSize, 0 );
	}

	/**
	 * Create a ChunkedArrayList pre-filled with nulls of the given size,
	 * using the default chunk size. Chunks are pre-allocated and the size
	 * is set in one operation — no per-element add() calls needed.
	 *
	 * @param count number of null elements to pre-fill
	 *
	 * @return a new ChunkedArrayList of the given size, all nulls
	 */
	public static <E> ChunkedArrayList<E> ofNulls( int count ) {
		return ofNulls( DEFAULT_CHUNK_SIZE, count );
	}

	/**
	 * Create a ChunkedArrayList pre-filled with nulls of the given size.
	 * Chunks are pre-allocated and the size is set in one operation —
	 * no per-element add() calls needed. Since Java initializes Object arrays
	 * to null, the data is already correct after chunk allocation.
	 *
	 * @param chunkSize chunk size (rounded up to next power of 2)
	 * @param count     number of null elements to pre-fill
	 *
	 * @return a new ChunkedArrayList of the given size, all nulls
	 */
	public static <E> ChunkedArrayList<E> ofNulls( int chunkSize, int count ) {
		ChunkedArrayList<E> list = new ChunkedArrayList<>( chunkSize, count );
		list.size.set( count );
		return list;
	}

	/**
	 * Create a ChunkedArrayList populated with the contents of the given array,
	 * using the default chunk size. Data is bulk-copied directly into chunks
	 * via System.arraycopy — no per-element overhead.
	 *
	 * @param data the array of elements to copy into this list
	 */
	public ChunkedArrayList( E[] data ) {
		this( DEFAULT_CHUNK_SIZE, data.length );
		int	remaining	= data.length;
		int	srcPos		= 0;
		while ( remaining > 0 ) {
			Object[]	chunk	= ensureAndGetChunk( srcPos );
			int			toCopy	= Math.min( this.chunkSize, remaining );
			System.arraycopy( data, srcPos, chunk, 0, toCopy );
			srcPos		+= toCopy;
			remaining	-= toCopy;
		}
		this.size.set( data.length );
	}

	/**
	 * Create a ChunkedArrayList with the specified chunk size and pre-allocate
	 * chunks for the given initial capacity. Pre-allocation eliminates all
	 * synchronization from the {@link #add(Object)} path for indices below
	 * the initial capacity.
	 *
	 * If the chunk size is not a power of 2, it will be rounded up to the
	 * nearest power of 2 automatically. Zero or negative values use the default.
	 *
	 * @param chunkSize       number of data slots per chunk (rounded up to next power of 2)
	 * @param initialCapacity number of elements to pre-allocate chunks for
	 */
	public ChunkedArrayList( int chunkSize, int initialCapacity ) {
		if ( chunkSize <= 0 ) {
			chunkSize = DEFAULT_CHUNK_SIZE;
		} else if ( ( chunkSize & ( chunkSize - 1 ) ) != 0 ) {
			chunkSize = Integer.highestOneBit( chunkSize ) << 1;
		}
		this.chunkSize	= chunkSize;
		this.chunkShift	= Integer.numberOfTrailingZeros( chunkSize );
		this.chunkMask	= chunkSize - 1;
		this.layerShift	= this.chunkShift << 1;
		this.layerMask	= ( chunkSize * chunkSize ) - 1;
		this.chunks		= new Object[ chunkSize ];

		// Pre-allocate chunks for initialCapacity
		if ( initialCapacity > 0 ) {
			for ( int i = 0; i < initialCapacity; i += chunkSize ) {
				ensureAndGetChunk( i );
			}
		}
	}

	@Override
	public int size() {
		return this.size.get();
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public E get( int index ) {
		rangeCheck( index );
		return ( E ) getChunkForIndex( index )[ index & this.chunkMask ];
	}

	@Override
	@SuppressWarnings( "unchecked" )
	public E set( int index, E element ) {
		rangeCheck( index );
		Object[]	chunk	= getChunkForIndex( index );
		int			offset	= index & this.chunkMask;
		E			old		= ( E ) chunk[ offset ];
		chunk[ offset ] = element;
		return old;
	}

	/**
	 * Append an element to the end of the list.
	 * Safe for concurrent use from multiple threads.
	 *
	 * @param e element to append
	 *
	 * @return true
	 */
	@Override
	public boolean add( E e ) {
		int			index	= this.size.getAndIncrement();
		Object[]	chunk	= ensureAndGetChunk( index );
		chunk[ index & this.chunkMask ] = e;
		return true;
	}

	/**
	 * Insert an element at the specified index, shifting subsequent elements.
	 * NOT thread-safe — requires external synchronization.
	 *
	 * @param index   position to insert at
	 * @param element element to insert
	 */
	@Override
	public void add( int index, E element ) {
		int s = this.size.get();
		if ( index < 0 || index > s ) {
			throw new IndexOutOfBoundsException( "Index: " + index + ", Size: " + s );
		}
		this.size.incrementAndGet();
		ensureAndGetChunk( s );
		for ( int i = s; i > index; i-- ) {
			setDirect( i, getDirect( i - 1 ) );
		}
		setDirect( index, element );
	}

	/**
	 * Remove the element at the specified index, shifting subsequent elements.
	 * NOT thread-safe — requires external synchronization.
	 *
	 * @param index position to remove
	 *
	 * @return the removed element
	 */
	@Override
	@SuppressWarnings( "unchecked" )
	public E remove( int index ) {
		int s = this.size.get();
		rangeCheck( index );
		E old = ( E ) getDirect( index );
		for ( int i = index; i < s - 1; i++ ) {
			setDirect( i, getDirect( i + 1 ) );
		}
		setDirect( s - 1, null );
		this.size.decrementAndGet();
		return old;
	}

	@Override
	public void clear() {
		Arrays.fill( this.chunks, null );
		this.size.set( 0 );
	}

	/**
	 * Release all chunks beyond the current size, freeing memory.
	 * NOT thread-safe — requires external synchronization or a happens-before
	 * guarantee that no concurrent adds are in progress.
	 */
	public void trimToSize() {
		int s = this.size.get();
		if ( s == 0 ) {
			Arrays.fill( this.chunks, null );
			return;
		}
		// The last used index is s - 1
		int	lastIndex		= s - 1;
		int	lastTopIndex	= ( lastIndex & this.layerMask ) >> this.chunkShift;
		int	lastDepth		= lastIndex >> this.layerShift;

		// Null out all top-level chunk slots beyond the last used one
		for ( int top = lastTopIndex + 1; top < this.chunkSize; top++ ) {
			this.chunks[ top ] = null;
		}

		// For the last used top-level slot, null out depth links beyond the last used depth
		Object[] chunk = ( Object[] ) this.chunks[ lastTopIndex ];
		if ( chunk != null ) {
			for ( int d = 0; d < lastDepth; d++ ) {
				Object[] next = ( Object[] ) chunk[ this.chunkSize ];
				if ( next == null ) {
					break;
				}
				chunk = next;
			}
			// Null out the link beyond the last used depth
			chunk[ this.chunkSize ] = null;
		}

		// Null out data slots in the last chunk beyond the last used offset
		if ( chunk != null ) {
			int lastOffset = lastIndex & this.chunkMask;
			for ( int i = lastOffset + 1; i < this.chunkSize; i++ ) {
				chunk[ i ] = null;
			}
		}
	}

	@Override
	public Object[] toArray() {
		int			s			= this.size.get();
		Object[]	result		= new Object[ s ];
		int			remaining	= s;
		int			resultIndex	= 0;
		int			depth		= 0;
		while ( remaining > 0 ) {
			for ( int top = 0; top < this.chunkSize && remaining > 0; top++ ) {
				Object[] chunk = getChunkAtDepth( top, depth );
				if ( chunk == null ) {
					remaining = 0;
					break;
				}
				int toCopy = Math.min( this.chunkSize, remaining );
				System.arraycopy( chunk, 0, result, resultIndex, toCopy );
				resultIndex	+= toCopy;
				remaining	-= toCopy;
			}
			depth++;
		}
		return result;
	}

	@SuppressWarnings( "unchecked" )
	@Override
	public <T> T[] toArray( T[] a ) {
		int s = this.size.get();
		if ( a.length < s ) {
			a = ( T[] ) java.lang.reflect.Array.newInstance( a.getClass().getComponentType(), s );
		}
		Object[] flat = toArray();
		System.arraycopy( flat, 0, a, 0, s );
		if ( a.length > s ) {
			a[ s ] = null;
		}
		return a;
	}

	// ----------------------------------------------------------
	// Internal helpers
	// ----------------------------------------------------------

	/**
	 * Navigate to the chunk containing the given index.
	 * The chunk must already exist.
	 *
	 * @param index the list index
	 *
	 * @return the chunk array containing the data for that index
	 */
	private Object[] getChunkForIndex( int index ) {
		int			depth		= index >> this.layerShift;
		int			topIndex	= ( index & this.layerMask ) >> this.chunkShift;
		Object[]	chunk		= ( Object[] ) this.chunks[ topIndex ];
		for ( int d = 0; d < depth; d++ ) {
			chunk = ( Object[] ) chunk[ this.chunkSize ];
		}
		return chunk;
	}

	/**
	 * Navigate to the chunk at a specific top index and depth, returning null
	 * if any chunk in the chain doesn't exist.
	 *
	 * @param topIndex top-level array index
	 * @param depth    number of links to follow
	 *
	 * @return the chunk array, or null if it doesn't exist
	 */
	private Object[] getChunkAtDepth( int topIndex, int depth ) {
		Object[] chunk = ( Object[] ) this.chunks[ topIndex ];
		if ( chunk == null ) {
			return null;
		}
		for ( int d = 0; d < depth; d++ ) {
			chunk = ( Object[] ) chunk[ this.chunkSize ];
			if ( chunk == null ) {
				return null;
			}
		}
		return chunk;
	}

	/**
	 * Ensure the chunk for the given index exists, creating it if needed.
	 * Only the chunk creation is synchronized — the common case (chunk exists)
	 * is lock-free.
	 *
	 * @param index the list index that needs a chunk
	 *
	 * @return the chunk array for the given index
	 */
	private Object[] ensureAndGetChunk( int index ) {
		int			depth		= index >> this.layerShift;
		int			topIndex	= ( index & this.layerMask ) >> this.chunkShift;

		Object[]	topChunk	= ( Object[] ) this.chunks[ topIndex ];
		if ( topChunk == null ) {
			synchronized ( this ) {
				topChunk = ( Object[] ) this.chunks[ topIndex ];
				if ( topChunk == null ) {
					topChunk				= new Object[ this.chunkSize + 1 ];
					this.chunks[ topIndex ]	= topChunk;
				}
			}
		}

		Object[] current = topChunk;
		for ( int d = 0; d < depth; d++ ) {
			Object[] next = ( Object[] ) current[ this.chunkSize ];
			if ( next == null ) {
				synchronized ( this ) {
					next = ( Object[] ) current[ this.chunkSize ];
					if ( next == null ) {
						next						= new Object[ this.chunkSize + 1 ];
						current[ this.chunkSize ]	= next;
					}
				}
			}
			current = next;
		}

		return current;
	}

	/**
	 * Get value at index without bounds checking.
	 */
	private Object getDirect( int index ) {
		return getChunkForIndex( index )[ index & this.chunkMask ];
	}

	/**
	 * Set value at index without bounds checking, ensuring chunk exists.
	 */
	private void setDirect( int index, Object value ) {
		ensureAndGetChunk( index )[ index & this.chunkMask ] = value;
	}

	/**
	 * Bounds check for get/set/remove.
	 */
	private void rangeCheck( int index ) {
		if ( index < 0 || index >= this.size.get() ) {
			throw new IndexOutOfBoundsException( "Index: " + index + ", Size: " + this.size.get() );
		}
	}
}
