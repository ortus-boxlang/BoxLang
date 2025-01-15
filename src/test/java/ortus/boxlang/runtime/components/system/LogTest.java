
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

package ortus.boxlang.runtime.components.system;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.PrintStream;
import java.nio.file.Paths;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.logging.LoggingService;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class LogTest {

	static BoxRuntime				instance;
	static String					logsDirectory;
	IBoxContext						context;
	IScope							variables;
	static Key						result		= new Key( "result" );
	static String					logFilePath;
	static String					logFileName;
	static ByteArrayOutputStream	outContent;
	static PrintStream				originalOut	= System.out;

	@BeforeAll
	public static void setUp() {
		instance		= BoxRuntime.getInstance( true );
		logsDirectory	= instance.getConfiguration().logging.logsDirectory;
		outContent		= new ByteArrayOutputStream();
		System.setOut( new PrintStream( outContent ) );
		logFileName	= "bxlog.log";
		logFilePath	= Paths.get( logsDirectory, "/" + logFileName ).normalize().toString();
	}

	@AfterAll
	public static void tearDown() {
		System.setOut( originalOut );
		LoggingService.getInstance().shutdownAppenders();
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

	@DisplayName( "It tests the BIF Log with Script parsing" )
	@Test
	public void testComponentScript() {
		instance.executeSource(
		    """
		    bx:log text="Hello Logger!" file="bxlog";
		    """,
		    context, BoxSourceType.BOXSCRIPT );
		assertTrue( StringUtils.contains( outContent.toString(), "Hello Logger!" ) );
	}

	@DisplayName( "It tests the BIF Log with CFML parsing" )
	@Test
	public void testComponentCF() {
		instance.executeSource(
		    """
		    <cflog text="Hello Logger!" file="bxlog.log" />
		    """,
		    context, BoxSourceType.CFTEMPLATE );
		assertTrue( StringUtils.contains( outContent.toString(), "Hello Logger!" ) );
	}

	@DisplayName( "It tests the BIF Log with BoxLang parsing" )
	@Test
	public void testComponentBX() {
		instance.executeSource(
		    """
		    <bx:log text="Hello Logger!" file="bxlog.log" />
		    """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertTrue( StringUtils.contains( outContent.toString(), "Hello Logger!" ) );
	}

}
