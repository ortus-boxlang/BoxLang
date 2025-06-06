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
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.text.StringEscapeUtils;

/**
 * Abstract Source class to represent the origin of the code
 */
public abstract class Source implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * Abstract method to get the code as a stream
	 *
	 * @return
	 */
	public abstract Stream<String> getCodeAsStream();

	/**
	 * Abstract method to get the code
	 *
	 * @return The code
	 */
	public abstract String getCode();

	/**
	 * Get the surrounding lines of code, 2 before and 2 after the given line number
	 *
	 * @param lineNo The line number to get the surrounding lines for
	 * @param html   If true, the output will be formatted as HTML
	 *
	 * @return The surrounding lines of code
	 */
	public String getSurroundingLines( int lineNo, boolean html ) {
		// read file, if exists, and return the surrounding lines of code, 2 before and 2 after

		List<String>	lines		= getCodeAsStream()
		    .skip( Math.max( 0, lineNo - 3 ) )
		    .limit( 5 )
		    .collect( Collectors.toList() );

		StringBuilder	codeSnippet	= new StringBuilder();
		for ( int i = 0; i < lines.size(); i++ ) {
			String theLine = StringEscapeUtils.escapeHtml4( lines.get( i ) );
			if ( i == 2 && html ) {
				codeSnippet.append( "<b>" ).append( lineNo - 2 + i ).append( ": " ).append( theLine ).append( "</b>" ).append( "<br>" );
			} else {
				codeSnippet.append( lineNo - 2 + i ).append( ": " ).append( theLine ).append( html ? "<br>" : "\n" );
			}
		}

		return codeSnippet.toString();
	}

}
