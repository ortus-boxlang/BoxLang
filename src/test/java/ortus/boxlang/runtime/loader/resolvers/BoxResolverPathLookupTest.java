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
package ortus.boxlang.runtime.loader.resolvers;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ortus.boxlang.runtime.loader.ClassLocation;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.util.FileSystemUtil;

/**
 * Tests for the file lookups the {@link BoxResolver} does to find a class file (BL-2747).
 * Every test works in its own temp directory and checks the same results the previous
 * per-extension {@link FileSystemUtil#pathExistsCaseInsensitive(Path)} loop produced.
 */
public class BoxResolverPathLookupTest extends AbstractResolverTest {

	private static final String	MAPPING	= "/bl2747";

	@TempDir
	Path						tempDir;

	private BoxResolver			boxResolver;

	/**
	 * The valid class extensions, in the precedence order the resolver tries them
	 */
	private List<String>		extensions;

	/**
	 * Whether the temp directory's file system treats names differing only in case as different files
	 */
	private boolean				caseSensitive;

	@BeforeEach
	public void setupResolver() {
		this.boxResolver	= runtime.getClassLocator().getBoxResolver();
		this.extensions		= new ArrayList<>( this.boxResolver.getValidExtensions() );
		assertThat( this.extensions.size() ).isAtLeast( 2 );
		this.caseSensitive = probeCaseSensitivity( this.tempDir );
		FileSystemUtil.clearDirectoryListingCache();
		runtime.getConfiguration().registerMapping( MAPPING, this.tempDir.toString() );
	}

	@AfterEach
	public void teardownResolver() {
		runtime.getConfiguration().unregisterMapping( MAPPING );
		FileSystemUtil.clearDirectoryListingCache();
	}

	@DisplayName( "It finds a class spelled with the file's exact case" )
	@Test
	void testExactCase() throws IOException {
		Path file = createFile( this.tempDir.resolve( "User." + this.extensions.get( 0 ) ) );

		assertThat( lookup( this.tempDir, "User" ) ).isEqualTo( file );
		assertThat( findViaMapping( "User" ) ).isEqualTo( file );
	}

	@DisplayName( "It finds a class spelled in a different case than the file" )
	@Test
	void testWrongCase() throws IOException {
		assumeCaseSensitiveFileSystem();
		Path file = createFile( this.tempDir.resolve( "User." + this.extensions.get( 0 ) ) );

		assertThat( lookup( this.tempDir, "uSER" ) ).isEqualTo( file );
		assertThat( findViaMapping( "uSER" ) ).isEqualTo( file );
	}

	@DisplayName( "It finds a class when only the second extension exists" )
	@Test
	void testSecondExtensionOnly() throws IOException {
		Path file = createFile( this.tempDir.resolve( "User." + this.extensions.get( 1 ) ) );

		assertThat( lookup( this.tempDir, "User" ) ).isEqualTo( file );
		assertThat( findViaMapping( "User" ) ).isEqualTo( file );
	}

	@DisplayName( "It finds a class in a different case when only the second extension exists" )
	@Test
	void testSecondExtensionOnlyWrongCase() throws IOException {
		assumeCaseSensitiveFileSystem();
		Path file = createFile( this.tempDir.resolve( "User." + this.extensions.get( 1 ) ) );

		assertThat( lookup( this.tempDir, "user" ) ).isEqualTo( file );
		assertThat( findViaMapping( "user" ) ).isEqualTo( file );
	}

	@DisplayName( "It keeps extension precedence when files exist for several extensions" )
	@Test
	void testExtensionPrecedence() throws IOException {
		Path first = createFile( this.tempDir.resolve( "User." + this.extensions.get( 0 ) ) );
		createFile( this.tempDir.resolve( "User." + this.extensions.get( 1 ) ) );

		assertThat( lookup( this.tempDir, "User" ) ).isEqualTo( first );
		assertThat( findViaMapping( "User" ) ).isEqualTo( first );
	}

	@DisplayName( "It keeps extension precedence when the first extension only matches case insensitively" )
	@Test
	void testExtensionPrecedenceWithCaseMismatch() throws IOException {
		assumeCaseSensitiveFileSystem();
		// The first extension only exists in another case, the second one in the exact case.
		// The previous loop returned the first extension's case-insensitive match, so we must too.
		Path first = createFile( this.tempDir.resolve( "USER." + this.extensions.get( 0 ) ) );
		createFile( this.tempDir.resolve( "User." + this.extensions.get( 1 ) ) );

		assertThat( lookup( this.tempDir, "User" ) ).isEqualTo( first );
		assertThat( findViaMapping( "User" ) ).isEqualTo( first );
	}

	@DisplayName( "It finds classes in nested folders, matching folder names case insensitively" )
	@Test
	void testNestedFolders() throws IOException {
		Path file = createFile( this.tempDir.resolve( "models/security/Role." + this.extensions.get( 1 ) ) );

		assertThat( lookup( this.tempDir, "models/security/Role" ) ).isEqualTo( file );
		assertThat( findViaMapping( "models.security.Role" ) ).isEqualTo( file );

		if ( this.caseSensitive ) {
			assertThat( lookup( this.tempDir, "Models/SECURITY/role" ) ).isEqualTo( file );
			assertThat( findViaMapping( "Models.SECURITY.role" ) ).isEqualTo( file );
		}
	}

