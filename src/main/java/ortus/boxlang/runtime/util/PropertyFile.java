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
package ortus.boxlang.runtime.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ortus.boxlang.runtime.bifs.global.string.LTrim;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.BLCollector;

/**
 * A fluent API for managing property files in BoxLang.
 */
public class PropertyFile {

	/**
	 * The line separator used for this property file.
	 * Defaults to the system line separator.
	 */
	private static final String		LINE_SEPARATOR		= System.lineSeparator();

	/**
	 * A regex pattern to match property delimiters.
	 * Matches =, :, or whitespace followed by a non-delimiter character.
	 */
	private static final Pattern	DELIMITER_PATTERN	= Pattern.compile( "(?<!\\\\)(=|:|\\s(?=[^=:]))" );

	/**
	 * ------------------------------------------------------------------------
	 * Properties
	 * ------------------------------------------------------------------------
	 */

	/**
	 * The path to the property file.
	 */
	private String					path;

	/**
	 * The list of lines in the property file.
	 * This includes comments, whitespace, and property lines.
	 * Each line is represented by a PropertyLine object.
	 */
	private List<IStruct>			lines;

	/**
	 * The maximum line width for formatting long property values.
	 * If a property value exceeds this width, it will be broken into multiple lines.
	 * Default is set to 150 characters.
	 */
	private int						maxLineWidth		= 150;

	/**
	 * Cache for the IStruct representation of the property file.
	 */
	private volatile IStruct		cachedStruct		= null;
	private volatile boolean		structCacheDirty	= true;

	/**
	 * ------------------------------------------------------------------------
	 * Constructors
	 * ------------------------------------------------------------------------
	 */

	/**
	 * Constructor - initializes a new PropertyFile instance
	 * Sets default values for maxLineWidth (150), empty lines list,
	 * and uses system line separator
	 */
	public PropertyFile() {
		this.lines = new ArrayList<>();
	}

	/**
	 * ------------------------------------------------------------------------
	 * Static Constructors
	 * ------------------------------------------------------------------------
	 */

	/**
	 * Creates a new PropertyFile from a Map
	 *
	 * @param properties The properties as a Map
	 *
	 * @return A new PropertyFile instance
	 */
	public static PropertyFile fromMap( Map<String, String> properties ) {
		return new PropertyFile().setMulti( properties );
	}

	/**
	 * Creates a new PropertyFile from an IStruct
	 *
	 * @param properties The properties as an IStruct
	 *
	 * @return A new PropertyFile instance
	 */
	public static PropertyFile fromIStruct( IStruct properties ) {
		return new PropertyFile().setMulti( properties );
	}

	/**
	 * ------------------------------------------------------------------------
	 * Fluent Methods
	 * ------------------------------------------------------------------------
	 */

	/**
	 * Loads a property file from the specified path using BoxLang FileSystemUtil
	 *
	 * @param path A fully qualified path to a property file
	 *
	 * @throws BoxRuntimeException If the file does not exist or cannot be read
	 *
	 * @return This PropertyFile instance for method chaining
	 */
	public PropertyFile load( String path ) {
		// Path Guards
		Objects.requireNonNull( path, "Path cannot be null" );
		if ( path.isBlank() ) {
			throw new BoxRuntimeException( "Path cannot be empty" );
		}
		if ( !FileSystemUtil.exists( path ) ) {
			throw new BoxRuntimeException( "Property file does not exist at path: " + path );
		}
		this.path = path;

		// Load and normalize file contents
		String		fileContents	= ( ( String ) FileSystemUtil.read( this.path ) )
		    .replace( "\r\n", "\n" )
		    .replace( "\r", "\n" );

		String[]	fileLines		= fileContents.split( "\n", -1 );
		int			lineNo			= 0;
		var			continuedLine	= new StringBuilder();
		var			originalLine	= new StringBuilder();

		for ( String line : fileLines ) {
			lineNo++;
			originalLine.append( line );

			// Apply LTrim to get the logical content
			String trimmedContent = LTrim.apply( line );
			continuedLine.append( trimmedContent );

			// Check for line continuation - but only on non-empty trimmed lines
			if ( !trimmedContent.isEmpty() && trimmedContent.endsWith( "\\" ) ) {
				// Remove continuation character and continue to next line
				continuedLine.setLength( continuedLine.length() - 1 );
				originalLine.append( "\n" );
				continue;
			}

			// Process the complete line (including empty lines)
			addLine(
			    continuedLine.toString(),
			    lineNo,
			    originalLine.toString().replace( "\n", LINE_SEPARATOR )
			);

			// Reset for next line
			originalLine.setLength( 0 );
			continuedLine.setLength( 0 );
		}

		return this;
	}

