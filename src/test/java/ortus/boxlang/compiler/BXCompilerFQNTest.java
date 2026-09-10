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
package ortus.boxlang.compiler;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * Verifies that {@link BXCompiler} bakes <b>deterministic, root-relative</b> generated class names
 * — derived from each file's path relative to the {@code --source} root, not its absolute build
 * path. Stable names are required for ahead-of-time targets (e.g. Android) that resolve the dexed
 * class by name via parent-first delegation instead of renaming it on load.
 */
class BXCompilerFQNTest {

	static BoxRuntime runtime;

	@BeforeAll
	static void setUp() {
		runtime = BoxRuntime.getInstance();
	}

	@DisplayName( "A nested .bxm bakes a root-relative FQN, not the absolute build path" )
	@Test
	void testTemplateFqnIsRootRelative( @TempDir Path source, @TempDir Path target ) throws IOException {
		Path srcFile = source.resolve( "views/main/index.bxm" );
		Files.createDirectories( srcFile.getParent() );
		Files.writeString( srcFile, "<bx:output>hi</bx:output>" );
		Path	targetFile	= target.resolve( "views/main/index.bxm" );

		boolean	ok			= BXCompiler.compileFile( srcFile, targetFile, source, true, runtime, new ArrayList<>() );
		assertThat( ok ).isTrue();

		String	baked				= readBakedClassName( targetFile );
		String	relative			= source.relativize( srcFile ).toString();
		String	expectedRelative	= ResolvedFilePath.of( "", "", relative, srcFile ).getFQN( "boxgenerated.templates" ).toString();
		String	absoluteVariant		= ResolvedFilePath.of( "", "", srcFile.toString(), srcFile ).getFQN( "boxgenerated.templates" ).toString();

		// The baked name is derived from the relative path...
		assertThat( baked ).isEqualTo( expectedRelative );
		// ...and is NOT the old absolute-path-derived name, nor does it leak the temp build dir.
		assertThat( baked ).isNotEqualTo( absoluteVariant );
		assertThat( baked ).contains( "views" );
		assertThat( baked ).doesNotContain( source.getFileName().toString() );
	}

	@DisplayName( "Legacy compileFile overload preserves full source-path-derived FQN behavior" )
	@Test
	void testLegacyCompileFileOverloadPreservesFullPathBehavior( @TempDir Path source, @TempDir Path target ) throws IOException {
		Path srcFile = source.resolve( "views/main/index.bxm" );
		Files.createDirectories( srcFile.getParent() );
		Files.writeString( srcFile, "<bx:output>hi</bx:output>" );
		Path	targetFile	= target.resolve( "views/main/index.bxm" );

		boolean	ok			= BXCompiler.compileFile( srcFile, targetFile, true, runtime, new ArrayList<>() );
		assertThat( ok ).isTrue();

		String	baked				= readBakedClassName( targetFile );
		String	expectedFullPath	= ResolvedFilePath.of( "", "", srcFile.toString(), srcFile ).getFQN( "boxgenerated.templates" ).toString();
		String	fileNameOnly		= ResolvedFilePath.of( "", "", srcFile.getFileName().toString(), srcFile ).getFQN( "boxgenerated.templates" ).toString();

		assertThat( baked ).isEqualTo( expectedFullPath );
		assertThat( baked ).isNotEqualTo( fileNameOnly );
	}

	/**
	 * Read the original class name baked into the head of a BXCompiler container:
	 * {@code int magic, int nameLength, byte[] name (UTF-8), ...class entries}.
	 */
	private String readBakedClassName( Path container ) throws IOException {
		ByteBuffer buffer = ByteBuffer.wrap( Files.readAllBytes( container ) );
		buffer.getInt();								// magic (0xCAFEBABE)
		byte[] nameBytes = new byte[ buffer.getInt() ];	// nameLength
		buffer.get( nameBytes );
		return new String( nameBytes, StandardCharsets.UTF_8 );
	}

}
