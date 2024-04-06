
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

package ortus.boxlang.runtime.bifs.global.io;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.runnables.IBoxRunnable;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class ExpandPathTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

	@BeforeAll
	public static void setUp() throws IOException {
		instance = BoxRuntime.getInstance( true );
		// Create a mapping for the test
		instance.getConfiguration().runtime.mappings.put( "/expand/path/test",
		    Path.of( "src/test/java/ortus/boxlang/runtime/bifs/global/io/" ).toAbsolutePath().toString() );
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@Test
	public void testAbsolutePath() {
		// Calculate the absolute path. We should get it back, untouched.
		variables.put( Key.of( "testFile" ), Path.of( "src/test/java/ortus/boxlang/runtime/bifs/global/io/expandPathTest.txt" ).toAbsolutePath().toString() );
		instance.executeSource(
		    """
		    result = ExpandPath( variables.testFile );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( variables.get( Key.of( "testFile" ) ) );
	}

	@Test
	public void testRelativeRootPath() {
		// path relative to root mapping
		variables.put( Key.of( "testFile" ), "/src/test/java/ortus/boxlang/runtime/bifs/global/io/expandPathTest.txt" );
		String abs = Path.of( "src/test/java/ortus/boxlang/runtime/bifs/global/io/expandPathTest.txt" ).toAbsolutePath().toString();
		instance.executeSource(
		    """
		    result = ExpandPath( variables.testFile );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( abs );
	}

	@Test
	public void testRelativeRootPathNoLeadingSlash() {
		// path relative to root mapping
		variables.put( Key.of( "testFile" ), "src/test/java/ortus/boxlang/runtime/bifs/global/io/expandPathTest.txt" );
		String abs = Path.of( "src/test/java/ortus/boxlang/runtime/bifs/global/io/expandPathTest.txt" ).toAbsolutePath().toString();
		instance.executeSource(
		    """
		    result = ExpandPath( variables.testFile );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( abs );
	}

	@Test
	public void testMapping() {
		// path relative to custom mapping
		String abs = Path.of( "src/test/java/ortus/boxlang/runtime/bifs/global/io/expandPathTest.txt" ).toAbsolutePath().toString();
		instance.executeSource(
		    """
		    result = ExpandPath( "/expand/path/test/expandPathTest.txt" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( abs );
	}

	@Test
	public void testRelative() {
		String abs = Path.of( "src/test/java/ortus/boxlang/runtime/bifs/global/io/expandPathTest.txt" ).toAbsolutePath().toString();
		context.pushTemplate( new IBoxRunnable() {

			@Override
			public List<ImportDefinition> getImports() {
				return List.of();
			}

			@Override
			public long getRunnableCompileVersion() {
				return 0L;
			}

			@Override
			public LocalDateTime getRunnableCompiledOn() {
				return null;
			}

			@Override
			public Object getRunnableAST() {
				return null;
			}

			@Override
			public Path getRunnablePath() {
				return Path.of( "src/test/java/ortus/boxlang/runtime/bifs/global/io/ExpandPathTest.java" ).toAbsolutePath();
			}

			@Override
			public BoxSourceType getSourceType() {
				return BoxSourceType.BOXTEMPLATE;
			}

		} );
		instance.executeSource(
		    """
		    result = ExpandPath( "expandPathTest.txt" );
		    """,
		    context );
		context.popTemplate();
		assertThat( variables.get( result ) ).isEqualTo( abs );
	}
}