	/**
	 * Stores the property file to the specified path, if it doesn't exist, it will create the directory structure.
	 *
	 * @param path The path to store the file (optional, uses loaded path if not provided)
	 *
	 * @return This PropertyFile instance for method chaining
	 */
	public PropertyFile store( String path ) {
		if ( path == null || path.isBlank() ) {
			path = this.path;
		}

		FileSystemUtil.createDirectoryIfMissing( path );
		FileSystemUtil.write( path, getLinesAsText() );

		return this;
	}

	/**
	 * Stores the property file to the currently loaded path
	 *
	 * @return This PropertyFile instance for method chaining
	 */
	public PropertyFile store() {
		return store( this.path );
	}

	/**
	 * Gets a property value by name
	 *
	 * @param name The property name
	 *
	 * @return The property value
	 *
	 * @throws IllegalArgumentException If the key doesn't exist
	 */
	public String get( String name ) {
		IStruct data = getAsStruct();
		if ( data.containsKey( Key.of( name ) ) ) {
			return data.getAsString( Key.of( name ) );
		}

		throw new IllegalArgumentException(
		    "Key [" + name + "] does not exist in this properties file. Valid keys are " +
		        String.join( ", ", data.getKeysAsStrings() )
		);
	}

	/**
	 * Gets a property value by name with a default value
	 *
	 * @param name         The property name
	 * @param defaultValue The default value if the key doesn't exist
	 *
	 * @return The property value or default value
	 */
	public String get( String name, String defaultValue ) {
		IStruct data = getAsStruct();
		return data.containsKey( Key.of( name ) ) ? data.getAsString( Key.of( name ) ) : defaultValue;
	}

	/**
	 * Adds a comment line to the property file
	 *
	 * @param comment The comment text (without # prefix)
	 *
	 * @return This PropertyFile instance for method chaining
	 */
	public PropertyFile addComment( String comment ) {
		IStruct commentLine = Struct.of(
		    Key.type, "comment",
		    Key.value, "# " + comment,
		    Key.lineNumber, lines.size() + 1
		);
		lines.add( commentLine );
		return this;
	}

	/**
	 * Adds a blank line to the property file
	 *
	 * @return This PropertyFile instance for method chaining
	 */
	public PropertyFile addBlankLine() {
		IStruct blankLine = Struct.of(
		    Key.type, "whitespace",
		    Key.value, "",
		    Key.lineNumber, lines.size() + 1
		);
		lines.add( blankLine );
		return this;
	}

	/**
	 * Sets a property value
	 *
	 * @param name  The property name
	 * @param value The property value
	 *
	 * @return This PropertyFile instance for method chaining
	 */
	public PropertyFile set( String name, String value ) {
		int lineIndex = findLineIndex( name );

		if ( lineIndex >= 0 ) {
			IStruct lineData = lines.get( lineIndex );
			if ( !lineData.get( Key.value ).equals( value ) ) {
				lineData.put( Key.value, value );
				lineData.remove( Key.originalLine ); // Remove original line to mark as modified
			}
		}
		// If the property does not exist, create a new one
		else {
			IStruct newLine = Struct.of(
			    Key.type, "property",
			    Key._name, name,
			    Key.value, value,
			    Key.delimiter, "=",
			    Key.lineNumber, lines.size() + 1
			);
			lines.add( newLine );
		}
		this.structCacheDirty = true;
		return this;
	}

	/**
	 * Sets multiple property values at once
	 *
	 * @param properties An IStruct containing key-value pairs to set
	 *
	 * @return This PropertyFile instance for method chaining
	 */
	public PropertyFile setMulti( IStruct properties ) {
		properties.entrySet().forEach( entry -> {
			set( entry.getKey().getName(), entry.getValue().toString() );
		} );
		return this;
	}

