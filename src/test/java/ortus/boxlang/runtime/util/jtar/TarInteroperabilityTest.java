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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

public class TarInteroperabilityTest {

	@Test
	public void testReadsArchivesCreatedBySystemTar() throws Exception {
		Assumptions.assumeTrue( commandExists( "tar" ) );
		Path work = Files.createTempDirectory( "jtar-interoperability" );
		try {
			Path source = work.resolve( "source" );
			Files.createDirectories( source );
			String	fileName	= "interop.txt";
			Path	file		= source.resolve( fileName );
			Files.writeString( file, "system tar content" );
			for ( String format : new String[] { "ustar", "pax" } ) {
				Path archive = work.resolve( format + ".tar" );
				run( work, "tar", "--format=" + format, "-cf", archive.toString(), "-C", source.toString(), fileName );
				try ( TarInputStream input = new TarInputStream( new BufferedInputStream( Files.newInputStream( archive ) ) ) ) {
					boolean		found	= false;
					TarEntry	entry;
					while ( ( entry = input.getNextEntry() ) != null ) {
						if ( entry.getName().replace( "\\", "/" ).endsWith( fileName ) ) {
							found = true;
							assertThat( readCurrentEntry( input ) ).isEqualTo( "system tar content" );
						}
					}
					assertThat( found ).isTrue();
				}
			}
		} finally {
			try ( var paths = Files.walk( work ) ) {
				paths.sorted( ( left, right ) -> right.compareTo( left ) ).forEach( path -> path.toFile().delete() );
			}
		}
	}

	private boolean commandExists( String command ) {
		try {
			new ProcessBuilder( command, "--version" ).start().destroy();
			return true;
		} catch ( IOException e ) {
			return false;
		}
	}

	private void run( Path directory, String... command ) throws Exception {
		Process process = new ProcessBuilder( command ).directory( directory.toFile() ).redirectErrorStream( true ).start();
		if ( process.waitFor() != 0 )
			throw new IOException( "tar command failed" );
	}

	private String readCurrentEntry( TarInputStream input ) throws IOException {
		return new String( input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8 );
	}
}
