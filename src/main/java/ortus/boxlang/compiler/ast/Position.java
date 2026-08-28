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
	private final boolean		compactable;

	/**
	 * Creates a position
	 *
	 * @param start the start position in the source code
	 * @param end   the end position in the source code
	 */
	public Position( Point start, Point end ) {
		this( start.getLine(), start.getColumn(), end.getLine(), end.getColumn() );
	}

	/**
	 * Creates a position from primitive coordinates.
	 *
	 * @param startLine   the start line
	 * @param startColumn the start column
	 * @param endLine     the end line
	 * @param endColumn   the end column
	 */
	public Position( int startLine, int startColumn, int endLine, int endColumn ) {
		this( startLine, startColumn, endLine, endColumn, null );
	}

	/**
	 * Creates a position including the file information
	 *
	 * @param start  the start position in the source code
	 * @param end    the end position in the source code
	 * @param source the source file reference
	 */
	public Position( Point start, Point end, Source source ) {
		this( start.getLine(), start.getColumn(), end.getLine(), end.getColumn(), source );
	}

	/**
	 * Creates a position from primitive coordinates including source information.
	 *
	 * @param startLine   the start line
	 * @param startColumn the start column
	 * @param endLine     the end line
	 * @param endColumn   the end column
	 * @param source      the source reference
	 */
	public Position( int startLine, int startColumn, int endLine, int endColumn, Source source ) {
		this( startLine, startColumn, endLine, endColumn, source, -1, -1 );
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
		this( start.getLine(), start.getColumn(), end.getLine(), end.getColumn(), source, startIndex, endIndex );
	}

	/**
	 * Creates a position from primitive coordinates with an exclusive character range in its source.
	 *
	 * @param startLine   the start line
	 * @param startColumn the start column
	 * @param endLine     the end line
	 * @param endColumn   the end column
	 * @param source      the source reference
	 * @param startIndex  the inclusive source character index
	 * @param endIndex    the exclusive source character index
	 */
	public Position( int startLine, int startColumn, int endLine, int endColumn, Source source, int startIndex, int endIndex ) {
		this( startLine, startColumn, endLine, endColumn, source, startIndex, endIndex, false );
	}

	private Position( int startLine, int startColumn, int endLine, int endColumn, Source source, int startIndex, int endIndex, boolean compactable ) {
		this.start			= pack( startLine, startColumn );
		this.end			= pack( endLine, endColumn );
		this.source			= source;
		this.startIndex		= startIndex;
		this.endIndex		= endIndex;
		this.compactable	= compactable;
	}

	/**
	 * Creates a position intended for compact storage by an AST node.
	 *
	 * @param startLine   the start line
	 * @param startColumn the start column
	 * @param endLine     the end line
	 * @param endColumn   the end column
	 * @param source      the source reference
	 * @param startIndex  the inclusive source character index
	 * @param endIndex    the exclusive source character index
	 *
	 * @return a compactable position
	 */
	public static Position compact( int startLine, int startColumn, int endLine, int endColumn, Source source, int startIndex, int endIndex ) {
		return new Position( startLine, startColumn, endLine, endColumn, source, startIndex, endIndex, true );
	}

	/**
	 * Creates a position intended for compact storage by an AST node.
	 *
	 * @param startLine   the start line
	 * @param startColumn the start column
	 * @param endLine     the end line
	 * @param endColumn   the end column
	 * @param source      the source reference
	 *
	 * @return a compactable position
	 */
	public static Position compact( int startLine, int startColumn, int endLine, int endColumn, Source source ) {
		return compact( startLine, startColumn, endLine, endColumn, source, -1, -1 );
	}

	/**
	 * Creates a position intended for compact storage by an AST node.
	 *
	 * @param start the start position in the source code
	 * @param end   the end position in the source code
	 *
	 * @return a compactable position
	 */
	public static Position compact( Point start, Point end ) {
		return compact( start.getLine(), start.getColumn(), end.getLine(), end.getColumn(), null );
	}

	/**
	 * Creates a position intended for compact storage by an AST node.
	 *
	 * @param start  the start position in the source code
	 * @param end    the end position in the source code
	 * @param source the source reference
	 *
	 * @return a compactable position
	 */
	public static Position compact( Point start, Point end, Source source ) {
		return compact( start.getLine(), start.getColumn(), end.getLine(), end.getColumn(), source );
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
		setPackedEnd( pack( end.getLine(), end.getColumn() ) );
	}

	/**
	 * Extends this position through the end of another position while preserving
	 * its source range when both positions refer to the same source.
	 *
	 * @param endPosition position supplying the new end
	 */
	public void setEnd( Position endPosition ) {
		setPackedEnd( endPosition.getPackedEnd() );
		if ( getPositionSource() == endPosition.getPositionSource() && getStartIndex() >= 0 ) {
			setEndIndex( endPosition.getEndIndex() );
		} else {
			setStartIndex( -1 );
			setEndIndex( -1 );
		}
	}

	/**
	 * Set the end point
	 *
	 * @param start the end point of the region
	 */
	public void setStart( Point start ) {
		setPackedStart( pack( start.getLine(), start.getColumn() ) );
	}

	/**
	 * Returns the source of the position
	 *
	 * @return the start point of the region
	 *
	 * @see Source
	 */
	public Source getSource() {
		return getPositionSource();
	}

	/**
	 * Set the source of the position
	 *
	 * @param source The source of the position (i.e. file)
	 *
	 * @see Source
	 */
	public void setSource( Source source ) {
		if ( getPositionSource() != source ) {
			setStartIndex( -1 );
			setEndIndex( -1 );
		}
		setPositionSource( source );
	}

	/**
	 * Whether this position can resolve its original source text.
	 *
	 * @return true when a valid source range is available
	 */
	public boolean hasSourceText() {
		return getPositionSource() != null && getStartIndex() >= 0 && getEndIndex() >= getStartIndex();
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
		Source	source		= getPositionSource();
		String	code		= source.getCode();
		int		charStart	= source.toCharIndex( getStartIndex() );
		int		charEnd		= source.toCharIndex( getEndIndex() );
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
		Source	source		= getPositionSource();
		String	code		= source.getCode();
		int		charStart	= source.toCharIndex( getStartIndex() );
		int		charEnd		= source.toCharIndex( getEndIndex() );
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
		long			start	= getPackedStart();
		long			end		= getPackedEnd();
		StringBuilder	sb		= new StringBuilder();
		if ( getPositionSource() != null ) {
			sb.append( this.getSource() );
			sb.append( ": " );
		}
		sb.append( unpackLine( start ) )
		    .append( "," )
		    .append( unpackColumn( start ) );
		sb.append( " - " );
		sb.append( unpackLine( end ) )
		    .append( "," )
		    .append( unpackColumn( end ) );

		return sb.toString();
	}

	public Map<String, Object> toMap() {
		long				start	= getPackedStart();
		long				end		= getPackedEnd();
		Map<String, Object>	map		= new HashMap<String, Object>();

		map.put( "start", Map.of( "line", unpackLine( start ), "column", unpackColumn( start ) ) );
		map.put( "end", Map.of( "line", unpackLine( end ), "column", unpackColumn( end ) ) );
		return map;
	}

	/**
	 * Returns a position that can be retained independently of its owner.
	 *
	 * @return this position when already independent, or a detached copy otherwise
	 */
	public Position snapshot() {
		return this;
	}

	protected long getPackedStart() {
		return this.start;
	}

	protected void setPackedStart( long start ) {
		this.start = start;
	}

	protected long getPackedEnd() {
		return this.end;
	}

	protected void setPackedEnd( long end ) {
		this.end = end;
	}

	protected Source getPositionSource() {
		return this.source;
	}

	protected void setPositionSource( Source source ) {
		this.source = source;
	}

	protected int getStartIndex() {
		return this.startIndex;
	}

	protected void setStartIndex( int startIndex ) {
		this.startIndex = startIndex;
	}

	protected int getEndIndex() {
		return this.endIndex;
	}

	protected void setEndIndex( int endIndex ) {
		this.endIndex = endIndex;
	}

	protected boolean isCompactable() {
		return this.compactable;
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
			super( unpackLine( start ? position.getPackedStart() : position.getPackedEnd() ),
			    unpackColumn( start ? position.getPackedStart() : position.getPackedEnd() ) );
			this.position	= position;
			this.start		= start;
		}

		@Override
		public int getLine() {
			return unpackLine( this.start ? this.position.getPackedStart() : this.position.getPackedEnd() );
		}

		@Override
		public int getColumn() {
			return unpackColumn( this.start ? this.position.getPackedStart() : this.position.getPackedEnd() );
		}

		@Override
		public Point setColumn( int column ) {
			if ( this.start ) {
				this.position.setPackedStart( pack( getLine(), column ) );
			} else {
				this.position.setPackedEnd( pack( getLine(), column ) );
			}
			return this;
		}

		@Override
		public Point setLine( int line ) {
			if ( this.start ) {
				this.position.setPackedStart( pack( line, getColumn() ) );
			} else {
				this.position.setPackedEnd( pack( line, getColumn() ) );
			}
			return this;
		}

		@Override
		public Map<String, Object> toMap() {
			return Map.of( "line", getLine(), "column", getColumn() );
		}
	}

}
