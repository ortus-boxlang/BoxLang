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

import java.util.List;

import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.config.CLIOptions;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.types.IStruct;

public class ServerScopeTest {

	BoxRuntime	runtime;
	IBoxContext	context;

	@Test
	public void testConstructor() {
		CLIOptions options = new CLIOptions(
		    null,
		    true,
		    null,
		    null,
		    false,
		    false,
		    null,
		    false,
		    List.of( "path/to/template.bxs",
		        "--debug",
		        "--!verbose",
		        "--bundles=Spec",
		        "-o='/path/to/file'",
		        "-v",
		        "--directory=",
		        "-d=",
		        "targetModule",
		        "-abc"
		    ),
		    new String[] {
		        "path/to/template.bxs",
		        "--debug",
		        "--!verbose",
		        "--bundles=Spec",
		        "-o='/path/to/file'",
		        "-v",
		        "--directory=",
		        "-d=",
		        "targetModule",
		        "-abc"
		    },
		    null,
		    null
		);
		runtime	= BoxRuntime.getInstance( options );
		context	= new ScriptingRequestBoxContext();
		IScope scope = new ServerScope().initialize();

		System.out.println( scope.get( "cli" ) );

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
		context = new ScriptingRequestBoxContext();
		IScope scope = new ServerScope().initialize();
		scope.assign( context, Key.of( "brad" ), "wood" );
		scope.put( Key.of( "luis" ), "majano" );

		assertThrows( Throwable.class, () -> scope.assign( context, Key.of( "java" ), "" ) );
		assertThrows( Throwable.class, () -> scope.put( Key.of( "os" ), "" ) );

	}

}
