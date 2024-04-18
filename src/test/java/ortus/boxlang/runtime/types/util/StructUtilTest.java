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
package ortus.boxlang.runtime.types.util;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.types.Struct;

public class StructUtilTest {

	@DisplayName( "Can parse a struct to a query string" )
	@Test
	void testStructToQueryString() {
		Struct struct = new Struct();
		struct.put( "foo", "bar" );
		struct.put( "baz", "qux" );
		assertThat( StructUtil.toQueryString( struct ) ).isEqualTo( "foo=bar&baz=qux" );

		struct = new Struct();
		assertThat( StructUtil.toQueryString( struct ) ).isEqualTo( "" );
	}

	@DisplayName( "Can parse a query string to a struct using another delimiter" )
	@Test
	void testQueryStringToStruct() {
		Struct struct = new Struct();
		struct.put( "foo", "bar" );
		struct.put( "baz", "qux" );

		assertThat( StructUtil.toQueryString( struct, ";" ) ).isEqualTo( "foo=bar;baz=qux" );
	}

}
