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
 * Represents a position in the source code with line and column
 */
public class Point {

	private final int	line;
	private final int	column;

	/**
	 * Create a point
	 *
	 * @param line   line withing the code
	 * @param column
	 */
	public Point( int line, int column ) {
		this.line	= line;
		this.column	= column;
	}

	/**
	 * Returns the line
	 *
	 * @return line in the code
	 */
	public int getLine() {
		return line;
	}

	/**
	 * Returns the column
	 *
	 * @return column in the code
	 */
	public int getColumn() {
		return column;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<String, Object>();

		map.put( "line", line );
		map.put( "column", column );
		return map;
	}
}