	@DisplayName( "It returns nothing for a missing class" )
	@Test
	void testMissingClass() throws IOException {
		createFile( this.tempDir.resolve( "User." + this.extensions.get( 0 ) ) );

		assertThat( lookup( this.tempDir, "Nope" ) ).isNull();
		assertThat( lookup( this.tempDir, "missing/folder/User" ) ).isNull();
		assertThat( findViaMapping( "Nope" ) ).isNull();
	}

	@DisplayName( "It finds a file added after a lookup missed it" )
	@Test
	void testFileAddedAfterMiss() throws IOException {
		Path folder = Files.createDirectories( this.tempDir.resolve( "models" ) );
		createFile( folder.resolve( "Other." + this.extensions.get( 0 ) ) );
		// Age the folders so their listings get cached, then miss on purpose
		ageDirectory( folder );
		ageDirectory( this.tempDir );
		assertThat( lookup( this.tempDir, "models/User" ) ).isNull();
		assertThat( lookup( this.tempDir, "MODELS/user" ) ).isNull();

		Path file = createFile( folder.resolve( "User." + this.extensions.get( 1 ) ) );

		assertThat( lookup( this.tempDir, "models/User" ) ).isEqualTo( file );
		if ( this.caseSensitive ) {
			assertThat( lookup( this.tempDir, "MODELS/user" ) ).isEqualTo( file );
		}
	}

	@DisplayName( "It resolves every class in a large folder" )
	@Test
	void testManyClassesInOneFolder() throws IOException {
		Path folder = Files.createDirectories( this.tempDir.resolve( "entities" ) );
		for ( int i = 0; i < 300; i++ ) {
			createFile( folder.resolve( "Entity" + i + "." + this.extensions.get( i % 2 ) ) );
		}
		for ( int i = 0; i < 300; i++ ) {
			Path expected = folder.resolve( "Entity" + i + "." + this.extensions.get( i % 2 ) );
			assertThat( lookup( this.tempDir, "entities/Entity" + i ) ).isEqualTo( expected );
			if ( this.caseSensitive ) {
				assertThat( lookup( this.tempDir, "ENTITIES/entity" + i ) ).isEqualTo( expected );
			}
		}
	}

	/**
	 * Resolves a name the way the resolver does for relative and class path lookups
	 *
	 * @param parent    The folder to look in
	 * @param slashName The class name with slashes, no extension
	 *
	 * @return The found path or null
	 */
	private Path lookup( Path parent, String slashName ) {
		List<Path> candidates = new ArrayList<>();
		for ( String extension : this.extensions ) {
			candidates.add( parent.resolve( slashName + "." + extension ).normalize() );
		}
		return BoxResolver.findFirstExistingPath( candidates );
	}

	/**
	 * Resolves a dotted class name through the test mapping, without loading the class
	 *
	 * @param dottedName The class name relative to the mapping
	 *
	 * @return The found path or null
	 */
	private Path findViaMapping( String dottedName ) {
		Optional<ClassLocation> location = this.boxResolver.findFromLocal(
		    context,
		    MAPPING.substring( 1 ) + "." + dottedName,
		    new ArrayList<>(),
		    false,
		    Struct.EMPTY
		);
		return location.map( l -> Path.of( l.path() ) ).orElse( null );
	}

	/**
	 * Creates an empty file and its parent folders
	 *
	 * @param file The file to create
	 *
	 * @return The created file
	 */
	private static Path createFile( Path file ) throws IOException {
		Files.createDirectories( file.getParent() );
		return Files.createFile( file );
	}

	/**
	 * Sets a directory's last-modified time an hour in the past so its listing can be cached
	 *
	 * @param directory The directory to age
	 */
	private static void ageDirectory( Path directory ) throws IOException {
		Files.setLastModifiedTime( directory, FileTime.fromMillis( System.currentTimeMillis() - 3_600_000 ) );
	}

	/**
	 * Checks whether a directory's file system is case sensitive
	 *
	 * @param directory The directory to probe
	 *
	 * @return true when names differing only in case are different files
	 */
	private static boolean probeCaseSensitivity( Path directory ) {
		try {
			Path probe = Files.createTempFile( directory, "caseprobe", ".tmp" );
			try {
				return !Files.exists( probe.resolveSibling( probe.getFileName().toString().toUpperCase() ) );
			} finally {
				Files.delete( probe );
			}
		} catch ( IOException e ) {
			throw new RuntimeException( e );
		}
	}

	/**
	 * Skips the test on case-insensitive file systems, where case-insensitive matching is not used
	 */
	private void assumeCaseSensitiveFileSystem() {
		assumeTrue( this.caseSensitive, "Requires a case-sensitive file system" );
	}

}
