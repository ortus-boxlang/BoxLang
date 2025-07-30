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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * A transient representation of the contents of a property file.
 * Create a new version for separate property files. Can be interacted with via
 * methods or public properties that represent the keys.
 *
 * Based on https://fmpp.sourceforge.net/properties.html
 */
public class PropertyFile {

    // Private fields
    private String              path;
    private List<PropertyLine>  lines;
    private int                 maxLineWidth;
    private String              lineSeparator;

    /**
     * Constructor - initializes a new PropertyFile instance
     * Sets default values for maxLineWidth (150), empty lines list,
     * and uses system line separator
     */
    public PropertyFile() {
        this.maxLineWidth   = 150;
        this.lines          = new ArrayList<>();
        this.lineSeparator  = System.lineSeparator();
    }

    /**
     * Loads a property file from the specified path using BoxLang FileSystemUtil
     *
     * @param path A fully qualified path to a property file
     * @return This PropertyFile instance for method chaining
     */
    public PropertyFile load( String path ) {
        this.path = path;

        String fileContents = FileSystemUtil.read( Path.of( path ) )
            .replace( "\r\n", "\n" )
            .replace( "\r", "\n" );

        String[] fileLines = fileContents.split( "\n", -1 );

        int lineNo = 0;
        StringBuilder nextLine = new StringBuilder();
        StringBuilder originalLine = new StringBuilder();

        for ( String line : fileLines ) {
            lineNo++;
            originalLine.append( line );
            nextLine.append( line.trim() );

            if ( nextLine.toString().endsWith( "\\" ) ) {
                // Line continuation
                nextLine.setLength( nextLine.length() - 1 );
                originalLine.append( "\n" );
                continue;
            } else {
                addLine(
                    nextLine.toString(),
                    lineNo,
                    originalLine.toString().replace( "\n", lineSeparator )
                );
                originalLine.setLength( 0 );
                nextLine.setLength( 0 );
            }
        }

        return this;
    }

