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
package ortus.boxlang.compiler.ast;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a region of code within a text
 */
public class Position implements Serializable {

	private static final long	serialVersionUID	= 1L;

	private long				start;
	private long				end;
	private Source				source;
	private int					startIndex;
	private int					endIndex;

	/**
	 * Creates a position
	 *
	 * @param start the start position in the source code
	 * @param end   the end position in the source code
	 */
	public Position( Point start, Point end ) {
		this( start, end, null );
	}

	/**
	 * Creates a position including the file information
	 *
	 * @param start  the start position in the source code
	 * @param end    the end position in the source code
	 * @param source the source file reference
	 */
	public Position( Point start, Point end, Source source ) {
		this( start, end, source, -1, -1 );
	}

	/**
	 * Creates a position with an exclusive character range in its source.
	 *
	 * @param start      the start position in the source code
	 * @param end        the end position in the source code
	 * @param source     the source reference
	 * @param startIndex the inclusive source character index
	 * @param endIndex   the exclusive source character index
	 */
	public Position( Point start, Point end, Source source, int startIndex, int endIndex ) {
		this.start		= pack( start.getLine(), start.getColumn() );
		this.end		= pack( end.getLine(), end.getColumn() );
		this.source		= source;
		this.startIndex	= startIndex;
		this.endIndex	= endIndex;
	}

	/**
	 * Returns the start point
	 *
	 * @return the start point of the region
	 */
	public Point getStart() {
		return new PositionPoint( this, true );
	}

	/**
	 * Returns the end point
	 *
	 * @return the end point of the region
	 */
	public Point getEnd() {
		return new PositionPoint( this, false );
	}

	/**
	 * Set the end point
	 *
	 * @param end the end point of the region
	 */
	public void setEnd( Point end ) {
		this.end = pack( end.getLine(), end.getColumn() );
	}

	/**
	 * Extends this position through the end of another position while preserving
	 * its source range when both positions refer to the same source.
	 *
	 * @param endPosition position supplying the new end
	 */
	public void setEnd( Position endPosition ) {
		this.end = endPosition.end;
		if ( this.source == endPosition.source && this.startIndex >= 0 ) {
			this.endIndex = endPosition.endIndex;
		} else {
			this.startIndex	= -1;
			this.endIndex	= -1;
		}
	}

	/**
	 * Set the end point
	 *
	 * @param start the end point of the region
	 */
	public void setStart( Point start ) {
		this.start = pack( start.getLine(), start.getColumn() );
	}

	/**
	 * Returns the source of the position
	 *
	 * @return the start point of the region
	 *
	 * @see Source
	 */
	public Source getSource() {
		return source;
	}

	/**
	 * Set the source of the position
	 *
	 * @param source The source of the position (i.e. file)
	 *
	 * @see Source
	 */
	public void setSource( Source source ) {
		if ( this.source != source ) {
			this.startIndex	= -1;
			this.endIndex	= -1;
		}
		this.source = source;
	}

	/**
	 * Whether this position can resolve its original source text.
	 *
	 * @return true when a valid source range is available
	 */
	public boolean hasSourceText() {
		return this.source != null && this.startIndex >= 0 && this.endIndex >= this.startIndex;
	}

	/**
	 * Tests source text without allocating another source substring.
	 *
	 * @param text candidate source text
	 *
	 * @return true when the candidate equals this position's source range
	 */
	public boolean sourceTextEquals( String text ) {
		if ( text == null || !hasSourceText() ) {
			return false;
		}
		String	code		= this.source.getCode();
		int		charStart	= this.source.toCharIndex( this.startIndex );
		int		charEnd		= this.source.toCharIndex( this.endIndex );
		return text.length() == charEnd - charStart && charEnd <= code.length() && code.regionMatches( charStart, text, 0, text.length() );
	}

	/**
	 * Resolves this position's source range on demand.
	 *
	 * @return source text, or null when no source range is available
	 */
	public String getSourceText() {
		if ( !hasSourceText() ) {
			return null;
		}
		String	code		= this.source.getCode();
		int		charStart	= this.source.toCharIndex( this.startIndex );
		int		charEnd		= this.source.toCharIndex( this.endIndex );
		if ( charEnd > code.length() ) {
			return null;
		}
		return code.substring( charStart, charEnd );
	}

	/**
	 * String representation of the Position
	 *
	 * @return a String representation of the position including the source file if available
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if ( this.source != null ) {
			sb.append( this.getSource() );
			sb.append( ": " );
		}
		sb.append( unpackLine( this.start ) )
		    .append( "," )
		    .append( unpackColumn( this.start ) );
		sb.append( " - " );
		sb.append( unpackLine( this.end ) )
		    .append( "," )
		    .append( unpackColumn( this.end ) );

		return sb.toString();
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<String, Object>();

		map.put( "start", Map.of( "line", unpackLine( this.start ), "column", unpackColumn( this.start ) ) );
		map.put( "end", Map.of( "line", unpackLine( this.end ), "column", unpackColumn( this.end ) ) );
		return map;
	}

	private static long pack( int line, int column ) {
		return ( ( long ) line << 32 ) | ( column & 0xffffffffL );
	}

	private static int unpackLine( long point ) {
		return ( int ) ( point >> 32 );
	}

	private static int unpackColumn( long point ) {
		return ( int ) point;
	}

	private static class PositionPoint extends Point {

		private static final long	serialVersionUID	= 1L;

		private final Position		position;
		private final boolean		start;

		private PositionPoint( Position position, boolean start ) {
			super( unpackLine( start ? position.start : position.end ), unpackColumn( start ? position.start : position.end ) );
			this.position	= position;
			this.start		= start;
		}

		@Override
		public int getLine() {
			return unpackLine( this.start ? this.position.start : this.position.end );
		}

		@Override
		public int getColumn() {
			return unpackColumn( this.start ? this.position.start : this.position.end );
		}

		@Override
		public Point setColumn( int column ) {
			if ( this.start ) {
				this.position.start = pack( getLine(), column );
			} else {
				this.position.end = pack( getLine(), column );
			}
			return this;
		}

		@Override
		public Point setLine( int line ) {
			if ( this.start ) {
				this.position.start = pack( line, getColumn() );
			} else {
				this.position.end = pack( line, getColumn() );
			}
			return this;
		}

		@Override
		public Map<String, Object> toMap() {
			return Map.of( "line", getLine(), "column", getColumn() );
		}
	}

}
