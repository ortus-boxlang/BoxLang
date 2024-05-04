
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
import ortus.boxlang.runtime.util.ResolvedFilePath;

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
			public ResolvedFilePath getRunnablePath() {
				return ResolvedFilePath.of( Path.of( "src/test/java/ortus/boxlang/runtime/bifs/global/io/ExpandPathTest.java" ).toAbsolutePath() );
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

	@Test
	public void testCanonicalize() {
		// This test assumes the project is checked out at least 2 folders deep. If this becomes an issue
		// then change the test to set the root `/` mapping equals to a fake folder at least 2 levels deep.
		String	rootMapping						= ( String ) context.getConfigItems( Key.runtime, Key.mappings, Key.of( "/" ) );
		String	parentOfRootMappings			= Path.of( rootMapping ).getParent().toString();
		String	parentOfParentOfRootMappings	= Path.of( parentOfRootMappings ).getParent().toString();
		instance.executeSource(
		    """
		    result1 = expandPath('.') //		`/dir/subdir`
		    result2 = expandPath('..') //		`/dir`
		    result3 = expandPath('./') //		`/dir/subdir/`
		    result4 = expandPath('../') //		`/dir/`
		    result5 = expandPath('./.') //		`/dir/subdir`
		    result6 = expandPath('../..') //	``
		      """,
		    context );
		assertThat( variables.get( Key.of( "result1" ) ) ).isEqualTo( rootMapping );
		assertThat( variables.get( Key.of( "result2" ) ) ).isEqualTo( parentOfRootMappings );
		assertThat( variables.get( Key.of( "result3" ) ) ).isEqualTo( rootMapping );
		assertThat( variables.get( Key.of( "result4" ) ) ).isEqualTo( parentOfRootMappings );
		assertThat( variables.get( Key.of( "result5" ) ) ).isEqualTo( rootMapping );
		assertThat( variables.get( Key.of( "result6" ) ) ).isEqualTo( parentOfParentOfRootMappings );
	}
}
