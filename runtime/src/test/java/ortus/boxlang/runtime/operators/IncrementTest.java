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

import org.junit.Ignore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class IncrementTest {

	@DisplayName( "It can Increment numbers" )
	@Test
	void testItCanAddNumbers() {
		assertThat( Increment.invoke( 3 ) ).isEqualTo( 4 );
		assertThat( Increment.invoke( 3.5 ) ).isEqualTo( 4.5 );
	}

	@DisplayName( "It can Increment strings" )
	@Test
	void testItCanAddStrings() {
		assertThat( Increment.invoke( "3" ) ).isEqualTo( 4 );
		assertThat( Increment.invoke( "3.5" ) ).isEqualTo( 4.5 );
	}

}
