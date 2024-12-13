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
		assertEquals( "src.test.java.ortus.boxlang.runtime.util.Fqntest$java", fqn.toString() );
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
		assertEquals( "ortus.boxlang.runtime.util.Fqntest$java", fqn.toString() );
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
		assertEquals( "Fqntest$java", fqn.toString() );
		assertEquals( "", fqn.getPackageString() );
		// Ensure we don't get array out of bounds exceptions
		assertEquals( "", fqn.getPackage().getPackage().getPackageString() );
	}

	@DisplayName( "Can parse string" )
	@Test
	void testItCanParseString() {
		FQN fqn = new FQN( "foo.Bar.baz" );
		assertEquals( "foo.bar.Baz", fqn.toString() );
	}

	@DisplayName( "Can parse string with prefix" )
	@Test
	void testItCanParseStringWithPrefix() {
		FQN fqn = new FQN( "brad.wood", "foo.Bar.baz" );
		assertEquals( "brad.wood.foo.bar.Baz", fqn.toString() );
	}

	@DisplayName( "Can parse fqn with prefix" )
	@Test
	void testItCanParseFQNWithPrefix() {
		FQN fqn = new FQN( "brad.wood", FQN.of( "foo.Bar.baz" ) );
		assertEquals( "brad.wood.foo.bar.Baz", fqn.toString() );
	}

	@DisplayName( "Can clean invalid parts" )
	@Test
	void testItCanCleanInvalidParts() {
		FQN fqn = new FQN( Paths.get( "/src\\\\test\\class/ORTUS//switch/45/2run-time/util/fqntest.java" ) );
		assertEquals( "src.test._class.ortus._switch._45._2run__time.util.Fqntest$java", fqn.toString() );
	}
}
