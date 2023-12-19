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

public class TestStruct extends TestBase {

	@Test
	public void unorderedStructLiterals() throws IOException {

		assertEqualsNoWhiteSpaces(
		    "new Struct()",
		    transformExpression( "{}" )
		);
		assertEqualsNoWhiteSpaces(
		    """
		    Struct.of("something", Array.of("foo", "bar", Struct.of("luis", true)), "else", 42)
		    """,
		    transformExpression(
		        """
		        {
		          something : [
		        	"foo",
		        	"bar",
		        	{ 'luis': true }
		          ],
		          "else" : 42
		        }
		             """ )
		);
	}

	@Test
	public void orderedStructLiterals() throws IOException {

		assertEqualsNoWhiteSpaces(
		    "new Struct(Struct.Type.LINKED)",
		    transformExpression( "[:]" )
		);
		assertEqualsNoWhiteSpaces(
		    """
		    Struct.linkedOf("brad","wood","luis","majano")
		    """,
		    transformExpression(
		        """
		        			[ "brad" : "wood", "luis" : "majano" ]
		        """ )
		);

		assertEqualsNoWhiteSpaces(
		    """
		    Struct.linkedOf("something", Array.of("foo", "bar", Struct.linkedOf("luis", true)), "else", 42)
		     """,
		    transformExpression(
		        """
		        [
		        	// This is still an array literal
		          something : [
		        	"foo",
		        	"bar",
		        	// But this is a nested ordered struct literal
		        	[ 'luis': true ]
		          ],
		          "else" : 42
		        ]
		              """ )
		);
	}

}
