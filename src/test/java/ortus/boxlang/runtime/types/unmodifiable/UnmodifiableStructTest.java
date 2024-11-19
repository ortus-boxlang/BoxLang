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
package ortus.boxlang.runtime.types.unmodifiable;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.UnmodifiableException;

public class UnmodifiableStructTest {

	private IBoxContext context = new ScriptingRequestBoxContext();

	@DisplayName( "Test equals and hash code with no data" )
	@Test
	void testEqualsAndHashCode() {
		UnmodifiableStruct	unmodifiablestruct1	= new UnmodifiableStruct();
		UnmodifiableStruct	unmodifiablestruct2	= new UnmodifiableStruct();

		// Test equals()
		assertThat( unmodifiablestruct1 ).isEqualTo( unmodifiablestruct2 );

		// Test hashCode()
		assertThat( unmodifiablestruct1.hashCode() ).isEqualTo( unmodifiablestruct2.hashCode() );
	}

	@Test
	void testCanUnmodifiableStructOf() {
		UnmodifiableStruct unmodifiablestruct = UnmodifiableStruct.of(
		    "foo", "bar",
		    Key.of( "baz" ), "bum",
		    5D, "Brad"
		);

		assertThat( unmodifiablestruct.size() ).isEqualTo( 3 );
		assertThat( unmodifiablestruct.get( Key.of( "foo" ) ) ).isEqualTo( "bar" );
		assertThat( unmodifiablestruct.get( Key.of( "baz" ) ) ).isEqualTo( "bum" );
		assertThat( unmodifiablestruct.get( Key.of( "5" ) ) ).isEqualTo( "Brad" );

		assertThrows( Throwable.class, () -> UnmodifiableStruct.of( "test" ) );
		assertThrows( Throwable.class, () -> UnmodifiableStruct.of( null, "foo" ) );
		assertThrows( Throwable.class, () -> UnmodifiableStruct.of( new HashMap<Object, Object>(), "foo" ) );

		unmodifiablestruct = UnmodifiableStruct.of();
		assertThat( unmodifiablestruct.size() ).isEqualTo( 0 );
	}

	@Test
	void testImmutability() {
		final UnmodifiableStruct unmodifiablestruct = UnmodifiableStruct.of();

		assertThat( unmodifiablestruct instanceof IUnmodifiable ).isTrue();
		assertThrows( UnmodifiableException.class, () -> unmodifiablestruct.assign( context, Key.of( "brad" ), "" ) );
		assertThrows( UnmodifiableException.class, () -> unmodifiablestruct.clear() );
		assertThrows( UnmodifiableException.class, () -> unmodifiablestruct.addAll( Map.of() ) );
		assertThrows( UnmodifiableException.class, () -> unmodifiablestruct.putAll( Map.of() ) );
		assertThrows( UnmodifiableException.class, () -> unmodifiablestruct.remove( "" ) );
		assertThrows( UnmodifiableException.class, () -> unmodifiablestruct.remove( Key.of( "brad" ) ) );
		assertThrows( UnmodifiableException.class, () -> unmodifiablestruct.putIfAbsent( Key.of( "brad" ), "" ) );
		assertThrows( UnmodifiableException.class, () -> unmodifiablestruct.put( Key.of( "brad" ), "" ) );
	}
}