	/**
	 * Sets multiple property values at once from a Map
	 *
	 * @param properties A Map containing key-value pairs to set
	 *
	 * @return This PropertyFile instance for method chaining
	 */
	public PropertyFile setMulti( Map<String, String> properties ) {
		properties.entrySet().forEach( entry -> {
			set( entry.getKey(), entry.getValue() );
		} );
		return this;
	}

	/**
	 * Removes a property
	 *
	 * @param name The property name to remove
	 *
	 * @return This PropertyFile instance for method chaining
	 */
	public PropertyFile remove( String name ) {
		int lineIndex = findLineIndex( name );

		if ( lineIndex >= 0 ) {
			lines.remove( lineIndex );
			this.structCacheDirty = true;
		}

		return this;
	}

	/**
	 * Removes multiple properties at once
	 *
	 * @param names The property names to remove
	 *
	 * @return This PropertyFile instance for method chaining
	 */
	public PropertyFile removeMulti( Array names ) {
		names.forEach( name -> remove( name.toString() ) );
		this.structCacheDirty = true;
		return this;
	}

	/**
	 * Checks if a property exists
	 *
	 * @param name The property name
	 *
	 * @return true if the property exists, false otherwise
	 */
	public boolean exists( String name ) {
		return findLineIndex( name ) >= 0;
	}

	/**
	 * Checks if the property file has any properties
	 *
	 * @return true if the property file contains at least one property, false otherwise
	 */
	public boolean hasProperties() {
		return this.lines.stream().anyMatch( line -> "property".equals( line.get( Key.type ) ) );
	}

	/**
	 * Gets the number of properties in the file
	 *
	 * @return The count of properties
	 */
	public int size() {
		return ( int ) this.lines.stream().filter( line -> "property".equals( line.get( Key.type ) ) ).count();
	}

	/**
	 * Clears all properties while preserving comments and whitespace
	 *
	 * @return This PropertyFile instance for method chaining
	 */
	public PropertyFile clearProperties() {
		this.lines.removeIf( line -> "property".equals( line.get( Key.type ) ) );
		this.structCacheDirty = true;
		return this;
	}

	/**
	 * Clears everything including comments and whitespace
	 *
	 * @return This PropertyFile instance for method chaining
	 */
	public PropertyFile clear() {
		this.lines.clear();
		this.cachedStruct		= null;
		this.structCacheDirty	= true;
		return this;
	}

	/**
	 * Gets all properties as a BoxLang IStruct
	 * Uses LinkedStruct to preserve property order as they appear in the file
	 *
	 * @return An IStruct containing all properties with insertion order preserved
	 */
	public IStruct getAsStruct() {
		if ( this.cachedStruct == null || this.structCacheDirty ) {
			synchronized ( this ) {
				this.cachedStruct = new Struct( IStruct.TYPES.LINKED );
				for ( IStruct line : lines ) {
					if ( "property".equals( line.get( Key.type ) ) ) {
						this.cachedStruct.put( Key.of( line.get( Key._name ) ), line.get( Key.value ) );
					}
				}
				this.structCacheDirty = false;
			}
		}
		return this.cachedStruct;
	}

	/**
	 * Gets all properties as a standard Java Map
	 * Properties are returned in the order they appear in the file
	 *
	 * @return A LinkedHashMap containing all properties with insertion order preserved
	 */
	public Map<String, String> getAsMap() {
		Map<String, String> result = new LinkedHashMap<>();

		for ( IStruct line : lines ) {
			if ( "property".equals( line.get( Key.type ) ) ) {
				result.put( line.getAsString( Key._name ), line.getAsString( Key.value ) );
			}
		}

		return result;
	}

	/**
	 * Merges properties from an IStruct into this PropertyFile
	 *
	 * @param incomingStruct The IStruct containing properties to merge
	 *
	 * @return This PropertyFile instance for method chaining
	 */
	public PropertyFile mergeStruct( IStruct incomingStruct ) {
		incomingStruct.entrySet().forEach( entry -> {
			set( entry.getKey().getName(), entry.getValue().toString() );
		} );
		return this;
	}

