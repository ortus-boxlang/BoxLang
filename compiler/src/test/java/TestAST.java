import org.junit.Test;
import ourtus.boxlang.ast.ParsingResult;
import ourtus.boxlang.parser.BoxLangParser;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class TestAST extends TestBase {

	@Test
	public void testParser() throws IOException {
		BoxLangParser parser = new BoxLangParser();
		//		List<Path> files = scanForFiles( testboxDirectory, Set.of( "cfc", "cfm", "cfml" ) );
		List<Path> files = scanForFiles( "/home/madytyoo/IdeaProjects/boxlang/examples/cf_to_java/HelloWorld",
			Set.of( "cfc", "cfm", "cfml" ) );
		for ( Path file : files ) {
			System.out.println( file );
			ParsingResult result = parser.parse( file.toFile() );
			if ( !result.isCorrect() ) {
				result.getIssues().forEach( error -> System.out.println( error ) );
			}
		}
	}
}
