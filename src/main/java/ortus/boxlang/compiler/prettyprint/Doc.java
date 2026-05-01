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
package ortus.boxlang.compiler.prettyprint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSON.Feature;

public class Doc {

	private DocType			docType;
	private List<Object>	contents;
	private boolean			containsDoc;
	private boolean			willBreak;
	private boolean			isAppended;

	public Doc( DocType docType ) {
		this.docType		= docType;
		this.contents		= new ArrayList<>();
		this.containsDoc	= false;
		this.willBreak		= false;
		this.isAppended		= false;
	}

	public void setIsAppended() {
		this.isAppended = true;
	}

	public DocType getDocType() {
		return docType;
	}

	public boolean willBreak() {
		return willBreak;
	}

	public List<Object> getContents() {
		return contents;
	}

	public boolean isEmpty() {
		return contents.isEmpty();
	}

	public boolean isSimple() {
		return docType == DocType.ARRAY && !containsDoc;
	}

	public Doc append( String str ) {
		validateAppend();
		contents.add( str );
		return this;
	}

	public Doc append( Line line ) {
		validateAppend();
		contents.add( line );
		return this;
	}

	public Doc append( Doc doc ) {
		validateAppend();
		doc.setIsAppended();
		if ( doc.isSimple() ) {
			contents.addAll( doc.getContents() );
		} else {
			contents.add( doc );
			containsDoc = true;
		}
		return this;
	}

	/**
	 * I propagate the willBreak flag up the tree to ensure that if any child node
	 * will break, the parent will also be marked as willBreak.
	 * This is technically only applicable to DocType.GROUP, but easier to
	 * set on all types and ignore the flag for other types.
	 */
	public void propagateWillBreak() {
		for ( Object element : contents ) {
			if ( element instanceof Doc childDoc ) {
				childDoc.propagateWillBreak();
				if ( childDoc.willBreak() ) {
					willBreak = true;
				}
			} else if ( element instanceof Line line && ( line == Line.HARD || line == Line.BREAK_PARENT ) ) {
				willBreak = true;
			}
		}
	}

	/**
	 * I condense my contents to combine consecutive strings into a single string.
	 */
	public void condense() {
		List<Object>	condensed		= new ArrayList<>();
		StringBuilder	currentString	= new StringBuilder();

		for ( Object element : contents ) {
			if ( element instanceof String str ) {
				currentString.append( str );
			} else {
				if ( currentString.length() > 0 ) {
					condensed.add( currentString.toString() );
					currentString.setLength( 0 );
				}
				if ( element instanceof Doc doc ) {
					doc.condense(); // Condense sub-nodes
				}
				condensed.add( element );
			}
		}

		if ( currentString.length() > 0 ) {
			condensed.add( currentString.toString() );
		}

		contents = condensed;
	}

	/**
	 * Converts the Doc object to a Map representation for JSON serialization.
	 * 
	 * @return A Map containing the Doc's properties and contents.
	 */
	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "docType", docType.toString() );
		map.put( "willBreak", willBreak );
		List<Object> contentsList = new ArrayList<>();
		for ( Object element : contents ) {
			if ( element instanceof Doc doc ) {
				contentsList.add( doc.toMap() );
			} else if ( element instanceof Line ) {
				// for a line enum, add a map with a type of "line" and the value of the line
				Map<String, String> lineMap = new LinkedHashMap<>();
				lineMap.put( "type", "LINE" );
				lineMap.put( "value", element.toString() );
				contentsList.add( lineMap );
			} else {
				contentsList.add( element );
			}
		}
		map.put( "contents", contentsList );
		return map;
	}

	/**
	 * Converts the Doc object to a JSON string.
	 * 
	 * @return A JSON string representation of the Doc object.
	 * 
	 * @throws RuntimeException if JSON conversion fails.
	 */
	public String toJSON() {
		try {
			return JSON.std
			    .with( Feature.PRETTY_PRINT_OUTPUT, Feature.WRITE_NULL_PROPERTIES )
			    .asString( toMap() );
		} catch ( IOException e ) {
			throw new RuntimeException( "Failed to convert to JSON", e );
		}
	}

	/**
	 * Returns a string representation of the document.
	 *
	 * @return Formatted string representation
	 */
	@Override
	public String toString() {
		return contents.stream()
		    .map( element -> switch ( element ) {
			    case String str -> "`" + str + "`\n";
			    case Doc doc -> doc.toString().replace( "\n", "\n  " ) + "\n";
			    case Line line -> "Line." + line + "\n";
			    default -> element.toString() + "\n";
		    } )
		    .collect( Collectors.joining( "",
		        docType + ( docType == DocType.GROUP ? "[willBreak=" + willBreak + "]" : "" ) + " {\n",
		        "}" ) );
	}

	private void validateAppend() {
		if ( isAppended ) {
			throw new IllegalStateException( "Cannot append to a Doc that has been appended to a parent" );
		}
	}
}