	/**
	 * Merges properties from a Map into this PropertyFile
	 *
	 * @param incomingMap The Map containing properties to merge
	 *
	 * @return This PropertyFile instance for method chaining
	 */
	public PropertyFile mergeMap( Map<String, String> incomingMap ) {
		incomingMap.entrySet().forEach( entry -> {
			set( entry.getKey(), entry.getValue() );
		} );
		return this;
	}

	/**
	 * Gets all property names
	 *
	 * @return An array of property names
	 */
	public Array getPropertyNames() {
		return this.lines
		    .stream()
		    .filter( line -> "property".equals( line.get( Key.type ) ) )
		    .map( line -> line.getAsString( Key._name ) )
		    .collect( BLCollector.toArray() );
	}

	// Private helper methods

	/**
	 * Adds a line from the source file to the internal structure
	 * Parses the line content to determine if it's a comment, whitespace, or property
	 *
	 * @param contents     The trimmed content of the line
	 * @param lineNo       The source line number for error reporting
	 * @param originalLine The original line content including formatting
	 *
	 * @throws IllegalArgumentException If the property line format is invalid
	 */
	private void addLine( String contents, int lineNo, String originalLine ) {
		IStruct line = Struct.of(
		    Key.line, "",
		    Key.value, "",
		    Key._name, "",
		    Key.delimiter, "",
		    Key.lineNumber, lineNo,
		    Key.originalLine, originalLine
		);

		// Check if the line is a comment or whitespace
		if ( contents.startsWith( "#" ) || contents.startsWith( "!" ) ) {
			line.put( Key.type, "comment" );
			line.put( Key.value, contents );
		}
		// Check for whitespace
		else if ( contents.trim().isEmpty() ) {
			line.put( Key.type, "whitespace" );
			line.put( Key.value, contents );
		}
		// Otherwise, treat it as a property line
		else {
			// Look for the first = or : or <space> that is not escaped by a \
			// unless it's a space immediatley followed by a = or :, in which case nevermind, use the = or :
			// foo=bar
			// foo:bar
			// foo bar
			// foo = bar
			// foo : bar
			// foo \bar=baz -> kay name is "foo bar"
			Matcher matcher = DELIMITER_PATTERN.matcher( contents );
			if ( matcher.find() ) {
				int		delimIndex	= matcher.start();
				char	delimiter	= contents.charAt( delimIndex );

				String	keyPart		= contents.substring( 0, delimIndex ).trim();
				String	valuePart	= contents.substring( delimIndex + 1 );

				// Handle comments at end of value - find first unescaped # or !
				String	actualValue	= extractValueBeforeComment( valuePart );

				line.put( Key._name, unEscapeToken( keyPart ) );
				line.put( Key.delimiter, String.valueOf( delimiter ) );
				line.put( Key.value, unEscapeToken( actualValue.trim() ) );
				line.put( Key.type, "property" );
			}
			// If no delimiter is found, treat the whole line as a key with empty value
			else if ( !contents.isEmpty() ) {
				line.put( Key._name, unEscapeToken( contents.trim() ) );
				line.put( Key.delimiter, "=" );
				line.put( Key.value, "" );
				line.put( Key.type, "property" );
			} else {
				throw new IllegalArgumentException( "Invalid property file format, line " + lineNo );
			}
		}
		lines.add( line );
	}

	/**
	 * Extracts the value part of a property line before any comment marker (# or !)
	 *
	 * @param valuePart The value part of the property line
	 *
	 * @return The extracted value before any comment marker
	 */
	private String extractValueBeforeComment( String valuePart ) {
		StringBuilder	result	= new StringBuilder();
		boolean			escaped	= false;

		for ( int i = 0; i < valuePart.length(); i++ ) {
			char ch = valuePart.charAt( i );

			if ( escaped ) {
				result.append( ch );
				escaped = false;
			} else if ( ch == '\\' ) {
				result.append( ch );
				escaped = true;
			} else if ( ch == '#' || ch == '!' ) {
				// Found unescaped comment marker - stop processing
				break;
			} else {
				result.append( ch );
			}
		}

		return result.toString();
	}

