
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

import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.util.FileSystemUtil;

@Execution( ExecutionMode.SAME_THREAD )
public class LogTest {

	static BoxRuntime	instance;
	static String		logsDirectory;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );
	static String		logFilePath;
	static String		logFileName;

	@BeforeAll
	public static void setUp() {
		instance		= BoxRuntime.getInstance( true );
		logsDirectory	= instance.getConfiguration().logging.logsDirectory;
		logFileName		= "bxlog.log";
		// NOTE: No leading "/" on the second argument — otherwise Java treats it as an absolute path
		logFilePath		= Paths.get( logsDirectory, logFileName ).normalize().toString();
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It tests the BIF Log with Script parsing" )
	@Test
	public void testComponentScript() {
		instance.executeSource(
		    """
		    bx:log text="Hello Logger!" file="bxlog";
		    """,
		    context, BoxSourceType.BOXSCRIPT );

		String logContent = readLogFile();
		assertTrue(
		    logContent.contains( "Hello Logger!" ),
		    "Log file should contain 'Hello Logger!' but was: [" + logContent + "]"
		);
	}

	@DisplayName( "It tests the BIF Log with CFML parsing" )
	@Test
	public void testComponentCF() {
		instance.executeSource(
		    """
		    <cflog text="Hello CF!" file="bxlog.log" />
		    """,
		    context, BoxSourceType.CFTEMPLATE );

		String logContent = readLogFile();
		assertTrue(
		    logContent.contains( "Hello CF!" ),
		    "Log file should contain 'Hello CF!' but was: [" + logContent + "]"
		);
	}

	@DisplayName( "It tests the BIF Log with BoxLang parsing" )
	@Test
	public void testComponentBX() {
		instance.executeSource(
		    """
		    <bx:log text="Hello BX!" file="bxlog.log" />
		    """,
		    context, BoxSourceType.BOXTEMPLATE );

		String logContent = readLogFile();
		assertTrue(
		    logContent.contains( "Hello BX!" ),
		    "Log file should contain 'Hello BX!' but was: [" + logContent + "]"
		);
	}

	/**
	 * Reads the log file content for assertion. If the file does not exist,
	 * returns a diagnostic string including the expected path.
	 *
	 * @return The log file content or a diagnostic message if not found.
	 */
	private String readLogFile() {
		if ( !FileSystemUtil.exists( logFilePath ) ) {
			return "(log file not found at: " + logFilePath + ")";
		}
		return StringCaster.cast( FileSystemUtil.read( logFilePath ) );
	}

}
