package ortus.boxlang.compiler.prettyprint;

import java.io.IOException;

import org.junit.Test;

public class PropertyTest extends PrettyPrintTest {

	@Test
	public void test() throws IOException {
		printTestWithConfigFile( "property", "element_count_2" );
	}

	@Test
	public void testMinLength10() throws IOException {
		printTestWithConfigFile( "property", "min_length_10" );
	}

	@Test
	public void testKeyValuePadding() throws IOException {
		printTestWithConfigFile( "property", "key_value_padding_true" );
	}
}
