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
package ortus.boxlang.compiler;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.compiler.prettyprint.Config;
import ortus.boxlang.compiler.prettyprint.Printer;
import ortus.boxlang.compiler.prettyprint.Visitor;
import ortus.boxlang.runtime.BoxRuntime;

public class PrettyPrintTest extends TestBase {

	static BoxRuntime				instance;

	private static final String		TEST_RESOURCES_PATH	= "src/test/resources/prettyprint/";
	private static final String[]	fileExts			= { "bx", "bxs", "bxm", "cfc", "cfm", "cfs" };

	protected Parser				parser				= new Parser();

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@Test
	public void testIndent() throws IOException {
		// indent with tabs
		Config config = Config.builder().build();
		formatTest( "indent", "tabs", config );

		// indent with spaces
		config = Config.builder()
		    .withTabIndent( false )
		    .build();
		formatTest( "indent", "spaces", config );

		// indent with 2 spaces
		config = Config.builder()
		    .withTabIndent( false )
		    .withIndentSize( 2 )
		    .build();
		formatTest( "indent", "spaces_2", config );
	}

	private void formatTest( String resourceFolder, String outputExt, Config config ) throws IOException {
		for ( String ext : fileExts ) {
			File inputFile = new File( TEST_RESOURCES_PATH + resourceFolder + "/input." + ext );
			if ( inputFile.exists() ) {
				ParsingResult	result			= parser.parse( inputFile, false );
				String			expectedOutput	= readFile( TEST_RESOURCES_PATH + resourceFolder + "/output_" + outputExt + "." + ext );
				String			actualOutput	= format( result, config );
				assertEquals( expectedOutput, actualOutput );
			}
		}
	}

	private String format( ParsingResult result, Config config ) {
		Visitor visitor = new Visitor( result.getBoxSourceType(), config );
		result.getRoot().accept( visitor );
		var doc = visitor.getRoot();
		doc.condense();
		doc.propagateWillBreak();

		var printer = new Printer( config );
		return printer.print( doc );
	}

	private String readFile( String filePath ) throws IOException {
		Path path = Paths.get( filePath );
		return Files.readString( path );
	}

}