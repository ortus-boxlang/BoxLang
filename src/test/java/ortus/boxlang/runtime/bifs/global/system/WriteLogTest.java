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

package ortus.boxlang.runtime.bifs.global.system;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.PrintStream;
import java.nio.file.Paths;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.logging.LoggingService;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class WriteLogTest {

	static BoxRuntime				instance;
	static String					logsDirectory;
	static IBoxContext				context;
	static IScope					variables;
	static Key						result		= new Key( "result" );
	static String					logFilePath;
	static ByteArrayOutputStream	outContent;
	static PrintStream				originalOut	= System.out;
	static String					defaultLogFilePath;

	@BeforeAll
	public static void setUp() {
		instance		= BoxRuntime.getInstance( true );
		logsDirectory	= instance.getConfiguration().logging.logsDirectory;
		outContent		= new ByteArrayOutputStream();
		System.setOut( new PrintStream( outContent ) );
		logFilePath			= Paths.get( logsDirectory, "/writelog.log" ).normalize().toString();
		defaultLogFilePath	= Paths.get( logsDirectory, "/runtime.log" ).normalize().toString();
	}

	@AfterAll
	public static void teardown() {
		System.setOut( originalOut );
		LoggingService.getInstance().shutdownAppenders();
		if ( FileSystemUtil.exists( logFilePath ) ) {
			try {
				FileSystemUtil.deleteFile( logFilePath );
			} catch ( Exception e ) {
				// Leave this due to stupid windows file locking
				// We can't use prudent also due to rolling file appenders
				e.printStackTrace();
			}
		}
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
		outContent.reset();
	}

	@DisplayName( "It can write to the default log file" )
	@Test
	public void testPrint() {
		// @formatter:off
		instance.executeSource(
		    """
				writeLog( "Hello Logger!" )
				writeLog( "Hola!" )
				writeLog( text="Hola debug", type="Debug" )
				writeLog( text="Hola debug", type="error" )
				writeLog( text="Hola debug", type="warning" )
				writeLog( text="Hola debug", type="info" )
		    """,
		    context );
		// @formatter:on

		// Assert we got here
		assertThat( FileSystemUtil.exists( defaultLogFilePath ) ).isTrue();
		assertThat( outContent.toString() ).contains( "Hello Logger!" );
	}

	@DisplayName( "It can write a default with a compat log argument" )
	@Test
	public void testCategory() {
		instance.executeSource(
		    """
		    writeLog( text="Hola Logger!", log="Custom" )
		    """,
		    context );
		// Assert we got here
		assertThat( StringUtils.contains( outContent.toString(), "Hola Logger!" ) ).isTrue();
	}

	@DisplayName( "It can write a default with a custom log file" )
	@Test
	public void testCustomFile() {
		instance.executeSource(
		    """
		    writeLog( text="Custom Logger!", file="writelog.log" )
		    """,
		    context );
		assertTrue( FileSystemUtil.exists( logFilePath ) );
		String fileContent = StringCaster.cast( FileSystemUtil.read( logFilePath ) );
		assertTrue( StringUtils.contains( fileContent, "Custom Logger!" ) );
	}

	@DisplayName( "It can write a default with a custom log file and type" )
	@Test
	public void testCustomFileAndLevel() {
		instance.executeSource(
		    """
		    writeLog( text="Hello Error Logger!", file="writelog.log", type="Error" );
		    """,
		    context );

		String fileContent = StringCaster.cast( FileSystemUtil.read( logFilePath ) );
		assertThat( fileContent ).contains( "ERROR" );
		assertThat( fileContent ).contains( "Hello Error Logger!" );

	}

}
