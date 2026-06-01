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
import java.util.IdentityHashMap;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.jr.ob.api.ValueWriter;
import com.fasterxml.jackson.jr.ob.impl.JSONWriter;

/**
 * JSON serializer for {@link java.util.Set} (including {@code BoxSet}).
 *
 * <p>
 * Renders a set as a JSON array, preserving iteration order of the underlying Set
 * implementation. Includes cycle detection so a Set that ends up containing itself
 * (directly or transitively) produces {@code "recursive-set-skipping"} instead of
 * blowing the stack.
 */
public class BoxSetSerializer implements ValueWriter {

	private static final ThreadLocal<IdentityHashMap<Set<?>, Boolean>> visited = ThreadLocal.withInitial( IdentityHashMap::new );

	@Override
	public void writeValue( JSONWriter context, JsonGenerator g, Object value ) throws IOException {
		Set<?>								bxSet	= ( Set<?> ) value;
		IdentityHashMap<Set<?>, Boolean>	seen	= visited.get();

		if ( seen.containsKey( bxSet ) ) {
			g.writeString( "recursive-set-skipping" );
			return;
		}
		seen.put( bxSet, Boolean.TRUE );
		try {
			g.writeStartArray();
			for ( Object element : bxSet ) {
				context.writeValue( element );
			}
			g.writeEndArray();
		} finally {
			seen.remove( bxSet );
		}
	}

	@Override
	public Class<?> valueType() {
		return Set.class;
	}

}
