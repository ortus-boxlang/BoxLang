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
package ortus.boxlang.runtime.operators;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.BaseBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;

public class ElvisTest {

	@DisplayName( "It can null coalesce" )
	@Test
	void testItNullCoalesce() {
		IBoxContext context = new BaseBoxContext();
		assertThat( Elvis.invoke( context, "left", ( c ) -> "right" ) ).isEqualTo( "left" );
		assertThat( Elvis.invoke( context, null, ( c ) -> "right" ) ).isEqualTo( "right" );

	}

}
