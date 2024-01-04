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
package ortus.boxlang.runtime.types.immutable;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.UnmodifiableException;

public class ImmutableStructTest {

	private IBoxContext context = new ScriptingBoxContext();

	@DisplayName( "Test equals and hash code with no data" )
	@Test
	void testEqualsAndHashCode() {
		ImmutableStruct	immutablestruct1	= new ImmutableStruct();
		ImmutableStruct	immutablestruct2	= new ImmutableStruct();

		// Test equals()
		assertThat( immutablestruct1 ).isEqualTo( immutablestruct2 );

		// Test hashCode()
		assertThat( immutablestruct1.hashCode() ).isEqualTo( immutablestruct2.hashCode() );
	}

	@Test
	void testCanImmutableStructOf() {
		ImmutableStruct immutablestruct = ImmutableStruct.of(
		    "foo", "bar",
		    Key.of( "baz" ), "bum",
		    5D, "Brad"
		);

		assertThat( immutablestruct.size() ).isEqualTo( 3 );
		assertThat( immutablestruct.get( Key.of( "foo" ) ) ).isEqualTo( "bar" );
		assertThat( immutablestruct.get( Key.of( "baz" ) ) ).isEqualTo( "bum" );
		assertThat( immutablestruct.get( Key.of( "5" ) ) ).isEqualTo( "Brad" );

		assertThrows( Throwable.class, () -> ImmutableStruct.of( "test" ) );
		assertThrows( Throwable.class, () -> ImmutableStruct.of( null, "foo" ) );
		assertThrows( Throwable.class, () -> ImmutableStruct.of( new HashMap<Object, Object>(), "foo" ) );

		immutablestruct = ImmutableStruct.of();
		assertThat( immutablestruct.size() ).isEqualTo( 0 );
	}

	@Test
	void testImmutability() {
		final ImmutableStruct immutablestruct = ImmutableStruct.of();

		assertThat( immutablestruct instanceof IImmutable ).isTrue();
		assertThrows( UnmodifiableException.class, () -> immutablestruct.assign( context, Key.of( "brad" ), "" ) );
		assertThrows( UnmodifiableException.class, () -> immutablestruct.clear() );
		assertThrows( UnmodifiableException.class, () -> immutablestruct.addAll( Map.of() ) );
		assertThrows( UnmodifiableException.class, () -> immutablestruct.putAll( Map.of() ) );
		assertThrows( UnmodifiableException.class, () -> immutablestruct.remove( "" ) );
		assertThrows( UnmodifiableException.class, () -> immutablestruct.remove( Key.of( "brad" ) ) );
		assertThrows( UnmodifiableException.class, () -> immutablestruct.putIfAbsent( Key.of( "brad" ), "" ) );
		assertThrows( UnmodifiableException.class, () -> immutablestruct.put( Key.of( "brad" ), "" ) );
	}
}
