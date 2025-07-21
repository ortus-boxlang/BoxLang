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

public class StringQuoteTest extends PrettyPrintTest {

	@Test
	public void testStringQuotes() throws IOException {
		// test with double quotes
		Config config = new Config();
		printTest( "stringquote", "double", config );

		// test with single quotes
		config = new Config()
		    .setSingleQuote( true );
		printTest( "stringquote", "single", config );
	}
}
