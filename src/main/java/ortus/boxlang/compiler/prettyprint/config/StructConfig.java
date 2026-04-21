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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.jr.ob.JSON;
import com.fasterxml.jackson.jr.ob.JSON.Feature;

public class StructConfig {

	private boolean			padding			= false;
	@JsonProperty( "empty_padding" )
	private boolean			emptyPadding	= false;
	@JsonProperty( "quote_keys" )
	private boolean			quoteKeys		= false;
	private Separator		separator		= Separator.COLON_SPACE;
	private MultilineConfig	multiline		= new MultilineConfig();

	public StructConfig() {
	}

	public MultilineConfig getMultiline() {
		return multiline;
	}

	public StructConfig setMultiline( MultilineConfig multiline ) {
		this.multiline = multiline;
		return this;
	}

	public Separator getSeparator() {
		return separator;
	}

	public StructConfig setSeparator( Separator separator ) {
		this.separator = separator;
		return this;
	}

	public boolean getQuoteKeys() {
		return quoteKeys;
	}

	public StructConfig setQuoteKeys( boolean quoteKeys ) {
		this.quoteKeys = quoteKeys;
		return this;
	}

	public boolean getPadding() {
		return padding;
	}

	public StructConfig setPadding( boolean padding ) {
		this.padding = padding;
		return this;
	}

	public boolean getEmptyPadding() {
		return emptyPadding;
	}

	public StructConfig setEmptyPadding( boolean emptyPadding ) {
		this.emptyPadding = emptyPadding;
		return this;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "padding", padding );
		map.put( "empty_padding", emptyPadding );
		map.put( "quote_keys", quoteKeys );
		map.put( "separator", separator.getSymbol() );
		map.put( "multiline", multiline.toMap() );
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
}