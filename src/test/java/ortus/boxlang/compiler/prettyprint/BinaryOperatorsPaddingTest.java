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
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ortus.boxlang.compiler.prettyprint.config.Config;

public class BinaryOperatorsPaddingTest extends PrettyPrintTest {

	@ParameterizedTest( name = "Binary Operators Padding: {0}" )
	@MethodSource( "binaryOperatorsPaddingConfigs" )
	public void testBinaryOperatorsPadding( String testName, Config config, String expectedSuffix ) throws IOException {
		printTest( "binary_operators", expectedSuffix, config );
	}

	@Test
	public void test() throws IOException {
		singlePrintTest( "struct_emptypadding/input.bx", "struct_emptypadding/output_true.bx", new Config() );
	}

	static Stream<Arguments> binaryOperatorsPaddingConfigs() {
		return Stream.of(
		    Arguments.of( "with padding", new Config(), "true" ),
		    Arguments.of( "no padding", new Config().setBinaryOperatorsPadding( false ), "false" )
		);
	}
}
