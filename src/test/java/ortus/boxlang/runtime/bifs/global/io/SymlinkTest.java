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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Query;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Tests the symbolic link BIFs and the link handling of the file and directory BIFs.
 * The tests are skipped on systems where symbolic links cannot be created.
 */
public class SymlinkTest {

	static BoxRuntime	instance;
	static Key			result	= new Key( "result" );
	IBoxContext			context;
	IScope				variables;
	Path				root;
	Path				outside;
	Path				tree;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {
		// Nothing to do, each test cleans up its own temp folder
	}

	@BeforeEach
	public void setupEach() throws IOException {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
		root		= Files.createTempDirectory( "symlinkTest" ).toRealPath();
		outside		= Files.createDirectories( root.resolve( "outside" ) );
		Files.writeString( outside.resolve( "keep.txt" ), "keep" );
		Files.createDirectories( outside.resolve( "deep" ) );
		Files.writeString( outside.resolve( "deep/inner.txt" ), "inner" );
		tree = Files.createDirectories( root.resolve( "tree" ) );
		Files.writeString( tree.resolve( "a.txt" ), "a" );

		// Skip everything if this system can't create links
		Path probe = root.resolve( "probe" );
		try {
			Files.createSymbolicLink( probe, outside );
			Files.delete( probe );
		} catch ( UnsupportedOperationException | IOException | SecurityException e ) {
			assumeTrue( false, "Symbolic links are not supported here: " + e.getMessage() );
		}
		variables.put( Key.of( "root" ), root.toString() );
		variables.put( Key.of( "outside" ), outside.toString() );
		variables.put( Key.of( "tree" ), tree.toString() );
	}

	@AfterEach
	public void cleanup() throws IOException {
		// deleteDirectory no longer follows links by default, so this cannot touch anything outside root
		ortus.boxlang.runtime.util.FileSystemUtil.deleteDirectory( root.toString(), true );
	}

