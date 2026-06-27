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
import java.util.Objects;

import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.BoxStringBuilderMeta;

/**
 * BoxLang first-class {@code StringBuilder} type.
 *
 * <p>
 * Wraps {@link java.lang.StringBuilder} and exposes a fluent, mutable string-buffer API
 * to BoxLang code. All positional parameters are <em>1-based</em> (BoxLang convention);
 * the wrapper subtracts/adds 1 before delegating to the underlying buffer.
 *
 * <p>
 * A {@code BoxStringBuilder} silently casts to {@code String} anywhere the runtime needs
 * a string — via {@link ortus.boxlang.runtime.dynamic.casters.StringCasterStrict}. This
 * allows all existing string BIFs and member methods to operate on an {@code sb} value
 * without any explicit conversion.
 *
 * <p>
 * Create instances via:
 * <ul>
 * <li>Literal: {@code sb"initial value"} (Box parser only)</li>
 * <li>BIF: {@code stringBuilderNew()} or {@code stringBuilderNew("initial", 128)}</li>
 * </ul>
 */
public class BoxStringBuilder implements IType, Comparable<BoxStringBuilder>, Serializable {

	// Serializable
	private static final long				serialVersionUID	= 1L;

	/** The underlying mutable character buffer. */
	private final java.lang.StringBuilder	buffer;

	/** Lazy-initialised metadata. */
	public transient BoxMeta<?>				$bx;

	// -------------------------------------------------------------------------
	// Construction
	// -------------------------------------------------------------------------

	/**
	 * Creates an empty builder.
	 */
	public BoxStringBuilder() {
		this.buffer = new java.lang.StringBuilder();
	}

	/**
	 * Creates a builder pre-seeded with {@code initialValue}.
	 *
	 * @param initialValue starting content; {@code null} is treated as {@code ""}
	 */
	public BoxStringBuilder( String initialValue ) {
		this.buffer = new java.lang.StringBuilder( initialValue != null ? initialValue : "" );
	}

	/**
	 * Creates a builder with a specific initial capacity (no initial content).
	 *
	 * @param capacity the initial internal capacity in characters
	 */
	public BoxStringBuilder( int capacity ) {
		this.buffer = new java.lang.StringBuilder( capacity );
	}

	/**
	 * Creates a builder with specific initial capacity and pre-seeded content.
	 *
	 * @param initialValue starting content; {@code null} is treated as {@code ""}
	 * @param capacity     the initial internal capacity in characters
	 */
	public BoxStringBuilder( String initialValue, int capacity ) {
		this.buffer = new java.lang.StringBuilder( Math.max( capacity, initialValue != null ? initialValue.length() : 0 ) );
		this.buffer.append( initialValue != null ? initialValue : "" );
	}

	/**
	 * Wraps an existing Java StringBuilder so mutations stay shared with the original
	 * object.
	 *
	 * @param buffer the existing Java StringBuilder to wrap
	 *
	 * @throws NullPointerException if buffer is null
	 */
	public BoxStringBuilder( java.lang.StringBuilder buffer ) {
		this.buffer = Objects.requireNonNull( buffer, "buffer cannot be null" );
	}

	/**
	 * Static factory — casts any BoxLang value to string and wraps it.
	 * Used by the {@code sb"..."} literal code-generator.
	 *
	 * @param value any BoxLang value; cast to string via {@link StringCaster}
	 *
	 * @return new {@code BoxStringBuilder} containing the string representation
	 */
	public static BoxStringBuilder of( Object value ) {
		return new BoxStringBuilder( value == null ? "" : StringCaster.cast( value ) );
	}

	/**
	 * Returns the underlying {@link java.lang.StringBuilder}.
	 *
	 * @return the raw buffer
	 */
	public java.lang.StringBuilder getBuffer() {
		return this.buffer;
	}

	// -------------------------------------------------------------------------
	// IType contract
	// -------------------------------------------------------------------------

	@Override
	public BoxMeta<?> getBoxMeta() {
		if ( this.$bx == null ) {
			this.$bx = new BoxStringBuilderMeta( this );
		}
		return this.$bx;
	}

	@Override
	public String getBoxTypeName() {
		return "StringBuilder";
	}

	@Override
	public String asString() {
		return this.buffer.toString();
	}

	// -------------------------------------------------------------------------
	// Object overrides
	// -------------------------------------------------------------------------

	@Override
	public String toString() {
		return this.buffer.toString();
	}

