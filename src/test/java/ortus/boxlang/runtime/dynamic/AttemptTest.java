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
package ortus.boxlang.runtime.dynamic;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.NoElementException;

public class AttemptTest {

	private Attempt		attempt;
	private IBoxContext	context	= new ScriptingRequestBoxContext();

	@DisplayName( "Test creation with no values" )
	@Test
	void testCreation() {
		attempt = new Attempt();
		// Assertions
		assertThat( attempt ).isNotNull();
		assertThrows( NoElementException.class, () -> {
			attempt.get();
		} );
		assertThat( attempt.isEmpty() ).isTrue();
		assertThat( attempt.isPresent() ).isFalse();

		attempt.ifPresentOrElse( ( value ) -> {
			assertThat( false ).isTrue();
		}, () -> {
			assertThat( true ).isTrue();
		} );

		assertThrows( RuntimeException.class, () -> {
			attempt.ifEmpty( () -> {
				throw new RuntimeException( "This should not be called" );
			} );
		} );

		var result = attempt.or( () -> new Attempt( "test" ) );
		assertThat( result.get() ).isEqualTo( "test" );

		var result2 = attempt.orElse( "test" );
		assertThat( result2 ).isEqualTo( "test" );

		result2 = attempt.orElseGet( () -> "test" );
		assertThat( result2 ).isEqualTo( "test" );

		assertThat( attempt.map( ( value ) -> {
			return value;
		} ).isEmpty() ).isTrue();

		assertThrows( NoElementException.class, () -> {
			attempt.orThrow();
		} );

		assertThrows( NoElementException.class, () -> {
			attempt.orThrow( "Invalid" );
		} );

		assertThrows( BoxRuntimeException.class, () -> {
			attempt.orThrow( new BoxRuntimeException( "invalid" ) );
		} );

		assertThat( attempt.stream() ).isEmpty();
		assertThat( attempt.toString() ).isEqualTo( "Attempt.empty" );
	}

	@DisplayName( "Test creation with values" )
	@Test
	void testCreationWithValues() {
		attempt = new Attempt( "test" );
		// Assertions
		assertThat( attempt ).isNotNull();
		assertThat( attempt.get() ).isEqualTo( "test" );
		assertThat( attempt.isEmpty() ).isFalse();
		assertThat( attempt.isPresent() ).isTrue();

		attempt.ifPresentOrElse( ( value ) -> {
			assertThat( false ).isFalse();
		}, () -> {
			assertThat( true ).isFalse();
		} );

		attempt.ifEmpty( () -> {
			throw new RuntimeException( "This should not be called" );
		} );

		var result = attempt.or( () -> new Attempt( "bogus" ) );
		assertThat( result.get() ).isEqualTo( "test" );

		var result2 = attempt.orElse( "boxlang" );
		assertThat( result2 ).isEqualTo( "test" );

		result2 = attempt.orElseGet( () -> "boxlang" );
		assertThat( result2 ).isEqualTo( "test" );

		assertThat( attempt.map( value -> {
			return value + " mapped";
		} ).get() ).isEqualTo( "test mapped" );

		attempt.orThrow();
		attempt.orThrow( "Invalid" );
		attempt.orThrow( new BoxRuntimeException( "invalid" ) );

		assertThat( attempt.stream() ).containsExactly( "test" );
		assertThat( attempt.toString() ).isEqualTo( "Attempt[test]" );
	}

	@Test
	void testValidationPredicate() {
		attempt = new Attempt( "test" );

		attempt.toBeValid( result -> {
			var value = ( String ) result;
			return value.isBlank() == false;
		} );

		assertThat( attempt.isValid() ).isTrue();

	}

}