	@DisplayName( "fileCreateSymlink, fileIsSymlink and fileReadSymlink work together" )
	@Test
	public void testCreateIsRead() {
		instance.executeSource(
		    """
		    result = fileCreateSymlink( variables.root & "/current", variables.outside );
		    isLink = fileIsSymlink( variables.root & "/current" );
		    notLink = fileIsSymlink( variables.outside & "/keep.txt" );
		    missing = fileIsSymlink( variables.root & "/nope" );
		    target = fileReadSymlink( variables.root & "/current" );
		    """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( root.resolve( "current" ).toString() );
		assertThat( variables.get( Key.of( "isLink" ) ) ).isEqualTo( true );
		assertThat( variables.get( Key.of( "notLink" ) ) ).isEqualTo( false );
		assertThat( variables.get( Key.of( "missing" ) ) ).isEqualTo( false );
		assertThat( variables.getAsString( Key.of( "target" ) ) ).isEqualTo( outside.toString() );
		assertThat( Files.isSymbolicLink( root.resolve( "current" ) ) ).isTrue();
	}

	@DisplayName( "fileReadSymlink returns the stored target, or an absolute one with resolve" )
	@Test
	public void testReadRelativeTarget() {
		instance.executeSource(
		    """
		    fileCreateSymlink( variables.root & "/rel", "outside" );
		    stored = fileReadSymlink( variables.root & "/rel" );
		    resolved = fileReadSymlink( path=variables.root & "/rel", resolve=true );
		    """,
		    context );
		assertThat( variables.getAsString( Key.of( "stored" ) ) ).isEqualTo( "outside" );
		assertThat( variables.getAsString( Key.of( "resolved" ) ) ).isEqualTo( outside.toString() );
	}

	@DisplayName( "fileIsSymlink is true for a dangling link" )
	@Test
	public void testDanglingLink() {
		instance.executeSource(
		    """
		    fileCreateSymlink( variables.root & "/dangling", variables.root & "/does-not-exist" );
		    result = fileIsSymlink( variables.root & "/dangling" );
		    """,
		    context );
		assertThat( variables.get( result ) ).isEqualTo( true );
	}

	@DisplayName( "fileReadSymlink throws on a path that is not a link" )
	@Test
	public void testReadNonLink() {
		assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    fileReadSymlink( variables.outside & "/keep.txt" );
		    """,
		    context ) );
	}

	@DisplayName( "fileCreateSymlink throws if the path exists unless overwrite is true" )
	@Test
	public void testOverwrite() {
		instance.executeSource(
		    """
		    fileCreateSymlink( variables.root & "/current", variables.outside );
		    """,
		    context );
		assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    fileCreateSymlink( variables.root & "/current", variables.tree );
		    """,
		    context ) );
		instance.executeSource(
		    """
		    fileCreateSymlink( variables.root & "/current", variables.tree, true );
		    result = fileReadSymlink( variables.root & "/current" );
		    """,
		    context );
		assertThat( variables.getAsString( result ) ).isEqualTo( tree.toString() );
	}

	@DisplayName( "fileCreateSymlink never replaces a directory" )
	@Test
	public void testOverwriteDirectory() {
		assertThrows( BoxRuntimeException.class, () -> instance.executeSource(
		    """
		    fileCreateSymlink( variables.tree, variables.outside, true );
		    """,
		    context ) );
		assertThat( Files.isDirectory( tree, LinkOption.NOFOLLOW_LINKS ) ).isTrue();
	}

	@DisplayName( "fileInfo and getFileInfo report isLink and linkTarget" )
	@Test
	public void testFileInfo() {
		instance.executeSource(
		    """
		    fileCreateSymlink( variables.root & "/current", variables.outside );
		    linkInfo = fileInfo( variables.root & "/current" );
		    linkVerbose = getFileInfo( variables.root & "/current" );
		    fileInfoStruct = fileInfo( variables.outside & "/keep.txt" );
		    fileVerbose = getFileInfo( variables.outside & "/keep.txt" );
		    """,
		    context );
		IStruct linkInfo = variables.getAsStruct( Key.of( "linkInfo" ) );
		assertThat( linkInfo.get( Key.isLink ) ).isEqualTo( true );
		assertThat( linkInfo.getAsString( Key.linkTarget ) ).isEqualTo( outside.toString() );
		IStruct linkVerbose = variables.getAsStruct( Key.of( "linkVerbose" ) );
		assertThat( linkVerbose.get( Key.isLink ) ).isEqualTo( true );
		assertThat( linkVerbose.getAsString( Key.linkTarget ) ).isEqualTo( outside.toString() );
		IStruct fileInfo = variables.getAsStruct( Key.of( "fileInfoStruct" ) );
		assertThat( fileInfo.get( Key.isLink ) ).isEqualTo( false );
		assertThat( fileInfo.getAsString( Key.linkTarget ) ).isEqualTo( "" );
		IStruct fileVerbose = variables.getAsStruct( Key.of( "fileVerbose" ) );
		assertThat( fileVerbose.get( Key.isLink ) ).isEqualTo( false );
	}

	@DisplayName( "directoryDelete removes a nested link and leaves its target alone" )
	@Test
	public void testDeleteNestedLink() throws IOException {
		Files.createSymbolicLink( tree.resolve( "lnk" ), outside );
		instance.executeSource(
		    """
		    directoryDelete( variables.tree, true );
		    """,
		    context );
		assertThat( Files.exists( tree ) ).isFalse();
		assertThat( Files.exists( outside.resolve( "keep.txt" ) ) ).isTrue();
		assertThat( Files.exists( outside.resolve( "deep/inner.txt" ) ) ).isTrue();
	}

	@DisplayName( "directoryDelete on a link path removes only the link" )
	@Test
	public void testDeleteTopLevelLink() throws IOException {
		Path link = Files.createSymbolicLink( root.resolve( "lnk" ), outside );
		variables.put( Key.of( "link" ), link.toString() );
		instance.executeSource(
		    """
		    directoryDelete( variables.link, true );
		    """,
		    context );
		assertThat( Files.exists( link, LinkOption.NOFOLLOW_LINKS ) ).isFalse();
		assertThat( Files.exists( outside.resolve( "keep.txt" ) ) ).isTrue();
	}

	@DisplayName( "directoryDelete with followLinks=true empties the link target" )
	@Test
	public void testDeleteFollowLinks() throws IOException {
		Files.createSymbolicLink( tree.resolve( "lnk" ), outside );
		instance.executeSource(
		    """
		    directoryDelete( variables.tree, true, true );
		    """,
		    context );
		assertThat( Files.exists( tree ) ).isFalse();
		assertThat( Files.exists( outside.resolve( "keep.txt" ) ) ).isFalse();
	}

	@DisplayName( "directoryDelete handles links to files and dangling links" )
	@Test
	public void testDeleteFileAndDanglingLinks() throws IOException {
		Files.createSymbolicLink( tree.resolve( "fileLink" ), outside.resolve( "keep.txt" ) );
		Files.createSymbolicLink( tree.resolve( "dangling" ), root.resolve( "nope" ) );
		instance.executeSource(
		    """
		    directoryDelete( variables.tree, true );
		    """,
		    context );
		assertThat( Files.exists( tree ) ).isFalse();
		assertThat( Files.exists( outside.resolve( "keep.txt" ) ) ).isTrue();
	}

	@DisplayName( "directoryList does not descend into links by default and does with followLinks" )
	@Test
	public void testListFollowLinks() throws IOException {
		Files.createSymbolicLink( tree.resolve( "lnk" ), outside );
		instance.executeSource(
		    """
		    noFollow = directoryList( variables.tree, true, "name" );
		    follow = directoryList( path=variables.tree, recurse=true, listInfo="name", followLinks=true );
		    """,
		    context );
		Array	noFollow	= variables.getAsArray( Key.of( "noFollow" ) );
		Array	follow		= variables.getAsArray( Key.of( "follow" ) );
		assertThat( noFollow ).containsExactly( "a.txt", "lnk" );
		assertThat( follow ).contains( "keep.txt" );
		assertThat( follow ).contains( "inner.txt" );
	}

	@DisplayName( "directoryList lists the contents of a link given as the path" )
	@Test
	public void testListTopLevelLink() throws IOException {
		Path link = Files.createSymbolicLink( root.resolve( "lnk" ), outside );
		variables.put( Key.of( "link" ), link.toString() );
		instance.executeSource(
		    """
		    result = directoryList( variables.link, false, "name" );
		    paths = directoryList( variables.link, false, "path" );
		    """,
		    context );
		assertThat( variables.getAsArray( result ) ).containsExactly( "deep", "keep.txt" );
		// Paths are reported under the path that was asked for
		assertThat( variables.getAsArray( Key.of( "paths" ) ).get( 0 ).toString() ).startsWith( link.toString() );
	}

	@DisplayName( "directoryList query includes isLink and linkTarget" )
	@Test
	public void testListQueryColumns() throws IOException {
		Files.createSymbolicLink( tree.resolve( "lnk" ), outside );
		instance.executeSource(
		    """
		    result = directoryList( variables.tree, false, "query", "", "name" );
		    """,
		    context );
		Query q = variables.getAsQuery( result );
		assertThat( q.size() ).isEqualTo( 2 );
		assertThat( q.getCell( Key.isLink, 0 ) ).isEqualTo( false );
		assertThat( q.getCell( Key.linkTarget, 0 ) ).isEqualTo( "" );
		assertThat( q.getCell( Key.isLink, 1 ) ).isEqualTo( true );
		assertThat( q.getCell( Key.linkTarget, 1 ) ).isEqualTo( outside.toString() );
	}

	@DisplayName( "directoryCopy copies links as links by default" )
	@Test
	public void testCopyLinksAsLinks() throws IOException {
		Files.createSymbolicLink( tree.resolve( "lnk" ), outside );
		variables.put( Key.of( "dest" ), root.resolve( "dest" ).toString() );
		instance.executeSource(
		    """
		    directoryCopy( variables.tree, variables.dest, true );
		    """,
		    context );
		Path copied = root.resolve( "dest/lnk" );
		assertThat( Files.isSymbolicLink( copied ) ).isTrue();
		assertThat( Files.readSymbolicLink( copied ) ).isEqualTo( outside );
		assertThat( Files.exists( root.resolve( "dest/a.txt" ) ) ).isTrue();
	}

	@DisplayName( "directoryCopy with followLinks=true copies the link content" )
	@Test
	public void testCopyFollowLinks() throws IOException {
		Files.createSymbolicLink( tree.resolve( "lnk" ), outside );
		variables.put( Key.of( "dest" ), root.resolve( "dest" ).toString() );
		instance.executeSource(
		    """
		    directoryCopy( variables.tree, variables.dest, true, "*", true, false, true );
		    """,
		    context );
		Path copied = root.resolve( "dest/lnk" );
		assertThat( Files.isSymbolicLink( copied ) ).isFalse();
		assertThat( Files.readString( copied.resolve( "keep.txt" ) ) ).isEqualTo( "keep" );
		assertThat( Files.readString( copied.resolve( "deep/inner.txt" ) ) ).isEqualTo( "inner" );
	}

}
