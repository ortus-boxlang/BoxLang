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

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ortus.boxlang.runtime.BoxRuntime;

public class FileSystemUtilTest {

	@BeforeAll
	public static void setup() {
		BoxRuntime.getInstance( true );
	}

	@DisplayName( "createFileUri encodes spaces in Unix absolute paths" )
	@Test
	void testCreateFileUriUnixAbsoluteWithSpaces() {
		URI uri = FileSystemUtil.createFileUri( "/ortus solutions/test.bxs" );
		assertThat( uri.toString() ).doesNotContain( " " );
		assertThat( uri.toString() ).contains( "%20" );
		assertThat( uri.getScheme() ).isEqualTo( "file" );
	}

	@DisplayName( "createFileUri handles Unix absolute paths without spaces" )
	@Test
	void testCreateFileUriUnixAbsoluteWithoutSpaces() {
		URI uri = FileSystemUtil.createFileUri( "/ortus/test.bxs" );
		assertThat( uri.toString() ).isEqualTo( "file:///ortus/test.bxs" );
		assertThat( uri.getScheme() ).isEqualTo( "file" );
	}

	@DisplayName( "createFileUri encodes spaces in relative paths" )
	@Test
	void testCreateFileUriRelativeWithSpaces() {
		URI uri = FileSystemUtil.createFileUri( "ortus solutions/test.bxs" );
		assertThat( uri.toString() ).doesNotContain( " " );
		assertThat( uri.toString() ).contains( "%20" );
		assertThat( uri.isAbsolute() ).isFalse();
	}

	@DisplayName( "createFileUri handles relative paths without spaces" )
	@Test
	void testCreateFileUriRelativeWithoutSpaces() {
		URI uri = FileSystemUtil.createFileUri( "ortus/test.bxs" );
		assertThat( uri.toString() ).isEqualTo( "ortus/test.bxs" );
		assertThat( uri.isAbsolute() ).isFalse();
	}

	@DisplayName( "createFileUri converts backslashes in relative paths" )
	@Test
	void testCreateFileUriRelativeBackslashes() {
		URI uri = FileSystemUtil.createFileUri( "ortus\\test.bxs" );
		assertThat( uri.toString() ).doesNotContain( "\\" );
		assertThat( uri.toString() ).contains( "/" );
		assertThat( uri.isAbsolute() ).isFalse();
	}

	@DisplayName( "readString detects the charset from a BOM" )
	@Test
	void testReadStringDetectsBOMCharset() throws Exception {
		byte[]					text	= "BOM text".getBytes( StandardCharsets.UTF_16LE );
		ByteArrayOutputStream	bytes	= new ByteArrayOutputStream();
		bytes.write( 0xFF );
		bytes.write( 0xFE );
		bytes.write( text );
		Path file = Files.createTempFile( "boxlang-bom", ".txt" );
		try {
			Files.write( file, bytes.toByteArray() );
			assertThat( FileSystemUtil.readString( file.toString() ) ).isEqualTo( "BOM text" );
			assertThat( FileSystemUtil.readBinary( file.toString() ) ).isEqualTo( bytes.toByteArray() );
		} finally {
			Files.deleteIfExists( file );
		}
	}

	@DisplayName( "pathExistsCaseInsensitive resolves the real case of every segment" )
	@Test
	void testPathExistsCaseInsensitive( @TempDir Path tempDir ) throws IOException {
		Path file = Files.createDirectories( tempDir.resolve( "Models/Security" ) ).resolve( "Role.bx" );
		Files.createFile( file );

		assertThat( FileSystemUtil.pathExistsCaseInsensitive( file ) ).isEqualTo( file );
		assertThat( FileSystemUtil.pathExistsCaseInsensitive( tempDir.resolve( "Models/Security/Nope.bx" ) ) ).isNull();
		assertThat( FileSystemUtil.pathExistsCaseInsensitive( tempDir.resolve( "Models/Security/Role.bx/child" ) ) ).isNull();

		assumeTrue( !Files.exists( tempDir.resolve( "models/security/role.bx" ) ), "Requires a case-sensitive file system" );
		assertThat( FileSystemUtil.pathExistsCaseInsensitive( tempDir.resolve( "models/SECURITY/role.BX" ) ) ).isEqualTo( file );
	}

