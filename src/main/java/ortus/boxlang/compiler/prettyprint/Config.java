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

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSON.Feature;
import com.fasterxml.jackson.jr.ob.JSONObjectException;

import ortus.boxlang.runtime.types.util.JSONUtil;

public final class Config {

	private final int		indentSize;
	private final boolean	tabIndent;
	private final int		maxLineLength;
	private final String	newLine;
	private final boolean	singleQuote;

	private Config( Builder builder ) {
		this.indentSize		= builder.indentSize;
		this.tabIndent		= builder.tabIndent;
		this.maxLineLength	= builder.maxLineLength;
		this.newLine		= builder.newLine;
		this.singleQuote	= builder.singleQuote;
	}

	public int getIndentSize() {
		return indentSize;
	}

	public boolean isTabIndent() {
		return tabIndent;
	}

	public int getMaxLineLength() {
		return maxLineLength;
	}

	public String getNewLine() {
		return newLine;
	}

	public boolean isSingleQuote() {
		return singleQuote;
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
		return newLine == "os" ? System.lineSeparator() : newLine;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "indentSize", indentSize );
		map.put( "tabIndent", tabIndent );
		map.put( "maxLineLength", maxLineLength );
		map.put( "newLine", newLine );
		map.put( "singleQuote", singleQuote );
		return map;
	}

	public String toJSON() {
		try {
			return JSON.std.with( Feature.PRETTY_PRINT_OUTPUT, Feature.WRITE_NULL_PROPERTIES )
			    .asString( toMap() );
		} catch ( JSONObjectException e ) {
			e.printStackTrace();
		} catch ( IOException e ) {
			e.printStackTrace();
		}
		throw new RuntimeException( "Failed to convert to JSON" );
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {

		private int		indentSize		= 4;
		private boolean	tabIndent		= true;
		private int		maxLineLength	= 80;
		private String	newLine			= "os";
		private boolean	singleQuote		= false;

		private Builder() {
		}

		public Builder withIndentSize( int indentSize ) {
			this.indentSize = indentSize;
			return this;
		}

		public Builder withTabIndent( boolean tabIndent ) {
			this.tabIndent = tabIndent;
			return this;
		}

		public Builder withMaxLineLength( int maxLineLength ) {
			this.maxLineLength = maxLineLength;
			return this;
		}

		public Builder withNewLine( String newLine ) {
			this.newLine = Objects.requireNonNull( newLine, "newLine must not be null" );
			return this;
		}

		public Builder withSingleQuote( boolean singleQuote ) {
			this.singleQuote = singleQuote;
			return this;
		}

		@SuppressWarnings( "unchecked" )
		public Builder withConfigFile( String filePath ) {
			Objects.requireNonNull( filePath, "filePath must not be null" );
			Map<String, Object> config = ( Map<String, Object> ) JSONUtil.fromJSON( new File( filePath ) );
			applyMapConfig( config );
			return this;
		}

		public Builder withConfig( Map<String, Object> config ) {
			Objects.requireNonNull( config, "config must not be null" );
			applyMapConfig( config );
			return this;
		}

		public Config build() {
			return new Config( this );
		}

		private void applyMapConfig( Map<String, Object> config ) {
			if ( config.containsKey( "indentSize" ) && config.get( "indentSize" ) instanceof Number ) {
				this.indentSize = ( ( Number ) config.get( "indentSize" ) ).intValue();
			}
			if ( config.containsKey( "tabIndent" ) && config.get( "tabIndent" ) instanceof Boolean ) {
				this.tabIndent = ( Boolean ) config.get( "tabIndent" );
			}
			if ( config.containsKey( "maxLineLength" ) && config.get( "maxLineLength" ) instanceof Number ) {
				this.maxLineLength = ( ( Number ) config.get( "maxLineLength" ) ).intValue();
			}
			if ( config.containsKey( "newLine" ) && config.get( "newLine" ) instanceof String ) {
				this.newLine = ( String ) config.get( "newLine" );
			}
			if ( config.containsKey( "singleQuote" ) && config.get( "singleQuote" ) instanceof Boolean ) {
				this.singleQuote = ( Boolean ) config.get( "singleQuote" );
			}
		}
	}
}
