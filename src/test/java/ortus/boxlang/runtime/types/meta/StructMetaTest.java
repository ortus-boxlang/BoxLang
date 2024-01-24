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

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.immutable.ImmutableStruct;

public class StructMetaTest {

	private IBoxContext context = new ScriptingRequestBoxContext();

	@DisplayName( "Test struct meta" )
	@Test
	void testStructMeta() {

		IStruct		str	= new Struct();
		StructMeta	$bx	= ( StructMeta ) Referencer.get( context, str, BoxMeta.key, false );

		assertThat( $bx.$class ).isEqualTo( Struct.class );
		assertThat( $bx.meta instanceof IStruct ).isTrue();
		assertThat( $bx.meta.containsKey( "type" ) ).isTrue();
		assertThat( $bx.meta.containsKey( "immutable" ) ).isTrue();
		assertThat( $bx.meta.get( "type" ) ).isEqualTo( "DEFAULT" );
		assertThat( $bx.meta.get( "immutable" ) ).isEqualTo( false );

		str	= new ImmutableStruct( Struct.TYPES.SORTED );
		$bx	= ( StructMeta ) Referencer.get( context, str, BoxMeta.key, false );

		assertThat( $bx.$class ).isEqualTo( ImmutableStruct.class );
		assertThat( $bx.meta instanceof IStruct ).isTrue();
		assertThat( $bx.meta.containsKey( "type" ) ).isTrue();
		assertThat( $bx.meta.containsKey( "immutable" ) ).isTrue();
		assertThat( $bx.meta.get( "type" ) ).isEqualTo( "SORTED" );
		assertThat( $bx.meta.get( "immutable" ) ).isEqualTo( true );

	}

	@DisplayName( "Test struct change listener" )
	@Test
	void testStructListener() {

		Key			bradKey	= Key.of( "brad" );
		IStruct		str		= new Struct();
		StructMeta	$bx		= ( StructMeta ) Referencer.get( context, str, BoxMeta.key, false );

		// Listens to all keys
		$bx.registerChangeListener( ( key, newValue, oldValue ) -> {
			assertThat( newValue == null ).isEqualTo( false );
			assertThat( oldValue == null ).isEqualTo( true );
			System.out.println( "setting '" + newValue + "' into key " + key.getName() );
			return newValue;
		} );

		// Listens for key "brad" only
		$bx.registerChangeListener( bradKey, ( key, newValue, oldValue ) -> {
			assertThat( key ).isEqualTo( bradKey );
			assertThat( newValue ).isEqualTo( "wood" );

			// Override the value that's set
			return "woods";
		} );

		str.put( "foo", "bar" );
		str.put( "baz", "bum" );
		str.put( "brad", "wood" );

		// Check overridden value
		assertThat( str.get( "brad" ) ).isEqualTo( "woods" );

		$bx.removeChangeListener( bradKey );
		$bx.removeChangeListener( IListenable.ALL_KEYS );

		str.put( "luis", "majano" );
		str.put( "jorge", "reyes" );
		str.put( "brad", "pitt" );
	}

}
