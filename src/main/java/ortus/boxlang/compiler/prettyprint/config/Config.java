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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSON.Feature;
import com.fasterxml.jackson.jr.ob.JSONObjectException;

import ortus.boxlang.runtime.types.util.JSONUtil;

public final class Config {

	private int					indentSize				= 4;
	private boolean				tabIndent				= true;
	private int					maxLineLength			= 80;
	private String				newLine					= "os";
	private boolean				singleQuote				= false;
	private boolean				bracketPadding			= false;
	private boolean				parensPadding			= false;
	private boolean				binaryOperatorsPadding	= true;
	private boolean				semicolons				= true;
	private StructConfig		struct					= new StructConfig();
	private PropertyConfig		property				= new PropertyConfig();
	private ArrayConfig			array					= new ArrayConfig();

	@JsonProperty( "for_loop_semicolons" )
	private ForLoopSemicolons	forLoopSemicolons		= new ForLoopSemicolons();

	private FunctionConfig		function				= new FunctionConfig();

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

	public boolean getSemicolons() {
		return semicolons;
	}

	public Config setSemicolons( boolean semicolons ) {
		this.semicolons = semicolons;
		return this;
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

	public ForLoopSemicolons getForLoopSemicolons() {
		return forLoopSemicolons;
	}

	public Config setForLoopSemicolons( ForLoopSemicolons forLoopSemicolons ) {
		this.forLoopSemicolons = forLoopSemicolons;
		return this;
	}

	public PropertyConfig getProperty() {
		return property;
	}

	public Config setProperty( PropertyConfig property ) {
		this.property = property;
		return this;
	}

	public ArrayConfig getArray() {
		return array;
	}

	public Config setArray( ArrayConfig array ) {
		this.array = array;
		return this;
	}

	public FunctionConfig getFunction() {
		return function;
	}

	public Config setFunction( FunctionConfig function ) {
		this.function = function;
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
		map.put( "binaryOperatorsPadding", binaryOperatorsPadding );
		map.put( "semicolons", semicolons );
		map.put( "struct", struct.toMap() );
		map.put( "property", property.toMap() );
		map.put( "array", array.toMap() );
		map.put( "for_loop_semicolons", forLoopSemicolons.toMap() );
		map.put( "function", function.toMap() );
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

	@SuppressWarnings( "unchecked" )
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
		if ( config.containsKey( "binaryOperatorsPadding" ) && config.get( "binaryOperatorsPadding" ) instanceof Boolean binaryOperatorsPadding ) {
			this.binaryOperatorsPadding = binaryOperatorsPadding;
		}
		if ( config.containsKey( "semicolons" ) && config.get( "semicolons" ) instanceof Boolean semicolons ) {
			this.semicolons = semicolons;
		}
		if ( config.containsKey( "struct" ) && config.get( "struct" ) instanceof Map structMap ) {
			applyStructConfig( ( Map<String, Object> ) structMap );
		}
		if ( config.containsKey( "array" ) && config.get( "array" ) instanceof Map arrayMap ) {
			applyArrayConfig( ( Map<String, Object> ) arrayMap );
		}
		if ( config.containsKey( "property" ) && config.get( "property" ) instanceof Map propertyMap ) {
			applyPropertyConfig( ( Map<String, Object> ) propertyMap );
		}
		if ( config.containsKey( "for_loop_semicolons" ) && config.get( "for_loop_semicolons" ) instanceof Map forLoopMap ) {
			applyForLoopSemicolonsConfig( ( Map<String, Object> ) forLoopMap );
		}
		if ( config.containsKey( "function" ) && config.get( "function" ) instanceof Map functionMap ) {
			applyFunctionConfig( ( Map<String, Object> ) functionMap );
		}
	}

	private void applyStructConfig( Map<String, Object> config ) {
		if ( config.containsKey( "padding" ) && config.get( "padding" ) instanceof Boolean padding ) {
			this.struct.setPadding( padding );
		}
		if ( config.containsKey( "empty_padding" ) && config.get( "empty_padding" ) instanceof Boolean emptyPadding ) {
			this.struct.setEmptyPadding( emptyPadding );
		}
		if ( config.containsKey( "quote_keys" ) && config.get( "quote_keys" ) instanceof Boolean quoteKeys ) {
			this.struct.setQuoteKeys( quoteKeys );
		}
		if ( config.containsKey( "separator" ) && config.get( "separator" ) instanceof String separator ) {
			this.struct.setSeparator( parseSeparator( separator ) );
		}
		if ( config.containsKey( "multiline" ) && config.get( "multiline" ) instanceof Map multilineMap ) {
			applyMultilineConfig( this.struct.getMultiline(), multilineMap );
		}
	}

	private void applyArrayConfig( Map<String, Object> config ) {
		if ( config.containsKey( "padding" ) && config.get( "padding" ) instanceof Boolean padding ) {
			this.array.setPadding( padding );
		}
		if ( config.containsKey( "empty_padding" ) && config.get( "empty_padding" ) instanceof Boolean emptyPadding ) {
			this.array.setEmptyPadding( emptyPadding );
		}
		if ( config.containsKey( "multiline" ) && config.get( "multiline" ) instanceof Map multilineMap ) {
			applyMultilineConfig( this.array.getMultiline(), multilineMap );
		}
	}

	private void applyPropertyConfig( Map<String, Object> config ) {
		if ( config.containsKey( "multiline" ) && config.get( "multiline" ) instanceof Map multilineMap ) {
			applyMultilineConfig( this.property.getMultiline(), multilineMap );
		}
		if ( config.containsKey( "key_value" ) && config.get( "key_value" ) instanceof Map keyValueMap ) {
			if ( keyValueMap.containsKey( "padding" ) && keyValueMap.get( "padding" ) instanceof Boolean padding ) {
				this.property.getKeyValue().setPadding( padding );
			}
		}
	}

	private void applyForLoopSemicolonsConfig( Map<String, Object> config ) {
		if ( config.containsKey( "padding" ) && config.get( "padding" ) instanceof Boolean padding ) {
			this.forLoopSemicolons.setPadding( padding );
		}
	}

	@SuppressWarnings( "unchecked" )
	private void applyFunctionConfig( Map<String, Object> config ) {
		if ( config.containsKey( "style" ) && config.get( "style" ) instanceof String style ) {
			this.function.setStyle( style );
		}
		if ( config.containsKey( "parameters" ) && config.get( "parameters" ) instanceof Map parametersMap ) {
			applyParametersConfig( ( Map<String, Object> ) parametersMap );
		}
		if ( config.containsKey( "arrow" ) && config.get( "arrow" ) instanceof Map arrowMap ) {
			applyArrowConfig( ( Map<String, Object> ) arrowMap );
		}
	}

	private void applyParametersConfig( Map<String, Object> config ) {
		if ( config.containsKey( "comma_dangle" ) && config.get( "comma_dangle" ) instanceof Boolean commaDangle ) {
			this.function.getParameters().setCommaDangle( commaDangle );
		}
		if ( config.containsKey( "multiline_count" ) && config.get( "multiline_count" ) instanceof Number multilineCount ) {
			this.function.getParameters().setMultilineCount( multilineCount.intValue() );
		}
		if ( config.containsKey( "multiline_length" ) && config.get( "multiline_length" ) instanceof Number multilineLength ) {
			this.function.getParameters().setMultilineLength( multilineLength.intValue() );
		}
	}

	private void applyArrowConfig( Map<String, Object> config ) {
		if ( config.containsKey( "parens" ) && config.get( "parens" ) instanceof String parens ) {
			this.function.getArrow().setParens( parens );
		}
	}

	@SuppressWarnings( "rawtypes" )
	private void applyMultilineConfig( MultilineConfig multiline, Map config ) {
		if ( config.containsKey( "element_count" ) && config.get( "element_count" ) instanceof Number elementCount ) {
			multiline.setElementCount( elementCount.intValue() );
		}
		if ( config.containsKey( "comma_dangle" ) && config.get( "comma_dangle" ) instanceof Boolean commaDangle ) {
			multiline.setCommaDangle( commaDangle );
		}
		if ( config.containsKey( "min_length" ) && config.get( "min_length" ) instanceof Number minLength ) {
			multiline.setMinLength( minLength.intValue() );
		}
		if ( config.containsKey( "leading_comma" ) ) {
			multiline.setLeadingComma( config.get( "leading_comma" ) );
		}
	}

	private Separator parseSeparator( String separator ) {
		return switch ( separator ) {
			case ":" -> Separator.COLON;
			case "=" -> Separator.EQUALS;
			case ": " -> Separator.COLON_SPACE;
			case "= " -> Separator.EQUALS_SPACE;
			default -> Separator.COLON_SPACE;
		};
	}
}