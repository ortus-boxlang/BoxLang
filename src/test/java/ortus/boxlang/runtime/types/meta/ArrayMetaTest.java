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
package ortus.boxlang.runtime.types.meta;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.Struct;

public class ArrayMetaTest {

	@DisplayName( "Test array meta" )
	@Test
	void testArrayMeta() {

		Array		arr	= new Array();
		GenericMeta	$bx	= ( GenericMeta ) Referencer.get( arr, BoxMeta.key, false );

		assertThat( $bx.$class ).isEqualTo( Array.class );
		assertThat( $bx.meta instanceof Struct ).isTrue();

	}

	@DisplayName( "Test array change listener" )
	@Test
	void testArrayListener() {

		Array		arr	= new Array();
		GenericMeta	$bx	= ( GenericMeta ) Referencer.get( arr, BoxMeta.key, false );

		// Listens to all keys
		$bx.registerChangeListener( ( key, newValue, oldValue ) -> {
			assertThat( newValue == null ).isEqualTo( false );
			assertThat( oldValue == null ).isEqualTo( true );
			System.out.println( "setting '" + newValue + "' into key " + key.getName() );
			return newValue;
		} );

		// Listens for key 3 only
		$bx.registerChangeListener( Key.of( "3" ), ( key, newValue, oldValue ) -> {
			assertThat( key ).isEqualTo( Key.of( "3" ) );
			assertThat( newValue ).isEqualTo( "baz" );

			// Override the value that's set
			return "bum";
		} );

		arr.add( "foo" );
		arr.add( "bar" );
		arr.add( "baz" );

		// Check overridden value
		assertThat( arr.get( 2 ) ).isEqualTo( "bum" );

	}

}
