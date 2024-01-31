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
package ortus.boxlang.ast;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a region of code within a text
 */
public class Position {

	private Point	start;
	private Point	end;
	private Source	source;

	/**
	 * Creates a position
	 *
	 * @param start the start position in the source code
	 * @param end   the end position in the source code
	 */
	public Position( Point start, Point end ) {
		this.start	= start;
		this.end	= end;
		this.source	= null;
	}

	/**
	 * Creates a position including the file information
	 *
	 * @param start  the start position in the source code
	 * @param end    the end position in the source code
	 * @param source the source file reference
	 */
	public Position( Point start, Point end, Source source ) {
		this.start	= start;
		this.end	= end;
		this.source	= source;
	}

	/**
	 * Returns the start point
	 *
	 * @return the start point of the region
	 */
	public Point getStart() {
		return start;
	}

	/**
	 * Returns the end point
	 *
	 * @return the end point of the region
	 */
	public Point getEnd() {
		return end;
	}

	/**
	 * Set the end point
	 * 
	 * @param end the end point of the region
	 */
	public void setEnd( Point end ) {
		this.end = end;
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
		this.source = source;
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
		sb.append( this.getStart().getLine() )
		    .append( "," )
		    .append( this.getStart().getColumn() );
		sb.append( " - " );
		sb.append( this.getEnd().getLine() )
		    .append( "," )
		    .append( this.getEnd().getColumn() );

		return sb.toString();
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<String, Object>();

		map.put( "start", start.toMap() );
		map.put( "end", end.toMap() );
		return map;
	}

}
