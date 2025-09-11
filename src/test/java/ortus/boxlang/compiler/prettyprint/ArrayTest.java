package ortus.boxlang.compiler.prettyprint;

import java.io.IOException;

import org.junit.Test;

public class ArrayTest extends PrettyPrintTest {

	@Test
	public void testArrayEmptyPadding() throws IOException {
		printTestWithConfigFile( "array", "empty_padding_true" );
	}

	@Test
	public void testArrayEmptyPaddingDefault() throws IOException {
		printTestWithDefaultConfig( "array", "empty_padding_default" );
	}
}
