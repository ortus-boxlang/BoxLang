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
package ortus.boxlang.runtime.dynamic.casters;

import static com.google.common.truth.Truth.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class CollectionCasterTest {

	@DisplayName( "It can cast a List to a Collection" )
	@Test
	void testItCanCastAList() {
		Collection<Object> result = CollectionCaster.cast( Arrays.asList( new Object[] { "Brad", "Wood" } ) );
		assertThat( result instanceof Collection ).isTrue();
		Iterator<Object> it = result.iterator();
		assertThat( it.next() ).isEqualTo( "Brad" );
		assertThat( it.next() ).isEqualTo( "Wood" );
	}

	@DisplayName( "It can cast an Array to a Collection" )
	@Test
	void testItCanCastAnArray() {
		Collection<Object> result = CollectionCaster.cast( new Object[] { "Brad", "Wood" } );
		assertThat( result instanceof Collection ).isTrue();
		Iterator<Object> it = result.iterator();
		assertThat( it.next() ).isEqualTo( "Brad" );
		assertThat( it.next() ).isEqualTo( "Wood" );
	}

	@DisplayName( "It can cast a Map to a Collection" )
	@Test
	void testItCanCastAMap() {
		Collection<Object> result = CollectionCaster.cast( Map.of( "Brad", "Wood", "Luis", "Majano" ) );
		assertThat( result instanceof Collection ).isTrue();
		Iterator<Object> it = result.iterator();
		assertThat( result.contains( "Brad" ) ).isTrue();
		assertThat( result.contains( "Luis" ) ).isTrue();
	}

	@DisplayName( "It can cast a Scope to a Collection" )
	@Test
	void testItCanCastAScope() {
		IScope scope = new VariablesScope();
		scope.putAll( Map.of( Key.of( "Brad" ), "Wood", Key.of( "Luis" ), "Majano" ) );
		Collection<Object> result = CollectionCaster.cast( scope );
		assertThat( result instanceof Collection ).isTrue();
		Iterator<Object> it = result.iterator();
		assertThat( result.contains( "Brad" ) ).isTrue();
		assertThat( result.contains( "Luis" ) ).isTrue();
	}

}
