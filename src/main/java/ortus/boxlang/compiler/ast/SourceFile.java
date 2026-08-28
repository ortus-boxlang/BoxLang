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
package ortus.boxlang.compiler.ast;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Represent a File as source
 */
public class SourceFile extends Source {

	private final File					file;
	private transient volatile String	cachedCode;

	/**
	 * Create a source for a given file
	 *
	 * @param file source File
	 */
	public SourceFile( File file ) {
		this.file = file;
	}

	/**
	 * Returns the File associate to the source
	 *
	 * @return a File instance
	 */
	public File getFile() {
		return file;
	}

	/**
	 * Returns the File associate to the source as a real path, resolving any symbolic links and relative paths.
	 *
	 * @return a Path instance
	 */
	public Path getFileAsRealPath() {
		Path thePath = file.toPath();
		try {
			thePath = thePath.toRealPath();
		} catch ( IOException e ) {
			// If the file no longer exists or can't be accessed, then ignore.
		}
		return thePath;
	}

	/**
	 * Essentially, only used for error messages
	 * 
	 * @return teh source code in the given file
	 */
	public String getCode() {
		String code = this.cachedCode;
		if ( code == null ) {
			synchronized ( this ) {
				code = this.cachedCode;
				if ( code == null ) {
					try {
						code = Files.readString( this.file.toPath(), StandardCharsets.UTF_8 );
					} catch ( IOException e ) {
						e.printStackTrace();
						code = "";
					}
					this.cachedCode = code;
				}
			}
		}
		return code;
	}

	/**
	 * String representation of a file source
	 *
	 * @return the absolute path of the File
	 */
	@Override
	public String toString() {
		return this.file != null ? file.getAbsolutePath() : "";
	}

	public Stream<String> getCodeAsStream() {
		try {
			return Files.lines( file.toPath(), StandardCharsets.UTF_8 );
		} catch ( IOException e ) {
			e.printStackTrace();
		}
		return Stream.empty();
	}

}
