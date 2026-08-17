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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ortus.boxlang.runtime.config.CLIOptions;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Tests for the BoxRunner argument parsing and execution target resolution that
 * run in a vacuum: no BoxLang runtime is started, so these tests are safe to
 * run in parallel with other tests that own the shared runtime singleton.
 */
public class BoxRunnerParseTest {

	// ---------------------------------------------------------------------------
	// parseCommandLineOptions
	// ---------------------------------------------------------------------------

	@DisplayName( "It extracts the module: prefix into targetModule" )
	@Test
	void testModulePrefix() {
		CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] { "module:myModule", "arg1", "arg2" } );

		assertThat( options.targetModule() ).isEqualTo( "myModule" );
		assertThat( options.templatePath() ).isNull();
		assertThat( options.cliArgs() ).containsExactly( "arg1", "arg2" ).inOrder();
	}

	@DisplayName( "It honors --bx-home with an equals sign before a module" )
	@Test
	void testHomeEqualsBeforeModule() {
		CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] { "--bx-home=/path/to/home", "module:cli", "arg1" } );

		assertThat( options.runtimeHome() ).isEqualTo( "/path/to/home" );
		assertThat( options.targetModule() ).isEqualTo( "cli" );
		assertThat( options.cliArgs() ).containsExactly( "arg1" ).inOrder();
	}

	@DisplayName( "It extracts --bx-home placed AFTER the module so it never leaks to the module" )
	@Test
	void testHomeAfterModule() {
		CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] { "module:cli", "--bx-home=/path/to/home", "arg1" } );

		assertThat( options.runtimeHome() ).isEqualTo( "/path/to/home" );
		assertThat( options.targetModule() ).isEqualTo( "cli" );
		assertThat( options.cliArgs() ).containsExactly( "arg1" ).inOrder();
	}

	@DisplayName( "It supports --bx-config with a space-separated value" )
	@Test
	void testConfigSpaceSeparated() {
		CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] { "--bx-config", "/path/config.json", "module:cli" } );

		assertThat( options.configFile() ).isEqualTo( "/path/config.json" );
		assertThat( options.targetModule() ).isEqualTo( "cli" );
	}

	@DisplayName( "It supports --bx-config with an equals sign" )
	@Test
	void testConfigEquals() {
		CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] { "module:cli", "--bx-config=/other/config.json" } );

		assertThat( options.configFile() ).isEqualTo( "/other/config.json" );
		assertThat( options.targetModule() ).isEqualTo( "cli" );
	}

	@DisplayName( "It extracts --bx-debug from any position" )
	@Test
	void testDebugFlag() {
		CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] { "module:cli", "--bx-debug", "arg1" } );

		assertThat( options.isDebugMode() ).isTrue();
		assertThat( options.targetModule() ).isEqualTo( "cli" );
		assertThat( options.cliArgs() ).containsExactly( "arg1" ).inOrder();
	}

	@DisplayName( "It sets showHelp for --help" )
	@Test
	void testHelpFlag() {
		CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] { "--help" } );

		assertThat( options.showHelp() ).isTrue();
	}

	@DisplayName( "It sets showVersion for --version" )
	@Test
	void testVersionFlag() {
		CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] { "--version" } );

		assertThat( options.showVersion() ).isTrue();
	}

	@DisplayName( "It sets the code for --bx-code" )
	@Test
	void testCodeFlag() {
		CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] { "--bx-code", "2+2" } );

		assertThat( options.code() ).isEqualTo( "2+2" );
	}

	@DisplayName( "It recognizes action commands" )
	@Test
	void testActionCommand() {
		CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] { "compile", "--source", "./src" } );

		assertThat( options.actionCommand() ).isEqualTo( "compile" );
		assertThat( options.cliArgs() ).containsExactly( "--source", "./src" ).inOrder();
	}

	@DisplayName( "It accumulates positionals without classifying them" )
	@Test
	void testPositionals() {
		CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] { "somefile.bx", "arg1", "arg2" } );

		assertThat( options.templatePath() ).isNull();
		assertThat( options.targetModule() ).isNull();
		assertThat( options.cliArgs() ).containsExactly( "somefile.bx", "arg1", "arg2" ).inOrder();
	}

	@DisplayName( "It supports --bx-home with a space-separated value before a module" )
	@Test
	void testHomeSpaceSeparatedBeforeModule() {
		CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] { "--bx-home", "/path/to/home", "module:cli" } );

		assertThat( options.runtimeHome() ).isEqualTo( "/path/to/home" );
		assertThat( options.targetModule() ).isEqualTo( "cli" );
	}

	@DisplayName( "It supports --bx-home with a space-separated value after a module" )
	@Test
	void testHomeSpaceSeparatedAfterModule() {
		CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] { "module:cli", "--bx-home", "/path/to/home", "arg1" } );

		assertThat( options.runtimeHome() ).isEqualTo( "/path/to/home" );
		assertThat( options.targetModule() ).isEqualTo( "cli" );
		assertThat( options.cliArgs() ).containsExactly( "arg1" ).inOrder();
	}

	@DisplayName( "It supports --bx-config with a space-separated value after a module" )
	@Test
	void testConfigSpaceSeparatedAfterModule() {
		CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] { "module:cli", "--bx-config", "/path/config.json", "arg1" } );

		assertThat( options.configFile() ).isEqualTo( "/path/config.json" );
		assertThat( options.targetModule() ).isEqualTo( "cli" );
		assertThat( options.cliArgs() ).containsExactly( "arg1" ).inOrder();
	}

	@DisplayName( "It extracts --bx-debug placed before a module" )
	@Test
	void testDebugBeforeModule() {
		CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] { "--bx-debug", "module:cli" } );

		assertThat( options.isDebugMode() ).isTrue();
		assertThat( options.targetModule() ).isEqualTo( "cli" );
	}

	@DisplayName( "It extracts a standalone --bx-debug flag" )
	@Test
	void testDebugAlone() {
		CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] { "--bx-debug" } );

		assertThat( options.isDebugMode() ).isTrue();
		assertThat( options.targetModule() ).isNull();
	}

	@DisplayName( "It sets printAST for --bx-printAST" )
	@Test
	void testPrintASTFlag() {
		CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] { "--bx-printAST" } );

		assertThat( options.printAST() ).isTrue();
	}

	@DisplayName( "It sets transpile for --bx-transpile" )
	@Test
	void testTranspileFlag() {
		CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] { "--bx-transpile" } );

		assertThat( options.transpile() ).isTrue();
	}

	@DisplayName( "It sets showHelp for the -h short form" )
	@Test
	void testHelpShortFlag() {
		CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] { "-h" } );

		assertThat( options.showHelp() ).isTrue();
	}

	@DisplayName( "Help wins over version when both are present" )
	@Test
	void testHelpWinsOverVersion() {
		CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] { "--help", "--version" } );

		assertThat( options.showHelp() ).isTrue();
		assertThat( options.showVersion() ).isFalse();
	}

	@DisplayName( "It throws when --bx-code is missing its value" )
	@Test
	void testCodeMissingValue() {
		assertThrows( BoxRuntimeException.class, () -> BoxRunner.parseCommandLineOptions( new String[] { "--bx-code" } ) );
	}

	@DisplayName( "It throws when --bx-config is missing its value" )
	@Test
	void testConfigMissingValue() {
		assertThrows( BoxRuntimeException.class, () -> BoxRunner.parseCommandLineOptions( new String[] { "--bx-config" } ) );
	}

	@DisplayName( "It throws when --bx-home is missing its value" )
	@Test
	void testHomeMissingValue() {
		assertThrows( BoxRuntimeException.class, () -> BoxRunner.parseCommandLineOptions( new String[] { "--bx-home" } ) );
	}

	@DisplayName( "It accepts an empty value for --bx-config=" )
	@Test
	void testConfigEmptyEqualsValue() {
		CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] { "--bx-config=" } );

		assertThat( options.configFile() ).isEqualTo( "" );
	}

	@DisplayName( "It accepts an empty value for --bx-home=" )
	@Test
	void testHomeEmptyEqualsValue() {
		CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] { "--bx-home=" } );

		assertThat( options.runtimeHome() ).isEqualTo( "" );
	}

	@DisplayName( "It matches flags case-insensitively" )
	@Test
	void testCaseInsensitiveFlags() {
		CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] { "--BX-HOME=/x", "--BX-DEBUG", "--BX-CONFIG=/y", "module:cli" } );

		assertThat( options.runtimeHome() ).isEqualTo( "/x" );
		assertThat( options.configFile() ).isEqualTo( "/y" );
		assertThat( options.isDebugMode() ).isTrue();
		assertThat( options.targetModule() ).isEqualTo( "cli" );
	}

	@DisplayName( "A module: arg receives --help and --version instead of them being consumed" )
	@Test
	void testModuleTrumpsHelpVersion() {
		CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] { "module:cli", "--help", "--version" } );

		assertThat( options.targetModule() ).isEqualTo( "cli" );
		assertThat( options.showHelp() ).isFalse();
		assertThat( options.showVersion() ).isFalse();
		assertThat( options.cliArgs() ).containsExactly( "--help", "--version" ).inOrder();
	}

	@DisplayName( "A module: arg receives other flags instead of them being consumed" )
	@Test
	void testModuleTrumpsOtherFlags() {
		CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] { "module:cli", "--bx-printAST", "--bx-transpile", "--bx-code", "2+2" } );

		assertThat( options.targetModule() ).isEqualTo( "cli" );
		assertThat( options.printAST() ).isFalse();
		assertThat( options.transpile() ).isFalse();
		assertThat( options.code() ).isNull();
		assertThat( options.cliArgs() ).containsExactly( "--bx-printAST", "--bx-transpile", "--bx-code", "2+2" ).inOrder();
	}

	@DisplayName( "It handles a module: arg with no additional arguments" )
	@Test
	void testModuleNoArgs() {
		CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] { "module:cli" } );

		assertThat( options.targetModule() ).isEqualTo( "cli" );
		assertThat( options.cliArgs() ).isEmpty();
	}

	@DisplayName( "It handles an empty arguments array" )
	@Test
	void testEmptyArgs() {
		CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] {} );

		assertThat( options.templatePath() ).isNull();
		assertThat( options.targetModule() ).isNull();
		assertThat( options.code() ).isNull();
		assertThat( options.actionCommand() ).isNull();
		assertThat( options.cliArgs() ).isEmpty();
	}

	@DisplayName( "It recognizes all action commands" )
	@Test
	void testAllActionCommands() {
		for ( String command : List.of( "check", "compile", "cftranspile", "featureaudit", "format", "generatesecret", "schedule" ) ) {
			CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] { command, "arg1" } );
			assertThat( options.actionCommand() ).isEqualTo( command );
			assertThat( options.cliArgs() ).containsExactly( "arg1" ).inOrder();
		}
	}

	@DisplayName( "It recognizes action commands case-insensitively and preserves the original casing" )
	@Test
	void testActionCommandCaseInsensitive() {
		CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] { "COMPILE", "arg1" } );

		assertThat( options.actionCommand() ).isEqualTo( "COMPILE" );
	}

	@DisplayName( "It does not treat --bx-code= as an inline code flag (space form only)" )
	@Test
	void testCodeEqualsFormIsPositional() {
		CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] { "--bx-code=2+2" } );

		assertThat( options.code() ).isNull();
		assertThat( options.cliArgs() ).containsExactly( "--bx-code=2+2" ).inOrder();
	}

	@DisplayName( "It accumulates positionals interleaved with non-startup flags" )
	@Test
	void testPositionalsInterleaved() {
		CLIOptions options = BoxRunner.parseCommandLineOptions( new String[] { "file.bx", "--bx-printAST", "arg1", "--bx-transpile" } );

		assertThat( options.printAST() ).isTrue();
		assertThat( options.transpile() ).isTrue();
		assertThat( options.cliArgs() ).containsExactly( "file.bx", "arg1" ).inOrder();
	}

	// ---------------------------------------------------------------------------
	// resolveExecutionTarget
	// ---------------------------------------------------------------------------

	@DisplayName( "It classifies a registered module name as a module execution" )
	@Test
	void testRegisteredModuleWins() {
		CLIOptions	parsed	= BoxRunner.parseCommandLineOptions( new String[] { "myModule", "arg1" } );
		CLIOptions	options	= BoxRunner.resolveExecutionTarget( parsed, name -> name.equals( "myModule" ) );

		assertThat( options.targetModule() ).isEqualTo( "myModule" );
		assertThat( options.templatePath() ).isNull();
		assertThat( options.cliArgs() ).containsExactly( "arg1" ).inOrder();
	}

	@DisplayName( "It classifies an existing template file as a template execution" )
	@Test
	void testTemplateWins( @TempDir Path tempDir ) throws IOException {
		Path template = tempDir.resolve( "hello.bx" );
		Files.writeString( template, "println( 'Hello' );" );

		CLIOptions	parsed	= BoxRunner.parseCommandLineOptions( new String[] { template.toString(), "arg1" } );
		CLIOptions	options	= BoxRunner.resolveExecutionTarget( parsed, name -> false );

		assertThat( options.templatePath() ).isEqualTo( template.toAbsolutePath().toString() );
		assertThat( options.targetModule() ).isNull();
		assertThat( options.cliArgs() ).containsExactly( "arg1" ).inOrder();
	}

	@DisplayName( "It classifies a shebang script as a script execution" )
	@Test
	void testShebangScript( @TempDir Path tempDir ) throws IOException {
		Path script = tempDir.resolve( "script.sh" );
		Files.writeString( script, "#!/usr/bin/env boxlang\nprintln( 'Hello' );\n" );

		CLIOptions	parsed	= BoxRunner.parseCommandLineOptions( new String[] { script.toString(), "arg1" } );
		CLIOptions	options	= BoxRunner.resolveExecutionTarget( parsed, name -> false );

		// The shebang script is stripped to a temp .bxs file that points at the script content
		assertThat( options.templatePath() ).isNotNull();
		assertThat( options.templatePath() ).doesNotContain( "script.sh" );
		assertThat( options.targetModule() ).isNull();
		assertThat( options.cliArgs() ).containsExactly( "arg1" ).inOrder();
	}

	@DisplayName( "It falls back to a module execution for an unknown name" )
	@Test
	void testModuleFailsafe() {
		CLIOptions	parsed	= BoxRunner.parseCommandLineOptions( new String[] { "no-such-thing", "arg1" } );
		CLIOptions	options	= BoxRunner.resolveExecutionTarget( parsed, name -> false );

		assertThat( options.targetModule() ).isEqualTo( "no-such-thing" );
		assertThat( options.cliArgs() ).containsExactly( "arg1" ).inOrder();
	}

	@DisplayName( "It leaves an already-resolved target untouched" )
	@Test
	void testAlreadyResolvedTarget() {
		CLIOptions	parsed	= BoxRunner.parseCommandLineOptions( new String[] { "module:cli", "arg1" } );
		CLIOptions	options	= BoxRunner.resolveExecutionTarget( parsed, name -> false );

		assertThat( options.targetModule() ).isEqualTo( "cli" );
		assertThat( options.cliArgs() ).containsExactly( "arg1" ).inOrder();
	}

	@DisplayName( "It does nothing when there are no positional args" )
	@Test
	void testNoPositionals() {
		CLIOptions	parsed	= BoxRunner.parseCommandLineOptions( new String[] { "--version" } );
		CLIOptions	options	= BoxRunner.resolveExecutionTarget( parsed, name -> false );

		assertThat( options.showVersion() ).isTrue();
		assertThat( options.targetModule() ).isNull();
		assertThat( options.templatePath() ).isNull();
	}

	@DisplayName( "A registered module wins over an existing file with the same name" )
	@Test
	void testModuleWinsOverFile( @TempDir Path tempDir ) throws IOException {
		Path template = tempDir.resolve( "myModule.bx" );
		Files.writeString( template, "println( 'Hello' );" );

		CLIOptions	parsed	= BoxRunner.parseCommandLineOptions( new String[] { template.toString(), "arg1" } );
		CLIOptions	options	= BoxRunner.resolveExecutionTarget( parsed, name -> name.equals( template.toString() ) );

		assertThat( options.targetModule() ).isEqualTo( template.toString() );
		assertThat( options.templatePath() ).isNull();
		assertThat( options.cliArgs() ).containsExactly( "arg1" ).inOrder();
	}

	@DisplayName( "An executable template wins over a shebang script with the same file" )
	@Test
	void testTemplateWinsOverShebang( @TempDir Path tempDir ) throws IOException {
		// .bxs is an allowed template extension AND the file starts with a shebang
		Path template = tempDir.resolve( "script.bxs" );
		Files.writeString( template, "#!/usr/bin/env boxlang\nprintln( 'Hello' );\n" );

		CLIOptions	parsed	= BoxRunner.parseCommandLineOptions( new String[] { template.toString(), "arg1" } );
		CLIOptions	options	= BoxRunner.resolveExecutionTarget( parsed, name -> false );

		assertThat( options.templatePath() ).isEqualTo( template.toAbsolutePath().toString() );
		assertThat( options.targetModule() ).isNull();
		assertThat( options.cliArgs() ).containsExactly( "arg1" ).inOrder();
	}

	@DisplayName( "A nonexistent template with an allowed extension throws" )
	@Test
	void testNonexistentTemplateThrows( @TempDir Path tempDir ) {
		String		missing	= tempDir.resolve( "nope.bx" ).toString();

		CLIOptions	parsed	= BoxRunner.parseCommandLineOptions( new String[] { missing } );

		assertThrows( BoxRuntimeException.class, () -> BoxRunner.resolveExecutionTarget( parsed, name -> false ) );
	}

	@DisplayName( "An extension-less arg with no matching file falls back to a module" )
	@Test
	void testExtensionlessFailsafe() {
		CLIOptions	parsed	= BoxRunner.parseCommandLineOptions( new String[] { "noext", "arg1" } );
		CLIOptions	options	= BoxRunner.resolveExecutionTarget( parsed, name -> false );

		assertThat( options.targetModule() ).isEqualTo( "noext" );
		assertThat( options.cliArgs() ).containsExactly( "arg1" ).inOrder();
	}

	@DisplayName( "A disallowed-extension arg falls back to a module" )
	@Test
	void testDisallowedExtensionFailsafe() {
		CLIOptions	parsed	= BoxRunner.parseCommandLineOptions( new String[] { "foo.txt", "arg1" } );
		CLIOptions	options	= BoxRunner.resolveExecutionTarget( parsed, name -> false );

		assertThat( options.targetModule() ).isEqualTo( "foo.txt" );
		assertThat( options.cliArgs() ).containsExactly( "arg1" ).inOrder();
	}

	@DisplayName( "A directory arg falls back to a module" )
	@Test
	void testDirectoryFailsafe( @TempDir Path tempDir ) {
		Path dir = tempDir.resolve( "mydir" );
		dir.toFile().mkdirs();

		CLIOptions	parsed	= BoxRunner.parseCommandLineOptions( new String[] { dir.toString() } );
		CLIOptions	options	= BoxRunner.resolveExecutionTarget( parsed, name -> false );

		assertThat( options.targetModule() ).isEqualTo( dir.toString() );
	}

	@DisplayName( "It leaves options untouched when code is already set" )
	@Test
	void testCodeSetEarlyReturn() {
		CLIOptions	parsed	= BoxRunner.parseCommandLineOptions( new String[] { "--bx-code", "2+2", "extra" } );
		CLIOptions	options	= BoxRunner.resolveExecutionTarget( parsed, name -> false );

		assertThat( options.code() ).isEqualTo( "2+2" );
		assertThat( options.targetModule() ).isNull();
		assertThat( options.templatePath() ).isNull();
		// --bx-code breaks the parse loop immediately, so trailing args are dropped
		assertThat( options.cliArgs() ).isEmpty();
	}

	@DisplayName( "It leaves options untouched when an action command is already set" )
	@Test
	void testActionCommandSetEarlyReturn() {
		CLIOptions	parsed	= BoxRunner.parseCommandLineOptions( new String[] { "compile", "--source", "./src" } );
		CLIOptions	options	= BoxRunner.resolveExecutionTarget( parsed, name -> false );

		assertThat( options.actionCommand() ).isEqualTo( "compile" );
		assertThat( options.targetModule() ).isNull();
		assertThat( options.templatePath() ).isNull();
		assertThat( options.cliArgs() ).containsExactly( "--source", "./src" ).inOrder();
	}

}
