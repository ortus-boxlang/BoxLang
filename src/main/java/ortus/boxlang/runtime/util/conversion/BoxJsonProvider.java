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
package ortus.boxlang.runtime.util.conversion;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.jr.ob.api.ReaderWriterProvider;
import com.fasterxml.jackson.jr.ob.api.ValueReader;
import com.fasterxml.jackson.jr.ob.api.ValueWriter;
import com.fasterxml.jackson.jr.ob.impl.JSONReader;
import com.fasterxml.jackson.jr.ob.impl.JSONWriter;

import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.util.conversion.deserializers.ArrayJsonDeserializer;
import ortus.boxlang.runtime.util.conversion.deserializers.DateTimeJsonDeserializer;
import ortus.boxlang.runtime.util.conversion.deserializers.StructJsonDeserializer;

/**
 * This class provides a JSON provider for BoxLang using our lib: Jackson JR
 */
public class BoxJsonProvider extends ReaderWriterProvider {

	/**
	 * Custom BoxLang Serializers by type
	 */
	@Override
	public ValueWriter findValueWriter( JSONWriter writeContext, Class<?> type ) {

		if ( type == DateTime.class ) {
			return new DateTime();
		}

		return null;
	}

	/**
	 * Custom BoxLang Deserializers by type
	 */
	@Override
	public ValueReader findValueReader( JSONReader readContext, Class<?> type ) {

		// All date types funneled through DateTime
		if ( type.equals( DateTime.class ) || type.equals( LocalDate.class ) || type.equals( Date.class ) ) {
			return new DateTimeJsonDeserializer();
		}

		// All Map types funneled through Struct
		if ( type.isAssignableFrom( Map.class ) ) {
			return new StructJsonDeserializer();
		}

		// All Lists funneled through Array
		if ( type.isAssignableFrom( List.class ) ) {
			return new ArrayJsonDeserializer();
		}

		return null;
	}

}
