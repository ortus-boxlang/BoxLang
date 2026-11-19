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
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * End to end test of the launcher configuration on Windows: files and environment variables supply
 * JVM arguments and system properties before Java starts.
 *
 * Skipped when the generated launcher or a runtime jar is missing; run `gradlew
 * startShadowScripts` first.
 */
@EnabledOnOs( OS.WINDOWS )
public class LauncherConfigTest {

	@DisplayName( "It applies <launcher>.properties as system properties" )
	@Test
	void testLauncherPropertiesFile( @TempDir Path tempDir ) throws Exception {
		String output = runLauncher(
		    tempDir,
		    Map.of( "boxlang.properties", "box.cfg.property=from-properties" + System.lineSeparator() ),
		    Map.of(),
		    Map.of(),
		    false
		);

		assertThat( output ).contains( "PROPERTY=[from-properties]" );
		assertThat( output ).contains( "OPTION=[MISSING]" );
	}

	@DisplayName( "It applies <launcher>.options as JVM arguments" )
	@Test
	void testLauncherOptionsFile( @TempDir Path tempDir ) throws Exception {
		String output = runLauncher(
		    tempDir,
		    Map.of( "boxlang.options", "# a comment" + System.lineSeparator() + "-Dbox.cfg.option=from-options" + System.lineSeparator() + "-Xmx384m" ),
		    Map.of(),
		    Map.of(),
		    false
		);

		assertThat( output ).contains( "OPTION=[from-options]" );
		assertThat( output ).contains( "HEAP_UNDER_512=[true]" );
	}

	@DisplayName( "It applies the boxlang.properties and boxlang.options in the BoxLang home" )
	@Test
	void testHomeFilesApply( @TempDir Path tempDir ) throws Exception {
		String output = runLauncher(
		    tempDir,
		    Map.of(),
		    Map.of(
		        "boxlang.properties", "box.cfg.property=from-home" + System.lineSeparator(),
		        "boxlang.options", "-Dbox.cfg.option=from-home-options" + System.lineSeparator()
		    ),
		    Map.of(),
		    false
		);

		assertThat( output ).contains( "PROPERTY=[from-home]" );
		assertThat( output ).contains( "OPTION=[from-home-options]" );
	}

	@DisplayName( "It lets the launcher files override the BoxLang home files" )
	@Test
	void testAdjacentFilesOverrideHome( @TempDir Path tempDir ) throws Exception {
		String output = runLauncher(
		    tempDir,
		    Map.of( "boxlang.properties", "box.cfg.property=from-launcher" + System.lineSeparator() ),
		    Map.of( "boxlang.properties", "box.cfg.property=from-home" + System.lineSeparator() ),
		    Map.of(),
		    false
		);

		assertThat( output ).contains( "PROPERTY=[from-launcher]" );
	}

	@DisplayName( "It applies the general BOXLANG_OPTS and BOXLANG_PROPS" )
	@Test
	void testGeneralEnvVars( @TempDir Path tempDir ) throws Exception {
		String output = runLauncher(
		    tempDir,
		    Map.of(),
		    Map.of(),
		    Map.of(
		        "BOXLANG_OPTS", "-Dbox.cfg.option=from-general-args",
		        "BOXLANG_PROPS", "box.cfg.property=from-general-props"
		    ),
		    false
		);

		assertThat( output ).contains( "OPTION=[from-general-args]" );
		assertThat( output ).contains( "PROPERTY=[from-general-props]" );
	}

	@DisplayName( "It applies the process layer named after the launcher that was invoked" )
	@Test
	void testLauncherSpecificEnvVars( @TempDir Path tempDir ) throws Exception {
		String output = runLauncher(
		    tempDir,
		    Map.of(),
		    Map.of(),
		    Map.of(
		        "BXTEST_OPTS", "-Dbox.cfg.option=from-launcher-args",
		        "BXTEST_PROPS", "box.cfg.property=from-launcher-props"
		    ),
		    true
		);

		assertThat( output ).contains( "OPTION=[from-launcher-args]" );
		assertThat( output ).contains( "PROPERTY=[from-launcher-props]" );
	}

	@DisplayName( "It ignores env vars named for a launcher that was not invoked" )
	@Test
	void testUnrelatedLauncherEnvVarsIgnored( @TempDir Path tempDir ) throws Exception {
		String output = runLauncher(
		    tempDir,
		    Map.of(),
		    Map.of(),
		    Map.of( "BOX_OPTS", "-Dbox.cfg.option=from-box" ),
		    false
		);

		assertThat( output ).contains( "OPTION=[MISSING]" );
	}

