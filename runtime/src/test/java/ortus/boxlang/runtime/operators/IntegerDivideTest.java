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

public class IntegerDivideTest {

	@DisplayName( "It can Integer Divide numbers" )
	@Test
	void testItCanIntegerDivideNumbers() {
		assertThat( IntegerDivide.invoke( 9, 3 ) ).isEqualTo( 3 );
		assertThat( IntegerDivide.invoke( 3, 1.5 ) ).isEqualTo( 3 );
		assertThat( IntegerDivide.invoke( 3, 2 ) ).isEqualTo( 1 );
		assertThat( IntegerDivide.invoke( 1, 2 ) ).isEqualTo( 0 );
	}

	@DisplayName( "It can Integer Divide strings" )
	@Test
	void testItCanIntegerDivideStrings() {
		assertThat( IntegerDivide.invoke( "9", "3" ) ).isEqualTo( 3 );
		assertThat( IntegerDivide.invoke( "3", "1.5" ) ).isEqualTo( 3 );
	}

	@DisplayName( "It can not Integer Divide by zero" )
	@Test
	void testItCanNotIntegerDivideByZero() {
		assertThrows( RuntimeException.class, () -> IntegerDivide.invoke( 1, 0 ) );
	}


}
