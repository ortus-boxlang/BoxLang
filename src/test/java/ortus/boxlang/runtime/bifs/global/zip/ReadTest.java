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

package ortus.boxlang.runtime.bifs.global.zip;

import static com.google.common.truth.Truth.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

import static org.junit.jupiter.api.Assertions.assertThrows;

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

public class ReadTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;

	@TempDir
	Path				tempDirectory;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@BeforeEach
	public void setupEach() {
		this.context	= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		this.variables	= this.context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "It can read a ZIP entry using an explicitly supplied charset" )
	@Test
	public void testReadExplicitCharset() throws Exception {
		// create an archive with known contents.
		Path	archive		= this.tempDirectory.resolve( "example.zip" );
		String	expected	= "Hello, café!";

		try ( ZipOutputStream zip = new ZipOutputStream( Files.newOutputStream( archive ) ) ) {
			zip.putNextEntry( new ZipEntry( "docs/message.txt" ) );
			zip.write( expected.getBytes( StandardCharsets.UTF_16LE ) );
			zip.closeEntry();
		}

		this.variables.put( Key.source, archive.toString() );

		// call the BIF through BoxLang.
		instance.executeSource(
		    """
		    result = read( source, 'docs/message.txt', 'UTF-16LE' );
		    """,
		    this.context
		);
		assertThat( this.variables.get( Key.of( "result" ) ) ).isEqualTo( expected );
	}

	@DisplayName( "It reads a ZIP entry using the default charset when charset is omitted" )
	@Test
	public void testReadDefaultCharset() throws Exception {
		// create an archive with known contents.
		Path	archive		= this.tempDirectory.resolve( "example.zip" );
		String	expected	= "Hello, café!";

		try ( ZipOutputStream zip = new ZipOutputStream( Files.newOutputStream( archive ) ) ) {
			zip.putNextEntry( new ZipEntry( "docs/message.txt" ) );
			zip.write( expected.getBytes( java.nio.charset.Charset.defaultCharset() ) );
			zip.closeEntry();
		}

		this.variables.put( Key.source, archive.toString() );

		// call the BIF through BoxLang.
		instance.executeSource(
		    """
		    result = read( source, 'docs/message.txt' );
		    """,
		    this.context
		);
		assertThat( this.variables.get( Key.of( "result" ) ) ).isEqualTo( expected );
	}

	@DisplayName( "It throws an exception when the ZIP entry does not exist" )
	@Test
	public void testReadMissingEntry() throws Exception {
		// create a empty archive.
		Path archive = this.tempDirectory.resolve( "example.zip" );

		try ( ZipOutputStream zip = new ZipOutputStream( Files.newOutputStream( archive ) ) ) {
		}

		this.variables.put( Key.source, archive.toString() );

		// call the BIF through BoxLang.
		BoxRuntimeException exception = assertThrows(
		    BoxRuntimeException.class,
		    () -> instance.executeSource(
		        """
		        result = read( source, 'docs/message.txt' );
		        """,
		        this.context
		    )
		);

		assertThat( exception.getMessage() ).contains( "Entry not found in zip file" );
	}
}
