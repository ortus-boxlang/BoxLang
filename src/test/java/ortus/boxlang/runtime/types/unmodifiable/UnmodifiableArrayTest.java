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

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.exceptions.UnmodifiableException;

public class UnmodifiableArrayTest {

	private IBoxContext context = new ScriptingRequestBoxContext();

	@DisplayName( "Test Constructors" )
	@Test
	void testConstructors() {
		final UnmodifiableArray unmodifiablearray = new UnmodifiableArray();

		assertThat( unmodifiablearray instanceof IUnmodifiable ).isTrue();

		assertThat( unmodifiablearray.size() ).isEqualTo( 0 );
		assertThrows( UnmodifiableException.class, () -> unmodifiablearray.assign( context, Key.of( "-1" ), "foo" ) );
		assertThrows( UnmodifiableException.class, () -> unmodifiablearray.add( "foo" ) );
		assertThrows( UnmodifiableException.class, () -> unmodifiablearray.clear() );
		assertThrows( UnmodifiableException.class, () -> unmodifiablearray.remove( 0 ) );
		assertThrows( UnmodifiableException.class, () -> unmodifiablearray.removeAll( List.of() ) );
		assertThrows( UnmodifiableException.class, () -> unmodifiablearray.retainAll( List.of() ) );
		assertThrows( UnmodifiableException.class, () -> unmodifiablearray.addAll( List.of() ) );
		assertThrows( UnmodifiableException.class, () -> unmodifiablearray.remove( "" ) );

		Array unmodifiablearray2 = new UnmodifiableArray( 50 );
		assertThat( unmodifiablearray2.size() ).isEqualTo( 0 );

		unmodifiablearray2 = new UnmodifiableArray( new Object[] { "foo", "bar" } );
		assertThat( unmodifiablearray2.size() ).isEqualTo( 2 );

		unmodifiablearray2 = new UnmodifiableArray( List.of( "foo", "bar", "baz" ) );
		assertThat( unmodifiablearray2.size() ).isEqualTo( 3 );

		unmodifiablearray2 = UnmodifiableArray.fromArray( new Object[] { "foo", "bar" } );
		assertThat( unmodifiablearray2.size() ).isEqualTo( 2 );

		unmodifiablearray2 = UnmodifiableArray.fromList( List.of( "foo", "bar", "baz" ) );
		assertThat( unmodifiablearray2.size() ).isEqualTo( 3 );

		unmodifiablearray2 = UnmodifiableArray.of( "foo", "bar", "baz" );
		assertThat( unmodifiablearray2.size() ).isEqualTo( 3 );
	}

}
