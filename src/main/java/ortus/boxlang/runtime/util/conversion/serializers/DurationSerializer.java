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
package ortus.boxlang.runtime.util.conversion.serializers;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.jr.ob.api.ValueWriter;
import com.fasterxml.jackson.jr.ob.impl.JSONWriter;

/**
 * This class provides JSON Serialization of a Java Duration as fractional days.
 */
public class DurationSerializer implements ValueWriter {

	private static final BigDecimal NANOS_PER_DAY = BigDecimal.valueOf( 86400L * 1_000_000_000L );

	@Override
	public void writeValue( JSONWriter context, JsonGenerator g, Object value ) throws IOException {
		Duration	duration	= ( Duration ) value;
		BigDecimal	days		= BigDecimal.valueOf( duration.toNanos() ).divide( NANOS_PER_DAY, 20, java.math.RoundingMode.HALF_UP ).stripTrailingZeros();
		g.writeNumber( days );
	}

	@Override
	public Class<?> valueType() {
		return Duration.class;
	}

}
