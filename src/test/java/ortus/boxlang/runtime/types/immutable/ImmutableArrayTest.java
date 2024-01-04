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

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.exceptions.UnmodifiableException;

public class ImmutableArrayTest {

	private IBoxContext context = new ScriptingBoxContext();

	@DisplayName( "Test Constructors" )
	@Test
	void testConstructors() {
		final ImmutableArray immutablearray = new ImmutableArray();

		assertThat( immutablearray instanceof IImmutable ).isTrue();

		assertThat( immutablearray.size() ).isEqualTo( 0 );
		assertThrows( UnmodifiableException.class, () -> immutablearray.assign( context, Key.of( "-1" ), "foo" ) );
		assertThrows( UnmodifiableException.class, () -> immutablearray.add( "foo" ) );
		assertThrows( UnmodifiableException.class, () -> immutablearray.clear() );
		assertThrows( UnmodifiableException.class, () -> immutablearray.remove( 0 ) );
		assertThrows( UnmodifiableException.class, () -> immutablearray.removeAll( List.of() ) );
		assertThrows( UnmodifiableException.class, () -> immutablearray.retainAll( List.of() ) );
		assertThrows( UnmodifiableException.class, () -> immutablearray.addAll( List.of() ) );
		assertThrows( UnmodifiableException.class, () -> immutablearray.remove( "" ) );

		Array immutablearray2 = new ImmutableArray( 50 );
		assertThat( immutablearray2.size() ).isEqualTo( 0 );

		immutablearray2 = new ImmutableArray( new Object[] { "foo", "bar" } );
		assertThat( immutablearray2.size() ).isEqualTo( 2 );

		immutablearray2 = new ImmutableArray( List.of( "foo", "bar", "baz" ) );
		assertThat( immutablearray2.size() ).isEqualTo( 3 );

		immutablearray2 = ImmutableArray.fromArray( new Object[] { "foo", "bar" } );
		assertThat( immutablearray2.size() ).isEqualTo( 2 );

		immutablearray2 = ImmutableArray.fromList( List.of( "foo", "bar", "baz" ) );
		assertThat( immutablearray2.size() ).isEqualTo( 3 );

		immutablearray2 = ImmutableArray.of( "foo", "bar", "baz" );
		assertThat( immutablearray2.size() ).isEqualTo( 3 );
	}

}