    /**
     * Stores the property file to the specified path using BoxLang FileSystemUtil
     *
     * @param path The path to store the file (optional, uses loaded path if not provided)
     * @return This PropertyFile instance for method chaining
     */
    public PropertyFile store( String path ) {
        if ( path == null ) {
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
     * @return The property value
     * @throws IllegalArgumentException If the key doesn't exist
     */
    public String get( String name ) {
        IStruct data = getAsStruct();
        if ( data.containsKey( Key.of( name ) ) ) {
            return data.getAsString( Key.of( name ) );
        } else {
            throw new IllegalArgumentException(
                "Key [" + name + "] does not exist in this properties file. Valid keys are " +
                String.join( ", ", data.getKeysAsStrings() )
            );
        }
    }

    /**
     * Gets a property value by name with a default value
     *
     * @param name The property name
     * @param defaultValue The default value if the key doesn't exist
     * @return The property value or default value
     */
    public String get( String name, String defaultValue ) {
        IStruct data = getAsStruct();
        return data.containsKey( Key.of( name ) ) ?
            data.getAsString( Key.of( name ) ) :
            defaultValue;
    }

    /**
     * Sets a property value
     *
     * @param name The property name
     * @param value The property value
     * @return This PropertyFile instance for method chaining
     */
    public PropertyFile set( String name, String value ) {
        int lineIndex = findLineIndex( name );

        if ( lineIndex >= 0 ) {
            PropertyLine lineData = lines.get( lineIndex );
            if ( !lineData.getValue().equals( value ) ) {
                lineData.setValue( value );
                lineData.setOriginalLine( null ); // Mark as modified
            }
        } else {
            PropertyLine newLine = new PropertyLine()
                .setType( "property" )
                .setName( name )
                .setValue( value )
                .setDelimiter( "=" );
            lines.add( newLine );
        }

        return this;
    }

    /**
     * Sets multiple property values at once
     *
     * @param properties An IStruct containing key-value pairs to set
     * @return This PropertyFile instance for method chaining
     */
    public PropertyFile setAll( IStruct properties ) {
        properties.entrySet().forEach( entry -> {
            set( entry.getKey().getName(), entry.getValue().toString() );
        } );
        return this;
    }

    /**
     * Sets multiple property values at once from a Map
     *
     * @param properties A Map containing key-value pairs to set
     * @return This PropertyFile instance for method chaining
     */
    public PropertyFile setAll( Map<String, String> properties ) {
        properties.entrySet().forEach( entry -> {
            set( entry.getKey(), entry.getValue() );
        } );
        return this;
    }

    /**
     * Removes a property
     *
     * @param name The property name to remove
     * @return This PropertyFile instance for method chaining
     */
    public PropertyFile remove( String name ) {
        int lineIndex = findLineIndex( name );

        if ( lineIndex >= 0 ) {
            lines.remove( lineIndex );
        }

        return this;
    }

    /**
     * Removes multiple properties at once
     *
     * @param names The property names to remove
     * @return This PropertyFile instance for method chaining
     */
    public PropertyFile removeAll( String... names ) {
        for ( String name : names ) {
            remove( name );
        }
        return this;
    }

    /**
     * Checks if a property exists
     *
     * @param name The property name
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
        return lines.stream().anyMatch( line -> "property".equals( line.getType() ) );
    }

    /**
     * Gets the number of properties in the file
     *
     * @return The count of properties
     */
    public int size() {
        return (int) lines.stream().filter( line -> "property".equals( line.getType() ) ).count();
    }

    /**
     * Clears all properties while preserving comments and whitespace
     *
     * @return This PropertyFile instance for method chaining
     */
    public PropertyFile clearProperties() {
        lines.removeIf( line -> "property".equals( line.getType() ) );
        return this;
    }

    /**
     * Clears everything including comments and whitespace
     *
     * @return This PropertyFile instance for method chaining
     */
    public PropertyFile clear() {
        lines.clear();
        return this;
    }

    /**
     * Gets all properties as a BoxLang IStruct
     * Uses LinkedStruct to preserve property order as they appear in the file
     *
     * @return An IStruct containing all properties with insertion order preserved
     */
    public IStruct getAsStruct() {
        IStruct result = new Struct( IStruct.TYPES.LINKED );

        for ( PropertyLine line : lines ) {
            if ( "property".equals( line.getType() ) ) {
                result.put( Key.of( line.getName() ), line.getValue() );
            }
        }

        return result;
    }

    /**
     * Gets all properties as a standard Java Map
     * Properties are returned in the order they appear in the file
     *
     * @return A LinkedHashMap containing all properties with insertion order preserved
     */
    public Map<String, String> getAsMap() {
        Map<String, String> result = new LinkedHashMap<>();

        for ( PropertyLine line : lines ) {
            if ( "property".equals( line.getType() ) ) {
                result.put( line.getName(), line.getValue() );
            }
        }

        return result;
    }

    /**
     * Merges properties from an IStruct into this PropertyFile
     *
     * @param incomingStruct The IStruct containing properties to merge
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
     * @return A list of all property names
     */
    public List<String> getPropertyNames() {
        return lines.stream()
            .filter( line -> "property".equals( line.getType() ) )
            .map( PropertyLine::getName )
            .toList();
    }

    // Private helper methods

    /**
     * Adds a line from the source file to the internal structure
     * Parses the line content to determine if it's a comment, whitespace, or property
     *
     * @param contents The trimmed content of the line
     * @param lineNo The source line number for error reporting
     * @param originalLine The original line content including formatting
     * @throws IllegalArgumentException If the property line format is invalid
     */
    private void addLine( String contents, int lineNo, String originalLine ) {
        PropertyLine line = new PropertyLine();

        if ( contents.startsWith( "#" ) || contents.startsWith( "!" ) ) {
            line.setType( "comment" );
            line.setValue( contents );
        } else if ( contents.trim().isEmpty() ) {
            line.setType( "whitespace" );
            line.setValue( contents );
        } else {
            // Look for property delimiter
            Pattern delimPattern = Pattern.compile( "[^\\\\](([=:])|([ ][^=:]))" );
            Matcher matcher = delimPattern.matcher( contents );

            if ( matcher.find() ) {
                int delimIndex = matcher.start() + 1;
                line.setName( unEscapeToken( contents.substring( 0, delimIndex ).trim() ) );
                line.setDelimiter( String.valueOf( contents.charAt( delimIndex ) ) );
                line.setValue( unEscapeToken( contents.substring( delimIndex + 1 ).trim() ) );
                line.setType( "property" );
                line.setOriginalLine( originalLine );
            } else {
                throw new IllegalArgumentException( "Invalid property file format, line " + lineNo );
            }
        }

        lines.add( line );
    }

    /**
     * Unescapes tokens according to property file standards
     * Handles escape sequences like \t, \n, \r, \f, \\, \", \', and \uxxxx
     *
     * @param token The token string to unescape
     * @return The unescaped string with proper character substitutions
     * @throws IllegalArgumentException If unicode escape sequences are malformed
     */
    private String unEscapeToken( String token ) {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;
        boolean unicode = false;
        StringBuilder unicodeString = new StringBuilder();

        for ( char ch : token.toCharArray() ) {
            if ( !escaped && !unicode && ch == '\\' ) {
                escaped = true;
            } else if ( escaped && !unicode ) {
                switch ( ch ) {
                    case 't':
                        result.append( '\t' );
                        break;
                    case 'n':
                        result.append( '\n' );
                        break;
                    case 'r':
                        result.append( '\r' );
                        break;
                    case 'f':
                        result.append( '\f' );
                        break;
                    case '\\':
                        result.append( '\\' );
                        break;
                    case '"':
                        result.append( '"' );
                        break;
                    case '\'':
                        result.append( '\'' );
                        break;
                    case 'u':
                        unicode = true;
                        break;
                    default:
                        result.append( ch );
                        break;
                }
                escaped = false;
            } else if ( unicode ) {
                if ( !Character.toString( ch ).matches( "[0-9a-fA-F]" ) ) {
                    throw new IllegalArgumentException( "Invalid unicode character [" + ch + "] in token [" + token + "]" );
                }
                unicodeString.append( ch );
                if ( unicodeString.length() == 4 ) {
                    result.append( (char) Integer.parseInt( unicodeString.toString(), 16 ) );
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
     * @param token The string token to escape
     * @param escapeName Whether to escape name-specific characters (space, =, :)
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
     * @param ch The character to escape
     * @param escapeName Whether to escape name-specific characters (space, =, :)
     * @return The escaped character sequence as a string
     */
    private String escapeChar( char ch, boolean escapeName ) {
        int codePoint = (int) ch;

        switch ( codePoint ) {
            case 9:
                return "\\t";
            case 10:
                return "\\n";
            case 13:
                return "\\r";
            case 12:
                return "\\f";
            case 34:
                return "\\\"";
            case 39:
                return "\\'";
            case 92:
                return "\\\\";
            case 32:
                return escapeName ? "\\ " : " ";
            case 61:
                return escapeName ? "\\=" : "=";
            case 58:
                return escapeName ? "\\:" : ":";
            default:
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
     * @param text The text to break into multiple lines if needed
     * @param padding The number of spaces to use for continuation line indentation
     * @return The formatted text with line breaks and continuations as needed
     */
    private String breakLongLine( String text, int padding ) {
        StringBuilder result = new StringBuilder();
        int len = text.length() + padding;

        while ( len > maxLineWidth ) {
            String prefix = result.length() > 0 ? " ".repeat( padding ) : "";
            int cutPoint = maxLineWidth - padding;

            // Don't cut right before a space
            while ( cutPoint < text.length() && text.charAt( cutPoint ) == ' ' ) {
                cutPoint++;
            }

            if ( cutPoint < text.length() ) {
                result.append( prefix ).append( text.substring( 0, cutPoint ) ).append( "\\" ).append( lineSeparator );
                text = text.substring( cutPoint );
                len = text.length() + padding;
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
            PropertyLine line = lines.get( i );

            switch ( line.getType() ) {
                case "comment":
                case "whitespace":
                    result.append( line.getValue() );
                    break;
                case "property":
                    if ( line.getOriginalLine() != null ) {
                        result.append( line.getOriginalLine() );
                    } else {
                        String name = escapeToken( line.getName(), true );
                        result.append( name )
                              .append( line.getDelimiter() )
                              .append( breakLongLine( escapeToken( line.getValue(), false ), name.length() + 1 ) );
                    }
                    break;
            }

            if ( i < lines.size() - 1 ) {
                result.append( lineSeparator );
            }
        }

        return result.toString();
    }

    /**
     * Finds the index of a property line by name within the lines list
     * Used for locating existing properties for updates or deletions
     *
     * @param name The property name to search for
     * @return The zero-based index of the property line, or -1 if not found
     */
    private int findLineIndex( String name ) {
        for ( int i = 0; i < lines.size(); i++ ) {
            PropertyLine line = lines.get( i );
            if ( "property".equals( line.getType() ) && name.equals( line.getName() ) ) {
                return i;
            }
        }
        return -1;
    }

    // Getters and Setters (Fluent Style)

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
     * @return This PropertyFile instance for method chaining
     */
    public PropertyFile setPath( String path ) {
        this.path = path;
        return this;
    }

    /**
     * Gets the list of all lines (comments, whitespace, and properties) in the file
     *
     * @return List of PropertyLine objects representing the file structure
     */
    public List<PropertyLine> getLines() {
        return lines;
    }

    /**
     * Sets the list of all lines in the file
     *
     * @param lines List of PropertyLine objects representing the file structure
     * @return This PropertyFile instance for method chaining
     */
    public PropertyFile setLines( List<PropertyLine> lines ) {
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
     * @return This PropertyFile instance for method chaining
     */
    public PropertyFile setMaxLineWidth( int maxLineWidth ) {
        this.maxLineWidth = maxLineWidth;
        return this;
    }

    /**
     * Gets the line separator used for this property file
     *
     * @return The line separator string (e.g., \n, \r\n)
     */
    public String getLineSeparator() {
        return lineSeparator;
    }

    /**
     * Sets the line separator used for this property file
     *
     * @param lineSeparator The line separator string (e.g., \n, \r\n)
     * @return This PropertyFile instance for method chaining
     */
    public PropertyFile setLineSeparator( String lineSeparator ) {
        this.lineSeparator = lineSeparator;
        return this;
    }

    /**
     * Inner class representing a line in the property file
     * Can represent comments, whitespace, or property key-value pairs
     */
    public static class PropertyLine {
        private String type;        // "comment", "whitespace", or "property"
        private String value;       // The value portion of a property or the full content for comments/whitespace
        private String name;        // The key name for property lines
        private String delimiter;   // The delimiter used (=, :, or space) for property lines
        private String originalLine; // The original line content for preserving formatting

        // Fluent Getters and Setters

        /**
         * Gets the type of this line (comment, whitespace, or property)
         *
         * @return The line type
         */
        public String getType() {
            return type;
        }

        /**
         * Sets the type of this line
         *
         * @param type The line type ("comment", "whitespace", or "property")
         * @return This PropertyLine instance for method chaining
         */
        public PropertyLine setType( String type ) {
            this.type = type;
            return this;
        }

        /**
         * Gets the value portion of a property or full content for comments/whitespace
         *
         * @return The line value
         */
        public String getValue() {
            return value;
        }

        /**
         * Sets the value portion of a property or full content for comments/whitespace
         *
         * @param value The line value
         * @return This PropertyLine instance for method chaining
         */
        public PropertyLine setValue( String value ) {
            this.value = value;
            return this;
        }

        /**
         * Gets the property name (key) for property lines
         *
         * @return The property name, or null for non-property lines
         */
        public String getName() {
            return name;
        }

        /**
         * Sets the property name (key) for property lines
         *
         * @param name The property name
         * @return This PropertyLine instance for method chaining
         */
        public PropertyLine setName( String name ) {
            this.name = name;
            return this;
        }

        /**
         * Gets the delimiter used between key and value for property lines
         *
         * @return The delimiter character (=, :, or space)
         */
        public String getDelimiter() {
            return delimiter;
        }

        /**
         * Sets the delimiter used between key and value for property lines
         *
         * @param delimiter The delimiter character (=, :, or space)
         * @return This PropertyLine instance for method chaining
         */
        public PropertyLine setDelimiter( String delimiter ) {
            this.delimiter = delimiter;
            return this;
        }

        /**
         * Gets the original line content for preserving formatting
         *
         * @return The original line content, or null if the line has been modified
         */
        public String getOriginalLine() {
            return originalLine;
        }

        /**
         * Sets the original line content for preserving formatting
         * Setting to null indicates the line has been modified and should be reformatted
         *
         * @param originalLine The original line content
         * @return This PropertyLine instance for method chaining
         */
        public PropertyLine setOriginalLine( String originalLine ) {
            this.originalLine = originalLine;
            return this;
        }
    }
}