	@Override
	public int hashCode() {
		return this.buffer.toString().hashCode();
	}

	@Override
	public boolean equals( Object other ) {
		if ( other == this )
			return true;
		if ( other instanceof BoxStringBuilder bsb ) {
			return this.buffer.toString().equals( bsb.buffer.toString() );
		}
		if ( other instanceof String s ) {
			return this.buffer.toString().equals( s );
		}
		return false;
	}

	@Override
	public int compareTo( BoxStringBuilder other ) {
		return this.buffer.toString().compareTo( other.buffer.toString() );
	}

	// -------------------------------------------------------------------------
	// Buffer read methods
	// -------------------------------------------------------------------------

	/**
	 * Returns the current length (number of characters) of the buffer.
	 *
	 * @return character count
	 */
	public int length() {
		return this.buffer.length();
	}

	/**
	 * Returns {@code true} when the buffer contains no characters.
	 *
	 * @return {@code true} if empty
	 */
	public boolean isEmpty() {
		return this.buffer.isEmpty();
	}

	/**
	 * Exposes the underlying buffer as a CharSequence for internal helpers.
	 *
	 * @return the backing buffer
	 */
	public CharSequence asCharSequence() {
		return this.buffer;
	}

	/**
	 * Returns the leftmost characters from the buffer.
	 *
	 * @param count the number of characters to return; negative values omit characters from the right
	 *
	 * @return the extracted substring
	 */
	public String left( int count ) {
		if ( count == 0 ) {
			throw new ortus.boxlang.runtime.types.exceptions.BoxRuntimeException( "Count cannot be zero" );
		}
		if ( count > 0 ) {
			return this.buffer.substring( 0, Math.min( count, this.buffer.length() ) );
		}
		int end = this.buffer.length() + count;
		if ( end < 0 ) {
			return this.buffer.toString();
		}
		return this.buffer.substring( 0, end );
	}

	/**
	 * Returns the rightmost characters from the buffer.
	 *
	 * @param count the number of characters to return; negative values omit characters from the left
	 *
	 * @return the extracted substring
	 */
	public String right( int count ) {
		if ( count == 0 ) {
			throw new ortus.boxlang.runtime.types.exceptions.BoxRuntimeException( "Count cannot be zero." );
		}
		if ( count > 0 ) {
			return this.buffer.substring( Math.max( 0, this.buffer.length() - count ) );
		}
		int start = -count;
		if ( start > this.buffer.length() ) {
			return this.buffer.toString();
		}
		return this.buffer.substring( start );
	}

	/**
	 * Returns a substring from the middle of the buffer using BoxLang's 1-based indexing.
	 *
	 * @param start the 1-based starting position
	 * @param count the number of characters to return; {@code null} means "to the end"
	 *
	 * @return the extracted substring
	 */
	public String mid( int start, Integer count ) {
		if ( start < 1 ) {
			start = 1;
		}
		if ( count == null ) {
			count = this.buffer.length();
		}
		if ( count < 1 ) {
			count = 0;
		}

		int end = start + count - 1;
		if ( start > this.buffer.length() ) {
			return "";
		}
		if ( end > this.buffer.length() ) {
			end = this.buffer.length();
		}
		return this.buffer.substring( start - 1, end );
	}

	/**
	 * Returns the 1-based position of the first occurrence of a substring.
	 *
	 * @param substring the substring to search for
	 * @param start     the 1-based start position
	 *
	 * @return 0 if not found
	 */
	public int find( CharSequence substring, int start ) {
		return find( substring, start, false );
	}

	/**
	 * Returns the 1-based position of the first occurrence of a substring.
	 *
	 * @param substring the substring to search for
	 * @param start     the 1-based start position
	 * @param noCase    whether to ignore case
	 *
	 * @return 0 if not found
	 */
	public int find( CharSequence substring, int start, boolean noCase ) {
		if ( start < 1 ) {
			start = 1;
		}
		String search = substring == null ? "" : substring.toString();
		if ( noCase ) {
			String	haystack	= this.buffer.toString().toLowerCase();
			int		position	= haystack.indexOf( search.toLowerCase(), start - 1 ) + 1;
			return Math.max( position, 0 );
		}
		int position = this.buffer.indexOf( search, start - 1 ) + 1;
		return Math.max( position, 0 );
	}

