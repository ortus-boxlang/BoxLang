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
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.types.IStruct;

public class ServerScopeTest {

	@Test
	public void testConstructor() {
		// The ServerScope constructor (seedScope) and initialize() both need a
		// runtime. Any instance works; the seeded values come from JVM state.
		BoxRuntime.getInstance();

		IScope scope = new ServerScope().initialize();

		assertThat( scope.size() ).isGreaterThan( 0 );
		assertThat( scope.containsKey( Key.of( "os" ) ) ).isTrue();
		assertThat( scope.containsKey( Key.of( "java" ) ) ).isTrue();

		assertThat( scope.containsKey( Key.of( "separator" ) ) ).isTrue();
		IStruct separator = ( IStruct ) scope.get( Key.of( "separator" ) );
		assertThat( separator.containsKey( Key.of( "path" ) ) ).isTrue();
		assertThat( separator.get( Key.of( "path" ) ) ).isEqualTo( System.getProperty( "path.separator", "" ) );
		assertThat( separator.containsKey( Key.of( "file" ) ) ).isTrue();
		assertThat( separator.get( Key.of( "file" ) ) ).isEqualTo( System.getProperty( "file.separator", "" ) );
		assertThat( separator.containsKey( Key.of( "line" ) ) ).isTrue();
		assertThat( separator.get( Key.of( "line" ) ) ).isEqualTo( System.getProperty( "line.separator", "" ) );

		assertThat( scope.containsKey( Key.of( "system" ) ) ).isTrue();
		IStruct system = ( IStruct ) scope.get( Key.of( "system" ) );
		assertThat( system.containsKey( Key.of( "environment" ) ) ).isTrue();
		assertThat( system.containsKey( Key.of( "properties" ) ) ).isTrue();

	}

	@Test
	void testUnmodifiableKeys() {
		BoxRuntime.getInstance( true );

		IScope scope = new ServerScope().initialize();

		// Non-unmodifiable keys can still be assigned after initialization
		scope.put( Key.of( "brad" ), "wood" );
		scope.put( Key.of( "luis" ), "majano" );

		assertThrows( Throwable.class, () -> scope.put( Key.of( "java" ), "" ) );
		assertThrows( Throwable.class, () -> scope.put( Key.of( "os" ), "" ) );

	}

}
