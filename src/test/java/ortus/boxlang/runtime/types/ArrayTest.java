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
package ortus.boxlang.runtime.types;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;

public class ArrayTest {

	private IBoxContext context = new ScriptingRequestBoxContext();

	@DisplayName( "Test Constructors" )
	@Test
	void testConstructors() {
		Array array = new Array();
		assertThat( array.size() ).isEqualTo( 0 );
		array.add( "foo" );
		int i = array.append( "bar" );
		assertThat( array.size() ).isEqualTo( 2 );
		assertThat( i ).isEqualTo( 2 );

		array = new Array( 50 );
		assertThat( array.size() ).isEqualTo( 0 );

		array = new Array( new Object[] { "foo", "bar" } );
		assertThat( array.size() ).isEqualTo( 2 );

		array = new Array( List.of( "foo", "bar", "baz" ) );
		assertThat( array.size() ).isEqualTo( 3 );

		array = Array.fromArray( new Object[] { "foo", "bar" } );
		assertThat( array.size() ).isEqualTo( 2 );

		array = Array.fromList( List.of( "foo", "bar", "baz" ) );
		assertThat( array.size() ).isEqualTo( 3 );

		array = Array.of( "foo", "bar", "baz" );
		assertThat( array.size() ).isEqualTo( 3 );
	}

	@DisplayName( "Test referencing" )
	@Test
	void testReferencing() {
		Array array = new Array();
		array.assign( context, Key.of( "1" ), "foo" );
		array.assign( context, Key.of( 1 ), "foo" );
		assertThat( array.size() ).isEqualTo( 1 );
		assertThat( array.get( 0 ) ).isEqualTo( "foo" );
		assertThat( array.dereference( context, Key.of( "1" ), false ) ).isEqualTo( "foo" );
		assertThat( array.dereference( context, Key.of( 1 ), false ) ).isEqualTo( "foo" );
		assertThat( array.dereferenceAndInvoke( new ScriptingRequestBoxContext(), Key.of( "get" ), new Object[] { 0 }, false ) ).isEqualTo( "foo" );

		// Can't reference negative, string, non-int, or out-of-bounds indexes
		assertThrows( Throwable.class, () -> array.assign( context, Key.of( "-1" ), "foo" ) );
		assertThrows( Throwable.class, () -> array.assign( context, Key.of( "sdf" ), "foo" ) );
		assertThrows( Throwable.class, () -> array.dereference( context, Key.of( "-1" ), false ) );
		assertThrows( Throwable.class, () -> array.dereference( context, Key.of( "1.5" ), false ) );
		assertThrows( Throwable.class, () -> array.dereference( context, Key.of( "sdf" ), false ) );
		assertThrows( Throwable.class, () -> array.dereference( context, Key.of( "999" ), false ) );

		// Unless we're playing it safe, then anything goes.
		assertThat( array.dereference( context, Key.of( "99999" ), true ) ).isEqualTo( null );
		assertThat( array.dereference( context, Key.of( "-1" ), true ) ).isEqualTo( null );
		assertThat( array.dereference( context, Key.of( "1.5" ), true ) ).isEqualTo( null );
		assertThat( array.dereference( context, Key.of( "sdf" ), true ) ).isEqualTo( null );
		assertThat( array.dereference( context, Key.of( "999" ), true ) ).isEqualTo( null );

		// Auto-expand the array with nulls
		array.assign( context, Key.of( "100" ), "foo100" );
		array.assign( context, Key.of( 100 ), "foo100" );
		assertThat( array.size() ).isEqualTo( 100 );
		assertThat( array.dereference( context, Key.of( "100" ), false ) ).isEqualTo( "foo100" );
		assertThat( array.dereference( context, Key.of( 100 ), false ) ).isEqualTo( "foo100" );
		assertThat( array.dereference( context, Key.of( "99" ), false ) ).isEqualTo( null );
		assertThat( array.dereference( context, Key.of( 99 ), false ) ).isEqualTo( null );
	}

}
