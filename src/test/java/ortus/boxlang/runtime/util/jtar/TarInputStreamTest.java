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
package ortus.boxlang.runtime.util.jtar;

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class TarInputStreamTest {

	@Test
	public void testCurrentOffsetIncludesHeadersDataAndPaddingWhenSkipping() throws Exception {
		ByteArrayOutputStream archive = new ByteArrayOutputStream();
		try ( TarOutputStream tarOutput = new TarOutputStream( archive ) ) {
			writeEntry( tarOutput, "one.txt", "one" );
			writeEntry( tarOutput, "two.txt", "two" );
		}

		try ( TarInputStream tarInput = new TarInputStream( new ByteArrayInputStream( archive.toByteArray() ) ) ) {
			tarInput.setDefaultSkip( true );
			assertThat( tarInput.getNextEntry().getName() ).isEqualTo( "one.txt" );
			assertThat( tarInput.getCurrentOffset() ).isEqualTo( 512L );
			assertThat( tarInput.getNextEntry().getName() ).isEqualTo( "two.txt" );
			assertThat( tarInput.getCurrentOffset() ).isEqualTo( 1536L );
		}
	}

	@Test
	public void testTarEntryPreservesPosixPermissionsWhenAvailable() throws Exception {
		java.nio.file.Path file = Files.createTempFile( "jtar-permissions", ".txt" );
		try {
			if ( Files.getFileAttributeView( file, java.nio.file.attribute.PosixFileAttributeView.class ) == null ) {
				return;
			}
			Files.setPosixFilePermissions( file, Set.of( PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE, PosixFilePermission.GROUP_READ ) );
			TarEntry entry = new TarEntry( file.toFile(), "permissions.txt" );
			assertThat( entry.getHeader().mode ).isEqualTo( 0640 );
		} finally {
			Files.deleteIfExists( file );
		}
	}

	@Test
	public void testTarEntryIdentifiesLinkTypes() throws Exception {
		TarHeader header = TarHeader.createHeader( "link", 0, 0, false, 0644 );
		header.typeflag = TarHeader.LF_SYMLINK;
		TarEntry entry = new TarEntry( header );
		assertThat( entry.isLink() ).isTrue();
	}

	@Test
	public void testTarEntrySupportsLongUstarNames() throws Exception {
		String				name	= "directory/" + "long-name-".repeat( 12 ) + "file.txt";
		java.nio.file.Path	file	= Files.createTempFile( "jtar-long-name", ".txt" );
		try {
			Files.writeString( file, "long name" );
			TarEntry				entry	= new TarEntry( file.toFile(), name );
			ByteArrayOutputStream	archive	= new ByteArrayOutputStream();
			try ( TarOutputStream output = new TarOutputStream( archive ) ) {
				output.putNextEntry( entry );
				output.write( "long name".getBytes( StandardCharsets.UTF_8 ) );
			}
			try ( TarInputStream input = new TarInputStream( new ByteArrayInputStream( archive.toByteArray() ) ) ) {
				assertThat( input.getNextEntry().getName() ).isEqualTo( name );
			}
		} finally {
			Files.deleteIfExists( file );
		}
	}

	private void writeEntry( TarOutputStream tarOutput, String name, String content ) throws Exception {
		java.nio.file.Path file = java.nio.file.Files.createTempFile( "jtar", ".txt" );
		try {
			java.nio.file.Files.writeString( file, content, StandardCharsets.UTF_8 );
			tarOutput.putNextEntry( new TarEntry( file.toFile(), name ) );
			tarOutput.write( content.getBytes( StandardCharsets.UTF_8 ) );
		} finally {
			java.nio.file.Files.deleteIfExists( file );
		}
	}
}