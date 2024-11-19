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

import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.types.DateTime;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.util.conversion.deserializers.ArrayDeserializer;
import ortus.boxlang.runtime.util.conversion.deserializers.DateTimeDeserializer;
import ortus.boxlang.runtime.util.conversion.deserializers.LocalDateDeserializer;
import ortus.boxlang.runtime.util.conversion.deserializers.LocalDateTimeDeserializer;
import ortus.boxlang.runtime.util.conversion.deserializers.StructDeserializer;
import ortus.boxlang.runtime.util.conversion.serializers.BoxArraySerializer;
import ortus.boxlang.runtime.util.conversion.serializers.BoxClassSerializer;
import ortus.boxlang.runtime.util.conversion.serializers.BoxFunctionSerializer;
import ortus.boxlang.runtime.util.conversion.serializers.BoxStructSerializer;
import ortus.boxlang.runtime.util.conversion.serializers.DynamicObjectSerializer;

/**
 * This class provides a JSON provider for BoxLang using our lib: Jackson JR
 */
public class BoxJsonProvider extends ReaderWriterProvider {

	/**
	 * Custom BoxLang Serializers by type
	 */
	@Override
	public ValueWriter findValueWriter( JSONWriter writeContext, Class<?> type ) {

		if ( type == DateTime.class || type == LocalDate.class || type == Date.class || type == java.sql.Date.class ) {
			return new DateTime();
		}

		if ( IClassRunnable.class.isAssignableFrom( type ) ) {
			return new BoxClassSerializer();
		}

		if ( List.class.isAssignableFrom( type ) ) {
			return new BoxArraySerializer();
		}

		if ( Function.class.isAssignableFrom( type ) ) {
			return new BoxFunctionSerializer();
		}

		if ( Map.class.isAssignableFrom( type ) ) {
			return new BoxStructSerializer();
		}

		if ( type == DynamicObject.class ) {
			return new DynamicObjectSerializer();
		}

		return null;
	}

	/**
	 * Custom BoxLang Deserializers by type
	 */
	@Override
	public ValueReader findValueReader( JSONReader readContext, Class<?> type ) {

		// DateTime Objects
		if ( type.equals( DateTime.class ) ) {
			return new DateTimeDeserializer();
		}

		// Java Date Objects
		if ( type.equals( LocalDate.class ) ) {
			return new LocalDateDeserializer();
		}

		// Java DateTime Objects
		if ( type.equals( java.time.LocalDateTime.class ) ) {
			return new LocalDateTimeDeserializer();
		}

		// All Map types funneled through Struct
		if ( type.isAssignableFrom( Map.class ) ) {
			return new StructDeserializer();
		}

		// All Lists funneled through Array
		if ( type.isAssignableFrom( List.class ) ) {
			return new ArrayDeserializer();
		}

		return null;
	}

}
