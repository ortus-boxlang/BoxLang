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
package ortus.boxlang.compiler.prettyprint;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;

import ortus.boxlang.compiler.TestBase;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.compiler.prettyprint.config.Config;
import ortus.boxlang.runtime.BoxRuntime;

public abstract class PrettyPrintTest extends TestBase {

	protected static BoxRuntime		instance;

	protected static final String	TEST_RESOURCES_PATH	= "src/test/resources/prettyprint/";
	protected static final String[]	fileExts			= { "bx", "bxs", "bxm", "cfc", "cfm", "cfs" };

	protected Parser				parser				= new Parser();

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	protected void singlePrintTest( String inputFilePath, String expectedFilePath, Config config ) throws IOException {
		File			inputFile		= new File( TEST_RESOURCES_PATH + inputFilePath );
		ParsingResult	result			= parser.parse( inputFile, false );
		String			actualOutput	= PrettyPrint.prettyPrint( result.getRoot(), config );
		String			expectedOutput	= readFile( TEST_RESOURCES_PATH + expectedFilePath );
		assertEquals( expectedOutput, actualOutput );
	}

	protected void singlePrintTest( String inputFilePath, String expectedFilePath, String configFilePath ) throws IOException {
		File			inputFile		= new File( TEST_RESOURCES_PATH + inputFilePath );
		ParsingResult	result			= parser.parse( inputFile, false );
		String			actualOutput	= PrettyPrint.prettyPrint( result.getRoot(), new Config().loadFromConfigFile( TEST_RESOURCES_PATH + configFilePath ) );
		String			expectedOutput	= readFile( TEST_RESOURCES_PATH + expectedFilePath );
		assertEquals( expectedOutput, actualOutput );
	}

	protected void printTestWithConfigFile( String resourceFolder, String name ) throws IOException {
		_printTestWithConfig( resourceFolder, name, Config.loadConfig( TEST_RESOURCES_PATH + resourceFolder + "/" + name + ".json" ) );
	}

	protected void printTestWithDefaultConfig( String resourceFolder, String name ) throws IOException {
		_printTestWithConfig( resourceFolder, name, new Config() );
	}

	protected void _printTestWithConfig( String resourceFolder, String name, Config config ) throws IOException {
		for ( String ext : fileExts ) {
			File inputFile = new File( TEST_RESOURCES_PATH + resourceFolder + "/" + name + "_input." + ext );
			if ( inputFile.exists() ) {
				ParsingResult	result			= parser.parse( inputFile, false );
				String			actualOutput	= PrettyPrint.prettyPrint( result.getRoot(), config );
				String			expectedOutput	= readFile( TEST_RESOURCES_PATH + resourceFolder + "/" + name + "_output." + ext );
				assertEquals( expectedOutput, actualOutput );
			}
		}
	}

	protected void printTest( String resourceFolder, String outputExt, Config config ) throws IOException {
		for ( String ext : fileExts ) {
			File inputFile = new File( TEST_RESOURCES_PATH + resourceFolder + "/input." + ext );
			if ( inputFile.exists() ) {
				ParsingResult	result			= parser.parse( inputFile, false );
				String			actualOutput	= PrettyPrint.prettyPrint( result.getRoot(), config );
				String			expectedOutput	= readFile( TEST_RESOURCES_PATH + resourceFolder + "/output_" + outputExt + "." + ext );
				assertEquals( expectedOutput, actualOutput );
			}
		}
	}

	protected String readFile( String filePath ) throws IOException {
		Path path = Paths.get( filePath );
		return Files.readString( path );
	}

}