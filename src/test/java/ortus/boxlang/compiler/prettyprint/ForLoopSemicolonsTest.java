package ortus.boxlang.compiler.prettyprint;

import java.io.IOException;

import org.junit.Test;

public class ForLoopSemicolonsTest extends PrettyPrintTest {

	@Test
	public void test() throws IOException {
		printTestWithConfigFile( "for_loop_semicolons", "padding_false" );
	}
}