	/**
	 * Stages a copy of the generated launcher next to a runtime jar, drops the given launcher
	 * configuration files in beside it and in the BoxLang home, and runs a probe script.
	 *
	 * @param tempDir           Scratch directory for the staged image.
	 * @param adjacentFiles     File name to contents, written next to the launcher.
	 * @param homeFiles         File name to contents, written into the BoxLang home.
	 * @param extraEnv          Environment variables for the launched process.
	 * @param useAliasLauncher  Stage a module style launcher that records BOXLANG_PROCESS_PATH.
	 */
	private String runLauncher( Path tempDir, Map<String, String> adjacentFiles, Map<String, String> homeFiles,
	    Map<String, String> extraEnv, boolean useAliasLauncher ) throws IOException, InterruptedException {
		Path launcher = Path.of( "build/scriptsShadow/boxlang.bat" ).toAbsolutePath();
		Path runtimeJar = findRuntimeJar();

		assumeTrue( Files.isRegularFile( launcher ), "Run startShadowScripts to generate " + launcher );
		assumeTrue( runtimeJar != null, "Run the jar task to produce a BoxLang runtime jar in build/libs" );

		Path binDirectory = Files.createDirectories( tempDir.resolve( "bin" ) );
		Path libDirectory = Files.createDirectories( tempDir.resolve( "lib" ) );
		Path homeDirectory = Files.createDirectories( tempDir.resolve( "home" ) );
		Path workDirectory = Files.createDirectories( tempDir.resolve( "work" ) );

		Files.copy( launcher, binDirectory.resolve( "boxlang.bat" ) );
		Files.copy( runtimeJar, libDirectory.resolve( runtimeJar.getFileName() ) );
		for( Map.Entry<String, String> entry : adjacentFiles.entrySet() ) {
			Files.writeString( binDirectory.resolve( entry.getKey() ), entry.getValue() );
		}
		for( Map.Entry<String, String> entry : homeFiles.entrySet() ) {
			Files.writeString( homeDirectory.resolve( entry.getKey() ), entry.getValue() );
		}
		Files.writeString(
		    workDirectory.resolve( "check.bxs" ),
		    """
		    println( 'PROPERTY=[' & getSystemSetting( 'box.cfg.property', 'MISSING' ) & ']' )
		    println( 'OPTION=[' & getSystemSetting( 'box.cfg.option', 'MISSING' ) & ']' )
		    heapMB = createObject( 'java', 'java.lang.Runtime' ).getRuntime().maxMemory() / 1024 / 1024
		    println( 'HEAP_UNDER_512=[' & ( heapMB <= 512 ) & ']' )
		    """
		);

		String invoked;
		if ( useAliasLauncher ) {
			Files.writeString(
			    binDirectory.resolve( "bxtest.bat" ),
			    "@echo off" + System.lineSeparator()
			        + "if not defined BOXLANG_PROCESS_PATH set \"BOXLANG_PROCESS_PATH=%~f0\"" + System.lineSeparator()
			        + "\"%~dp0boxlang.bat\" %*" + System.lineSeparator()
			);
			invoked = binDirectory.resolve( "bxtest.bat" ).toString();
		} else {
			invoked = binDirectory.resolve( "boxlang.bat" ).toString();
		}

		List<String> command = List.of( "cmd.exe", "/c", invoked, "check.bxs" );

		ProcessBuilder builder = new ProcessBuilder( command ).directory( workDirectory.toFile() );
		builder.environment().put( "BOXLANG_HOME", homeDirectory.toString() );
		for( Map.Entry<String, String> entry : extraEnv.entrySet() ) {
			builder.environment().put( entry.getKey(), entry.getValue() );
		}
		builder.redirectErrorStream( true );
		Process process = builder.start();
		String	output	= new String( process.getInputStream().readAllBytes() );

		assertThat( process.waitFor( 120, TimeUnit.SECONDS ) ).isTrue();

		return output;
	}

	/**
	 * Finds a runnable BoxLang jar in build/libs, ignoring the sources, javadoc, and sibling
	 * artifacts that are not a runtime.
	 */
	private Path findRuntimeJar() throws IOException {
		Path libsDirectory = Path.of( "build/libs" ).toAbsolutePath();
		if ( !Files.isDirectory( libsDirectory ) ) {
			return null;
		}
		try ( Stream<Path> files = Files.list( libsDirectory ) ) {
			return files
			    .filter( path -> path.getFileName().toString().matches( "boxlang-\\d.*\\.jar" ) )
			    .max( Comparator.comparing( path -> path.toFile().lastModified() ) )
			    .orElse( null );
		}
	}

}
