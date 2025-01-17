/**
 * [BoxLang]
 * <p>
 * Copyright [2023] [Ortus Solutions, Corp]
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.compiler;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.util.JSONUtil;
import ortus.boxlang.runtime.util.RegexBuilder;

/**
 * Contains some utilities for working with non-class files in the class generation dir
 */
public class DiskClassUtil {

	/**
	 * The location of the disk store
	 */
	private Path diskStore;

	/**
	 * Constructor
	 *
	 * @param diskStore disk store location path
	 */
	public DiskClassUtil( Path diskStore ) {
		this.diskStore = diskStore;
	}

	/**
	 * Check if a JSON exists on disk
	 *
	 * @param name class name
	 *
	 * @return true if JSON exists on disk
	 */
	public boolean hasLineNumbers( String classPoolName, String name ) {
		return generateDiskpath( classPoolName, name, "json" ).toFile().exists();
	}

	/**
	 * Generate a disk path for a class file
	 *
	 * @param classPoolName class pool name
	 * @param name          class name
	 * @param extension     file extension
	 *
	 * @return The path to the file
	 */
	private Path generateDiskpath( String classPoolName, String name, String extension ) {
		return Paths.get(
		    diskStore.toString(),
		    RegexBuilder.of( classPoolName, RegexBuilder.NON_ALPHANUMERIC ).replaceAllAndGet( "_" ),
		    new StringBuilder( name.replace( ".", File.separator ) ).append( "." ).append( extension ).toString()
		);
	}

	/**
	 * Write line numbers to disk
	 *
	 * @param fqn            The fully qualified name of the class
	 * @param lineNumberJSON The JSON representation of the line numbers
	 */
	public void writeLineNumbers( String classPoolName, String fqn, String lineNumberJSON ) {
		if ( lineNumberJSON == null ) {
			return;
		}

		writeBytes( classPoolName, fqn, "json", lineNumberJSON.getBytes() );
	}

	/**
	 * Read line numbers.
	 *
	 * @returns array of maps. Null if not found.
	 */
	public SourceMap readLineNumbers( String classPoolName, String fqn ) {
		if ( !hasLineNumbers( classPoolName, fqn ) ) {
			return null;
		}
		Path diskPath = generateDiskpath( classPoolName, fqn, "json" );
		try {
			String json = new String( Files.readAllBytes( diskPath ) );
			return JSONUtil.fromJSON( SourceMap.class, json );
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Unable to read line number JSON file from disk", e );
		}
	}

	/**
	 * This is really just for debugging purposes. It's not used in production.
	 *
	 * @param fqn        The fully qualified name of the class
	 * @param javaSource The Java source code
	 */
	public void writeJavaSource( String classPoolName, String fqn, String javaSource ) {
		writeBytes( classPoolName, fqn, "java", javaSource.getBytes() );
	}

	/**
	 * Write a file to the directory configured for BoxLang
	 *
	 * @param fqn       The fully qualified name of the class
	 * @param extension The extension of the file
	 * @param bytes     The bytes to write
	 */
	public void writeBytes( String classPoolName, String fqn, String extension, byte[] bytes ) {
		writeBytes( classPoolName, fqn, extension, bytes, 0L );
	}

	/**
	 * Write a file to the directory configured for BoxLang
	 *
	 * @param fqn       The fully qualified name of the class
	 * @param extension The extension of the file
	 * @param bytes     The bytes to write
	 */
	public void writeBytes( String classPoolName, String fqn, String extension, byte[] bytes, long lastModifiedDate ) {
		Path diskPath = generateDiskpath( classPoolName, fqn, extension );
		diskPath.toFile().getParentFile().mkdirs();
		try {
			Files.write( diskPath, bytes );
			if ( lastModifiedDate > 0 ) {
				diskPath.toFile().setLastModified( lastModifiedDate );
			}
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Unable to write Java Sourece file to disk", e );
		}
	}

	/**
	 * Read the bytes from the class file and all inner classes from disk and return them
	 *
	 * @param fqn The fully qualified name of the class
	 *
	 * @return A list of byte arrays, one for each class file
	 */
	public List<byte[]> readClassBytes( String classPoolName, String fqn ) {
		List<byte[]>	bytes		= new ArrayList<>();
		Path			diskPath	= generateDiskpath( classPoolName, fqn, "class" );
		bytes.add( fqn.getBytes() );
		try {
			// Read the main class file
			bytes.add( Files.readAllBytes( diskPath ) );

			// Read the inner class files
			File	directory		= diskPath.getParent().toFile();  // The directory where the class files are stored
			String	outerClassName	= diskPath.getFileName().toString().replace( ".class", "" );  // The name of the outer class

			// List the files in the directory and filter them based on the naming convention of the inner classes
			File[]	innerClassFiles	= directory.listFiles( file -> file.getName().startsWith( outerClassName + "$" ) && file.getName().endsWith( ".class" ) );

			if ( innerClassFiles != null ) {
				for ( File innerClassFile : innerClassFiles ) {
					bytes.add( Files.readAllBytes( innerClassFile.toPath() ) );
				}
			}
		} catch ( IOException e ) {
			throw new BoxRuntimeException( "Unable to read class file from disk", e );
		}

		return bytes;
	}

	/**
	 * Checkf if the file is a Java bytecode file or source file
	 *
	 * @param sourceFile The file to check
	 *
	 * @return true if the file is a Java bytecode file
	 */
	public boolean isJavaBytecode( File sourceFile ) {
		try ( FileInputStream fis = new FileInputStream( sourceFile );
		    DataInputStream dis = new DataInputStream( fis ) ) {
			// File may be empty! At least 4 bytes are needed to read an int
			if ( dis.available() < 4 ) {
				return false;
			}
			// Are we the Java Magic number?
			return dis.readInt() == 0xCAFEBABE;
		} catch ( IOException e ) {
			throw new RuntimeException( "Failed to read file", e );
		}
	}

}
