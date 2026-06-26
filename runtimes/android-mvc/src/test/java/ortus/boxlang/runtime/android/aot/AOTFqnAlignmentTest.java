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
package ortus.boxlang.runtime.android.aot;

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

import ortus.boxlang.compiler.BXCompiler;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.runtime.util.Mapping;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * The decisive end-to-end check for Android AOT: the class name baked by {@code BXCompiler} at
 * build time (relative to the {@code --source} app root) must be <b>identical</b> to the name the
 * runtime derives when it resolves the same template through its <b>logical mapping path</b>
 * (e.g. {@code /views/main/index.bxm}) — because on device the dexed class is found by name via
 * parent-first delegation, never renamed.
 * <p>
 * This is why {@code AndroidBoxRuntime} wires {@code ViewRenderer} with the logical mapping roots
 * ({@code /views}, {@code /layouts}) rather than absolute filesystem paths: an absolute path
 * bypasses the prefabricated directory mappings and yields an absolute-path FQN that would never
 * match the build.
 */
class AOTFqnAlignmentTest {

	static BoxRuntime runtime;

	@BeforeAll
	static void setUp() {
		runtime = BoxRuntime.getInstance();
	}

	@DisplayName( "A view resolved via its /views logical mapping derives the same FQN BXCompiler bakes" )
	@Test
	void testViewFqnMatchesBuild( @TempDir Path appRoot ) throws IOException {
		assertAligned( appRoot, "views", "/views", "main/index.bxm", "boxgenerated.templates" );
	}

	@DisplayName( "A layout resolved via its /layouts logical mapping derives the same FQN BXCompiler bakes" )
	@Test
	void testLayoutFqnMatchesBuild( @TempDir Path appRoot ) throws IOException {
		assertAligned( appRoot, "layouts", "/layouts", "main.bxm", "boxgenerated.templates" );
	}

	/**
	 * Compile {@code appRoot/dir/rel} with the app root as the source root (the build FQN), then
	 * resolve the same file through its logical mapping {@code mappingName + "/" + rel} (the runtime
	 * FQN) and assert the two names are identical.
	 */
	private void assertAligned( Path appRoot, String dir, String mappingName, String rel, String prefix ) throws IOException {
		// --- Build side: BXCompiler bakes a name relative to the app root (--source = appRoot) ---
		Path srcFile = appRoot.resolve( dir ).resolve( rel );
		Files.createDirectories( srcFile.getParent() );
		Files.writeString( srcFile, "<bx:output>hi</bx:output>" );

		Path	target	= appRoot.resolve( "compiled" ).resolve( dir ).resolve( rel );
		boolean	ok		= BXCompiler.compileFile( srcFile, target, appRoot, true, runtime, new ArrayList<>() );
		assertThat( ok ).isTrue();
		String				buildFqn	= readBakedClassName( target );

		// --- Runtime side: resolve via the logical mapping path, as ViewRenderer now does ---
		IStruct				mappings	= Struct.ofNonConcurrent(
		    Key.of( mappingName ), Mapping.ofInternal( mappingName, appRoot.resolve( dir ).toString() ),
		    Key.of( "/" ), Mapping.ofInternal( "/", appRoot.toString() )
		);
		ResolvedFilePath	resolved	= FileSystemUtil.expandPath( mappings, mappingName + "/" + rel, null, false );
		String				runtimeFqn	= resolved.getFQN( prefix ).toString();

		assertThat( runtimeFqn ).isEqualTo( buildFqn );
		// Sanity: the mapping-name segment (e.g. "views") survives into the FQN.
		assertThat( runtimeFqn ).contains( dir );
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
