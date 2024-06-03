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
package ortus.boxlang.runtime.util.conversion.deserializers;

import java.io.IOException;
import java.time.LocalDateTime;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.jr.ob.api.ValueReader;
import com.fasterxml.jackson.jr.ob.impl.JSONReader;

import ortus.boxlang.runtime.dynamic.casters.DateTimeCaster;

public class LocalDateTimeDeserializer extends ValueReader {

	/**
	 * Constructor
	 */
	public LocalDateTimeDeserializer() {
		super( LocalDateTime.class );
	}

	@Override
	public Object read( JSONReader reader, JsonParser jsonParser ) throws IOException {
		return DateTimeCaster.cast( jsonParser.getText() );
	}

}
