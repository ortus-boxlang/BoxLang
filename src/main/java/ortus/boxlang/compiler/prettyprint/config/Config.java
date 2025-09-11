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
package ortus.boxlang.compiler.prettyprint.config;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSON.Feature;
import com.fasterxml.jackson.jr.ob.JSONObjectException;

import ortus.boxlang.runtime.types.util.JSONUtil;

public final class Config {

	private int				indentSize				= 4;
	private boolean			tabIndent				= true;
	private int				maxLineLength			= 80;
	private String			newLine					= "os";
	private boolean			singleQuote				= false;
	private boolean			bracketPadding			= false;
	private boolean			parensPadding			= false;
	private boolean			binaryOperatorsPadding	= true;
	private StructConfig	struct					= new StructConfig();

	public Config() {
	}

	public StructConfig getStruct() {
		return struct;
	}

	public Config setStruct( StructConfig struct ) {
		this.struct = struct;
		return this;
	}

	public Config setBinaryOperatorsPadding( boolean value ) {
		this.binaryOperatorsPadding = value;
		return this;
	}

	public boolean getBinaryOperatorsPadding() {
		return this.binaryOperatorsPadding;
	}

	public int getIndentSize() {
		return indentSize;
	}

	public Config setIndentSize( int indentSize ) {
		this.indentSize = indentSize;
		return this;
	}

	public boolean getTabIndent() {
		return tabIndent;
	}

	public Config setTabIndent( boolean tabIndent ) {
		this.tabIndent = tabIndent;
		return this;
	}

	public int getMaxLineLength() {
		return maxLineLength;
	}

	public Config setMaxLineLength( int maxLineLength ) {
		this.maxLineLength = maxLineLength;
		return this;
	}

	public String getNewLine() {
		return newLine;
	}

	public Config setNewLine( String newLine ) {
		this.newLine = newLine;
		return this;
	}

	public boolean getSingleQuote() {
		return singleQuote;
	}

	public Config setSingleQuote( boolean singleQuote ) {
		this.singleQuote = singleQuote;
		return this;
	}

	public boolean getBracketPadding() {
		return bracketPadding;
	}

	public Config setBracketPadding( boolean bracketPadding ) {
		this.bracketPadding = bracketPadding;
		return this;
	}

	public boolean getParensPadding() {
		return parensPadding;
	}

	public Config setParensPadding( boolean parensPadding ) {
		this.parensPadding = parensPadding;
		return this;
	}

	public static Config loadConfig( String filePath ) throws JSONObjectException, IOException {
		return JSONUtil.getJSONBuilder().beanFrom( Config.class, new File( filePath ) );
	}

	@SuppressWarnings( "unchecked" )
	public Config loadFromConfigFile( String filePath ) {
		Map<String, Object> config = ( Map<String, Object> ) JSONUtil.fromJSON( new File( filePath ) );
		applyMapConfig( config );
		return this;
	}

	public Config loadFromConfig( Map<String, Object> config ) {
		applyMapConfig( config );
		return this;
	}

	public int indentColumn( int indentLevel ) {
		return indentSize * indentLevel;
	}

	public String indentToColumn( int column ) {
		if ( tabIndent ) {
			int	tabs	= column / indentSize;
			int	spaces	= column % indentSize;
			return "\t".repeat( tabs ) + " ".repeat( spaces );
		}
		return " ".repeat( column );
	}

	public String indentToLevel( int indentLevel ) {
		return indentToColumn( indentColumn( indentLevel ) );
	}

	public String lineSeparator() {
		return newLine.equals( "os" ) ? System.lineSeparator() : newLine;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "indentSize", indentSize );
		map.put( "tabIndent", tabIndent );
		map.put( "maxLineLength", maxLineLength );
		map.put( "newLine", newLine );
		map.put( "singleQuote", singleQuote );
		map.put( "bracketPadding", bracketPadding );
		map.put( "parensPadding", parensPadding );
		return map;
	}

	public String toJSON() {
		try {
			return JSON.std.with( Feature.PRETTY_PRINT_OUTPUT, Feature.WRITE_NULL_PROPERTIES )
			    .asString( toMap() );
		} catch ( IOException e ) {
			e.printStackTrace();
			throw new RuntimeException( "Failed to convert to JSON", e );
		}
	}

	private void applyMapConfig( Map<String, Object> config ) {
		if ( config.containsKey( "indentSize" ) && config.get( "indentSize" ) instanceof Number indentSize ) {
			this.indentSize = indentSize.intValue();
		}
		if ( config.containsKey( "tabIndent" ) && config.get( "tabIndent" ) instanceof Boolean tabIndent ) {
			this.tabIndent = tabIndent;
		}
		if ( config.containsKey( "maxLineLength" ) && config.get( "maxLineLength" ) instanceof Number maxLineLength ) {
			this.maxLineLength = maxLineLength.intValue();
		}
		if ( config.containsKey( "newLine" ) && config.get( "newLine" ) instanceof String newLine ) {
			this.newLine = newLine;
		}
		if ( config.containsKey( "singleQuote" ) && config.get( "singleQuote" ) instanceof Boolean singleQuote ) {
			this.singleQuote = singleQuote;
		}
		if ( config.containsKey( "bracketPadding" ) && config.get( "bracketPadding" ) instanceof Boolean bracketPadding ) {
			this.bracketPadding = bracketPadding;
		}
		if ( config.containsKey( "parensPadding" ) && config.get( "parensPadding" ) instanceof Boolean parensPadding ) {
			this.parensPadding = parensPadding;
		}
	}
}