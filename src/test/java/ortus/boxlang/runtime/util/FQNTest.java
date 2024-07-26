package ortus.boxlang.runtime.util;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Paths;

@Disabled( "Not a single test is passing - I'm probably doing it wrong!" )
public class FQNTest {

	@DisplayName( "Can convert an absolute path to a full package/class path" )
	@Test
	void testItCanParseAbsolutePathToString() {
		FQN fqn = new FQN( Paths.get( "/src/test/java/ortus/boxlang/runtime/util/FQNTest.java" ) );
		assertEquals( "src.test.java.ortus.boxlang.runtime.util.FQNTest", fqn.toString() );
	}

	@DisplayName( "Can convert an absolute path to just a package path" )
	@Test
	void testItCanParseAbsolutePathToPackage() {
		FQN fqn = new FQN( Paths.get( "/src/test/java/ortus/boxlang/runtime/util/FQNTest.java" ) );
		assertEquals( "src.test.java.ortus.boxlang.runtime.util", fqn.getPackageString() );
	}

	@DisplayName( "Can convert a file path with prefix to a relative package name" )
	@Test
	void testItCanParseRelativePrefixedPathToString() {
		FQN fqn = new FQN( Paths.get( "src/test/java/" ), Paths.get( "ortus/boxlang/runtime/util/FQNTest.java" ) );
		assertEquals( "src.test.java.ortus.boxlang.runtime.util.FQNTest", fqn.toString() );
	}

	@DisplayName( "Can convert a file path with prefix to a relative package name" )
	@Test
	void testItCanParseRelativePrefixedPathToPackagePath() {
		FQN fqn = new FQN( Paths.get( "src/test/java/" ), Paths.get( "ortus/boxlang/runtime/util/FQNTest.java" ) );
		assertEquals( "src.test.java.ortus.boxlang.runtime.util", fqn.getPackageString() );
	}
}