	/**
	 * Unescapes tokens according to property file standards
	 * Handles escape sequences like <code>\t, \n, \r, \f, \\, \", \', and \\uxxxx</code>
	 *
	 * @param token The token string to unescape
	 *
	 * @return The unescaped string with proper character substitutions
	 *
	 * @throws IllegalArgumentException If unicode escape sequences are malformed
	 */
	private String unEscapeToken( String token ) {
		StringBuilder	result			= new StringBuilder();
		boolean			escaped			= false;
		boolean			unicode			= false;
		StringBuilder	unicodeString	= new StringBuilder();

		for ( char ch : token.toCharArray() ) {
			if ( !escaped && !unicode && ch == '\\' ) {
				escaped = true;
			} else if ( escaped && !unicode ) {
				switch ( ch ) {
					case 't' :
						result.append( '\t' );
						break;
					case 'n' :
						result.append( '\n' );
						break;
					case 'r' :
						result.append( '\r' );
						break;
					case 'f' :
						result.append( '\f' );
						break;
					case '\\' :
						result.append( '\\' );
						break;
					case '"' :
						result.append( '"' );
						break;
					case '\'' :
						result.append( '\'' );
						break;
					case 'u' :
						unicode = true;
						break;
					default :
						result.append( ch );
						break;
				}
				escaped = false;
			} else if ( unicode ) {
				if ( Character.digit( ch, 16 ) == -1 ) {
					throw new IllegalArgumentException(
					    "Invalid unicode character [" + ch + "] in token [" + token + "]" );
				}
				unicodeString.append( ch );
				if ( unicodeString.length() == 4 ) {
					result.append( ( char ) Integer.parseInt( unicodeString.toString(), 16 ) );
					unicodeString.setLength( 0 );
					unicode = false;
				}
			} else {
				result.append( ch );
			}
		}

		if ( unicode ) {
			throw new IllegalArgumentException( "Incomplete unicode escape at the end of token [" + token + "]" );
		}

		return result.toString();
	}

	/**
	 * Escapes tokens for writing to property files
	 * Converts special characters to their escaped equivalents
	 *
	 * @param token      The string token to escape
	 * @param escapeName Whether to escape name-specific characters (space, =, :)
	 *
	 * @return The escaped token suitable for property file format
	 */
	private String escapeToken( String token, boolean escapeName ) {
		StringBuilder result = new StringBuilder();

		for ( char ch : token.toCharArray() ) {
			result.append( escapeChar( ch, escapeName ) );
		}

		return result.toString();
	}

	/**
	 * Escapes individual characters according to property file standards
	 * Handles special characters like tab, newline, quotes, backslash, etc.
	 *
	 * @param ch         The character to escape
	 * @param escapeName Whether to escape name-specific characters (space, =, :)
	 *
	 * @return The escaped character sequence as a string
	 */
	private String escapeChar( char ch, boolean escapeName ) {
		int codePoint = ( int ) ch;

		switch ( codePoint ) {
			case 9 :
				return "\\t";
			case 10 :
				return "\\n";
			case 13 :
				return "\\r";
			case 12 :
				return "\\f";
			case 34 :
				return "\\\"";
			case 39 :
				return "\\'";
			case 92 :
				return "\\\\";
			case 32 :
				return escapeName ? "\\ " : " ";
			case 61 :
				return escapeName ? "\\=" : "=";
			case 58 :
				return escapeName ? "\\:" : ":";
			default :
				if ( codePoint > 255 ) {
					String hexString = Integer.toHexString( codePoint ).toUpperCase();
					return "\\u" + "0000".substring( hexString.length() ) + hexString;
				} else {
					return String.valueOf( ch );
				}
		}
	}

