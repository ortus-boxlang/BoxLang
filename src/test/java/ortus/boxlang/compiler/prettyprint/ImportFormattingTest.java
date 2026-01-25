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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName( "Import Formatting Tests" )
public class ImportFormattingTest extends PrettyPrintTest {

	@Test
	@DisplayName( "Import sort disabled (default) - maintains original order" )
	public void testImportSortDisabled() throws IOException {
		printTestWithConfigFile( "import", "sort_disabled" );
	}

	@Test
	@DisplayName( "Import sort enabled - sorts imports alphabetically" )
	public void testImportSortEnabled() throws IOException {
		printTestWithConfigFile( "import", "sort_enabled" );
	}

	@Test
	@DisplayName( "Import group disabled - no blank lines between package groups" )
	public void testImportGroupDisabled() throws IOException {
		printTestWithConfigFile( "import", "group_disabled" );
	}

	@Test
	@DisplayName( "Import group enabled - adds blank lines between package groups" )
	public void testImportGroupEnabled() throws IOException {
		printTestWithConfigFile( "import", "group_enabled" );
	}
}
