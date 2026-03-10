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

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ArrayConfig {

	private boolean			padding			= false;
	@JsonProperty( "empty_padding" )
	private boolean			emptyPadding	= false;
	private MultilineConfig	multiline		= new MultilineConfig();

	public MultilineConfig getMultiline() {
		return multiline;
	}

	public ArrayConfig setMultiline( MultilineConfig multiline ) {
		this.multiline = multiline;
		return this;
	}

	public boolean getPadding() {
		return padding;
	}

	public ArrayConfig setPadding( boolean padding ) {
		this.padding = padding;
		return this;
	}

	public boolean getEmptyPadding() {
		return emptyPadding;
	}

	public ArrayConfig setEmptyPadding( boolean emptyPadding ) {
		this.emptyPadding = emptyPadding;
		return this;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put( "padding", padding );
		map.put( "empty_padding", emptyPadding );
		map.put( "multiline", multiline.toMap() );
		return map;
	}

}
