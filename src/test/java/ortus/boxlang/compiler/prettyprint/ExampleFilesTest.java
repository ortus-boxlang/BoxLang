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

import java.io.IOException;

import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.prettyprint.config.Config;

public class ExampleFilesTest extends PrettyPrintTest {

	private static final String	FOLDER		= "example_files/";
	private static final String	CONFIG_PATH	= TEST_RESOURCES_PATH + FOLDER + ".cfformat.json";

	@Test
	public void testFileA() throws IOException {
		Config config = Config.loadConfigAutoDetect( CONFIG_PATH );
		singlePrintTest( FOLDER + "fileA.cfc", FOLDER + "fileA_expected.cfc", config );
	}

	@Test
	public void testFileB() throws IOException {
		Config config = Config.loadConfigAutoDetect( CONFIG_PATH );
		singlePrintTest( FOLDER + "fileB.cfc", FOLDER + "fileB_expected.cfc", config );
	}

}
