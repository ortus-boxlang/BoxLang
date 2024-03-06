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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import ortus.boxlang.ast.expression.BoxBinaryOperation;
import ortus.boxlang.parser.BoxParser;
import ortus.boxlang.parser.ParsingResult;

public class TestAST extends TestBase {

	protected BoxParser parser = new BoxParser();

	@Test
	public void testBinaryOperation() throws IOException {
		String[] epressions = new String[] {
		    "1 + 2",
		    "1 + 2.0",
		    "1 + \"a\"",
		    "1 + (1 + a)",
		    "1 + (-1)",
		    "1 - 2",
		    "1 * 2",
		    "1 / 2",
		    "1 + variables['system']",
		    "1 + create('a')",
		    "1 + a.create('a')",
		    "1 + a.create(p1='a')",
		    "1 + a.b",
		    "true && false",
		    "true || false"
			// "1 % 2",

		};

		for ( int i = 0; i < epressions.length; i++ ) {
			ParsingResult result = parser.parseExpression( epressions[ i ] );
			assertTrue( result.isCorrect() );
			assertTrue( "Root is " + result.getRoot().getClass().getName(), result.getRoot() instanceof BoxBinaryOperation );

			BoxBinaryOperation operation = ( BoxBinaryOperation ) result.getRoot();
			assertEquals( 2, operation.getChildren().size() );
			operation.getChildren().forEach( it -> {
				assertEquals( it.getParent(), operation );
			} );

		}
	}

}
