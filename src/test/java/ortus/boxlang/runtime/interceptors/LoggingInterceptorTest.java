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
package ortus.boxlang.runtime.interceptors;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Paths;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class LoggingInterceptorTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;

	static Logging		loggingInterceptor;
	static String		tmpDirectory	= "src/test/resources/tmp/FileSystemStoreTest";
	static String		testLogFile		= "LoggingInterceptorTest.log";
	static String		logFilePath;
	static String		absoluteLogeFilePath;
	static String		logDirectory;

	@BeforeAll
	public static void setUp() {
		instance				= BoxRuntime.getInstance( true );
		loggingInterceptor		= new Logging( instance );
		logDirectory			= instance.getConfiguration().logsDirectory;
		logFilePath				= Paths.get( logDirectory, "/", testLogFile ).toString();
		absoluteLogeFilePath	= Paths.get( tmpDirectory, testLogFile ).toAbsolutePath().toString();
	}

	@AfterAll
	public static void teardown() {
		if ( FileSystemUtil.exists( logFilePath ) ) {
			FileSystemUtil.deleteFile( logFilePath );
		}
		if ( FileSystemUtil.exists( absoluteLogeFilePath ) ) {
			FileSystemUtil.deleteFile( absoluteLogeFilePath );
		}
	}

	@DisplayName( "It can log a message" )
	@Test
	void testLogMessage() {
		System.out.println( logFilePath );
		loggingInterceptor.logMessage( Struct.of(
		    Key.text, "Hello, World!",
		    Key.type, "INFO",
		    Key.file, testLogFile,
		    Key.log, "Test"
		) );
		assertTrue( StringCaster.cast( FileSystemUtil.read( logFilePath ) ).indexOf( "Hello" ) > -1 );
	}

	@DisplayName( "It can log a message to an absolute path" )
	@Test
	void testLogAbsolute() {
		loggingInterceptor.logMessage( Struct.of(
		    Key.text, "Hello, Absolute Path!",
		    Key.type, "INFO",
		    Key.file, absoluteLogeFilePath,
		    Key.log, "Test"
		) );
		assertTrue( StringCaster.cast( FileSystemUtil.read( absoluteLogeFilePath ) ).indexOf( "Hello" ) > -1 );
	}

}
