/**
 * Improved test structure using parameterized tests
 */
package ortus.boxlang.compiler.prettyprint;

import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class PaddingTest extends PrettyPrintTest {

	@ParameterizedTest( name = "Bracket Padding: {0}" )
	@MethodSource( "bracketPaddingConfigs" )
	public void testBracketPadding( String testName, Config config, String expectedSuffix ) throws IOException {
		printTest( "bracket_padding", expectedSuffix, config );
	}

	static Stream<Arguments> bracketPaddingConfigs() {
		return Stream.of(
		    Arguments.of( "padding disabled", new Config().setBracketPadding( false ), "padding_false" ),
		    Arguments.of( "padding enabled", new Config().setBracketPadding( true ), "padding_true" )
		);
	}

	@ParameterizedTest( name = "Parens Padding: {0}" )
	@MethodSource( "parensPaddingConfigs" )
	public void testParensPadding( String testName, Config config, String expectedSuffix ) throws IOException {
		printTest( "parens_padding", expectedSuffix, config );
	}

	static Stream<Arguments> parensPaddingConfigs() {
		return Stream.of(
		    Arguments.of( "padding disabled", new Config().setParensPadding( false ), "padding_false" ),
		    Arguments.of( "padding enabled", new Config().setParensPadding( true ), "padding_true" )
		);
	}
}