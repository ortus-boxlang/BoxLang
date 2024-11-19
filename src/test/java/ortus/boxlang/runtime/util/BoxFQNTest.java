package ortus.boxlang.runtime.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class BoxFQNTest {

	@DisplayName( "Can convert an absolute path to a full package/class path" )
	@Test
	void testItCanParseAbsolutePathToString() {
		BoxFQN fqn = new BoxFQN( Paths.get( "/src/test/java/ortus/boxlang/runtime/util/BoxFQNTest.bx" ) );
		assertEquals( "src.test.java.ortus.boxlang.runtime.util.BoxFQNTest", fqn.toString() );
	}

	@DisplayName( "Can convert an absolute path to just a package path" )
	@Test
	void testItCanParseAbsolutePathToPackage() {
		BoxFQN fqn = new BoxFQN( Paths.get( "/src/test/java/ortus/boxlang/runtime/util/BoxFQNTest.bx" ) );
		assertEquals( "src.test.java.ortus.boxlang.runtime.util", fqn.getPackageString() );
	}

	@DisplayName( "Can convert a file path to a relative package name" )
	@Test
	void testItCanParseRelativePathToString() {
		BoxFQN fqn = new BoxFQN( Paths.get( "src/test/java/" ), Paths.get( "src/test/java/ortus/boxlang/runtime/util/BoxFQNTest.bx" ) );
		assertEquals( "ortus.boxlang.runtime.util.BoxFQNTest", fqn.toString() );
	}

	@DisplayName( "Can convert a file path with prefix to a relative package name" )
	@Test
	void testItCanParseRelativePrefixedPathToPackagePath() {
		BoxFQN fqn = new BoxFQN( Paths.get( "src/test/java/" ), Paths.get( "src/test/java/ortus/boxlang/runtime/util/BoxFQNTest.bx" ) );
		assertEquals( "ortus.boxlang.runtime.util", fqn.getPackageString() );
	}

	@DisplayName( "Can parse with no package" )
	@Test
	void testItCanParseWithNoPackage() {
		BoxFQN fqn = new BoxFQN( Paths.get( "BoxFQNTest.java" ) );
		assertEquals( "BoxFQNTest", fqn.toString() );
		assertEquals( "", fqn.getPackageString() );
		// Ensure we don't get array out of bounds exceptions
		assertEquals( "", fqn.getPackage().getPackage().getPackageString() );
	}

	@DisplayName( "Can clean invalid parts" )
	@Test
	void testItCanCleanInvalidParts() {
		BoxFQN fqn = new BoxFQN( Paths.get( "/src\\\\test\\class/ORTUS//switch/45/2run-time/util/fqntest.bx" ) );
		assertEquals( "src.test.class.ORTUS.switch.45.2run-time.util.fqntest", fqn.toString() );
	}
}
