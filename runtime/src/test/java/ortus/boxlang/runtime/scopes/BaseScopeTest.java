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

import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.types.Struct;

public class BaseScopeTest {

	@Test
	public void testConstructor() {
		IScope scope = new ServerScope();

		assertThat( scope.size() ).isGreaterThan( 0 );
		assertThat( scope.containsKey( Key.of( "os" ) ) ).isTrue();
		assertThat( scope.containsKey( Key.of( "java" ) ) ).isTrue();

		assertThat( scope.containsKey( Key.of( "separator" ) ) ).isTrue();
		Struct separator = ( Struct ) scope.get( Key.of( "separator" ) );
		assertThat( separator.containsKey( Key.of( "path" ) ) ).isTrue();
		assertThat( separator.get( Key.of( "path" ) ) ).isEqualTo( System.getProperty( "path.separator", "" ) );
		assertThat( separator.containsKey( Key.of( "file" ) ) ).isTrue();
		assertThat( separator.get( Key.of( "file" ) ) ).isEqualTo( System.getProperty( "file.separator", "" ) );
		assertThat( separator.containsKey( Key.of( "line" ) ) ).isTrue();
		assertThat( separator.get( Key.of( "line" ) ) ).isEqualTo( System.getProperty( "line.separator", "" ) );

		assertThat( scope.containsKey( Key.of( "system" ) ) ).isTrue();
		Struct system = ( Struct ) scope.get( Key.of( "system" ) );
		assertThat( system.containsKey( Key.of( "environment" ) ) ).isTrue();
		assertThat( system.containsKey( Key.of( "properties" ) ) ).isTrue();

	}

}
