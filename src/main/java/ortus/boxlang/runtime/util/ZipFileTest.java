package ortus.boxlang.runtime.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * JUnit tests for the ZipFile fluent API.
 *
 * Run via Gradle:
 *   ./gradlew test --tests "ortus.boxlang.runtime.util.ZipFileTest"
 */
@DisplayName( "ZipFile Fluent API" )
class ZipFileTest {

	private Path	tempDir;
	private Path	sourceDir;
	private Path	zipOutput;
	private Path	extractDir;

	@BeforeEach
	void setUp() throws IOException {
		tempDir    = Files.createTempDirectory( "zipfile-test" );
		sourceDir  = tempDir.resolve( "source" );
		zipOutput  = tempDir.resolve( "output.zip" );
		extractDir = tempDir.resolve( "extracted" );

		// Create a small directory structure to compress
		Files.createDirectories( sourceDir.resolve( "subdir" ) );
		Files.writeString( sourceDir.resolve( "hello.txt" ), "Hello, BoxLang!" );
		Files.writeString( sourceDir.resolve( "subdir/nested.txt" ), "Nested file content" );
	}

	@AfterEach
	void tearDown() throws IOException {
		// Recursively clean up temp directory
		if ( Files.exists( tempDir ) ) {
			Files.walk( tempDir )
			    .sorted( java.util.Comparator.reverseOrder() )
			    .forEach( path -> {
				    try {
					    Files.deleteIfExists( path );
				    } catch ( IOException e ) {
					    // ignore cleanup errors
				    }
			    } );
		}
	}

	// ---------------------------------------------------------------
	// compress() tests
	// ---------------------------------------------------------------

	@Test
	@DisplayName( "It can compress a directory into a ZIP file" )
	void testCompressDirectory() {
		new ZipFile()
		    .source( sourceDir.toString() )
		    .to( zipOutput.toString() )
		    .compress();

		assertTrue( Files.exists( zipOutput ), "ZIP file should be created" );
		assertTrue( Files.size( zipOutput ) > 0, "ZIP file should not be empty" );
	}

	@Test
	@DisplayName( "It can compress a single file into a ZIP" )
	void testCompressSingleFile() {
		Path singleFile = sourceDir.resolve( "hello.txt" );

		new ZipFile()
		    .source( singleFile.toString() )
		    .to( zipOutput.toString() )
		    .compress();

		assertTrue( Files.exists( zipOutput ) );
	}

	@Test
	@DisplayName( "compress() throws if source does not exist" )
	void testCompressNonExistentSource() {
		assertThrows( BoxRuntimeException.class, () -> {
			new ZipFile()
			    .source( "/nonexistent/path" )
			    .to( zipOutput.toString() )
			    .compress();
		} );
	}

	@Test
	@DisplayName( "compress() throws if source or destination not set" )
	void testCompressMissingFields() {
		assertThrows( BoxRuntimeException.class, () -> {
			new ZipFile().compress();
		} );

		assertThrows( BoxRuntimeException.class, () -> {
			new ZipFile()
			    .source( sourceDir.toString() )
			    .compress();
		} );
	}

	// ---------------------------------------------------------------
	// extract() tests
	// ---------------------------------------------------------------

	@Test
	@DisplayName( "It can extract a ZIP archive into a directory" )
	void testExtract() throws IOException {
		// First compress
		new ZipFile()
		    .source( sourceDir.toString() )
		    .to( zipOutput.toString() )
		    .compress();

		// Then extract
		Files.createDirectories( extractDir );
		new ZipFile()
		    .source( zipOutput.toString() )
		    .to( extractDir.toString() )
		    .extract();

		// Verify extracted files exist
		assertTrue( Files.exists( extractDir.resolve( "hello.txt" ) ) );
		assertTrue( Files.exists( extractDir.resolve( "subdir/nested.txt" ) ) );

		// Verify content is preserved
		assertEquals( "Hello, BoxLang!", Files.readString( extractDir.resolve( "hello.txt" ) ) );
		assertEquals( "Nested file content", Files.readString( extractDir.resolve( "subdir/nested.txt" ) ) );
	}

	@Test
	@DisplayName( "extract() throws if source or destination not set" )
	void testExtractMissingFields() {
		assertThrows( BoxRuntimeException.class, () -> {
			new ZipFile().extract();
		} );
	}

	// ---------------------------------------------------------------
	// list() tests
	// ---------------------------------------------------------------

	@Test
	@DisplayName( "It can list contents of a ZIP without extracting" )
	void testList() {
		// Compress first
		new ZipFile()
		    .source( sourceDir.toString() )
		    .to( zipOutput.toString() )
		    .compress();

		// List entries
		List<String> entries = new ZipFile()
		    .source( zipOutput.toString() )
		    .list();

		assertNotNull( entries );
		assertFalse( entries.isEmpty() );
		assertTrue( entries.contains( "hello.txt" ) );
		assertTrue( entries.contains( "subdir/nested.txt" ) );
	}

	@Test
	@DisplayName( "list() throws if source not set" )
	void testListMissingSource() {
		assertThrows( BoxRuntimeException.class, () -> {
			new ZipFile().list();
		} );
	}

	// ---------------------------------------------------------------
	// Fluent API tests
	// ---------------------------------------------------------------

	@Test
	@DisplayName( "Fluent methods return this for chaining" )
	void testFluentChaining() {
		ZipFile zf = new ZipFile();
		assertSame( zf, zf.source( "/some/path" ) );
		assertSame( zf, zf.to( "/some/dest" ) );
	}

	@Test
	@DisplayName( "Getters return the correct values" )
	void testGetters() {
		ZipFile zf = new ZipFile()
		    .source( "/my/source" )
		    .to( "/my/dest" );

		assertEquals( "/my/source", zf.getSource() );
		assertEquals( "/my/dest", zf.getDestination() );
	}

	@Test
	@DisplayName( "Constructor with source sets the source path" )
	void testConstructorWithSource() {
		ZipFile zf = new ZipFile( "/my/source" );
		assertEquals( "/my/source", zf.getSource() );
	}

	// ---------------------------------------------------------------
	// Round-trip test
	// ---------------------------------------------------------------

	@Test
	@DisplayName( "Full round-trip: compress -> list -> extract preserves data" )
	void testRoundTrip() throws IOException {
		// Compress
		new ZipFile()
		    .source( sourceDir.toString() )
		    .to( zipOutput.toString() )
		    .compress();

		// List
		List<String> entries = new ZipFile()
		    .source( zipOutput.toString() )
		    .list();
		assertEquals( 2, entries.size() );

		// Extract
		Files.createDirectories( extractDir );
		new ZipFile()
		    .source( zipOutput.toString() )
		    .to( extractDir.toString() )
		    .extract();

		// Verify
		assertEquals(
		    Files.readString( sourceDir.resolve( "hello.txt" ) ),
		    Files.readString( extractDir.resolve( "hello.txt" ) )
		);
		assertEquals(
		    Files.readString( sourceDir.resolve( "subdir/nested.txt" ) ),
		    Files.readString( extractDir.resolve( "subdir/nested.txt" ) )
		);
	}
}
