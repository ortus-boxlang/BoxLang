/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.compiler;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DiskClassUtilTest {

	private static final byte[] JAVABYTECODE_HEADER = { ( byte ) 0xCA, ( byte ) 0xFE, ( byte ) 0xBA, ( byte ) 0xBE };

	@Test
	public void testIsJavaByteCodeFromHeader() {
		assertThat( DiskClassUtil.isJavaByteCode( JAVABYTECODE_HEADER ) ).isTrue();

		byte[] headerWithBody = new byte[ 16 ];
		System.arraycopy( JAVABYTECODE_HEADER, 0, headerWithBody, 0, 4 );
		assertThat( DiskClassUtil.isJavaByteCode( headerWithBody ) ).isTrue();

		assertThat( DiskClassUtil.isJavaByteCode( "class Foo {}".getBytes( StandardCharsets.UTF_8 ) ) ).isFalse();
		assertThat( DiskClassUtil.isJavaByteCode( new byte[] { ( byte ) 0xCA, ( byte ) 0xFE, ( byte ) 0xBA } ) ).isFalse();
		assertThat( DiskClassUtil.isJavaByteCode( new byte[ 0 ] ) ).isFalse();
		assertThat( DiskClassUtil.isJavaByteCode( ( byte[] ) null ) ).isFalse();
	}

	@Test
	public void testIsJavaByteCodeFromFile( @TempDir Path tempDir ) throws Exception {
		Path bytecode = tempDir.resolve( "HeaderOnly.class" );
		Files.write( bytecode, JAVABYTECODE_HEADER );
		assertThat( DiskClassUtil.isJavaByteCode( bytecode.toFile() ) ).isTrue();

		Path source = tempDir.resolve( "Source.bx" );
		Files.writeString( source, "class Source {}" );
		assertThat( DiskClassUtil.isJavaByteCode( source.toFile() ) ).isFalse();

		// A truncated header is not bytecode
		Path truncated = tempDir.resolve( "Truncated.class" );
		Files.write( truncated, new byte[] { ( byte ) 0xCA, ( byte ) 0xFE, ( byte ) 0xBA } );
		assertThat( DiskClassUtil.isJavaByteCode( truncated.toFile() ) ).isFalse();

		Path empty = tempDir.resolve( "Empty.class" );
		Files.write( empty, new byte[ 0 ] );
		assertThat( DiskClassUtil.isJavaByteCode( empty.toFile() ) ).isFalse();

		assertThat( DiskClassUtil.isJavaByteCode( ( File ) null ) ).isFalse();
		assertThat( DiskClassUtil.isJavaByteCode( tempDir.resolve( "missing.class" ).toFile() ) ).isFalse();
	}
}
