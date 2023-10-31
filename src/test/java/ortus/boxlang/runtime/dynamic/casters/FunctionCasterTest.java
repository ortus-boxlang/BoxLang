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
package ortus.boxlang.runtime.dynamic.casters;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.SampleUDF;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxLangException;

public class FunctionCasterTest {

	@DisplayName( "It can cast a Function to a Function" )
	@Test
	void testItCanCastAFunction() {
		Function func = new SampleUDF( null, Key.of( "Func" ), null, null, null );
		assertThat( FunctionCaster.cast( func ).getName() ).isEqualTo( Key.of( "Func" ) );
	}

	@DisplayName( "It can not cast a non-function" )
	@Test
	void testItCanNotCastANonFunction() {
		assertThrows( BoxLangException.class, () -> FunctionCaster.cast( "" ) );
		assertThrows( BoxLangException.class, () -> FunctionCaster.cast( null ) );
		assertThrows( BoxLangException.class, () -> FunctionCaster.cast( 5 ) );
		assertThrows( BoxLangException.class, () -> FunctionCaster.cast( new Struct() ) );

	}

	@DisplayName( "It can attempt to cast" )
	@Test
	void testItCanAttemptToCast() {
		Function				func	= new SampleUDF( null, Key.of( "Func" ), null, null, null );
		CastAttempt<Function>	attempt	= FunctionCaster.attempt( func );
		assertThat( attempt.wasSuccessful() ).isTrue();
		assertThat( attempt.get().getName() ).isEqualTo( Key.of( "Func" ) );
		assertThat( attempt.ifSuccessful( ( v ) -> System.out.println( v ) ) );

		final CastAttempt<Function> attempt2 = FunctionCaster.attempt( "Brad" );
		assertThat( attempt2.wasSuccessful() ).isFalse();

		assertThrows( BoxLangException.class, () -> attempt2.get() );
		assertThat( attempt2.ifSuccessful( ( v ) -> System.out.println( v ) ) );
	}

}
