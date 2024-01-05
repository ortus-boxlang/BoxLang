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

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingBoxContext;
import ortus.boxlang.runtime.types.Struct;

public class ArgumentsScopeTest {

	private static ArgumentsScope	scope;
	private IBoxContext				context	= new ScriptingBoxContext();

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
		scope.assign( context, first, "brad" );
		scope.assign( context, second, "luis" );
		scope.assign( context, third, "jorge" );

		// check string keys
		assertThat( scope.get( first ) ).isEqualTo( "brad" );
		assertThat( scope.get( second ) ).isEqualTo( "luis" );
		assertThat( scope.get( third ) ).isEqualTo( "jorge" );

		// check numeric keys
		assertThat( scope.get( Key._1 ) ).isEqualTo( "brad" );
		assertThat( scope.get( Key._2 ) ).isEqualTo( "luis" );
		assertThat( scope.get( Key._3 ) ).isEqualTo( "jorge" );

		// override key by name
		scope.assign( context, first, "gavin" );
		assertThat( scope.get( first ) ).isEqualTo( "gavin" );
		assertThat( scope.get( Key._1 ) ).isEqualTo( "gavin" );
		assertThat( scope.get( Key.of( "1" ) ) ).isEqualTo( "gavin" );

		// override key by number
		scope.assign( context, Key._2, "edgardo" );
		assertThat( scope.get( second ) ).isEqualTo( "edgardo" );
		assertThat( scope.get( Key._2 ) ).isEqualTo( "edgardo" );
		assertThat( scope.get( Key.of( "2" ) ) ).isEqualTo( "edgardo" );

		// add
		scope.assign( context, Key._4, "eric" );
		assertThat( scope.get( Key._4 ) ).isEqualTo( "eric" );

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
