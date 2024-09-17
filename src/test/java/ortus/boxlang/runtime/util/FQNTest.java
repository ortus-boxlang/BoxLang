package ortus.boxlang.runtime.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Paths;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

	@DisplayName( "Can convert a file path to a relative package name" )
	@Test
	void testItCanParseRelativePathToString() {
		FQN fqn = new FQN( Paths.get( "src/test/java/" ), Paths.get( "src/test/java/ortus/boxlang/runtime/util/FQNTest.java" ) );
		assertEquals( "ortus.boxlang.runtime.util.FQNTest", fqn.toString() );
	}

	@DisplayName( "Can convert a file path with prefix to a relative package name" )
	@Test
	void testItCanParseRelativePrefixedPathToPackagePath() {
		FQN fqn = new FQN( Paths.get( "src/test/java/" ), Paths.get( "src/test/java/ortus/boxlang/runtime/util/FQNTest.java" ) );
		assertEquals( "ortus.boxlang.runtime.util", fqn.getPackageString() );
	}

	@DisplayName( "Can parse with no package" )
	@Test
	void testItCanParseWithNoPackage() {
		FQN fqn = new FQN( Paths.get( "FQNTest.java" ) );
		assertEquals( "FQNTest", fqn.toString() );
		assertEquals( "", fqn.getPackageString() );
		// Ensure we don't get array out of bounds exceptions
		assertEquals( "", fqn.getPackage().getPackage().getPackageString() );
	}
}
