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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;

/**
 * Unpacks the BoxLang AOT class container produced by {@code BXCompiler} into individual,
 * standard {@code .class} files that Android's D8/R8 can dex into an APK.
 * <p>
 * <b>Why this exists:</b> {@code BXCompiler} writes each compiled {@code .bx}/{@code .bxm}
 * as a single binary container — a magic int, the original FQN, then a sequence of
 * length-prefixed JVM {@code .class} entries (the main class plus any inner/aux classes).
 * The bytes inside are ordinary JVM bytecode; they are simply concatenated for the
 * disk-cache loader. To go through the Android toolchain we extract each entry to a real
 * {@code <internal/name>.class} file on disk, which D8 then dexes normally.
 * <p>
 * Container format (see {@code DiskClassLoader.defineClasses}):
 *
 * <pre>
 * int    magic
 * int    originalClassNameLength
 * byte[] originalClassName (UTF-8)
 * repeat:
 *   int    classByteLength
 *   byte[] classBytes      (a standard JVM .class)
 * </pre>
 *
 * Pure JVM — no Android dependencies — so it is unit-tested on a plain JVM.
 */
public final class BoxClassExtractor {

	private BoxClassExtractor() {
		// static utility
	}

	/**
	 * CLI entry for the Gradle AOT pipeline: extract every container under {@code <sourceDir>}
	 * into {@code <outputDir>} as {@code .class} files for D8/R8 to dex.
	 *
	 * @param args {@code [ sourceDir, outputDir ]}
	 *
	 * @throws IOException If extraction fails
	 */
	public static void main( String[] args ) throws IOException {
		if ( args.length < 2 ) {
			System.err.println( "Usage: BoxClassExtractor <sourceDir> <outputDir>" );
			System.exit( 1 );
		}
		List<String> written = extractTree( Paths.get( args[ 0 ] ), Paths.get( args[ 1 ] ) );
		System.out.println( "Extracted " + written.size() + " BoxLang class(es) to " + args[ 1 ] );
	}

	/**
	 * Extract every {@code .class} entry from a single AOT container file into {@code outputDir},
	 * preserving package structure based on each class's internal name.
	 *
	 * @param container The AOT container file (a compiled {@code .bx}/{@code .bxm})
	 * @param outputDir The root directory to write {@code .class} files into
	 *
	 * @return The list of fully-qualified (dotted) class names written
	 *
	 * @throws IOException If reading or writing fails
	 */
	public static List<String> extract( Path container, Path outputDir ) throws IOException {
		byte[]			bytes	= Files.readAllBytes( container );
		ByteBuffer		buffer	= ByteBuffer.wrap( bytes );
		List<String>	written	= new ArrayList<>();

		// Header: magic + original class name (we don't need the name itself).
		buffer.getInt();
		int		nameLength	= buffer.getInt();
		byte[]	nameBytes	= new byte[ nameLength ];
		buffer.get( nameBytes );

		// Body: length-prefixed .class entries.
		while ( buffer.hasRemaining() ) {
			int		length		= buffer.getInt();
			byte[]	classBytes	= new byte[ length ];
			buffer.get( classBytes );

			String	internalName	= new ClassReader( classBytes ).getClassName();		// e.g. boxgenerated/boxclass/Foo
			Path	target			= outputDir.resolve( internalName + ".class" );
			Files.createDirectories( target.getParent() );
			Files.write( target, classBytes );
			written.add( internalName.replace( '/', '.' ) );
		}

		return written;
	}

	/**
	 * Recursively extract every AOT container under {@code sourceDir} (files written by
	 * {@code BXCompiler --target}) into {@code outputDir} as {@code .class} files.
	 *
	 * @param sourceDir The directory of AOT container files
	 * @param outputDir The root directory to write {@code .class} files into
	 *
	 * @return The list of all fully-qualified class names written
	 *
	 * @throws IOException If traversal, reading, or writing fails
	 */
	public static List<String> extractTree( Path sourceDir, Path outputDir ) throws IOException {
		List<String> all = new ArrayList<>();
		try ( var stream = Files.walk( sourceDir ) ) {
			for ( Path file : ( Iterable<Path> ) stream.filter( Files::isRegularFile )::iterator ) {
				if ( isContainer( file ) ) {
					all.addAll( extract( file, outputDir ) );
				}
			}
		}
		return all;
	}

	/**
	 * @param file A candidate file
	 *
	 * @return {@code true} if the file begins with the AOT container magic ({@code 0xCAFEBABE})
	 *
	 * @throws IOException If the file cannot be read
	 *
	 * @implNote {@code BXCompiler} writes containers with a leading {@code 0xCAFEBABE} magic
	 *           (see {@code BXCompiler.compileFile}). A raw {@code .class} shares that magic, so
	 *           this is a sanity check, not a discriminator — the {@code BXCompiler} target
	 *           directory contains only containers.
	 */
	public static boolean isContainer( Path file ) throws IOException {
		byte[] header = readHeader( file, 4 );
		if ( header.length < 4 ) {
			return false;
		}
		return ByteBuffer.wrap( header ).getInt() == 0xCAFEBABE;
	}

	private static byte[] readHeader( Path file, int count ) throws IOException {
		try ( var in = Files.newInputStream( file ) ) {
			return in.readNBytes( count );
		}
	}

	/**
	 * @param nameBytes UTF-8 bytes of a name
	 *
	 * @return The decoded string (utility kept for symmetry with the container header)
	 */
	static String decode( byte[] nameBytes ) {
		return new String( nameBytes, StandardCharsets.UTF_8 );
	}
}
