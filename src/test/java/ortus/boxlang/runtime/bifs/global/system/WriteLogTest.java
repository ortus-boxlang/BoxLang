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
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class WriteLogTest {

	static BoxRuntime				instance;
	static IBoxContext				context;
	static IScope					variables;
	static Key						result		= new Key( "result" );
	static ByteArrayOutputStream	outContent;
	static PrintStream				originalOut	= System.out;
	static String					logsDirectory;

	@BeforeAll
	public static void setUp() {
		instance		= BoxRuntime.getInstance( true );
		logsDirectory	= instance.getConfiguration().logsDirectory;
		outContent		= new ByteArrayOutputStream();
		System.setOut( new PrintStream( outContent ) );
	}

	@AfterAll
	public static void teardown() {
		System.setOut( originalOut );
		String logFilePath = Paths.get( logsDirectory, "/foo.log" ).normalize().toString();
		if ( FileSystemUtil.exists( logFilePath ) ) {
			FileSystemUtil.deleteFile( logFilePath );
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
		instance.executeSource(
		    """
		    writeLog( "Hello Logger!" )
		    """,
		    context );

		// Assert we got here
		assertTrue( StringUtils.contains( outContent.toString(), "Hello Logger!" ) );
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
		assertTrue( StringUtils.contains( outContent.toString(), "Hola Logger!" ) );
	}

	@DisplayName( "It can write a default with a custom log file" )
	@Test
	public void testCustomFile() {
		String logFilePath = Paths.get( logsDirectory, "/foo.log" ).normalize().toString();
		instance.executeSource(
		    """
		    writeLog( text="Custom Logger!", file="foo.log" )
		    """,
		    context );
		assertTrue( FileSystemUtil.exists( logFilePath ) );
		String fileContent = StringCaster.cast( FileSystemUtil.read( logFilePath ) );
		assertTrue( StringUtils.contains( fileContent, "Custom Logger!" ) );

	}

	@DisplayName( "It can write a default with a custom log file" )
	@Test
	public void testCustomFileAndLevel() {
		String logFilePath = Paths.get( logsDirectory, "/foo.log" ).normalize().toString();
		instance.executeSource(
		    """
		    writeLog( text="Hello Error Logger!", file="foo.log", type="Error" );
		    """,
		    context );

		instance.executeSource(
		    """
		    writeLog( text="Hello Root Logger!" );
		    """,
		    context );
		assertTrue( FileSystemUtil.exists( logFilePath ) );
		String fileContent = StringCaster.cast( FileSystemUtil.read( logFilePath ) );
		assertTrue( StringUtils.contains( fileContent, "[ERROR]" ) );
		assertTrue( StringUtils.contains( fileContent, "Hello Error Logger!" ) );

	}

}