	@DisplayName( "pathExistsCaseInsensitive does not return a stale cached listing after a directory changes" )
	@Test
	void testPathExistsCaseInsensitiveSeesDirectoryChanges( @TempDir Path tempDir ) throws IOException {
		Path	original	= Files.createFile( tempDir.resolve( "user.bx" ) );
		Path	lookup		= tempDir.resolve( "USER.bx" );
		assumeTrue( !Files.exists( lookup ), "Requires a case-sensitive file system" );

		// Age the directory so its listing is cached
		Files.setLastModifiedTime( tempDir, FileTime.fromMillis( System.currentTimeMillis() - 3_600_000 ) );
		assertThat( FileSystemUtil.pathExistsCaseInsensitive( lookup ) ).isEqualTo( original );
		assertThat( FileSystemUtil.pathExistsCaseInsensitive( tempDir.resolve( "OTHER.bx" ) ) ).isNull();

		// Renaming and adding files changes the directory's last-modified time, which invalidates the listing
		Path	renamed	= Files.move( original, tempDir.resolve( "User.bx" ) );
		Path	other	= Files.createFile( tempDir.resolve( "Other.bx" ) );
		assertThat( FileSystemUtil.pathExistsCaseInsensitive( lookup ) ).isEqualTo( renamed );
		assertThat( FileSystemUtil.pathExistsCaseInsensitive( tempDir.resolve( "OTHER.bx" ) ) ).isEqualTo( other );

	}

	@DisplayName( "writeAtomic writes contents and replaces the existing file" )
	@Test
	void testWriteAtomicReplacesFile( @TempDir Path tempDir ) throws IOException {
		Path target = tempDir.resolve( "tasks.json" );
		FileSystemUtil.writeAtomic( target.toString(), "[ 1, 2, 3 ]" );
		assertThat( Files.readString( target ) ).isEqualTo( "[ 1, 2, 3 ]" );

		// Replacing a longer file with a shorter one must not leave trailing bytes behind
		FileSystemUtil.writeAtomic( target.toString(), "[]" );
		assertThat( Files.readString( target ) ).isEqualTo( "[]" );
	}

	@DisplayName( "writeAtomic honours the charset and the ensureDirectory flag" )
	@Test
	void testWriteAtomicCharsetAndDirectory( @TempDir Path tempDir ) throws IOException {
		Path target = tempDir.resolve( "nested/deeper/tasks.json" );
		FileSystemUtil.writeAtomic( target.toString(), "café", StandardCharsets.UTF_16.name(), true );

		assertThat( Files.exists( target ) ).isTrue();
		assertThat( Files.readString( target, StandardCharsets.UTF_16 ) ).isEqualTo( "café" );
	}

	@DisplayName( "a reader never sees a partially written file" )
	@Test
	void testWriteAtomicNeverExposesAPartialFile( @TempDir Path tempDir ) throws Exception {
		Path	target	= tempDir.resolve( "tasks.json" );
		// Both payloads are valid JSON, so any observed value must be exactly one of them. A plain
		// truncating write would let a reader see "" or a truncated prefix instead.
		String	before	= "[" + "1,".repeat( 50_000 ) + "1]";
		String	after	= "[" + "2,".repeat( 50_000 ) + "2]";
		FileSystemUtil.writeAtomic( target.toString(), before );

		java.util.concurrent.atomic.AtomicReference<String>	bad		= new java.util.concurrent.atomic.AtomicReference<>();
		java.util.concurrent.atomic.AtomicBoolean			running	= new java.util.concurrent.atomic.AtomicBoolean( true );

		Thread												reader	= new Thread( () -> {
																		while ( running.get() ) {
																			try {
																				String seen = Files.readString( target );
																				if ( !seen.equals( before ) && !seen.equals( after ) ) {
																					bad.compareAndSet( null, "length=" + seen.length() );
																					return;
																				}
																				// A reader that never pauses holds the file open continuously, which on Windows
																				// blocks every replace. Brief gaps model the real reader (a watcher or a reload).
																				Thread.sleep( 1 );
																			} catch ( Exception e ) {
																				bad.compareAndSet( null, e.getClass().getSimpleName() + ": " + e.getMessage() );
																				return;
																			}
																		}
																	}, "atomic-write-reader" );
		reader.setDaemon( true );
		reader.start();

		try {
			for ( int i = 0; i < 200; i++ ) {
				FileSystemUtil.writeAtomic( target.toString(), i % 2 == 0 ? after : before );
			}
		} finally {
			running.set( false );
			reader.join( 10_000 );
		}

		assertThat( bad.get() ).isNull();
		assertThat( java.util.Set.of( before, after ) ).contains( Files.readString( target ) );
	}

	@DisplayName( "writeAtomic leaves no temporary files behind" )
	@Test
	void testWriteAtomicCleansUp( @TempDir Path tempDir ) throws IOException {
		Path target = tempDir.resolve( "tasks.json" );
		FileSystemUtil.writeAtomic( target.toString(), "[]" );
		FileSystemUtil.writeAtomic( target.toString(), "[ 1 ]" );

		try ( var entries = Files.list( tempDir ) ) {
			assertThat( entries.map( path -> path.getFileName().toString() ).toList() )
			    .containsExactly( "tasks.json" );
		}
	}
}
