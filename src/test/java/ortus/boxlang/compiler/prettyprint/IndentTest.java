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

public class IndentTest extends PrettyPrintTest {

	@Test
	public void testIndent() throws IOException {
		// indent with tabs
		Config config = Config.builder().build();
		printTest( "indent", "tabs", config );

		// indent with spaces
		config = Config.builder()
		    .withTabIndent( false )
		    .build();
		printTest( "indent", "spaces", config );

		// indent with 2 spaces
		config = Config.builder()
		    .withTabIndent( false )
		    .withIndentSize( 2 )
		    .build();
		printTest( "indent", "spaces_2", config );
	}
}
