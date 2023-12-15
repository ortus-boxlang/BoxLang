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
package ortus.boxlang.runtime.scopes;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.types.Struct;

public class ArgumentsScopeTest {

	private static ArgumentsScope scope;

	@BeforeAll
	public static void setUp() {
		scope = new ArgumentsScope();
	}

	@Test
	void testBasicGetAndSet() {

		Key	first	= Key.of( "first" );
		Key	second	= Key.of( "second" );
		Key	third	= Key.of( "third" );
		// set string keys in order
		scope.assign( first, "brad" );
		scope.assign( second, "luis" );
		scope.assign( third, "jorge" );

		// check string keys
		assertThat( scope.dereference( first, false ) ).isEqualTo( "brad" );
		assertThat( scope.dereference( second, false ) ).isEqualTo( "luis" );
		assertThat( scope.dereference( third, false ) ).isEqualTo( "jorge" );

		// check numeric keys
		assertThat( scope.dereference( Key._1, false ) ).isEqualTo( "brad" );
		assertThat( scope.dereference( Key._2, false ) ).isEqualTo( "luis" );
		assertThat( scope.dereference( Key._3, false ) ).isEqualTo( "jorge" );

		// override key by name
		scope.assign( first, "gavin" );
		assertThat( scope.dereference( first, false ) ).isEqualTo( "gavin" );
		assertThat( scope.dereference( Key._1, false ) ).isEqualTo( "gavin" );

		// override key by number
		scope.assign( Key._2, "edgardo" );
		assertThat( scope.dereference( second, false ) ).isEqualTo( "edgardo" );
		assertThat( scope.dereference( Key._2, false ) ).isEqualTo( "edgardo" );

		// add
		scope.assign( Key._4, "eric" );
		assertThat( scope.dereference( Key._4, false ) ).isEqualTo( "eric" );

		Object[] array = scope.asNativeArray();
		assertThat( array ).isNotNull();
		assertThat( array.length ).isEqualTo( 4 );
		assertThat( array[ 0 ] ).isEqualTo( "gavin" );	// first
		assertThat( array[ 1 ] ).isEqualTo( "edgardo" );	// second
		assertThat( array[ 2 ] ).isEqualTo( "jorge" );	// third
		assertThat( array[ 3 ] ).isEqualTo( "eric" );	// _4

		Struct struct = scope.asStruct();
		assertThat( struct ).isNotNull();
		assertThat( struct.size() ).isEqualTo( 4 );
		assertThat( struct.get( first ) ).isEqualTo( "gavin" );	// first
		assertThat( struct.get( second ) ).isEqualTo( "edgardo" );	// second
		assertThat( struct.get( third ) ).isEqualTo( "jorge" );	// third
		assertThat( struct.get( Key._4 ) ).isEqualTo( "eric" );	// _4

	}

}
