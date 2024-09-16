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
package ortus.boxlang.runtime.config;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;

public class CLIOptionsTest {

	@DisplayName( "Test CLIOptions" )
	@Test
	void testCLIOptions() {
		CLIOptions options = new CLIOptions(
		    "templatePath",
		    true,
		    "code",
		    "configFile",
		    true,
		    true,
		    "runtimeHome",
		    true,
		    List.of( "cliArgs" ),
		    new String[] { "cliArgsRaw" },
		    "targetModule",
		    "actionCommand"
		);

		assertThat( options.templatePath() ).isEqualTo( "templatePath" );
		assertThat( options.debug() ).isTrue();
		assertThat( options.code() ).isEqualTo( "code" );
		assertThat( options.configFile() ).isEqualTo( "configFile" );
		assertThat( options.printAST() ).isTrue();
		assertThat( options.transpile() ).isTrue();
		assertThat( options.runtimeHome() ).isEqualTo( "runtimeHome" );
		assertThat( options.showVersion() ).isTrue();
		assertThat( options.cliArgs() ).containsExactly( "cliArgs" );
		assertThat( options.targetModule() ).isEqualTo( "targetModule" );
		assertThat( options.actionCommand() ).isEqualTo( "actionCommand" );
	}

	@DisplayName( "Can build an options map using all permutations" )
	@Test
	void testOptionsMap() {
		CLIOptions	options		= new CLIOptions(
		    "templatePath",
		    true,
		    "code",
		    "configFile",
		    true,
		    true,
		    "runtimeHome",
		    true,
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
		    new String[] {},
		    "targetModule",
		    "actionCommand"
		);

		IStruct		results		= options.parseArguments();
		IStruct		optionsMap	= results.getAsStruct( Key.options );
		Array		positionals	= results.getAsArray( Key.positionals );

		System.out.println( results );

		assertThat( optionsMap.getAsBoolean( Key.of( "debug" ) ) ).isTrue();
		assertThat( optionsMap.getAsBoolean( Key.of( "verbose" ) ) ).isFalse();
		assertThat( optionsMap.getAsString( Key.of( "bundles" ) ) ).isEqualTo( "Spec" );
		assertThat( optionsMap.getAsString( Key.of( "o" ) ) ).isEqualTo( "/path/to/file" );
		assertThat( optionsMap.getAsBoolean( Key.of( "v" ) ) ).isTrue();
		assertThat( optionsMap.getAsString( Key.of( "directory" ) ) ).isEqualTo( "" );
		assertThat( optionsMap.getAsString( Key.of( "d" ) ) ).isEqualTo( "" );
		assertThat( optionsMap.getAsBoolean( Key.of( "a" ) ) ).isTrue();
		assertThat( optionsMap.getAsBoolean( Key.of( "b" ) ) ).isTrue();
		assertThat( optionsMap.getAsBoolean( Key.of( "c" ) ) ).isTrue();

		// Positionals
		assertThat( positionals.size() ).isEqualTo( 2 );
		assertThat( positionals.get( 0 ) ).isEqualTo( "path/to/template.bxs" );
		assertThat( positionals.get( 1 ) ).isEqualTo( "targetModule" );

	}

}