	/**
	 * Determines whether the buffer contains the specified substring.
	 *
	 * @param substring the substring to find
	 * @param noCase    whether to ignore case
	 *
	 * @return true when the substring exists
	 */
	public boolean contains( CharSequence substring, boolean noCase ) {
		return find( substring, 1, noCase ) > 0;
	}

	/**
	 * Determines whether the buffer starts with the specified prefix.
	 *
	 * @param prefix the prefix to check
	 *
	 * @return true when the buffer starts with the prefix
	 */
	public boolean startsWith( CharSequence prefix ) {
		String prefixString = prefix == null ? "" : prefix.toString();
		if ( prefixString.length() > this.buffer.length() ) {
			return false;
		}
		return this.buffer.substring( 0, prefixString.length() ).equals( prefixString );
	}

	/**
	 * Determines whether the buffer ends with the specified suffix.
	 *
	 * @param suffix the suffix to check
	 *
	 * @return true when the buffer ends with the suffix
	 */
	public boolean endsWith( CharSequence suffix ) {
		String suffixString = suffix == null ? "" : suffix.toString();
		if ( suffixString.length() > this.buffer.length() ) {
			return false;
		}
		return this.buffer.substring( this.buffer.length() - suffixString.length() ).equals( suffixString );
	}

	// -------------------------------------------------------------------------
	// Fluent mutation methods (all return {@code this})
	// -------------------------------------------------------------------------

	/**
	 * Appends the string representation of {@code value} to the end of the buffer.
	 *
	 * @param value any BoxLang value, coerced to string
	 *
	 * @return {@code this} for chaining
	 */
	public BoxStringBuilder append( Object value ) {
		this.buffer.append( value == null ? "" : StringCaster.cast( value ) );
		return this;
	}

	/**
	 * Inserts the string representation of {@code value} at the beginning of the buffer.
	 *
	 * @param value any BoxLang value, coerced to string
	 *
	 * @return {@code this} for chaining
	 */
	public BoxStringBuilder prepend( Object value ) {
		this.buffer.insert( 0, value == null ? "" : StringCaster.cast( value ) );
		return this;
	}

	/**
	 * Inserts {@code value} at the given 1-based {@code position}.
	 *
	 * @param position 1-based character position
	 * @param value    any BoxLang value, coerced to string
	 *
	 * @return {@code this} for chaining
	 *
	 * @throws ortus.boxlang.runtime.types.exceptions.BoxRuntimeException if position is out of range
	 */
public BoxStringBuilder insert( int position, Object value ) {
	int len = this.buffer.length();
	if ( position < 1 || position > len + 1 ) {
		throw new ortus.boxlang.runtime.types.exceptions.BoxRuntimeException( String.format( "Position [%d] is out of range. Valid range is 1..%d.", position, len + 1 ) );
	}
	this.buffer.insert( position - 1, value == null ? "" : StringCaster.cast( value ) );
	return this;
}

	/**
	 * Deletes characters from {@code start} to {@code end}, both 1-based and <em>inclusive</em>.
	 *
	 * @param start 1-based start position
	 * @param end   1-based end position (inclusive)
	 *
	 * @return {@code this} for chaining
	 */
	public BoxStringBuilder delete( int start, int end ) {
		this.buffer.delete( start - 1, end );
		return this;
	}

	/**
	 * Replaces characters from {@code start} to {@code end} (1-based, inclusive) with {@code value}.
	 *
	 * @param start 1-based start position
	 * @param end   1-based end position (inclusive)
	 * @param value replacement text; any BoxLang value, coerced to string
	 *
	 * @return {@code this} for chaining
	 */
	public BoxStringBuilder replace( int start, int end, Object value ) {
		this.buffer.replace( start - 1, end, value == null ? "" : StringCaster.cast( value ) );
		return this;
	}

	/**
	 * Reverses the contents of the buffer in place.
	 *
	 * @return {@code this} for chaining
	 */
	public BoxStringBuilder reverse() {
		this.buffer.reverse();
		return this;
	}

	/**
	 * Resets the buffer to empty without allocating a new internal object.
	 *
	 * @return {@code this} for chaining
	 */
	public BoxStringBuilder clear() {
		this.buffer.setLength( 0 );
		return this;
	}

	/**
	 * Strips leading and trailing whitespace from the buffer in place.
	 *
	 * @return {@code this} for chaining
	 */
	public BoxStringBuilder trim() {
		String trimmed = this.buffer.toString().trim();
		this.buffer.setLength( 0 );
		this.buffer.append( trimmed );
		return this;
	}

}
