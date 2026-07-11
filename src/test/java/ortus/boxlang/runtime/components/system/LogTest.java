
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

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.AfterAll;
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
import ortus.boxlang.runtime.logging.LoggingService;
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
		logFilePath		= Paths.get( logsDirectory, "/" + logFileName ).normalize().toString();
		deleteLogFile();
	}

	@AfterAll
	public static void tearDown() {
		LoggingService.getInstance().shutdownAppenders();
		deleteLogFile();
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
		assertThat( getLogFileContent( "Hello Logger!" ) ).contains( "Hello Logger!" );
	}

	@DisplayName( "It tests the BIF Log with CFML parsing" )
	@Test
	public void testComponentCF() {
		instance.executeSource(
		    """
		    <cflog text="Hello CF!" file="bxlog.log" />
		    """,
		    context, BoxSourceType.CFTEMPLATE );
		assertThat( getLogFileContent( "Hello CF!" ) ).contains( "Hello CF!" );
	}

	@DisplayName( "It tests the BIF Log with BoxLang parsing" )
	@Test
	public void testComponentBX() {
		instance.executeSource(
		    """
		    <bx:log text="Hello BX!" file="bxlog.log" />
		    """,
		    context, BoxSourceType.BOXTEMPLATE );
		assertThat( getLogFileContent( "Hello BX!" ) ).contains( "Hello BX!" );
	}

	private static String getLogFileContent( String expectedText ) {
		long	deadline	= System.nanoTime() + TimeUnit.SECONDS.toNanos( 2 );
		String	content		= "";
		System.out.println( "logFilePath: " + logFilePath );
		while ( System.nanoTime() < deadline ) {
			System.out.println( "Checking log file content..." );
			if ( FileSystemUtil.exists( logFilePath ) ) {
				System.out.println( "Log file exists." );
				content = StringCaster.cast( FileSystemUtil.read( logFilePath ) );
				System.out.println( "Log file content: " + content );
				if ( content.contains( expectedText ) ) {
					return content;
				}
			}
			LockSupport.parkNanos( TimeUnit.MILLISECONDS.toNanos( 25 ) );
		}
		System.out.println( "Failed to find expected text in log file within the deadline." );

		return content;
	}

	private static void deleteLogFile() {
		if ( FileSystemUtil.exists( logFilePath ) ) {
			FileSystemUtil.deleteFile( logFilePath );
		}
	}

}