	/**
	 * Breaks long lines for proper formatting according to maxLineWidth
	 * Adds line continuation characters (\) and proper indentation
	 *
	 * @param text    The text to break into multiple lines if needed
	 * @param padding The number of spaces to use for continuation line indentation
	 *
	 * @return The formatted text with line breaks and continuations as needed
	 */
	private String breakLongLine( String text, int padding ) {
		StringBuilder	result	= new StringBuilder();
		int				len		= text.length() + padding;

		while ( len > maxLineWidth ) {
			String	prefix		= result.length() > 0 ? " ".repeat( padding ) : "";
			int		cutPoint	= maxLineWidth - padding;

			// Don't cut right before a space
			while ( cutPoint < text.length() && text.charAt( cutPoint ) == ' ' ) {
				cutPoint++;
			}

			if ( cutPoint < text.length() ) {
				result.append( prefix ).append( text.substring( 0, cutPoint ) ).append( "\\" ).append( LINE_SEPARATOR );
				text	= text.substring( cutPoint );
				len		= text.length() + padding;
			} else {
				break;
			}
		}

		if ( result.length() > 0 ) {
			result.append( " ".repeat( padding ) ).append( text );
		} else {
			result.append( text );
		}

		return result.toString();
	}

	/**
	 * Converts all lines back to text format for writing to file
	 * Preserves original formatting for unmodified lines, generates new formatting for modified lines
	 *
	 * @return The complete property file content as a formatted string
	 */
	private String getLinesAsText() {
		StringBuilder result = new StringBuilder();

		for ( int i = 0; i < lines.size(); i++ ) {
			IStruct line = lines.get( i );

			switch ( line.getAsString( Key.type ) ) {
				case "comment" :
				case "whitespace" :
					result.append( line.getAsString( Key.value ) );
					break;
				case "property" :
					// If the original line is available, use it
					if ( line.containsKey( Key.originalLine ) ) {
						result.append( line.getAsString( Key.originalLine ) );
					} else {
						String	name		= escapeToken( line.getAsString( Key._name ), true );
						String	delimiter	= line.getAsString( Key.delimiter );
						String	value		= escapeToken( line.getAsString( Key.value ), false );

						result.append( name )
						    .append( delimiter )
						    .append( breakLongLine( value, name.length() + 1 ) );
					}
					break;
			}

			if ( i < lines.size() - 1 ) {
				result.append( LINE_SEPARATOR );
			}
		}

		return result.toString();
	}

	/**
	 * Finds the index of a property line by name within the lines list
	 * Used for locating existing properties for updates or deletions
	 *
	 * @param name The property name to search for
	 *
	 * @return The zero-based index of the property line, or -1 if not found
	 */
	private int findLineIndex( String name ) {
		// Linear search and update cache
		for ( int i = 0; i < lines.size(); i++ ) {
			IStruct line = lines.get( i );
			if ( "property".equals( line.get( Key.type ) ) && name.equals( line.get( Key._name ) ) ) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * -----------------------------------------------------------------------------
	 * Getters and Setters (Fluent Style)
	 * -----------------------------------------------------------------------------
	 */

	/**
	 * Gets the file path of this property file
	 *
	 * @return The fully qualified path to the property file
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Sets the file path of this property file
	 *
	 * @param path The fully qualified path to the property file
	 *
	 * @return This PropertyFile instance for method chaining
	 */
	public PropertyFile setPath( String path ) {
		this.path = path;
		return this;
	}

	/**
	 * Gets the list of all lines (comments, whitespace, and properties) in the file
	 *
	 * @return List of IStruct objects representing the file structure
	 */
	public List<IStruct> getLines() {
		return lines;
	}

	/**
	 * Sets the list of all lines in the file
	 *
	 * @param lines List of IStruct objects representing the file structure
	 *
	 * @return This PropertyFile instance for method chaining
	 */
	public PropertyFile setLines( List<IStruct> lines ) {
		this.lines = lines;
		return this;
	}

	/**
	 * Gets the maximum line width for formatting long property values
	 *
	 * @return The maximum line width in characters
	 */
	public int getMaxLineWidth() {
		return maxLineWidth;
	}

	/**
	 * Sets the maximum line width for formatting long property values
	 *
	 * @param maxLineWidth The maximum line width in characters
	 *
	 * @return This PropertyFile instance for method chaining
	 */
	public PropertyFile setMaxLineWidth( int maxLineWidth ) {
		this.maxLineWidth = maxLineWidth;
		return this;
	}

}