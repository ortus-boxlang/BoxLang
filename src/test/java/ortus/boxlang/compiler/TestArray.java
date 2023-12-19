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

import java.io.IOException;

import org.junit.jupiter.api.Test;

public class TestArray extends TestBase {

	@Test
	public void arrayLiterals() throws IOException {

		assertEqualsNoWhiteSpaces(
		    "new Array()",
		    transformExpression( "[]" )
		);
		assertEqualsNoWhiteSpaces(
		    "Array.of(1,2,3)",
		    transformExpression( "[1,2,3]" )
		);
		assertEqualsNoWhiteSpaces(
		    """
		    Array.of("foo","bar")
		    """,
		    transformExpression(
		        """
		        			["foo","bar"]
		        """ )
		);

		assertEqualsNoWhiteSpaces(
		    """
		    Array.of(
		    Array.of(1,2),
		    Array.of(3,4),
		    "brad")
		      """,
		    transformExpression(
		        """
		        [
		          [1,2],
		          [3,4],
		          "brad"
		        ]
		        		        """ )
		);
	}

}
