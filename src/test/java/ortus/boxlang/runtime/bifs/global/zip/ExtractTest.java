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

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import org.itadaki.bzip2.BZip2OutputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.util.jtar.TarEntry;
import ortus.boxlang.runtime.util.jtar.TarOutputStream;

public class ExtractTest {

	private Path workingDirectory;

	@AfterEach
	public void cleanup() throws Exception {
		if ( workingDirectory != null ) {
			try ( var paths = Files.walk( workingDirectory ) ) {
				paths.sorted( ( left, right ) -> right.compareTo( left ) ).forEach( path -> {
					try {
						Files.deleteIfExists( path );
					} catch ( Exception e ) {
						throw new RuntimeException( e );
					}
				} );
			}
		}
	}

	@Test
	public void testExtractTar() throws Exception {
		workingDirectory = Files.createTempDirectory( "boxlang-extract-tar" );
		Path	sourceFile	= workingDirectory.resolve( "archive.tar" );
		Path	destination	= workingDirectory.resolve( "destination" );
		Path	entryFile	= Files.createTempFile( workingDirectory, "entry", ".txt" );
		byte[]	content		= "tar content".getBytes( StandardCharsets.UTF_8 );
		Files.write( entryFile, content );

		try ( TarOutputStream tarOutputStream = new TarOutputStream( new BufferedOutputStream( new FileOutputStream( sourceFile.toFile() ) ) ) ) {
			TarEntry entry = new TarEntry( entryFile.toFile(), "folder/example.txt" );
			tarOutputStream.putNextEntry( entry );
			try ( InputStream inputStream = Files.newInputStream( entryFile ) ) {
				inputStream.transferTo( tarOutputStream );
			}
		}

		BoxRuntime	instance	= BoxRuntime.getInstance( true );
		IBoxContext	context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		context.getScopeNearby( VariablesScope.name ).put( Key.source, sourceFile.toString() );
		context.getScopeNearby( VariablesScope.name ).put( Key.destination, destination.toString() );

		instance.executeSource( "extract( source=source, destination=destination );", context );

		assertThat( Files.readString( destination.resolve( "folder/example.txt" ) ) ).isEqualTo( "tar content" );
	}

	@Test
	public void testExtractTgz() throws Exception {
		workingDirectory = Files.createTempDirectory( "boxlang-extract-tgz" );
		Path	sourceFile	= workingDirectory.resolve( "archive.tgz" );
		Path	destination	= workingDirectory.resolve( "destination" );
		Path	entryFile	= Files.createTempFile( workingDirectory, "entry", ".txt" );
		Files.writeString( entryFile, "tgz content" );

		try ( GZIPOutputStream gzipOutputStream = new GZIPOutputStream( Files.newOutputStream( sourceFile ) );
		    TarOutputStream tarOutputStream = new TarOutputStream( gzipOutputStream ) ) {
			TarEntry entry = new TarEntry( entryFile.toFile(), "folder/example.txt" );
			tarOutputStream.putNextEntry( entry );
			try ( InputStream inputStream = Files.newInputStream( entryFile ) ) {
				inputStream.transferTo( tarOutputStream );
			}
		}

		BoxRuntime	instance	= BoxRuntime.getInstance( true );
		IBoxContext	context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		context.getScopeNearby( VariablesScope.name ).put( Key.source, sourceFile.toString() );
		context.getScopeNearby( VariablesScope.name ).put( Key.destination, destination.toString() );

		instance.executeSource( "extract( source=source, destination=destination );", context );

		assertThat( Files.readString( destination.resolve( "folder/example.txt" ) ) ).isEqualTo( "tgz content" );
	}

	@Test
	public void testExtractRequiresDetectableFormat() throws Exception {
		workingDirectory = Files.createTempDirectory( "boxlang-extract-unknown" );
		Path	sourceFile	= workingDirectory.resolve( "archive.data" );
		Path	destination	= workingDirectory.resolve( "destination" );
		Files.writeString( sourceFile, "not an archive" );

		BoxRuntime	instance	= BoxRuntime.getInstance( true );
		IBoxContext	context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		context.getScopeNearby( VariablesScope.name ).put( Key.source, sourceFile.toString() );
		context.getScopeNearby( VariablesScope.name ).put( Key.destination, destination.toString() );

		org.junit.jupiter.api.Assertions.assertThrows(
		    RuntimeException.class,
		    () -> instance.executeSource( "extract( source=source, destination=destination );", context )
		);
	}

	@Test
	public void testExtractBzipAndBzip2Aliases() throws Exception {
		workingDirectory = Files.createTempDirectory( "boxlang-extract-bzip" );
		Path	sourceFile	= workingDirectory.resolve( "content.bz2" );
		Path	destination	= workingDirectory.resolve( "destination" );
		Files.writeString( workingDirectory.resolve( "content" ), "bzip content" );
		try ( BZip2OutputStream output = new BZip2OutputStream( Files.newOutputStream( sourceFile ) ) ) {
			output.write( "bzip content".getBytes( StandardCharsets.UTF_8 ) );
		}

		BoxRuntime instance = BoxRuntime.getInstance( true );
		for ( String format : new String[] { "bzip", "bzip2" } ) {
			IBoxContext context = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
			context.getScopeNearby( VariablesScope.name ).put( Key.source, sourceFile.toString() );
			context.getScopeNearby( VariablesScope.name ).put( Key.destination, destination.toString() );
			instance.executeSource( "extract( format='" + format + "', source=source, destination=destination, overwrite=true );", context );
			assertThat( Files.readString( destination.resolve( "content" ) ) ).isEqualTo( "bzip content" );
		}
	}

	@Test
	public void testExtractTbzAliases() throws Exception {
		workingDirectory = Files.createTempDirectory( "boxlang-extract-tbz" );
		Path entryFile = workingDirectory.resolve( "entry.txt" );
		Files.writeString( entryFile, "tbz content" );
		Path	sourceFile	= workingDirectory.resolve( "archive.tbz2" );
		Path	destination	= workingDirectory.resolve( "destination" );

		try ( BZip2OutputStream bzipOutput = new BZip2OutputStream( Files.newOutputStream( sourceFile ) );
		    TarOutputStream tarOutput = new TarOutputStream( bzipOutput ) ) {
			TarEntry entry = new TarEntry( entryFile.toFile(), "folder/example.txt" );
			tarOutput.putNextEntry( entry );
			try ( InputStream input = Files.newInputStream( entryFile ) ) {
				input.transferTo( tarOutput );
			}
		}

		BoxRuntime instance = BoxRuntime.getInstance( true );
		for ( String format : new String[] { "tbz", "tbz2", "tar.bz" } ) {
			IBoxContext context = new ScriptingRequestBoxContext( instance.getRuntimeContext() );
			context.getScopeNearby( VariablesScope.name ).put( Key.source, sourceFile.toString() );
			context.getScopeNearby( VariablesScope.name ).put( Key.destination, destination.toString() );
			instance.executeSource( "extract( format='" + format + "', source=source, destination=destination, overwrite=true );", context );
			assertThat( Files.readString( destination.resolve( "folder/example.txt" ) ) ).isEqualTo( "tbz content" );
		}
	}
}
