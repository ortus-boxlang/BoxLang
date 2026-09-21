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
package ortus.boxlang.runtime;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.config.CLIOptions;
import ortus.boxlang.runtime.util.ConfigSecretUtil;

class BoxRunnerTest {

	@Test
	void appCanGreat() {
		new BoxRunner();
	}

	/**
	 * Verifies the generateSecret action produces a prefixed value that the active runtime can decrypt.
	 */
	@DisplayName( "generateSecret encrypts plaintext with the active runtime configuration" )
	@Test
	void testGenerateSecretAction() {
		BoxRuntime				runtime		= BoxRuntime.getInstance( true );
		CLIOptions				options		= new CLIOptions( null, null, null, null, false, false, null, false, false, List.of( "my-sensitive-value" ),
		    new String[ 0 ], null, "generatesecret" );
		PrintStream				original	= System.out;
		ByteArrayOutputStream	capture		= new ByteArrayOutputStream();
		System.setOut( new PrintStream( capture ) );
		try {
			BoxRunner.runActionCommand( options, runtime );
		} finally {
			System.setOut( original );
		}

		String secret = capture.toString().trim();
		assertThat( ConfigSecretUtil.isEncrypted( secret ) ).isTrue();
		assertThat( ConfigSecretUtil.decryptIfEncrypted( secret ) ).isEqualTo( "my-sensitive-value" );
	}

	@DisplayName( "It can execute a task template" )
	@Test
	@Disabled
	public void testItCanExecuteATaskTemplate() {
		String		testTemplate	= "src/test/bx/Task.bx";
		String[]	args			= { testTemplate };

		assertDoesNotThrow( () -> BoxRunner.main( args ) );
	}

	@DisplayName( "It can execute a task template with arguments" )
	@Test
	@Disabled
	public void testItCanExecuteATaskWithArgs() {
		String		testTemplate	= "src/test/bx/Task.bx";
		String[]	args			= { testTemplate, "hola", "luis" };

		assertDoesNotThrow( () -> BoxRunner.main( args ) );
	}

	@Test
	@Disabled
	public void testExecuteClassNoExtension() {
		String		testTemplate	= "src/test/bx/Task";
		String[]	args			= { testTemplate, "hola", "luis" };

		BoxRunner.main( args );
	}

	@DisplayName( "printSourceAST defaults to BoxScript for inline code" )
	@Test
	public void testPrintASTDefaultsToBoxScript() {
		BoxRuntime				runtime		= BoxRuntime.getInstance( true );
		PrintStream				original	= System.out;
		ByteArrayOutputStream	capture		= new ByteArrayOutputStream();
		System.setOut( new PrintStream( capture ) );
		try {
			assertDoesNotThrow( () -> runtime.printSourceAST( "x = 1 + 2" ) );
		} finally {
			System.setOut( original );
		}
		String output = capture.toString();
		assertThat( output ).contains( "BoxScript" );
	}

	@DisplayName( "printSourceAST parses a .bxm template correctly" )
	@Test
	public void testPrintASTForBxmTemplate() {
		BoxRuntime				runtime		= BoxRuntime.getInstance( true );
		String					source		= "<bx:output>Hello World</bx:output>";
		PrintStream				original	= System.out;
		ByteArrayOutputStream	capture		= new ByteArrayOutputStream();
		System.setOut( new PrintStream( capture ) );
		try {
			assertDoesNotThrow( () -> runtime.printSourceAST( source, BoxSourceType.BOXTEMPLATE ) );
		} finally {
			System.setOut( original );
		}
		String output = capture.toString();
		assertThat( output ).isNotEmpty();
		assertThat( output ).contains( "BoxTemplate" );
	}

	@DisplayName( "printSourceAST parses a .cfm template correctly" )
	@Test
	public void testPrintASTForCfmTemplate() {
		BoxRuntime				runtime		= BoxRuntime.getInstance( true );
		String					source		= "<cfoutput>Hello World</cfoutput>";
		PrintStream				original	= System.out;
		ByteArrayOutputStream	capture		= new ByteArrayOutputStream();
		System.setOut( new PrintStream( capture ) );
		try {
			assertDoesNotThrow( () -> runtime.printSourceAST( source, BoxSourceType.CFTEMPLATE ) );
		} finally {
			System.setOut( original );
		}
		String output = capture.toString();
		assertThat( output ).isNotEmpty();
		assertThat( output ).contains( "BoxTemplate" );
	}

}
